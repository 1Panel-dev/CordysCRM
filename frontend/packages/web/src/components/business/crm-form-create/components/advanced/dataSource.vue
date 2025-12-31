<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="(props.fieldConfig.showLabel && !props.isSubTableRender) || props.isSubTableField"
    :path="props.path"
    :rule="props.fieldConfig.rules"
    :required="props.fieldConfig.rules.some((rule) => rule.key === 'required')"
    :label-placement="props.isSubTableField || props.isSubTableRender ? 'top' : props.formConfig?.labelPos"
  >
    <template #label>
      <div v-if="props.fieldConfig.showLabel" class="flex h-[22px] items-center gap-[4px] whitespace-nowrap">
        <div class="one-line-text">{{ props.fieldConfig.name }}</div>
        <CrmIcon v-if="props.fieldConfig.resourceFieldId" type="iconicon_correlation" />
      </div>
      <div v-else-if="props.isSubTableField || props.isSubTableRender" class="h-[22px]"></div>
    </template>
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
      :disabled="props.fieldConfig.editable === false || !!props.fieldConfig.resourceFieldId"
      :filter-params="getParams()"
      :fieldConfig="props.fieldConfig"
      :disabled-selection="props.disabledSelection"
      @change="($event, source) => emit('change', $event, source)"
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NDivider, NFormItem } from 'naive-ui';

  import { OperatorEnum } from '@lib/shared/enums/commonEnum';
  import { FieldDataSourceTypeEnum, FieldTypeEnum, type FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { transformData } from '@lib/shared/method/formCreate';
  import type { FormConfig } from '@lib/shared/models/system/module';

  import { FilterResult } from '@/components/pure/crm-advance-filter/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import { formKeyMap, sourceApi } from '@/components/business/crm-data-source-select/config';
  import CrmDataSource from '@/components/business/crm-data-source-select/index.vue';

  import useFormCreateApi from '@/hooks/useFormCreateApi';

  import { multipleValueTypeList } from '../../config';
  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formConfig?: FormConfig;
    path: string;
    needInitDetail?: boolean; // 判断是否编辑情况
    formDetail?: Record<string, any>;
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
    disabledSelection?: (row: Record<string, any>) => boolean;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: (string | number)[], source: Record<string, any>[]): void;
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

  const { fieldList, initFormConfig } = useFormCreateApi({
    formKey: computed(
      () => formKeyMap[props.fieldConfig.dataSourceType || FieldDataSourceTypeEnum.CUSTOMER] as FormDesignKeyEnum
    ),
  });

  const { propsRes, loadList, setLoadListParams } = useTable(
    sourceApi[props.fieldConfig.dataSourceType || FieldDataSourceTypeEnum.CUSTOMER],
    {
      columns: [],
      showSetting: false,
    },
    (item, originalData) => {
      return transformData({
        item,
        originalData,
        fields: fieldList.value,
        needParseSubTable: true,
      });
    }
  );

  onMounted(async () => {
    if (!props.needInitDetail && props.fieldConfig.showFields?.length && props.fieldConfig.defaultValue?.length) {
      await initFormConfig();
      setLoadListParams({
        keyword: props.fieldConfig.initialOptions?.[0]?.name || '',
      });
      await loadList();
      const newRows = propsRes.value.data.filter((item) => props.fieldConfig.defaultValue?.includes(item.id));
      value.value = newRows.map((e) => e.id) as (string | number)[];
      emit('change', value.value, newRows);
    }
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      if (!props.needInitDetail) {
        value.value = val || value.value || [];
        emit('change', value.value, props.fieldConfig.initialOptions || []);
      }
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped></style>
