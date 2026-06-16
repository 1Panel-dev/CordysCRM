<template>
  <CrmDrawer
    v-model:show="show"
    :width="1000"
    :min-width="600"
    :ok-text="t('common.confirm')"
    :ok-disabled="loading"
    :title="t('process.process.flow.setTriggerCondition')"
    :footer="!props.readonly"
    @confirm="handleConfirm"
  >
    <n-spin :show="loading">
      <div class="flex flex-col gap-[16px]">
        <n-form ref="formRef" :model="form" label-placement="top" require-mark-placement="right">
          <n-form-item
            :label="t('process.process.flow.conditionName')"
            path="name"
            :rule="[
              {
                required: true,
                message: t('common.notNull', { value: t('process.process.flow.conditionName') }),
                trigger: ['input', 'blur'],
              },
            ]"
          >
            <n-input
              v-model:value="form.name"
              :disabled="props.readonly"
              :maxlength="255"
              type="text"
              :placeholder="t('common.pleaseInput')"
            />
          </n-form-item>
        </n-form>

        <FilterContent
          ref="filterContentRef"
          v-model:form-model="form.conditionConfig"
          no-filter-option
          :readonly="props.readonly"
          :config-list="filterConfigList"
          :custom-list="customFieldsFilterConfig"
        >
          <template #header>
            <div class="mb-[16px] flex items-center justify-between">
              <div>{{ t('process.process.flow.conditionRule') }}</div>
              <n-select
                v-model:value="form.sort"
                class="w-[100px]"
                size="small"
                :disabled="props.readonly"
                :options="props.priorityOptions ?? []"
              />
            </div>
          </template>
        </FilterContent>
      </div>
    </n-spin>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref, watch } from 'vue';
  import { FormInst, NForm, NFormItem, NInput, NSelect, NSpin } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { OperatorEnum } from '@lib/shared/enums/commonEnum';
  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { OpportunityStageConfig } from '@lib/shared/models/opportunity';
  import type { ApprovalConditionBranch } from '@lib/shared/models/system/process';

  import { operatorOptionsMap } from '@/components/pure/crm-advance-filter';
  import FilterContent from '@/components/pure/crm-advance-filter/components/filterContent.vue';
  import {
    type ConditionsItem,
    type FilterForm,
    type FilterFormItem,
    filterOptionKeyMap,
  } from '@/components/pure/crm-advance-filter/type';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import { getFormConfigApiMap, multipleValueTypeList } from '@/components/business/crm-form-create/config';
  import type { FormCreateField } from '@/components/business/crm-form-create/types';

  import { getContractStatusConfig, getOrderStatusConfig } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import { quotationStatus } from '@/config/opportunity';
  import { processStatusOptions } from '@/config/process';
  import useFormCreateFilter from '@/hooks/useFormCreateAdvanceFilter';

  defineOptions({
    name: 'SetConditionDrawer',
  });

  const props = defineProps<{
    branch: ApprovalConditionBranch | null;
    formType: string;
    optionMap?: Record<string, any[]>;
    priorityOptions?: Array<{ label: string; value: number }>;
    readonly?: boolean;
  }>();

  const emit = defineEmits<{
    (
      e: 'confirm',
      payload: {
        name: string;
        sort: number;
        conditionConfig: FilterForm;
      }
    ): void;
  }>();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  const { t } = useI18n();
  const { getFilterListConfig } = useFormCreateFilter();

  const formRef = ref<FormInst | null>(null);
  const filterContentRef = ref<InstanceType<typeof FilterContent> | null>(null);

  const loading = ref(false);

  const filterConfigList = ref<FilterFormItem[]>([]);
  const customFieldsFilterConfig = ref<FilterFormItem[]>([]);

  const businessStageConfig = ref<OpportunityStageConfig | null>(null);

  function createDefaultFormModel(): FilterForm {
    return {
      searchMode: 'AND',
      list: [{ dataIndex: null, operator: undefined, value: null, type: FieldTypeEnum.INPUT }],
    };
  }

  const form = ref<{
    name: string;
    sort: number;
    conditionConfig: FilterForm;
  }>({
    name: '',
    sort: 1,
    conditionConfig: createDefaultFormModel(),
  });

  function normalizeConditionList(conditionConfig: FilterForm) {
    const sourceList = conditionConfig.list?.length
      ? cloneDeep(conditionConfig.list)
      : conditionConfig.conditions?.map((item) => ({
          ...item,
          dataIndex: item.name ?? null,
          type: item.type ?? FieldTypeEnum.INPUT,
        })) ?? [];

    const configMap = new Map(
      [...filterConfigList.value, ...customFieldsFilterConfig.value].map((item) => [item.dataIndex, item])
    );

    return sourceList.map((sourceItem): FilterFormItem => {
      const item = cloneDeep(sourceItem) as FilterFormItem;
      const configItem = configMap.get(item.dataIndex);
      const optionKey = filterOptionKeyMap[item.type];

      if (optionKey && item.dataIndex) {
        const values = Array.isArray(item.value) ? item.value : [item.value];
        item[optionKey] =
          props.optionMap?.[item.dataIndex]?.filter((option: { id: string }) => values.includes(option.id)) ?? [];
      }

      return {
        ...cloneDeep(configItem),
        ...item,
      };
    });
  }

  function initDraft(branch: ApprovalConditionBranch | null) {
    form.value = {
      name: branch?.name ?? '',
      sort: branch?.sort ?? 1,
      conditionConfig: branch?.conditionConfig
        ? {
            ...branch.conditionConfig,
            list: normalizeConditionList(branch.conditionConfig),
          }
        : createDefaultFormModel(),
    };
  }

  function createDepartmentFilterItem(): FilterFormItem {
    return {
      title: t('opportunity.department'),
      dataIndex: 'departmentId',
      type: FieldTypeEnum.TREE_SELECT,
      treeSelectProps: {
        labelField: 'name',
        keyField: 'id',
        multiple: true,
        clearFilterAfterSelect: false,
        checkable: true,
        showContainChildModule: true,
      },
    };
  }

  function createApprovalStatusFilterItem(title: string): FilterFormItem {
    return {
      title,
      dataIndex: 'approvalStatus',
      type: FieldTypeEnum.SELECT_MULTIPLE,
      selectProps: {
        options: processStatusOptions,
      },
    };
  }

  const fieldChangedOperatorOption = {
    label: 'advanceFilter.operator.newNotEqualOld',
    value: OperatorEnum.NEW_NOT_EQUALS_OLD,
  };

  function appendFieldChangedOperator(item: FilterFormItem): FilterFormItem {
    const operatorOptions = item.operatorOption?.length ? item.operatorOption : operatorOptionsMap[item.type] ?? [];
    return {
      ...item,
      operatorOption: [
        ...operatorOptions,
        ...(!operatorOptions.some((option) => option.value === OperatorEnum.NEW_NOT_EQUALS_OLD)
          ? [fieldChangedOperatorOption]
          : []),
      ],
    };
  }

  function createOrderStatusFilterItem(): FilterFormItem {
    return {
      title: t('order.status'),
      dataIndex: 'stage',
      type: FieldTypeEnum.SELECT_MULTIPLE,
      selectProps: {
        options:
          businessStageConfig.value?.stageConfigList.map((item) => ({
            label: item.name,
            value: item.id,
          })) ?? [],
      },
    };
  }

  const formTypeConfigMap: Partial<Record<FormDesignKeyEnum, () => FilterFormItem[]>> = {
    [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: () => [
      {
        title: t('common.status'),
        dataIndex: 'invalid',
        type: FieldTypeEnum.SELECT_MULTIPLE,
        selectProps: {
          options: quotationStatus as any,
        },
      },
      createApprovalStatusFilterItem(t('common.approvalStatus')),
      createDepartmentFilterItem(),
      ...baseFilterConfigList,
    ],

    [FormDesignKeyEnum.CONTRACT]: () => [
      createDepartmentFilterItem(),
      {
        title: t('contract.status'),
        dataIndex: 'stage',
        type: FieldTypeEnum.SELECT_MULTIPLE,
        selectProps: {
          options:
            businessStageConfig.value?.stageConfigList.map((item) => ({
              label: item.name,
              value: item.id,
            })) ?? [],
        },
      },
      createApprovalStatusFilterItem(t('contract.approvalStatus')),
      ...baseFilterConfigList,
    ],

    [FormDesignKeyEnum.INVOICE]: () => [
      createDepartmentFilterItem(),
      createApprovalStatusFilterItem(t('contract.approvalStatus')),
      ...baseFilterConfigList,
    ],

    [FormDesignKeyEnum.ORDER]: () => [
      createDepartmentFilterItem(),
      createOrderStatusFilterItem(),
      createApprovalStatusFilterItem(t('common.approvalStatus')),
      ...baseFilterConfigList,
    ],
  };

  function createSystemFilterConfigList(): FilterFormItem[] {
    return formTypeConfigMap[props.formType as FormDesignKeyEnum]?.() ?? [...baseFilterConfigList];
  }

  function getFieldConfigProps(field: FormCreateField) {
    if (
      [FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE, FieldTypeEnum.RADIO, FieldTypeEnum.CHECKBOX].includes(
        field.type
      )
    ) {
      return {
        selectProps: {
          options: field.options,
          multiple: true,
        },
      };
    }

    if ([FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(field.type)) {
      return {
        dataSourceProps: {
          dataSourceType: field.dataSourceType,
          maxTagCount: 'responsive',
        },
      };
    }

    return {};
  }

  function createSubTableFilterConfigList(fields: FormCreateField[] = []) {
    return fields.flatMap((field) => {
      if (![FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(field.type) || !field.subFields?.length) {
        return [];
      }

      return field.subFields
        .filter(
          (subField) =>
            ![
              FieldTypeEnum.TEXTAREA,
              FieldTypeEnum.PICTURE,
              FieldTypeEnum.DIVIDER,
              FieldTypeEnum.SUB_PRICE,
              FieldTypeEnum.SUB_PRODUCT,
            ].includes(subField.type)
        )
        .map((subField) => ({
          title: `${field.name}.${subField.name}`,
          dataIndex: field.resourceFieldId?.length ? field.id : field.businessKey || field.id,
          type: subField.type,
          ...getFieldConfigProps(subField),
        })) as FilterFormItem[];
    });
  }

  async function loadFilterConfig() {
    loading.value = true;

    try {
      const api = getFormConfigApiMap[props.formType as FormDesignKeyEnum];
      const stageConfigApiMap: Partial<Record<FormDesignKeyEnum, () => Promise<OpportunityStageConfig>>> = {
        [FormDesignKeyEnum.CONTRACT]: getContractStatusConfig,
        [FormDesignKeyEnum.ORDER]: getOrderStatusConfig,
      };
      const stageConfigApi = stageConfigApiMap[props.formType as FormDesignKeyEnum];

      const [stageConfig, formConfig] = await Promise.all([stageConfigApi?.() ?? Promise.resolve(null), api()]);

      businessStageConfig.value = stageConfig;

      filterConfigList.value = createSystemFilterConfigList().map(appendFieldChangedOperator);

      customFieldsFilterConfig.value = [
        ...getFilterListConfig(formConfig),
        ...createSubTableFilterConfigList(formConfig.fields),
      ].map(appendFieldChangedOperator);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      filterConfigList.value = [];
      customFieldsFilterConfig.value = [];
    } finally {
      loading.value = false;
    }
  }

  async function initialize() {
    await loadFilterConfig();
    initDraft(props.branch);
  }

  watch(
    () => [show.value, props.branch?.id, props.formType],
    async ([visible]) => {
      if (visible) {
        await initialize();
      }
    }
  );

  function getParams() {
    const conditions: ConditionsItem[] = form.value.conditionConfig.list.map((item: any) => ({
      value: item.value,
      operator: item.operator,
      name: item.dataIndex ?? '',
      multipleValue: multipleValueTypeList.includes(item.type),
      type: item.type,
      containChildIds: item.containChildIds || [],
    }));

    return {
      list: form.value.conditionConfig.list,
      searchMode: form.value.conditionConfig.searchMode,
      conditions,
    };
  }

  async function handleConfirm() {
    try {
      await formRef.value?.validate();
      await filterContentRef.value?.formRef?.validate();

      emit('confirm', {
        name: form.value.name.trim(),
        sort: form.value.sort,
        conditionConfig: getParams(),
      });

      show.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
</script>

<style lang="less" scoped>
  :deep(.list-operator) {
    width: 140px;
  }
</style>
