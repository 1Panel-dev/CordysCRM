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
        />
      </div>
    </n-spin>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref, watch } from 'vue';
  import { FormInst, NForm, NFormItem, NInput, NSpin } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { OpportunityStageConfig } from '@lib/shared/models/opportunity';
  import type { ApprovalConditionBranch } from '@lib/shared/models/system/process';

  import FilterContent from '@/components/pure/crm-advance-filter/components/filterContent.vue';
  import {
    type ConditionsItem,
    type FilterForm,
    type FilterFormItem,
    filterOptionKeyMap,
  } from '@/components/pure/crm-advance-filter/type';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import { getFormConfigApiMap, multipleValueTypeList } from '@/components/business/crm-form-create/config';

  import { getOrderStatusConfig } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import { processStatusOptions } from '@/config/process';
  import useFormCreateFilter from '@/hooks/useFormCreateAdvanceFilter';

  defineOptions({
    name: 'SetConditionDrawer',
  });

  const props = defineProps<{
    branch: ApprovalConditionBranch | null;
    formType: string;
    optionMap?: Record<string, any[]>;
    readonly?: boolean;
  }>();

  const emit = defineEmits<{
    (
      e: 'confirm',
      payload: {
        name: string;
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

  const orderStageConfig = ref<OpportunityStageConfig | null>(null);

  function createDefaultFormModel(): FilterForm {
    return {
      searchMode: 'AND',
      list: [{ dataIndex: null, operator: undefined, value: null, type: FieldTypeEnum.INPUT }],
    };
  }

  const form = ref<{
    name: string;
    conditionConfig: FilterForm;
  }>({
    name: '',
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

  function createOrderStatusFilterItem(): FilterFormItem {
    return {
      title: t('order.status'),
      dataIndex: 'stage',
      type: FieldTypeEnum.SELECT_MULTIPLE,
      selectProps: {
        options:
          orderStageConfig.value?.stageConfigList.map((item) => ({
            label: item.name,
            value: item.id,
          })) ?? [],
      },
    };
  }

  const formTypeConfigMap: Partial<Record<FormDesignKeyEnum, () => FilterFormItem[]>> = {
    [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: () => [
      createApprovalStatusFilterItem(t('common.approvalStatus')),
      createDepartmentFilterItem(),
      ...baseFilterConfigList,
    ],

    [FormDesignKeyEnum.CONTRACT]: () => [
      createDepartmentFilterItem(),
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

  async function loadFilterConfig() {
    loading.value = true;

    try {
      const api = getFormConfigApiMap[props.formType as FormDesignKeyEnum];

      const [stageConfig, formConfig] = await Promise.all([
        props.formType === FormDesignKeyEnum.ORDER ? getOrderStatusConfig() : Promise.resolve(null),
        api(),
      ]);

      orderStageConfig.value = stageConfig;

      filterConfigList.value = createSystemFilterConfigList();

      customFieldsFilterConfig.value = getFilterListConfig(formConfig);
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
        conditionConfig: getParams(),
      });

      show.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
</script>
