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
              @update-value="(val, option) => leftFieldChange((option as any).fieldType, listIndex)"
            />
          </n-form-item>
          <n-form-item :path="`conditions[${listIndex}].operator`" class="block w-[105px]">
            <n-select
              v-model:value="item.operator"
              :options="getOperatorOptions(item.leftFieldId)"
              :disabled="!item.leftFieldId"
              :fallback-option="() => fallbackOption(item.leftFieldId)"
            />
          </n-form-item>
          <n-form-item
            :path="`conditions[${listIndex}].rightFieldId`"
            class="block flex-[1.5] overflow-hidden"
            :rule="['EMPTY', 'NOT_EMPTY'].includes(item.operator as string) ? [] : [{ required: true, message: t('common.value.nameNotNull') }]"
          >
            <n-select
              v-model:value="item.rightFieldId"
              :options="transformFieldsToOptions(props.rightFields, item.leftFieldType)"
              :placeholder="t('crmFormDesign.dataSourceFilterValuePlaceholder')"
              :fallback-option="() => fallbackOption(item.leftFieldId)"
              :disabled="['EMPTY', 'NOT_EMPTY'].includes(item.operator as string)"
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
  import { FormInst, NButton, NForm, NFormItem, NIcon, NSelect, SelectOption } from 'naive-ui';
  import { Add } from '@vicons/ionicons5';

  import { OperatorEnum } from '@lib/shared/enums/commonEnum';
  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { scrollIntoView } from '@lib/shared/method/dom';

  import { operatorOptionsMap } from '@/components/pure/crm-advance-filter/index';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import { multipleValueTypeList } from '@/components/business/crm-form-create/config';
  import { DataSourceFilterCombine, FormCreateField } from '@/components/business/crm-form-create/types';

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

  // 改变第一列值
  const leftFieldChange = (leftFieldType: FieldTypeEnum, index: number) => {
    // 显式类型注解，避免类型过深
    const currentFormList = formModel.value.conditions;
    const options = operatorOptionsMap[leftFieldType] || [];
    const optionsValueList = options.map((optionItem: { value: string; label: string }) => optionItem.value);
    currentFormList[index].operator = getDefaultOperator(optionsValueList);
    currentFormList[index].leftFieldType = leftFieldType;
  };

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
          if (multipleValueTypeList.includes(leftFieldType)) {
            return multipleValueTypeList.includes(e.type) && condition;
          }
          if (!multipleValueTypeList.includes(leftFieldType)) {
            return !multipleValueTypeList.includes(e.type) && condition;
          }
        }
        return condition;
      })
      .map((field) => ({
        label: field.name,
        value: leftFieldType ? field.id : field.businessKey || field.id, // 左侧字段需要业务Key，右侧字段需要id
        fieldType: field.type,
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
      };
      formModel.value.conditions.push(item);
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
