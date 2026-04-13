<template>
  <div class="flex w-full rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]">
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
    <div class="min-w-0 flex-1">
      <n-form ref="formRef" :model="formModel">
        <div
          v-for="(item, listIndex) in formModel.conditions"
          :key="item.leftFieldId || `filter_item_${listIndex}`"
          class="flex items-start gap-[8px]"
        >
          <n-form-item
            :path="`conditions[${listIndex}].leftFieldId`"
            :rule="[{ required: true, message: t('common.value.nameNotNull') }]"
            class="leftFieldId-col block flex-1 overflow-hidden"
          >
            <n-select
              v-model:value="item.leftFieldId"
              filterable
              :placeholder="props.dataIndexPlaceholder"
              :options="transformFieldsToOptions(props.leftFields)"
              :fallback-option="() => fallbackOption(item.leftFieldId)"
              @update-value="(val, option) => leftFieldChange((option as any), listIndex)"
            />
          </n-form-item>
          <n-form-item :path="`conditions[${listIndex}].operator`" class="block w-[105px]">
            <n-select
              v-model:value="item.operator"
              :options="getOperatorOptions(item.leftFieldId)"
              :disabled="!item.leftFieldId"
              :fallback-option="() => fallbackOption(item.leftFieldId)"
              @update:value="changeMatchTypeDefaultValue(item)"
            />
          </n-form-item>
          <n-form-item :path="`conditions[${listIndex}].matchType`" class="block w-[105px]">
            <n-select
              v-model:value="item.matchType"
              :options="[
                {
                  label: t('crmFormDesign.dataSourceFilterMatchingFields'),
                  value: 'MATCH_FIELD',
                },
                {
                  label: t('crmFormDesign.dataSourceFilterMatchingValue'),
                  value: 'MATCH_VALUE',
                },
              ]"
              :disabled="!item.leftFieldId"
              :fallback-option="() => fallbackOption(item.leftFieldId)"
              @update-value="(val) => matchTypeChange(val as DataSourceMatchType, listIndex)"
            />
          </n-form-item>
          <n-form-item
            v-if="isMatchField(item.matchType)"
            :path="`conditions[${listIndex}].rightFieldId`"
            class="block flex-[1.5] overflow-hidden"
            :rule="isValueDisabled(item) ? [] : [{ required: true, message: t('common.value.nameNotNull') }]"
          >
            <n-select
              v-model:value="item.rightFieldId"
              :options="transformFieldsToOptions(props.rightFields, item.leftFieldType)"
              :placeholder="t('crmFormDesign.dataSourceFilterValuePlaceholder')"
              :fallback-option="() => fallbackOption(item.leftFieldId)"
              :disabled="isValueDisabled(item)"
              @update-value="(val, option) => rightFieldChange((option as any).fieldType, listIndex)"
            />
          </n-form-item>
          <n-form-item
            v-else-if="isMatchValue(item.matchType)"
            :path="`conditions[${listIndex}].rightFieldCustomValue`"
            class="block flex-[1.5] overflow-hidden"
            :rule="isValueDisabled(item) ? [] : [{ required: true, message: t('common.value.nameNotNull') }]"
          >
            <CrmTimeRangePicker
              v-if="
                [FieldTypeEnum.TIME_RANGE_PICKER,FieldTypeEnum.DATE_TIME].includes(item.leftFieldType)  &&
                [OperatorEnum.DYNAMICS, OperatorEnum.FIXED].includes(item.operator as OperatorEnum)
              "
              v-model:value="item.rightFieldCustomValue"
              :time-range-type="item.operator"
              :disabled="isValueDisabled(item)"
              @update:value="valueChange"
            />
            <n-date-picker
              v-else-if=" ([FieldTypeEnum.TIME_RANGE_PICKER,FieldTypeEnum.DATE_TIME].includes(item.leftFieldType) &&
                            ![OperatorEnum.DYNAMICS, OperatorEnum.FIXED].includes(item.operator as OperatorEnum))"
              v-model:value="item.rightFieldCustomValue"
              :type="item.operator === OperatorEnum.BETWEEN ? 'datetimerange' : 'datetime'"
              clearable
              :disabled="isValueDisabled(item)"
              class="w-full"
              :default-time="item.operator === OperatorEnum.BETWEEN ? [undefined, '23:59:59'] : undefined"
              @update:value="valueChange"
            />
            <CrmInputNumber
              v-else-if="
                [FieldTypeEnum.INPUT_NUMBER, FieldTypeEnum.FORMULA].includes(item.leftFieldType) ||
                (item.leftFieldType === FieldTypeEnum.INPUT_MULTIPLE &&
                  [OperatorEnum.COUNT_LT, OperatorEnum.COUNT_GT].includes(item.operator as OperatorEnum))
              "
              v-model:value="item.rightFieldCustomValue"
              clearable
              :disabled="isValueDisabled(item)"
              :placeholder="t('common.pleaseInput')"
              class="w-full"
              @update:value="valueChange"
            />
            <CrmTagInput
              v-else-if="
                item.leftFieldType === FieldTypeEnum.INPUT_MULTIPLE &&
                ![OperatorEnum.COUNT_LT, OperatorEnum.COUNT_GT].includes(item.operator as OperatorEnum)
              "
              v-model:value="item.rightFieldCustomValue"
              clearable
              :disabled="isValueDisabled(item)"
              class="w-full"
              @update:value="valueChange"
            />
            <CrmDataSource
              v-else-if="[FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(item.leftFieldType)"
              v-model:value="item.rightFieldCustomValue"
              v-model:rows="item.selectedRows"
              :disabled="isValueDisabled(item)"
              v-bind="getSelectedProps(item.leftFieldId).dataSourceProps as DataSourceProps"
              @change="valueChange"
            />

            <n-select
              v-else-if="
                [
                  FieldTypeEnum.SELECT,
                  FieldTypeEnum.SELECT_MULTIPLE,
                  FieldTypeEnum.RADIO,
                  FieldTypeEnum.CHECKBOX,
                ].includes(item.leftFieldType)
              "
              v-model:value="item.rightFieldCustomValue"
              clearable
              max-tag-count="responsive"
              :disabled="isValueDisabled(item)"
              :placeholder="t('common.pleaseSelect')"
              v-bind="getSelectedProps(item.leftFieldId).selectProps"
              :multiple="
                item.leftFieldType === FieldTypeEnum.SELECT_MULTIPLE ||
                getSelectedProps(item.leftFieldId).selectProps?.multiple
              "
              @update:value="valueChange"
            />
            <CrmCitySelect
              v-else-if="item.leftFieldType === FieldTypeEnum.LOCATION"
              v-model:value="item.rightFieldCustomValue"
              :placeholder="t('common.pleaseInput')"
              :disabled="isValueDisabled(item)"
              clearable
              multiple
              check-strategy="parent"
              @update:value="valueChange"
            />
            <CrmIndustrySelect
              v-else-if="item.leftFieldType === FieldTypeEnum.INDUSTRY"
              v-model:value="item.rightFieldCustomValue"
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
                ].includes(item.leftFieldType)
              "
              v-model:value="item.rightFieldCustomValue"
              v-model:selected-list="item.selectedUserList"
              multiple
              :disabled="isValueDisabled(item)"
              :drawer-title="t('crmFormDesign.selectDataSource')"
              :api-type-key="MemberApiTypeEnum.FORM_FIELD"
              :member-types="
                [FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(item.leftFieldType)
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
                [FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(item.leftFieldType)
                  ? [DeptNodeTypeEnum.ORG, DeptNodeTypeEnum.ROLE]
                  : [DeptNodeTypeEnum.USER, DeptNodeTypeEnum.ROLE]
              "
            />
            <n-input
              v-else
              v-model:value="item.rightFieldCustomValue"
              allow-clear
              :disabled="isValueDisabled(item)"
              :maxlength="255"
              :placeholder="t('advanceFilter.inputPlaceholder')"
              @update:value="valueChange"
            />
          </n-form-item>
          <n-button ghost class="px-[7px]" @click="handleDeleteItem(listIndex)">
            <template #icon>
              <CrmIcon type="iconicon_minus_circle1" :size="16" />
            </template>
          </n-button>
        </div>
      </n-form>
      <n-button type="primary" text class="mt-[5px] w-[fit-content]" @click="handleAddItem">
        <template #icon>
          <n-icon><Add /></n-icon>
        </template>
        {{ t('advanceFilter.addCondition') }}
      </n-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import {
    FormInst,
    NButton,
    NDatePicker,
    NForm,
    NFormItem,
    NIcon,
    NInput,
    NSelect,
    SelectOption,
    SelectProps,
  } from 'naive-ui';
  import { Add } from '@vicons/ionicons5';

  import { OperatorEnum } from '@lib/shared/enums/commonEnum';
  import { FieldDataSourceTypeEnum, FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { scrollIntoView } from '@lib/shared/method/dom';

  import { operatorOptionsMap } from '@/components/pure/crm-advance-filter/index';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmIndustrySelect from '@/components/pure/crm-industry-select/index.vue';
  import CrmInputNumber from '@/components/pure/crm-input-number/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmTagInput from '@/components/pure/crm-tag-input/index.vue';
  import CrmCitySelect from '@/components/business/crm-city-select/index.vue';
  import CrmDataSource from '@/components/business/crm-data-source-select/index.vue';
  import { DataSourceProps } from '@/components/business/crm-data-source-select/type';
  import {
    DataSourceFilterCombine,
    DataSourceFilterItem,
    DataSourceMatchType,
    FormCreateField,
  } from '@/components/business/crm-form-create/types';
  import CrmTimeRangePicker from '@/components/business/crm-time-range-picker/index.vue';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  const { t } = useI18n();

  const props = defineProps<{
    selfId: string;
    leftFields: FormCreateField[];
    rightFields: FormCreateField[];
    dataIndexPlaceholder: string;
  }>();

  const formModel = defineModel<DataSourceFilterCombine>('formModel', {
    required: true,
  });
  // 过滤
  const formRef = ref<FormInst | null>(null);

  function changeAllOr() {
    formModel.value.searchMode = formModel.value.searchMode === 'AND' ? 'OR' : 'AND';
  }

  const isValueDisabled = (item: DataSourceFilterCombine['conditions'][number]) => {
    return ['EMPTY', 'NOT_EMPTY'].includes(item.operator as string);
  };

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

  function selectedValueIsArray(listItem: DataSourceFilterItem) {
    return (
      [
        FieldTypeEnum.SELECT_MULTIPLE,
        FieldTypeEnum.DEPARTMENT_MULTIPLE,
        FieldTypeEnum.MEMBER_MULTIPLE,
        FieldTypeEnum.DATA_SOURCE_MULTIPLE,
        FieldTypeEnum.DATA_SOURCE,
      ].includes(listItem.leftFieldType) ||
      (listItem.leftFieldType === FieldTypeEnum.INPUT_MULTIPLE &&
        ![OperatorEnum.COUNT_LT, OperatorEnum.COUNT_GT].includes(listItem.operator as OperatorEnum)) ||
      (listItem.leftFieldType === FieldTypeEnum.DATE_TIME && listItem.operator === OperatorEnum.BETWEEN)
    );
  }

  const isMatchField = (matchType?: string) => ['MATCH_FIELD'].includes(matchType || '');

  const isMatchValue = (matchType?: string) => ['MATCH_VALUE'].includes(matchType || '');

  function changeMatchTypeDefaultValue(item: DataSourceFilterItem) {
    if (!isMatchValue(item.matchType)) {
      return;
    }

    if (item.leftFieldType === FieldTypeEnum.DATE_TIME) {
      item.rightFieldCustomValue = null;
    } else {
      item.rightFieldCustomValue = selectedValueIsArray(item) ? [] : null;
    }

    if (item.selectedRows?.length) {
      item.selectedRows = [];
    }

    if (item.selectedUserList?.length) {
      item.selectedUserList = [];
    }

    formRef.value?.restoreValidation();
  }

  // 改变第一列值
  const leftFieldChange = (item: SelectOption, index: number) => {
    const leftFieldType = item.fieldType as FieldTypeEnum;
    // 显式类型注解，避免类型过深
    const currentFormList = formModel.value.conditions;
    const options = operatorOptionsMap[leftFieldType] || [];
    const optionsValueList = options.map((optionItem: { value: string; label: string }) => optionItem.value);
    currentFormList[index].operator = getDefaultOperator(optionsValueList);
    currentFormList[index].leftFieldType = leftFieldType;
    currentFormList[index].rightFieldId = undefined;
    currentFormList[index].rightFieldType = FieldTypeEnum.INPUT;
    changeMatchTypeDefaultValue(currentFormList[index]);
  };

  function rightFieldChange(rightFieldType: FieldTypeEnum, index: number) {
    const currentFormList = formModel.value.conditions;
    currentFormList[index].rightFieldType = rightFieldType;
  }

  function matchTypeChange(matchType: DataSourceMatchType, index: number) {
    const currentFormList = formModel.value.conditions;
    const currentItem = currentFormList[index];
    currentItem.matchType = matchType;
    currentItem.rightFieldId = undefined;
    currentItem.rightFieldType = FieldTypeEnum.INPUT;
    currentItem.rightFieldCustom = isMatchValue(matchType);

    if (isMatchField(matchType)) {
      currentItem.rightFieldCustomValue = '';
      if (currentItem.selectedRows?.length) {
        currentItem.selectedRows = [];
      }
      if (currentItem.selectedUserList?.length) {
        currentItem.selectedUserList = [];
      }
      return;
    }

    changeMatchTypeDefaultValue(currentItem);
  }

  function transformFieldsToOptions(fields: FormCreateField[], leftFieldType?: FieldTypeEnum): SelectOption[] {
    return fields
      .filter((e) => {
        const condition =
          ![FieldTypeEnum.DIVIDER, FieldTypeEnum.PICTURE, FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(
            e.type
          ) &&
          props.selfId !== e.id &&
          !e.resourceFieldId;
        if (leftFieldType) {
          return e.type === leftFieldType && condition;
        }
        return condition;
      })
      .map((field) => ({
        ...field,
        label: field.name,
        value: leftFieldType ? field.id : field.businessKey || field.id, // 左侧字段需要业务Key，右侧字段需要id
        fieldType: field.type,
        dataSourceType: field.dataSourceType,
      }));
  }

  function fallbackOption(val?: string | number) {
    return {
      label: t('common.optionNotExist'),
      value: val,
    };
  }

  // 获取操作符号
  function getOperatorOptions(leftFieldId: string | undefined) {
    const leftField = props.leftFields.find((field) => [field.id, field.businessKey].includes(leftFieldId || ''));
    if (!leftField) return [];
    return operatorOptionsMap[leftField.type].map((e) => {
      return {
        ...e,
        label: t(e.label),
      };
    });
  }

  const selectedPropsMap = computed(() => {
    const map = new Map<string, Record<string, any>>();

    transformFieldsToOptions(props.leftFields).forEach((field) => {
      const currentSelectedType = field.fieldType as FieldTypeEnum;
      const currentFieldProps: Record<string, any> = {};
      if (
        [FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE, FieldTypeEnum.RADIO, FieldTypeEnum.CHECKBOX].includes(
          currentSelectedType
        )
      ) {
        currentFieldProps.selectProps = {
          options: field.options,
          multiple: true,
        } as Partial<SelectProps>;
      }

      if ([FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(currentSelectedType)) {
        currentFieldProps.dataSourceProps = {
          dataSourceType: field.dataSourceType as FieldDataSourceTypeEnum,
          maxTagCount: 'responsive',
        } as Partial<DataSourceProps>;
      }
      map.set(field.value as string, currentFieldProps);
    });

    return map;
  });

  function getSelectedProps(leftFieldId: string | undefined): {
    selectProps?: Partial<SelectProps>;
    dataSourceProps?: Partial<DataSourceProps>;
  } {
    if (!leftFieldId) {
      return {};
    }
    return selectedPropsMap.value.get(leftFieldId) || {};
  }

  // 删除筛选项
  function handleDeleteItem(index: number) {
    formModel.value.conditions.splice(index, 1);
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
        leftFieldId: undefined,
        leftFieldType: FieldTypeEnum.INPUT,
        operator: undefined,
        rightFieldId: undefined,
        rightFieldCustom: false,
        rightFieldCustomValue: '',
        rightFieldType: FieldTypeEnum.INPUT, // 默认右侧字段类型为输入框
        matchType: 'MATCH_FIELD' as DataSourceMatchType,
      };
      formModel.value.conditions.push(item);
    });
  }

  function valueChange() {
    formRef.value?.validate();
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
