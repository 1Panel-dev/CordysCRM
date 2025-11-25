<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="props.fieldConfig.showLabel && !props.isSubTableRender"
    :path="props.path"
    :rule="props.fieldConfig.rules"
    :required="props.fieldConfig.rules.some((rule) => rule.key === 'required')"
  >
    <div
      v-if="props.fieldConfig.description && !props.isSubTableRender"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-divider v-if="props.isSubTableField && !props.isSubTableRender" class="!my-0" />
    <CrmDataSource
      v-model:value="value"
      :rows="props.fieldConfig.initialOptions"
      :multiple="fieldConfig.type === FieldTypeEnum.DATA_SOURCE_MULTIPLE"
      :data-source-type="props.fieldConfig.dataSourceType || FieldDataSourceTypeEnum.CUSTOMER"
      :disabled="props.fieldConfig.editable === false"
      :filter-params="getParams()"
      @change="($event) => emit('change', $event)"
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NDivider, NFormItem } from 'naive-ui';

  import { OperatorEnum } from '@lib/shared/enums/commonEnum';
  import { FieldDataSourceTypeEnum, FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';

  import { FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmDataSource from '@/components/business/crm-data-source-select/index.vue';

  import { multipleValueTypeList } from '../../config';
  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
    needInitDetail?: boolean; // 判断是否编辑情况
    formDetail?: Record<string, any>;
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
  }>();
  const emit = defineEmits<{
    (e: 'change', value: (string | number)[]): void;
  }>();

  const value = defineModel<(string | number)[]>('value', {
    default: [],
  });

  function getParams(): FilterResult {
    const conditions = props.fieldConfig.combineSearch?.conditions
      .map((item) => ({
        value: item.rightFieldCustom ? item.rightFieldCustomValue : props.formDetail?.[item.rightFieldId || ''],
        operator: item.operator,
        name: item.leftFieldId ?? '',
        multipleValue: multipleValueTypeList.includes(item.leftFieldType),
      }))
      .filter(
        (e) => e.operator === OperatorEnum.EMPTY || (e.value !== undefined && e.value !== null && e.value !== '')
      );

    return {
      searchMode: props.fieldConfig.combineSearch?.searchMode,
      conditions,
    };
  }

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      if (!props.needInitDetail) {
        value.value = val || value.value || [];
        emit('change', value.value);
      }
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped></style>
