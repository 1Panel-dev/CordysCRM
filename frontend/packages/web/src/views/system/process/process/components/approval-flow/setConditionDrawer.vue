<template>
  <CrmDrawer
    v-model:show="show"
    :width="1000"
    :min-width="600"
    :okText="t('common.confirm')"
    footer
    :title="t('process.process.flow.setTriggerCondition')"
    @confirm="handleConfirm"
  >
    <div class="flex flex-col gap-[16px]">
      <n-form label-placement="top" require-mark-placement="right">
        <n-form-item :label="t('process.process.flow.conditionName')">
          <n-input v-model:value="form.name" :maxlength="255" type="text" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
      </n-form>

      <FilterContent
        ref="filterContentRef"
        v-model:form-model="form.conditionConfig"
        :config-list="filterConfigList"
        :custom-list="customFieldsFilterConfig"
      />
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { reactive, ref, watch } from 'vue';
  import { NForm, NFormItem, NInput } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { OpportunityStageConfig } from '@lib/shared/models/opportunity';
  import type { ApprovalConditionBranch } from '@lib/shared/models/system/process';

  import FilterContent from '@/components/pure/crm-advance-filter/components/filterContent.vue';
  import type { FilterForm, FilterFormItem } from '@/components/pure/crm-advance-filter/type';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import { getFormConfigApiMap } from '@/components/business/crm-form-create/config';

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

  const filterContentRef = ref<InstanceType<typeof FilterContent>>();
  const filterConfigList = ref<FilterFormItem[]>([]);
  const customFieldsFilterConfig = ref<FilterFormItem[]>([]);
  const orderStageConfig = ref<OpportunityStageConfig | null>(null);

  function createDefaultFormModel(): FilterForm {
    return {
      searchMode: 'AND',
      list: [{ dataIndex: null, operator: undefined, value: null, type: FieldTypeEnum.INPUT }],
    };
  }

  const form = reactive<{
    name: string;
    conditionConfig: FilterForm;
  }>({
    name: '',
    conditionConfig: createDefaultFormModel(),
  });

  function initDraft(branch: ApprovalConditionBranch | null) {
    form.name = branch?.name ?? '';
    form.conditionConfig = cloneDeep(branch?.conditionConfig ?? createDefaultFormModel());
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

  function createSystemFilterConfigList(): FilterFormItem[] {
    if (props.formType === FormDesignKeyEnum.OPPORTUNITY_QUOTATION) {
      return [
        createApprovalStatusFilterItem(t('common.approvalStatus')),
        createDepartmentFilterItem(),
        ...baseFilterConfigList,
      ];
    }

    if ([FormDesignKeyEnum.CONTRACT, FormDesignKeyEnum.INVOICE].includes(props.formType as FormDesignKeyEnum)) {
      return [
        createDepartmentFilterItem(),
        createApprovalStatusFilterItem(t('contract.approvalStatus')),
        ...baseFilterConfigList,
      ];
    }

    if (props.formType === FormDesignKeyEnum.ORDER) {
      return [
        createDepartmentFilterItem(),
        {
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
        },
        createApprovalStatusFilterItem(t('common.approvalStatus')),
        ...baseFilterConfigList,
      ];
    }

    return [...baseFilterConfigList];
  }

  async function loadFilterConfig() {
    try {
      if (props.formType === FormDesignKeyEnum.ORDER) {
        orderStageConfig.value = await getOrderStatusConfig();
      } else {
        orderStageConfig.value = null;
      }

      const api = getFormConfigApiMap[props.formType as FormDesignKeyEnum];
      const res = await api();
      filterConfigList.value = createSystemFilterConfigList();
      customFieldsFilterConfig.value = getFilterListConfig(res);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      filterConfigList.value = [];
      customFieldsFilterConfig.value = [];
    }
  }

  watch(
    () => show.value,
    (visible) => {
      if (visible) {
        loadFilterConfig();
        initDraft(props.branch);
      }
    }
  );

  watch(
    () => [props.branch?.id, props.formType],
    () => {
      if (show.value) {
        loadFilterConfig();
        initDraft(props.branch);
      }
    }
  );

  function handleConfirm() {
    emit('confirm', {
      name: form.name.trim(),
      conditionConfig: cloneDeep(form.conditionConfig),
    });
    show.value = false;
  }
</script>
