<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :path="props.path"
    :rule="props.fieldConfig.rules"
    :required="props.fieldConfig.rules.some((rule) => rule.key === 'required')"
    :label-placement="props.isSubTableField || props.isSubTableRender ? 'top' : props.formConfig?.labelPos"
    :show-label="!props.isSubTableRender && !props.isDescriptionRender"
  >
    <template #label>
      <div v-if="props.fieldConfig.showLabel" class="flex h-[22px] items-center gap-[4px] whitespace-nowrap">
        <div class="one-line-text">{{ props.fieldConfig.name }}</div>
        <CrmIcon v-if="props.fieldConfig.resourceFieldId" type="iconicon_correlation" />
      </div>
      <div v-else class="h-[22px]"></div>
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
      :data-source-type="(props.fieldConfig.dataSourceType || FieldDataSourceTypeEnum.CUSTOMER) as DataSourceType"
      :disabled="props.fieldConfig.editable === false || !!props.fieldConfig.resourceFieldId"
      :filter-params="getParams()"
      :fieldConfig="props.fieldConfig"
      :disabled-selection="props.disabledSelection"
      :hide-child-tag="props.hideChildTag"
      :status="props.feedback ? 'error' : undefined"
      @delete="emit('delete', $event)"
      @change="($event, source, fields) => emit('change', $event, source, fields)"
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NDivider, NFormItem } from 'naive-ui';

  import { OperatorEnum } from '@lib/shared/enums/commonEnum';
  import { FieldDataSourceTypeEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { transformData } from '@lib/shared/method/formCreate';
  import type { FormConfig } from '@lib/shared/models/system/module';

  import { FilterResult } from '@/components/pure/crm-advance-filter/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import { formKeyMap, sourceApi } from '@/components/business/crm-data-source-select/config';
  import CrmDataSource from '@/components/business/crm-data-source-select/index.vue';
  import { getDataSourceFormKey, isCustomDataSourceType } from '@/components/business/crm-data-source-select/utils';

  import { getFieldCustomFormList } from '@/api/modules';
  import useFormCreateApi from '@/hooks/useFormCreateApi';

  import { multipleValueTypeList } from '../../config';
  import { DataSourceType, FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formConfig?: FormConfig;
    path: string;
    needInitDetail?: boolean; // 判断是否编辑情况
    formDetail?: Record<string, any>;
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
    isDescriptionRender?: boolean; // 是否是描述渲染
    feedback?: string;
    hideChildTag?: boolean;
    disabledSelection?: (row: Record<string, any>) => boolean;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: (string | number)[], source: Record<string, any>[], fields?: FormCreateField[]): void;
    (e: 'delete', id?: string | number): void;
  }>();

  const value = defineModel<(string | number)[]>('value', {
    default: [],
  });

  function normalizeSelectedIds(defaultValue?: string | number | (string | number)[]) {
    let defaultIds: (string | number)[] = [];
    if (Array.isArray(defaultValue)) {
      defaultIds = defaultValue;
    } else if (defaultValue !== undefined && defaultValue !== null && defaultValue !== '') {
      defaultIds = [defaultValue];
    }
    const currentValueIds = Array.isArray(value.value) ? value.value : [];
    return [...new Set([...defaultIds, ...currentValueIds])];
  }

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

  const dataSourceType = computed(() => props.fieldConfig.dataSourceType || FieldDataSourceTypeEnum.CUSTOMER);
  const isCustomForm = computed(() => isCustomDataSourceType(dataSourceType.value));
  const formKey = computed<FormDesignKeyEnum>(
    () => getDataSourceFormKey(dataSourceType.value, formKeyMap) as FormDesignKeyEnum
  );
  const isDatasourceFormConfig = computed(() => dataSourceType.value !== FieldDataSourceTypeEnum.BUSINESS_TITLE);

  const { fieldList, initFormConfig } = useFormCreateApi({
    formKey,
    customFormId: computed(() => (isCustomForm.value ? dataSourceType.value : undefined)),
    isDatasource: isDatasourceFormConfig.value,
  });

  const listApi = isCustomForm.value
    ? getFieldCustomFormList
    : sourceApi[dataSourceType.value as FieldDataSourceTypeEnum];

  const { propsRes, loadList, setLoadListParams, setAdvanceFilter } = useTable(
    listApi,
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

  watch(
    () => props.fieldConfig.initialOptions,
    async (val) => {
      if (!props.needInitDetail && !props.isSubTableField && !props.isSubTableRender && val?.length === 1) {
        if (fieldList.value.length === 0) {
          await initFormConfig();
        }
        setAdvanceFilter(getParams());
        setLoadListParams({
          keyword: val?.[0]?.name || '',
          customFormId: isCustomForm.value ? (dataSourceType.value as string | undefined) : undefined,
        });
        await loadList();
        const selectedIds = normalizeSelectedIds(props.fieldConfig.defaultValue);
        const newRows = propsRes.value.data.filter((item) => selectedIds.includes(item.id));
        const fallbackRows = val.filter((item) => selectedIds.includes(item.id));

        // 如果补全请求未查到目标项，仍需保留初始化值，避免出现先带入后被清空的问题
        const mergedRows = [...newRows, ...fallbackRows.filter((item) => !newRows.some((row) => row.id === item.id))];
        value.value = selectedIds;
        emit('change', value.value, mergedRows, fieldList.value);
      } else if (val?.some((e) => e.isFormLinkFilled)) {
        // 这里将表单联动填充的初始化选项 emit 出去，触发 change  让数据源显示字段回显
        emit('change', value.value, val, fieldList.value);
      }
    },
    {
      immediate: true,
    }
  );

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      if (!props.needInitDetail) {
        value.value = value.value || val || [];
        emit('change', value.value, props.fieldConfig.initialOptions || []);
      }
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped></style>
