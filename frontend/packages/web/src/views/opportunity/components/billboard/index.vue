<template>
  <div class="flex h-full flex-col gap-[16px]">
    <div class="flex items-center justify-between">
      <n-button
        v-if="hasAnyPermission(['OPPORTUNITY_MANAGEMENT:ADD']) && !props.readonly"
        :loading="createLoading"
        type="primary"
        @click="handleCreate"
      >
        {{ t('opportunity.createOpportunity') }}
      </n-button>
      <div class="flex items-center gap-[8px]">
        <CrmAdvanceFilter
          v-if="!props.hiddenAdvanceFilter"
          ref="tableAdvanceFilterRef"
          v-model:keyword="keyword"
          :search-placeholder="t('opportunity.searchPlaceholder')"
          :custom-fields-config-list="filterConfigList"
          :filter-config-list="customFieldsFilterConfig"
          @adv-search="handleAdvSearch"
          @keyword-search="searchByKeyword"
        />
        <n-tabs
          v-if="!props.isCustomerTab || !props.hiddenAdvanceFilter"
          v-model:value="activeShowType"
          type="segment"
          size="large"
          class="show-type-tabs"
        >
          <n-tab-pane name="table" class="hidden">
            <template #tab><CrmIcon type="iconicon_list" /></template>
          </n-tab-pane>
          <n-tab-pane name="billboard" class="hidden">
            <template #tab><CrmIcon type="iconicon_waterfalls" /></template>
          </n-tab-pane>
        </n-tabs>
        <n-button type="default" class="outline--secondary px-[8px]" @click="toggleFullScreen">
          <CrmIcon
            class="text-[var(--text-n1)]"
            :type="isFullScreen ? 'iconicon_off_screen' : 'iconicon_full_screen_one'"
            :size="16"
          />
        </n-button>
        <n-button type="default" class="outline--secondary px-[8px]">
          <CrmIcon class="text-[var(--text-n1)]" type="iconicon_refresh" :size="16" @click="refreshTimeStamp += 1" />
        </n-button>
      </div>
    </div>
    <n-scrollbar content-class="grid [grid-template-columns:repeat(7,300px)] gap-[16px] h-full" x-scrollable>
      <list
        ref="createListRef"
        :keyword="keyword"
        :field-list="fieldList"
        :stage="[OpportunityStatusEnum.CREATE]"
        :refresh-time-stamp="refreshTimeStamp"
        :advance-filter="advanceFilter"
        :enable-reason="enableReason"
        @change="refreshList"
      />
      <list
        ref="clearRequirementsListRef"
        :keyword="keyword"
        :field-list="fieldList"
        :stage="[OpportunityStatusEnum.CLEAR_REQUIREMENTS]"
        :refresh-time-stamp="refreshTimeStamp"
        :advance-filter="advanceFilter"
        :enable-reason="enableReason"
        @change="refreshList"
      />
      <list
        ref="schemeValidationListRef"
        :keyword="keyword"
        :field-list="fieldList"
        :stage="[OpportunityStatusEnum.SCHEME_VALIDATION]"
        :refresh-time-stamp="refreshTimeStamp"
        :advance-filter="advanceFilter"
        :enable-reason="enableReason"
        @change="refreshList"
      />
      <list
        ref="projectProposalReportListRef"
        :keyword="keyword"
        :field-list="fieldList"
        :stage="[OpportunityStatusEnum.PROJECT_PROPOSAL_REPORT]"
        :refresh-time-stamp="refreshTimeStamp"
        :advance-filter="advanceFilter"
        :enable-reason="enableReason"
        @change="refreshList"
      />
      <list
        ref="businessProcurementListRef"
        :keyword="keyword"
        :field-list="fieldList"
        :stage="[OpportunityStatusEnum.BUSINESS_PROCUREMENT]"
        :refresh-time-stamp="refreshTimeStamp"
        :advance-filter="advanceFilter"
        :enable-reason="enableReason"
        @change="refreshList"
      />
      <list
        ref="successListRef"
        :keyword="keyword"
        :field-list="fieldList"
        :stage="[StageResultEnum.SUCCESS]"
        :refresh-time-stamp="refreshTimeStamp"
        :advance-filter="advanceFilter"
        :enable-reason="enableReason"
        @change="refreshList"
      />
      <list
        ref="failListRef"
        :keyword="keyword"
        :field-list="fieldList"
        :stage="[StageResultEnum.FAIL]"
        :refresh-time-stamp="refreshTimeStamp"
        :advance-filter="advanceFilter"
        :enable-reason="enableReason"
        @fail="handleFailItem"
        @change="refreshList"
      />
    </n-scrollbar>
  </div>
  <CrmFormCreateDrawer
    v-model:visible="formCreateDrawerVisible"
    :form-key="realFormKey"
    :other-save-params="otherFollowRecordSaveParams"
    :source-id="activeSourceId"
    :initial-source-name="initialSourceName"
    :need-init-detail="needInitDetail"
    :link-form-info="linkFormInfo"
    :link-form-key="FormDesignKeyEnum.CUSTOMER"
    @saved="handleCreated"
  />
  <CrmModal
    v-model:show="updateStatusModal"
    :title="t('common.complete')"
    :ok-loading="updateStageLoading"
    size="small"
    @confirm="handleConfirm"
    @cancel="handleCancel"
  >
    <n-form ref="formRef" :model="form" label-placement="left" require-mark-placement="left">
      <n-form-item
        require-mark-placement="left"
        label-placement="left"
        path="failureReason"
        :label="t('opportunity.failureReason')"
        :rule="[{ required: true, message: t('common.notNull', { value: t('opportunity.failureReason') }) }]"
      >
        <n-select v-model:value="form.failureReason" :options="reasonList" :placeholder="t('common.pleaseSelect')" />
      </n-form-item>
    </n-form>
  </CrmModal>
