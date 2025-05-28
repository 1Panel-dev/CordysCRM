<template>
  <div class="flex rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]">
    <div class="and-or">
      <CrmTag
        type="primary"
        theme="light"
        :color="{ color: 'var(--primary-6)' }"
        class="z-[1] w-[38px]"
        @click="changeAllOr"
      >
        {{ formModel.searchMode === 'AND' ? 'and' : 'or' }}
      </CrmTag>
    </div>
    <div class="flex-1">
      <n-form ref="formRef" :model="formModel">
        <div
          v-for="(item, listIndex) in formModel.list"
          :key="item.dataIndex || `filter_item_${listIndex}`"
          class="flex items-start gap-[8px]"
        >
          <n-form-item
            :path="`list[${listIndex}].dataIndex`"
            :rule="[{ required: true, message: t('common.value.nameNotNull') }]"
            class="dataIndex-col block flex-1 overflow-hidden"
          >
            <n-select
              v-model:value="item.dataIndex"
              value-field="dataIndex"
              filterable
              :placeholder="t('common.pleaseSelect')"
              :options="currentOptions(item.dataIndex as string)"
              @update:value="(val) => dataIndexChange(val, listIndex)"
            />
          </n-form-item>
          <n-form-item :path="`list[${listIndex}].operator`" class="block w-[105px]">
            <n-select
              v-model:value="item.operator"
              :disabled="!item.dataIndex"
              :options="getOperatorOptions(item.type, item.dataIndex as string)"
              @update:value="operatorChange(item, listIndex)"
            />
          </n-form-item>
          <n-form-item
            :path="`list[${listIndex}].value`"
            class="block flex-[1.5] overflow-hidden"
            :rule="getRules(item)"
          >
            <CrmTimeRangePicker
              v-if="item.type === FieldTypeEnum.TIME_RANGE_PICKER"
              v-model:value="item.value"
              :time-range-type="item.operator as (OperatorEnum.DYNAMICS | OperatorEnum.FIXED)"
              @update:value="valueChange"
            />
            <CrmInputNumber
              v-else-if="item.type === FieldTypeEnum.INPUT_NUMBER"
              v-model:value="item.value"
              clearable
              :disabled="isValueDisabled(item)"
              :placeholder="item.numberProps?.placeholder ?? t('common.pleaseInput')"
              v-bind="{
                min: item.numberProps?.min,
              }"
              class="w-full"
              @update:value="valueChange"
            />
            <CrmTagInput
              v-else-if="item.type === FieldTypeEnum.INPUT_MULTIPLE"
              v-model:value="item.value"
              clearable
              :disabled="isValueDisabled(item)"
              class="w-full"
              @update:value="valueChange"
            />

            <CrmDataSource
              v-else-if="[FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(item.type)"
              v-model:value="item.value"
              v-model:rows="item.selectedRows"
              v-bind="{
                ...item.dataSourceProps,
                dataSourceType:item?.dataSourceProps?.dataSourceType as FieldDataSourceTypeEnum,
              }"
              :disabled="isValueDisabled(item)"
              @change="valueChange"
            />
            <n-select
              v-else-if="
                [
                  FieldTypeEnum.SELECT,
                  FieldTypeEnum.SELECT_MULTIPLE,
                  FieldTypeEnum.RADIO,
                  FieldTypeEnum.CHECKBOX,
                ].includes(item.type)
              "
              v-model:value="item.value"
              clearable
              max-tag-count="responsive"
              :disabled="isValueDisabled(item)"
              :placeholder="t('common.pleaseSelect')"
              v-bind="item.selectProps"
              :multiple="item.type === FieldTypeEnum.SELECT_MULTIPLE || item.selectProps?.multiple"
              @update:value="valueChange"
            />
            <CrmCitySelect
              v-else-if="item.type === FieldTypeEnum.LOCATION"
              v-model:value="item.value"
              :placeholder="t('common.pleaseInput')"
              :disabled="isValueDisabled(item)"
              clearable
              multiple
              check-strategy="parent"
              @update:value="valueChange"
            />
            <CrmUserTagSelector
              v-else-if="
                [
                  FieldTypeEnum.DEPARTMENT,
                  FieldTypeEnum.DEPARTMENT_MULTIPLE,
                  FieldTypeEnum.MEMBER,
                  FieldTypeEnum.MEMBER_MULTIPLE,
                ].includes(item.type)
              "
              v-model:value="item.value"
              v-model:selected-list="item.selectedUserList"
              multiple
              :drawer-title="t('crmFormDesign.selectDataSource')"
              :api-type-key="MemberApiTypeEnum.FORM_FIELD"
              :member-types="
                [FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(item.type)
                  ? [
                      {
                        label: t('menu.settings.org'),
                        value: MemberSelectTypeEnum.ORG,
                      },
                    ]
                  : [
                      {
                        label: t('menu.settings.org'),
                        value: MemberSelectTypeEnum.ONLY_ORG,
                      },
                    ]
              "
              :disabled-node-types="
                [FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(item.type)
                  ? [DeptNodeTypeEnum.ORG, DeptNodeTypeEnum.ROLE]
                  : [DeptNodeTypeEnum.USER, DeptNodeTypeEnum.ROLE]
              "
            />
            <n-tree-select
              v-else-if="item.type === FieldTypeEnum.TREE_SELECT"
              v-model:value="item.value"
              filterable
              clearable
              :disabled="isValueDisabled(item)"
              max-tag-count="responsive"
              :placeholder="t('common.pleaseSelect')"
              v-bind="item.treeSelectProps"
              @update:value="valueChange"
            />

            <n-date-picker
              v-else-if="item.type === FieldTypeEnum.DATE_TIME"
              v-model:value="item.value"
              :type="item.operator === OperatorEnum.BETWEEN ? 'datetimerange' : 'datetime'"
              clearable
              :disabled="isValueDisabled(item)"
              class="w-full"
              :default-time="item.operator === OperatorEnum.BETWEEN ? [undefined, '23:59:59'] : undefined"
              @update:value="valueChange"
            />

            <CrmUserSelect
              v-else-if="item.type === FieldTypeEnum.USER_SELECT"
              v-model:value="item.value"
              value-field="id"
              label-field="name"
              mode="remote"
              :disabled="isValueDisabled(item)"
              multiple
              :fetch-api="getUserOptions"
              max-tag-count="responsive"
              @update:value="valueChange"
            />

            <n-input
              v-else
              v-model:value="item.value"
              allow-clear
              :disabled="isValueDisabled(item)"
              :maxlength="255"
              :placeholder="t('advanceFilter.inputPlaceholder')"
              v-bind="item.inputProps"
              @update:value="valueChange"
            />
          </n-form-item>
          <n-form-item
            v-if="item.showScope"
            :path="`list[${listIndex}].scope`"
            class="block w-[150px] flex-initial overflow-hidden"
          >
            <n-select
              v-model:value="item.scope"
              :disabled="item.scopeProps?.disabled"
              :options="scopeOptions"
              multiple
            />
          </n-form-item>
          <n-button
            :disabled="props.keepOneLine && formModel.list.length === 1"
            ghost
            class="px-[7px]"
            @click="handleDeleteItem(listIndex)"
          >
            <template #icon>
              <CrmIcon type="iconicon_minus_circle1" :size="16" />
            </template>
          </n-button>
        </div>
      </n-form>
      <n-button
        type="primary"
        :disabled="formModel.list?.length === [...props.configList, ...(props.customList ?? [])].length"
        text
        class="mt-[5px] w-[fit-content]"
        @click="handleAddItem"
      >
        <template #icon>
          <n-icon><Add /></n-icon>
        </template>
        {{ t('advanceFilter.addCondition') }}
      </n-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { FormInst, NButton, NDatePicker, NForm, NFormItem, NIcon, NInput, NSelect, NTreeSelect } from 'naive-ui';
  import { Add } from '@vicons/ionicons5';

  import { OperatorEnum } from '@lib/shared/enums/commonEnum';
  import { FieldDataSourceTypeEnum, FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { scrollIntoView } from '@lib/shared/method/dom';

  import CrmInputNumber from '@/components/pure/crm-input-number/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmTagInput from '@/components/pure/crm-tag-input/index.vue';
  import CrmCitySelect from '@/components/business/crm-city-select/index.vue';
  import CrmDataSource from '@/components/business/crm-data-source-select/index.vue';
  import CrmTimeRangePicker from '@/components/business/crm-time-range-picker/index.vue';
  import CrmUserSelect from '@/components/business/crm-user-select/index.vue';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import { getUserOptions } from '@/api/modules';

  import { operatorOptionsMap, scopeOptions } from '../index';
  import type { FilterForm, FilterFormItem } from '../type';

  const { t } = useI18n();

  const props = defineProps<{
    configList: FilterFormItem[];
    customList?: FilterFormItem[];
    keepOneLine?: boolean; // 至少保留一行
  }>();

  const formModel = defineModel<FilterForm>('formModel', {
    required: true,
  });
  // 过滤
  const formRef = ref<FormInst | null>(null);

  function valueIsArray(listItem: FilterFormItem) {
    return (
      [
        FieldTypeEnum.SELECT_MULTIPLE,
        FieldTypeEnum.DEPARTMENT_MULTIPLE,
        FieldTypeEnum.MEMBER_MULTIPLE,
        FieldTypeEnum.DATA_SOURCE_MULTIPLE,
      ].includes(listItem.type) ||
      listItem.selectProps?.multiple ||
      listItem.cascaderProps?.multiple ||
      (listItem.type === FieldTypeEnum.INPUT_MULTIPLE &&
        ![OperatorEnum.COUNT_LT, OperatorEnum.COUNT_GT].includes(listItem.operator as OperatorEnum)) ||
      (listItem.type === FieldTypeEnum.DATE_TIME && listItem.operator === OperatorEnum.BETWEEN)
    );
  }

  function changeAllOr() {
    formModel.value.searchMode = formModel.value.searchMode === 'AND' ? 'OR' : 'AND';
  }

  function isValueDisabled(item: FilterFormItem) {
    return !item.dataIndex || ['EMPTY', 'NOT_EMPTY'].includes(item.operator as string);
  }

  // 第一列下拉数据
  const currentOptions = computed(() => {
    const allOptions = [...props.configList, ...(props.customList || [])];
    const formItems = formModel.value.list;

    return (currentDataIndex: string) => {
      const usedDataIndices = new Set(
        formItems.filter((item) => item.dataIndex !== currentDataIndex).map((item) => item.dataIndex)
      );

      return allOptions
        .filter(({ dataIndex }) => !usedDataIndices.has(dataIndex))
        .map((item) => ({ ...item, label: t(item.title as string) }));
    };
  });

  // 第二列默认：包含/属于/等于
  function getDefaultOperator(list: string[]) {
    if (list.includes(OperatorEnum.CONTAINS)) {
      return OperatorEnum.CONTAINS;
    }
    if (list.includes(OperatorEnum.DYNAMICS)) {
      return OperatorEnum.DYNAMICS;
    }
    if (list.includes(OperatorEnum.IN)) {
      return OperatorEnum.IN;
    }
    if (list.includes(OperatorEnum.EQUALS)) {
      return OperatorEnum.EQUALS;
    }
    return OperatorEnum.BETWEEN;
  }

  const getListItemByDataIndex = (dataIndex: string): FilterFormItem | undefined => {
    const mergedList: FilterFormItem[] = [...props.configList, ...(props.customList || [])];

    return mergedList.find((item) => item.dataIndex === dataIndex);
  };

  // 改变第一列值
  const dataIndexChange = (dataIndex: string, index: number) => {
    const listItem = getListItemByDataIndex(dataIndex);

    if (!listItem) return;

    const isArray = valueIsArray(listItem);
    const currentListItem: FilterFormItem = {
      ...listItem,
      value: isArray ? [] : undefined,
      ...(listItem.showScope ? { scope: listItem.scope } : undefined),
    };

    // 显式类型注解，避免类型过深
    const currentFormList = formModel.value.list as FilterFormItem[];

    currentFormList[index] = currentListItem;

    // 如果当前操作符没有值，自动赋默认
    const hasNoOperator = !currentFormList[index].operator?.length;

    if (hasNoOperator) {
      const options = listItem?.operatorOption?.length
        ? listItem.operatorOption
        : operatorOptionsMap[currentFormList[index].type] || [];

      const optionsValueList = options.map((optionItem: { value: string; label: string }) => optionItem.value);

      currentFormList[index].operator = getDefaultOperator(optionsValueList);
    }
  };

  // 获取操作符号
  function getOperatorOptions(type: FieldTypeEnum, dataIndex: string) {
    const listItem = getListItemByDataIndex(dataIndex);
    return (listItem?.operatorOption?.length ? listItem.operatorOption : operatorOptionsMap[type])?.map((e) => {
      return {
        ...e,
        label: t(e.label),
      };
    });
  }

  // 删除筛选项
  function handleDeleteItem(index: number) {
    formModel.value.list.splice(index, 1);
  }

  // 符号变化
  function operatorChange(item: FilterFormItem, index: number) {
    if (item.type === FieldTypeEnum.DATE_TIME) {
      formModel.value.list[index].value = null;
    } else {
      formModel.value.list[index].value = valueIsArray(item) ? [] : null;
    }
    if (isValueDisabled(item)) {
      formRef.value?.restoreValidation();
    }
  }

  function valueChange() {
    formRef.value?.validate();
  }

  // 获取校验规则
  function getRules(item: FilterFormItem) {
    if (isValueDisabled(item)) {
      return undefined;
    }
    return (
      item.rule ?? [
        {
          required: true,
          message: t('common.value.notNull'),
        },
      ]
    );
  }

  function validateForm(cb: (res?: Record<string, any>) => void) {
    formRef.value?.validate(async (errors) => {
      if (errors) {
        scrollIntoView(document.querySelector('.n-form-item-blank--error'), { block: 'center' });
        return;
      }
      if (typeof cb === 'function') {
        cb();
      }
    });
  }

  // 添加筛选项
  function handleAddItem() {
    validateForm(() => {
      const item = {
        key: '',
        type: FieldTypeEnum.INPUT,
        value: null,
      };
      formModel.value.list.push(item);
    });
  }

  defineExpose({
    formRef,
    validateForm,
  });
</script>

<style lang="less" scoped>
  .and-or {
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
    margin-right: 16px;
    height: auto;
    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 50%;
      width: 1px;
      height: 100%;
      background-color: var(--text-n8);
      transform: translateX(-50%);
    }
    :deep(.n-tag__content) {
      margin: 0 auto;
    }
  }
</style>
