<template>
  <CrmTable
    ref="crmTableRef"
    v-model:checked-row-keys="checkedRowKeys"
    v-bind="propsRes"
    :class="`crm-opportunity-table-${props.formKey}`"
    :not-show-table="activeShowType === 'billboard'"
    :not-show-table-filter="isAdvancedSearchMode"
    :action-config="actionConfig"
    :fullscreen-target-ref="props.fullscreenTargetRef"
    @page-change="propsEvent.pageChange"
    @page-size-change="propsEvent.pageSizeChange"
    @sorter-change="propsEvent.sorterChange"
    @filter-change="filterChange"
    @batch-action="handleBatchAction"
    @refresh="searchData"
  >
    <template #actionLeft>
      <slot
        v-if="props.readonly && props.formKey === FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY"
        name="searchTableTotal"
        :total="propsRes.crmPagination?.itemCount || 0"
      />
      <div class="flex items-center gap-[12px]">
        <n-button
          v-if="
            hasAnyPermission(['OPPORTUNITY_MANAGEMENT:ADD']) &&
            activeTab !== OpportunitySearchTypeEnum.OPPORTUNITY_SUCCESS &&
            !props.readonly
          "
          :loading="createLoading"
          type="primary"
          @click="handleCreate"
        >
          {{ t('opportunity.createOpportunity') }}
        </n-button>
        <CrmImportButton
          v-if="hasAnyPermission(['OPPORTUNITY_MANAGEMENT:IMPORT']) && !props.isCustomerTab && !props.readonly"
          :api-type="FormDesignKeyEnum.BUSINESS"
          :title="t('module.businessManagement')"
          @import-success="() => searchData()"
        />
        <n-button
          v-if="hasAnyPermission(['OPPORTUNITY_MANAGEMENT:EXPORT']) && !props.readonly"
          type="primary"
          ghost
          class="n-btn-outline-primary"
          :disabled="propsRes.data.length === 0"
          @click="handleExportAllClick"
        >
          {{ t('common.exportAll') }}
        </n-button>
      </div>
    </template>
    <template #actionRight>
      <CrmAdvanceFilter
        v-if="!props.hiddenAdvanceFilter"
        ref="tableAdvanceFilterRef"
        v-model:keyword="keyword"
        :search-placeholder="t('opportunity.searchPlaceholder')"
        :custom-fields-config-list="customFieldsFilterConfig"
        :filter-config-list="filterConfigList"
        @adv-search="handleAdvSearch"
        @keyword-search="searchByKeyword"
      />
      <n-tabs
        v-if="!props.isCustomerTab && !props.hiddenAdvanceFilter"
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
    </template>
    <template #view>
      <CrmViewSelect
        v-if="!props.isCustomerTab && !props.hiddenAdvanceFilter"
        v-model:active-tab="activeTab"
        :type="FormDesignKeyEnum.BUSINESS"
        :custom-fields-config-list="customFieldsFilterConfig"
        :filter-config-list="filterConfigList"
        :advanced-original-form="advancedOriginalForm"
        :route-name="OpportunityRouteEnum.OPPORTUNITY_OPT"
        @refresh-table-data="searchData"
        @generated-chart="handleGeneratedChart"
      />
    </template>
    <template v-if="activeShowType === 'billboard'" #other>
      <billboard ref="billboardRef" :keyword="keyword" :view-id="activeTab" :advance-filter="advanceFilter" />
    </template>
    <template v-if="showStatisticInfo" #totalRight>
      <div class="ml-[24px]">
        {{ t('opportunity.averageAmount') }}
        <span class="ml-[4px]">
          {{ abbreviateNumber(totalAmountInfo?.averageAmount, '').value }}
          <span class="unit">
            {{ abbreviateNumber(totalAmountInfo?.averageAmount, '').unit }}
          </span>
        </span>
      </div>
      <div class="ml-[24px]">
        {{ t('opportunity.totalAmount') }}
        <span class="ml-[4px]">
          {{ abbreviateNumber(totalAmountInfo?.amount, '').value }}
          <span class="unit">
            {{ abbreviateNumber(totalAmountInfo?.amount, '').unit }}
          </span>
        </span>
      </div>
    </template>
  </CrmTable>
  <TransferModal
    v-model:show="showTransferModal"
    :is-batch="true"
    :source-ids="checkedRowKeys"
    :save-api="transferOpt"
    @load-list="handleRefresh"
  />
  <OptOverviewDrawer
    v-model:show="showOverviewDrawer"
    :detail="activeOpportunity"
    @refresh="handleRefresh"
    @open-customer-drawer="handleOpenCustomerDrawer"
  />
  <CrmFormCreateDrawer
    v-model:visible="formCreateDrawerVisible"
    :form-key="realFormKey"
    :other-save-params="otherFollowRecordSaveParams"
    :source-id="activeSourceId"
    :initial-source-name="initialSourceName"
    :need-init-detail="needInitDetail"
    :link-form-info="linkFormInfo"
    :link-form-key="linkFormKey"
    :link-scenario="linkScenario"
    @saved="() => searchData()"
  />
  <CrmTableExportModal
    v-model:show="showExportModal"
    :params="exportParams"
    :export-columns="exportColumns"
    :is-export-all="isExportAll"
    type="opportunity"
    @create-success="handleExportCreateSuccess"
  />
  <openSeaOverviewDrawer
    v-model:show="showOpenSeaOverviewDrawer"
    :pool-id="openSea"
    :source-id="activeSourceId"
    :hidden-columns="props.openseaHiddenColumns || []"
    @change="searchData"
  />

  <CrmBatchEditModal
    v-model:visible="showEditModal"
    v-model:field-list="fieldList"
    :ids="checkedRowKeys"
    :form-key="FormDesignKeyEnum.BUSINESS"
    @refresh="handleRefresh"
  />
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import { DataTableRowKey, NButton, NTabPane, NTabs, useMessage } from 'naive-ui';

  import { FieldTypeEnum, FormDesignKeyEnum, FormLinkScenarioEnum } from '@lib/shared/enums/formDesignEnum';
  import { OpportunitySearchTypeEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import useLocale from '@lib/shared/locale/useLocale';
  import { abbreviateNumber, characterLimit } from '@lib/shared/method';
  import { ExportTableColumnItem } from '@lib/shared/models/common';
  import type { TransferParams } from '@lib/shared/models/customer/index';
  import type { OpportunityItem, OpportunityStageConfig } from '@lib/shared/models/opportunity';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmBatchEditModal from '@/components/business/crm-batch-edit-modal/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmImportButton from '@/components/business/crm-import-button/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmTableExportModal from '@/components/business/crm-table-export-modal/index.vue';
  import TransferModal from '@/components/business/crm-transfer-modal/index.vue';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import CrmViewSelect from '@/components/business/crm-view-select/index.vue';
  import billboard from './billboard/index.vue';
  import OptOverviewDrawer from './optOverviewDrawer.vue';
  import openSeaOverviewDrawer from '@/views/customer/components/openSeaOverviewDrawer.vue';

  import { batchDeleteOpt, deleteOpt, getOpportunityStageConfig, getOptStatistic, transferOpt } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import { defaultTransferForm, getOptHomeConditions } from '@/config/opportunity';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useLocalForage from '@/hooks/useLocalForage';
  import useModal from '@/hooks/useModal';
  import useViewChartParams, { STORAGE_VIEW_CHART_KEY, ViewChartResult } from '@/hooks/useViewChartParams';
  import { useUserStore } from '@/store';
  import { getExportColumns } from '@/utils/export';
  import { hasAllPermission, hasAnyPermission } from '@/utils/permission';

  import { OpportunityRouteEnum } from '@/enums/routeEnum';

  const useStore = useUserStore();
  const { setItem, getItem } = useLocalForage();

  const props = defineProps<{
    isCustomerTab?: boolean;
    sourceId?: string; // 客户详情下时传入客户 ID
    customerName?: string; // 客户名称
    fullscreenTargetRef?: HTMLElement | null;
    readonly?: boolean;
    openseaHiddenColumns?: string[];
    formKey:
      | FormDesignKeyEnum.CUSTOMER_OPPORTUNITY
      | FormDesignKeyEnum.BUSINESS
      | FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY;
    hiddenAdvanceFilter?: boolean;
    isLimitShowDetail?: boolean; // 是否根据权限限查看详情
    hiddenTotal?: boolean;
  }>();
  const emit = defineEmits<{
    (
      e: 'openCustomerDrawer',
      params: { customerId: string; inCustomerPool: boolean; poolId: string },
      readonly: boolean
    ): void;
    (e: 'init', val: { filterConfigList: FilterFormItem[]; customFieldsFilterConfig: FilterFormItem[] }): void;
  }>();

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();
  const route = useRoute();
  const { currentLocale } = useLocale(Message.loading);

  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const activeShowType = ref<'table' | 'billboard'>();
  const activeTab = ref();
  const keyword = ref('');
  const tableRefreshId = ref(0);

  const stageConfig = ref<OpportunityStageConfig>();
  async function initStageConfig() {
    try {
      stageConfig.value = await getOpportunityStageConfig();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
  const successStage = computed(() =>
    stageConfig.value?.stageConfigList.find((item) => item.type === 'END' && item.rate === '100')
  );
  const failureStage = computed(() =>
    stageConfig.value?.stageConfigList.find((item) => item.type === 'END' && item.rate === '0')
  );

  const actionConfig = computed<BatchActionConfig>(() => {
    if (props.readonly) {
      return {
        baseAction: [
          {
            label: t('common.exportChecked'),
            key: 'exportChecked',
            permission: ['OPPORTUNITY_MANAGEMENT:EXPORT'],
          },
        ],
      };
    }
    return {
      baseAction: [
        {
          label: t('common.exportChecked'),
          key: 'exportChecked',
          permission: ['OPPORTUNITY_MANAGEMENT:EXPORT'],
        },
        {
          label: t('common.batchEdit'),
          key: 'batchEdit',
          permission:
            activeTab.value !== OpportunitySearchTypeEnum.OPPORTUNITY_SUCCESS
              ? ['OPPORTUNITY_MANAGEMENT:UPDATE']
              : ['OPPORTUNITY_MANAGEMENT:UPDATE', 'OPPORTUNITY_MANAGEMENT:RESIGN'],
        },
        ...(activeTab.value !== OpportunitySearchTypeEnum.OPPORTUNITY_SUCCESS
          ? [
              {
                label: t('common.batchTransfer'),
                key: 'batchTransfer',
                permission: ['OPPORTUNITY_MANAGEMENT:TRANSFER'],
              },
            ]
          : []),
        {
          label: t('common.batchDelete'),
          key: 'batchDelete',
          permission: ['OPPORTUNITY_MANAGEMENT:DELETE'],
        },
      ],
    };
  });

  function handleRefresh() {
    checkedRowKeys.value = [];
    tableRefreshId.value += 1;
  }

  // 批量转移
  const showTransferModal = ref<boolean>(false);
  function handleBatchTransfer() {
    showTransferModal.value = true;
  }

  // 批量删除
  function handleBatchDelete() {
    openModal({
      type: 'error',
      title: t('opportunity.batchDeleteTitleTip', { number: checkedRowKeys.value.length }),
      content: t('opportunity.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await batchDeleteOpt(checkedRowKeys.value);
          Message.success(t('common.deleteSuccess'));
          handleRefresh();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  const showEditModal = ref(false);
  function handleBatchEdit() {
    showEditModal.value = true;
  }

  const showExportModal = ref<boolean>(false);
  const isExportAll = ref(false);

  function handleBatchAction(item: ActionsItem) {
    switch (item.key) {
      case 'batchTransfer':
        handleBatchTransfer();
        break;
      case 'batchDelete':
        handleBatchDelete();
        break;
      case 'batchEdit':
        handleBatchEdit();
        break;
      case 'exportChecked':
        isExportAll.value = false;
        showExportModal.value = true;
        break;
      default:
        break;
    }
  }

  const showOverviewDrawer = ref<boolean>(false);
  const activeSourceId = ref('');
  const activeOpportunity = ref<Partial<OpportunityItem>>();
  const formCreateDrawerVisible = ref(false);
  const realFormKey = ref<FormDesignKeyEnum>(FormDesignKeyEnum.BUSINESS);
  const initialSourceName = ref('');
  const needInitDetail = ref(false);

  function handleExportAllClick() {
    showExportModal.value = true;
    isExportAll.value = true;
  }

  function handleExportCreateSuccess() {
    checkedRowKeys.value = [];
  }

  const otherFollowRecordSaveParams = ref({
    type: 'BUSINESS',
    id: '',
    opportunityId: '',
  });

  const createLoading = ref(false);
  const linkFormKey = ref(FormDesignKeyEnum.CUSTOMER);
  const sourceId = ref(props.sourceId || '');
  const linkFormInfo = ref();
  const { initFormDetail, initFormConfig, linkFormFieldMap } = useFormCreateApi({
    formKey: computed(() => linkFormKey.value),
    sourceId,
  });
  const linkScenario = computed(() => {
    if (realFormKey.value === FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS) {
      return FormLinkScenarioEnum.OPPORTUNITY_TO_RECORD;
    }
    if (props.isCustomerTab) {
      return FormLinkScenarioEnum.CUSTOMER_TO_OPPORTUNITY;
    }
    return undefined;
  });

  // 编辑
  function handleEdit(id: string) {
    realFormKey.value = FormDesignKeyEnum.BUSINESS;
    otherFollowRecordSaveParams.value.id = id;
    needInitDetail.value = true;
    linkFormInfo.value = undefined;
    formCreateDrawerVisible.value = true;
  }

  // 跟进
  async function handleFollowUp(row: OpportunityItem) {
    realFormKey.value = FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS;
    linkFormKey.value = FormDesignKeyEnum.BUSINESS;
    sourceId.value = row.id;
    await initFormConfig();
    await initFormDetail(false, true);
    linkFormInfo.value = linkFormFieldMap.value;
    otherFollowRecordSaveParams.value.opportunityId = row.id;
    const { customerName, customerId, name } = row;
    initialSourceName.value = JSON.stringify({
      name,
      customerName,
      customerId,
    });
    needInitDetail.value = false;
    formCreateDrawerVisible.value = true;
  }

  // 删除
  function handleDelete(row: OpportunityItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('opportunity.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteOpt(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  const transferForm = ref<TransferParams>({
    owner: null,
    ids: [],
  });

  const transferFormRef = ref<InstanceType<typeof TransferForm>>();
  const transferLoading = ref(false);

  // 转移
  function handleTransfer(row: OpportunityItem, done?: () => void) {
    transferFormRef.value?.formRef?.validate(async (error) => {
      if (!error) {
        try {
          transferLoading.value = true;
          await transferOpt({
            ...transferForm.value,
            ids: [row.id],
          });
          Message.success(t('common.transferSuccess'));
          transferForm.value = { ...defaultTransferForm };
          tableRefreshId.value += 1;
          done?.();
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          transferLoading.value = false;
        }
      }
    });
  }

  function handleActionSelect(row: OpportunityItem, actionKey: string, done?: () => void) {
    activeSourceId.value = row.id;
    switch (actionKey) {
      case 'edit':
        handleEdit(row.id);
        break;
      case 'followUp':
        handleFollowUp(row);
        break;
      case 'pop-transfer':
        handleTransfer(row, done);
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  const hasBackStagePermission = computed(() =>
    hasAllPermission(['OPPORTUNITY_MANAGEMENT:UPDATE', 'OPPORTUNITY_MANAGEMENT:RESIGN'])
  );

  function getOperationGroupList(row: OpportunityItem): ActionsItem[] {
    const transferAction: ActionsItem[] = [
      {
        label: t('common.transfer'),
        key: 'transfer',
        popConfirmProps: {
          loading: transferLoading.value,
          title: t('common.transfer'),
          positiveText: t('common.confirm'),
          iconType: 'primary',
        },
        popSlotContent: 'transferPopContent',
        permission: ['OPPORTUNITY_MANAGEMENT:TRANSFER'],
      },
    ];

    const editAction: ActionsItem[] = [
      {
        label: t('common.edit'),
        key: 'edit',
        permission: ['OPPORTUNITY_MANAGEMENT:UPDATE'],
      },
    ];

    const deleteAction: ActionsItem[] = [
      {
        label: t('common.delete'),
        key: 'delete',
        permission: ['OPPORTUNITY_MANAGEMENT:DELETE'],
      },
    ];

    if (row.stage === failureStage.value?.id) {
      return [...transferAction, ...deleteAction];
    }

    if (row.stage === successStage.value?.id) {
      return hasBackStagePermission.value ? [...editAction, ...deleteAction] : [...deleteAction];
    }

    return [
      ...editAction,
      {
        label: t('opportunity.followUp'),
        key: 'followUp',
        permission: ['OPPORTUNITY_MANAGEMENT:UPDATE'],
      },
      ...transferAction,
      ...deleteAction,
    ];
  }

  const showOpenSeaOverviewDrawer = ref<boolean>(false);
  const openSea = ref<string | number>('');

  const handleAdvanceFilter = ref<null | ((...args: any[]) => void)>(null);
  const handleSearchData = ref<null | ((...args: any[]) => void)>(null);

  defineExpose({
    handleAdvanceFilter,
    handleSearchData,
  });

  function showCustomerDrawer(row: any) {
    activeSourceId.value = row.customerId;
    if (row.inCustomerPool) {
      openSea.value = row.poolId ?? '';
      showOpenSeaOverviewDrawer.value = true;
    } else {
      emit(
        'openCustomerDrawer',
        {
          customerId: row.customerId,
          inCustomerPool: row.inCustomerPool,
          poolId: row.poolId || '',
        },
        false
      );
    }
  }

  await initStageConfig();
  const { useTableRes, customFieldsFilterConfig, reasonOptions, fieldList } = await useFormCreateTable({
    formKey: props.formKey,
    excludeFieldIds: ['customerId'],
    containerClass: `.crm-opportunity-table-${props.formKey}`,
    operationColumn: props.readonly
      ? undefined
      : {
          key: 'operation',
          width: currentLocale.value === 'en-US' ? 250 : 200,
          fixed: 'right',
          render: (row: OpportunityItem) =>
            row.stage === successStage.value?.id && !hasBackStagePermission.value
              ? '-'
              : h(
                  CrmOperationButton,
                  {
                    groupList: getOperationGroupList(row),
                    onSelect: (key: string, done?: () => void) => handleActionSelect(row, key, done),
                    onCancel: () => {
                      transferForm.value = { ...defaultTransferForm };
                    },
                  },
                  {
                    transferPopContent: () => {
                      return h(TransferForm, {
                        class: 'w-[320px] mt-[16px]',
                        form: transferForm.value,
                        ref: transferFormRef,
                      });
                    },
                  }
                ),
        },
    specialRender: {
      name: (row: OpportunityItem) => {
        const createNameButton = () =>
          h(
            CrmTableButton,
            {
              onClick: () => {
                activeSourceId.value = row.id;
                activeOpportunity.value = row;
                realFormKey.value = FormDesignKeyEnum.BUSINESS;
                showOverviewDrawer.value = true;
              },
            },
            { default: () => row.name, trigger: () => row.name }
          );

        if (props.isLimitShowDetail) {
          return row.hasPermission ? createNameButton() : h(CrmNameTooltip, { text: row.name });
        }
        return props.readonly ? h(CrmNameTooltip, { text: row.name }) : createNameButton();
      },
      customerId: (row: OpportunityItem) => {
        return props.isCustomerTab ||
          props.formKey === FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY ||
          (!row.inCustomerPool && !hasAnyPermission(['CUSTOMER_MANAGEMENT:READ'])) ||
          (row.inCustomerPool && !hasAnyPermission(['CUSTOMER_MANAGEMENT_POOL:READ']))
          ? h(
              CrmNameTooltip,
              { text: row.customerName },
              {
                default: () => row.customerName,
              }
            )
          : h(
              CrmTableButton,
              {
                onClick: () => {
                  showCustomerDrawer(row);
                },
              },
              { default: () => row.customerName, trigger: () => row.customerName }
            );
      },
      stage: (row: OpportunityItem) => {
        return row.stageName || '-';
      },
    },
    permission: ['OPPORTUNITY_MANAGEMENT:UPDATE', 'OPPORTUNITY_MANAGEMENT:DELETE', 'OPPORTUNITY_MANAGEMENT:TRANSFER'],
    hiddenTotal: computed(() => !!props.hiddenTotal || activeShowType.value === 'billboard'),
    readonly: props.readonly,
    opportunityStage: stageConfig.value?.stageConfigList || [],
  });
  const {
    propsRes,
    propsEvent,
    tableQueryParams,
    loadList,
    setLoadListParams,
    setAdvanceFilter,
    filterItem,
    advanceFilter,
  } = useTableRes;

  const exportParams = computed(() => {
    return {
      ...tableQueryParams.value,
      ids: checkedRowKeys.value,
      customerId: props.sourceId,
    };
  });

  const showStatisticInfo = computed(
    () => propsRes.value.columns.find((i) => i.key === 'amount') && activeShowType.value !== 'billboard'
  );
  const statisticInfo = ref({ amount: 0, averageAmount: 0 });
  async function getStatistic(_keyword?: string) {
    try {
      const res = await getOptStatistic({
        keyword: _keyword ?? keyword.value,
        viewId: activeTab.value,
        customerId: props.sourceId,
        combineSearch: advanceFilter,
        filters: filterItem.value,
      });
      statisticInfo.value = res;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }
  const totalAmountInfo = computed(() => {
    if (checkedRowKeys.value.length > 0) {
      const amount = propsRes.value.data
        .filter((item: OpportunityItem) => checkedRowKeys.value.includes(item.id))
        .reduce((total: number, item: OpportunityItem) => total + (item.amount || 0), 0);
      return {
        averageAmount: amount / checkedRowKeys.value.length,
        amount,
      };
    }
    return {
      averageAmount: statisticInfo.value?.averageAmount ?? 0,
      amount: statisticInfo.value?.amount ?? 0,
    };
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const billboardRef = ref<InstanceType<typeof billboard>>();
  const isAdvancedSearchMode = ref(false);
  const advancedOriginalForm = ref<FilterForm | undefined>();
  function handleAdvSearch(filter: FilterResult, isAdvancedMode: boolean, originalForm?: FilterForm) {
    keyword.value = '';
    isAdvancedSearchMode.value = isAdvancedMode;
    advancedOriginalForm.value = originalForm;
    setAdvanceFilter(filter);
    if (activeShowType.value === 'billboard') {
      billboardRef.value?.refresh();
    } else {
      loadList();
      getStatistic();
      crmTableRef.value?.scrollTo({ top: 0 });
    }
  }

  handleAdvanceFilter.value = handleAdvSearch;

  const tableAdvanceFilterRef = ref<InstanceType<typeof CrmAdvanceFilter>>();

  const filterConfigList = computed<FilterFormItem[]>(() => {
    return [
      {
        title: t('opportunity.opportunityStage'),
        dataIndex: 'stage',
        type: FieldTypeEnum.SELECT_MULTIPLE,
        selectProps: {
          options:
            stageConfig.value?.stageConfigList.map((e) => ({
              label: e.name,
              value: e.id,
            })) || [],
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

  const exportColumns = computed<ExportTableColumnItem[]>(() =>
    getExportColumns(propsRes.value.columns, customFieldsFilterConfig.value as FilterFormItem[])
  );

  function searchData(_keyword?: string) {
    if (!activeTab.value && !props.isCustomerTab) return;
    setLoadListParams({
      keyword: _keyword ?? keyword.value,
      viewId: activeTab.value,
      customerId: props.sourceId,
    });
    if (activeShowType.value === 'billboard') {
      billboardRef.value?.refresh();
    } else {
      loadList();
      getStatistic(_keyword);
      crmTableRef.value?.scrollTo({ top: 0 });
    }
  }

  watch(
    () => activeShowType.value,
    async (val) => {
      if (val) {
        if (!props.isCustomerTab && !props.hiddenAdvanceFilter) {
          await setItem(`opportunity-active-show-type`, activeShowType.value as 'table' | 'billboard');
        }
        searchData();
      }
    }
  );

  function handleGeneratedChart(res: FilterResult, form: FilterForm) {
    advancedOriginalForm.value = form;
    setAdvanceFilter(res);
    tableAdvanceFilterRef.value?.setAdvancedFilter(res, true);
    searchData();
  }

  function filterChange(val: any) {
    propsEvent.value.filterChange(val);
    getStatistic();
  }
  handleSearchData.value = searchData;

  function searchByKeyword(val: string) {
    keyword.value = val;
    nextTick(() => {
      searchData();
    });
  }

  const homeDetailKey = computed(() => route.query.key as string);
  const isInitQuery = ref(true);
  async function setHomePageParams() {
    const { dim, status, timeField } = route.query;
    if (dim && timeField && homeDetailKey.value && isInitQuery.value) {
      const conditionParams = await getOptHomeConditions(
        dim as string,
        status as string,
        timeField as string,
        homeDetailKey.value
      );
      setAdvanceFilter(conditionParams);
      activeTab.value = route.query.type === 'SELF' ? route.query.type : useStore.getScopedValue;
      tableAdvanceFilterRef.value?.setAdvancedFilter(conditionParams, true);
    }
    isInitQuery.value = false;
  }

  const { initTableViewChartParams, getChartViewId } = useViewChartParams();

  function getViewId() {
    const { dim, timeField } = route.query;
    if (dim && timeField && isInitQuery.value) {
      return route.query.type === 'SELF' ? route.query.type : useStore.getScopedValue;
    }
    return getChartViewId() ?? activeTab.value;
  }

  function viewChartCallBack(params: ViewChartResult) {
    const { viewId, formModel, filterResult } = params;
    tableAdvanceFilterRef.value?.initFormModal(formModel, true);
    setAdvanceFilter(filterResult);
    activeTab.value = viewId;
  }

  watch(
    () => activeTab.value,
    async (val) => {
      if (val) {
        checkedRowKeys.value = [];
        setLoadListParams({
          keyword: keyword.value,
          viewId: getViewId(),
          customerId: props.sourceId,
        });
        setHomePageParams();
        initTableViewChartParams(viewChartCallBack);
        crmTableRef.value?.setColumnSort(val);
        getStatistic();
      }
    },
    { immediate: true }
  );

  watch(
    () => tableRefreshId.value,
    () => {
      checkedRowKeys.value = [];
      searchData();
    }
  );

  async function handleCreate() {
    try {
      createLoading.value = true;
      realFormKey.value = FormDesignKeyEnum.BUSINESS;
      activeSourceId.value = props.isCustomerTab ? props.sourceId || '' : '';
      initialSourceName.value = props.isCustomerTab ? props.customerName || '' : '';
      needInitDetail.value = false;
      if (props.isCustomerTab) {
        linkFormKey.value = FormDesignKeyEnum.CUSTOMER;
        await initFormConfig();
        await initFormDetail(false, true);
      }
      linkFormInfo.value = linkFormFieldMap.value;
      formCreateDrawerVisible.value = true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      createLoading.value = false;
    }
  }

  onBeforeMount(async () => {
    if (props.isCustomerTab) {
      searchData();
    }
  });

  onMounted(async () => {
    emit('init', {
      filterConfigList: filterConfigList.value,
      customFieldsFilterConfig: customFieldsFilterConfig.value as FilterFormItem[],
    });

    if (!props.isCustomerTab && !props.hiddenAdvanceFilter) {
      activeShowType.value = (await getItem<'billboard' | 'table'>(`opportunity-active-show-type`)) ?? 'table';
    } else {
      activeShowType.value = 'table';
    }

    if (route.query.id && !props.isCustomerTab) {
      activeOpportunity.value = {
        id: route.query.id as string,
        opportunityName: route.query.opportunityName as string,
      };
      activeSourceId.value = route.query.id as string;
      realFormKey.value = FormDesignKeyEnum.BUSINESS;
      showOverviewDrawer.value = true;
    } else if (route.query.customerId) {
      showCustomerDrawer({
        customerId: route.query.customerId as string,
        inCustomerPool: route.query.inCustomerPool === 'true',
        poolId: route.query.poolId as string,
      });
    }
    if (props.isCustomerTab) {
      realFormKey.value = FormDesignKeyEnum.CUSTOMER_OPPORTUNITY;
    }
  });

  function handleOpenCustomerDrawer(params: { customerId: string; inCustomerPool: boolean; poolId: string }) {
    if (props.isCustomerTab) {
      showOverviewDrawer.value = false;
    } else {
      emit('openCustomerDrawer', params, true);
    }
  }

  onBeforeUnmount(() => {
    sessionStorage.removeItem('homeData');
    sessionStorage.removeItem(STORAGE_VIEW_CHART_KEY);
  });
</script>

<style lang="less" scoped>
  .show-type-tabs {
    :deep(.n-tabs-tab) {
      padding: 6px;
    }
  }
</style>