</template>

<script setup lang="ts">
  import { FormInst, NButton, NForm, NFormItem, NScrollbar, NSelect, NTabPane, NTabs } from 'naive-ui';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { OpportunityStatusEnum, StageResultEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import type { Option } from '@/components/business/crm-select-user-drawer/type';
  import list from './list.vue';

  import { getReasonConfig, updateOptStage } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import { lastOpportunitySteps } from '@/config/opportunity';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useFullScreen from '@/hooks/useFullScreen';
  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    isCustomerTab?: boolean;
    sourceId?: string; // 客户详情下时传入客户 ID
    customerName?: string; // 客户名称
    readonly?: boolean;
    hiddenAdvanceFilter?: boolean;
    fullscreenTargetRef?: HTMLElement | null;
    openseaHiddenColumns?: string[];
    formKey:
      | FormDesignKeyEnum.CUSTOMER_OPPORTUNITY
      | FormDesignKeyEnum.BUSINESS
      | FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY;
  }>();

  const { t } = useI18n();
  const { toggleFullScreen, isFullScreen } = useFullScreen(props.fullscreenTargetRef);

  const activeShowType = defineModel<'billboard' | 'table'>('activeShowType', {
    default: 'billboard',
  });
  const createLoading = ref(false);
  const customerFormKey = ref(FormDesignKeyEnum.CUSTOMER);
  const linkFormInfo = ref();
  const { initFormDetail, initFormConfig, linkFormFieldMap } = useFormCreateApi({
    formKey: computed(() => customerFormKey.value),
    sourceId: computed(() => props.sourceId),
  });
  const { initFormConfig: initOptFormConfig, fieldList } = useFormCreateApi({
    formKey: computed(() => FormDesignKeyEnum.BUSINESS),
  });

  const activeSourceId = ref('');
  const formCreateDrawerVisible = ref(false);
  const realFormKey = ref<FormDesignKeyEnum>(FormDesignKeyEnum.BUSINESS);
  const initialSourceName = ref('');
  const needInitDetail = ref(false);
  const keyword = ref('');
  const otherFollowRecordSaveParams = ref({
    type: 'BUSINESS',
    id: '',
    opportunityId: '',
  });

  async function handleCreate() {
    try {
      createLoading.value = true;
      realFormKey.value = FormDesignKeyEnum.BUSINESS;
      activeSourceId.value = props.isCustomerTab ? props.sourceId || '' : '';
      initialSourceName.value = props.isCustomerTab ? props.customerName || '' : '';
      needInitDetail.value = false;
      await initFormDetail(false, true);
      linkFormInfo.value = linkFormFieldMap.value;
      formCreateDrawerVisible.value = true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      createLoading.value = false;
    }
  }

  const { useTableRes, customFieldsFilterConfig, reasonOptions } = await useFormCreateTable({
    formKey: props.formKey,
    excludeFieldIds: ['customerId'],
    containerClass: '',
    permission: ['OPPORTUNITY_MANAGEMENT:UPDATE', 'OPPORTUNITY_MANAGEMENT:DELETE'],
    readonly: props.readonly,
  });
  const { setAdvanceFilter, advanceFilter } = useTableRes;

  const filterConfigList = computed<FilterFormItem[]>(() => {
    return [
      {
        title: t('opportunity.opportunityStage'),
        dataIndex: 'stage',
        type: FieldTypeEnum.SELECT_MULTIPLE,
        selectProps: {
          options: lastOpportunitySteps,
        },
      },
      {
        title: t('opportunity.failureReason'),
        dataIndex: 'failureReason',
        type: FieldTypeEnum.SELECT_MULTIPLE,
        selectProps: {
          options: reasonOptions.value,
        },
      },
      {
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
          type: 'department',
        },
      },
      {
        title: t('customer.lastFollowUps'),
        dataIndex: 'follower',
        type: FieldTypeEnum.USER_SELECT,
      },
      {
        title: t('customer.lastFollowUpDate'),
        dataIndex: 'followTime',
        type: FieldTypeEnum.TIME_RANGE_PICKER,
      },
      {
        title: t('opportunity.actualEndTime'),
        dataIndex: 'actualEndTime',
        type: FieldTypeEnum.TIME_RANGE_PICKER,
      },
      ...baseFilterConfigList,
    ] as FilterFormItem[];
  });

  const refreshTimeStamp = ref(0);
  const isAdvancedSearchMode = ref(false);
  function handleAdvSearch(filter: FilterResult, isAdvancedMode: boolean) {
    keyword.value = '';
    isAdvancedSearchMode.value = isAdvancedMode;
    setAdvanceFilter(filter);
    refreshTimeStamp.value += 1;
  }

  function searchByKeyword(val: string) {
    keyword.value = val;
    nextTick(() => {
      refreshTimeStamp.value += 1;
    });
  }

  function handleCreated() {
    refreshTimeStamp.value += 1;
  }

  const failListRef = ref<InstanceType<typeof list>>();
  const createListRef = ref<InstanceType<typeof list>>();
  const clearRequirementsListRef = ref<InstanceType<typeof list>>();
  const schemeValidationListRef = ref<InstanceType<typeof list>>();
  const projectProposalReportListRef = ref<InstanceType<typeof list>>();
  const businessProcurementListRef = ref<InstanceType<typeof list>>();
  const successListRef = ref<InstanceType<typeof list>>();

  function refreshList(stage: string) {
    switch (stage) {
      case StageResultEnum.FAIL:
        failListRef.value?.refreshList();
        break;
      case OpportunityStatusEnum.CREATE:
        createListRef.value?.refreshList();
        break;
      case OpportunityStatusEnum.CLEAR_REQUIREMENTS:
        clearRequirementsListRef.value?.refreshList();
        break;
      case OpportunityStatusEnum.SCHEME_VALIDATION:
        schemeValidationListRef.value?.refreshList();
        break;
      case OpportunityStatusEnum.PROJECT_PROPOSAL_REPORT:
        projectProposalReportListRef.value?.refreshList();
        break;
      case OpportunityStatusEnum.BUSINESS_PROCUREMENT:
        businessProcurementListRef.value?.refreshList();
        break;
      case StageResultEnum.SUCCESS:
        successListRef.value?.refreshList();
        break;
      default:
        break;
    }
  }

  const form = ref({
    failureReason: null,
  });
  const formRef = ref<FormInst | null>(null);
  const updateStatusModal = ref<boolean>(false);
  const updateStageLoading = ref(false);
  const updateOptItem = ref<any>({});
  const enableReason = ref(false);
  const reasonList = ref<Option[]>([]);
  async function initReason() {
    try {
      const { dictList, enable } = await getReasonConfig(ReasonTypeEnum.OPPORTUNITY_FAIL_RS);
      enableReason.value = enable;
      reasonList.value = dictList.map((e) => ({ label: e.name, value: e.id }));
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  function handleFailItem(item: any) {
    updateOptItem.value = item;
    updateStatusModal.value = true;
    form.value.failureReason = null;
  }

  async function handleConfirm() {
    try {
      updateStageLoading.value = true;
      await failListRef.value?.sortItem(updateOptItem.value);
      await updateOptStage({
        id: updateOptItem.value.data.id,
        stage: StageResultEnum.FAIL,
        failureReason: form.value.failureReason || '',
      });
      updateStatusModal.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      updateStageLoading.value = false;
    }
  }

  function handleCancel() {
    updateOptItem.value = {};
    updateStatusModal.value = false;
    form.value.failureReason = null;
  }

  onBeforeMount(async () => {
    initOptFormConfig();
    initReason();
    if (props.isCustomerTab) {
      initFormConfig();
    }
  });
</script>

<style lang="less" scoped>
  .show-type-tabs {
    :deep(.n-tabs-tab) {
      padding: 6px;
    }
  }
  :deep(.n-scrollbar-rail--horizontal--bottom) {
    bottom: 0 !important;
  }
</style>
