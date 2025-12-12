<template>
  <CrmTable
    ref="crmTableRef"
    v-model:checked-row-keys="checkedRowKeys"
    v-bind="propsRes"
    :class="`crm-quotation-table-${props.formKey}`"
    :action-config="actionConfig"
    @page-change="propsEvent.pageChange"
    @page-size-change="propsEvent.pageSizeChange"
    @sorter-change="propsEvent.sorterChange"
    @filter-change="propsEvent.filterChange"
    @batch-action="handleBatchAction"
    @refresh="searchData"
  >
    <template #actionLeft>
      <div class="flex items-center gap-[12px]">
        <n-button
          v-if="!props.readonly && hasAnyPermission(['OPPORTUNITY_QUOTATION:ADD'])"
          type="primary"
          :loading="createLoading"
          @click="handleCreate"
        >
          {{ t('opportunity.quotation.new') }}
        </n-button>
      </div>
    </template>
    <template #actionRight>
      <CrmAdvanceFilter
        ref="tableAdvanceFilterRef"
        v-model:keyword="keyword"
        :search-placeholder="t('opportunity.quotation.searchPlaceholder')"
        :custom-fields-config-list="customFieldsFilterConfig"
        :filter-config-list="filterConfigList"
        @adv-search="handleAdvSearch"
        @keyword-search="searchByKeyword"
      />
    </template>

    <template #view>
      <CrmViewSelect
        v-if="!props.sourceId"
        v-model:active-tab="activeTab"
        :type="FormDesignKeyEnum.OPPORTUNITY_QUOTATION"
        :custom-fields-config-list="customFieldsFilterConfig"
        :filter-config-list="filterConfigList"
        @refresh-table-data="searchData"
      />
    </template>
  </CrmTable>
  <approvalModal v-model:show="showApprovalModal" :quotationIds="checkedRowKeys" @refresh="handleApprovalSuccess" />
  <detailDrawer
    v-model:visible="showDetailDrawer"
    :detail="activeRow"
    :refresh-id="tableRefreshId"
    @edit="handleEdit"
    @refresh="() => searchData()"
  />
  <CrmFormCreateDrawer
    v-model:visible="formCreateDrawerVisible"
    :form-key="activeFormKey"
    :source-id="activeSourceId"
    :need-init-detail="needInitDetail"
    :initial-source-name="initialSourceName"
    :other-save-params="otherSaveParams"
    :link-form-info="linkFormInfo"
    :link-form-key="linkFormKey"
    @saved="() => searchData()"
  />
  <batchOperationResultModal v-model:visible="resultVisible" :result="batchResult" :name="batchOperationName" />

  <OptOverviewDrawer
    v-model:show="showOverviewDrawer"
    :detail="activeOpportunity"
    @refresh="handleRefresh"
    @open-customer-drawer="handleShowCustomerDrawer"
  />

  <customerOverviewDrawer
    v-model:show="showCustomerOverviewDrawer"
    :source-id="activeSourceCustomerId"
    :readonly="isCustomerReadonly"
  />
  <openSeaOverviewDrawer
    v-model:show="showCustomerOpenseaOverviewDrawer"
    :source-id="activeSourceCustomerId"
    :readonly="isCustomerReadonly"
    :pool-id="poolId"
    :hidden-columns="hiddenColumns"
  />
</template>

<script setup lang="ts">
  import { DataTableRowKey, NButton, useMessage } from 'naive-ui';

  import { FieldTypeEnum, FormDesignKeyEnum, FormLinkScenarioEnum } from '@lib/shared/enums/formDesignEnum';
  import { QuotationStatusEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import useLocale from '@lib/shared/locale/useLocale';
  import { characterLimit } from '@lib/shared/method';
  import { formatNumberValue } from '@lib/shared/method/formCreate';
  import { BatchOperationResult, QuotationItem } from '@lib/shared/models/opportunity';
  import { CluePoolItem } from '@lib/shared/models/system/module';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import { inputNumberDefaultFieldConfig } from '@/components/business/crm-form-create/config';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmViewSelect from '@/components/business/crm-view-select/index.vue';
  import OptOverviewDrawer from '../optOverviewDrawer.vue';
  import approvalModal from './approvalModal.vue';
  import batchOperationResultModal from './batchOperationResultModal.vue';
  import detailDrawer from './detail.vue';
  import quotationStatus from './quotationStatus.vue';
  import customerOverviewDrawer from '@/views/customer/components/customerOverviewDrawer.vue';
  import openSeaOverviewDrawer from '@/views/customer/components/openSeaOverviewDrawer.vue';

  import { batchVoided, deleteQuotation, getOpenSeaOptions, revokeQuotation, voidQuotation } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import { quotationStatusOptions } from '@/config/opportunity';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';
  import useOpenNewPage from '@/hooks/useOpenNewPage';
  import { useUserStore } from '@/store';
  import { hasAnyPermission } from '@/utils/permission';

  import { FullPageEnum } from '@/enums/routeEnum';

  const { openModal } = useModal();
  const { t } = useI18n();
  const Message = useMessage();
  const { currentLocale } = useLocale(Message.loading);

  const useStore = useUserStore();
  const { openNewPage } = useOpenNewPage();

  const props = defineProps<{
    formKey: FormDesignKeyEnum.OPPORTUNITY_QUOTATION;
    sourceName?: string;
    sourceId?: string;
    readonly?: boolean;
    openseaHiddenColumns?: string[];
    refreshKey?: number;
  }>();

  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const resultVisible = ref(false);
  const batchResult = ref<BatchOperationResult>({
    success: 0,
    fail: 0,
    errorMessages: '',
  });
  const batchOperationName = ref(t('common.batchVoid'));
  const formCreateDrawerVisible = ref(false);
  const activeSourceId = ref('');
  const initialSourceName = ref('');
  const needInitDetail = ref(false);
  const activeFormKey = ref(FormDesignKeyEnum.OPPORTUNITY_QUOTATION);

  const activeTab = ref();
  const keyword = ref('');
  const activeQuotationId = ref('');
  const tableRefreshId = ref(0);
  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.batchApproval'),
        key: 'approval',
        permission: ['OPPORTUNITY_QUOTATION:APPROVAL'],
      },
      {
        label: t('common.batchVoid'),
        key: 'voided',
        permission: ['OPPORTUNITY_QUOTATION:VOIDED'],
      },
    ],
  };

  const showApprovalModal = ref(false);
  function handleBatchApproval() {
    showApprovalModal.value = true;
    batchOperationName.value = t('common.batchApproval');
  }

  function handleRefresh() {
    checkedRowKeys.value = [];
    tableRefreshId.value += 1;
  }

  function handleApprovalSuccess(val: BatchOperationResult) {
    batchResult.value = val;
    resultVisible.value = true;
    handleRefresh();
  }

  // 批量作废
  function handleBatchVoid() {
    batchOperationName.value = t('common.batchVoid');
    openModal({
      type: 'error',
      title: t('opportunity.quotation.batchInvalidTitleTip', { number: checkedRowKeys.value.length }),
      content: t('opportunity.quotation.invalidContentTip'),
      positiveText: t('common.confirmVoid'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          const result = await batchVoided({
            ids: checkedRowKeys.value,
            approvalStatus: QuotationStatusEnum.VOIDED,
          });
          batchResult.value = result;
          resultVisible.value = true;
          handleRefresh();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleBatchAction(item: ActionsItem) {
    switch (item.key) {
      case 'approval':
        handleBatchApproval();
        break;
      case 'voided':
        handleBatchVoid();
        break;
      default:
        break;
    }
  }

  const otherSaveParams = ref({
    id: '',
  });

  const createLoading = ref(false);
  const linkFormKey = ref(FormDesignKeyEnum.BUSINESS);
  const linkFormInfo = ref();
  const sourceId = ref(props.sourceId || '');
  const { initFormDetail, initFormConfig, linkFormFieldMap } = useFormCreateApi({
    formKey: computed(() => linkFormKey.value),
    sourceId,
  });
  async function handleCreate() {
    try {
      createLoading.value = true;
      activeFormKey.value = FormDesignKeyEnum.OPPORTUNITY_QUOTATION;
      activeSourceId.value = props.sourceId ?? '';
      initialSourceName.value = props.sourceId ? props.sourceName ?? '' : '';
      needInitDetail.value = false;
      if (props.sourceId) {
        linkFormKey.value = FormDesignKeyEnum.BUSINESS;
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

  function handleEdit(id: string) {
    activeFormKey.value = FormDesignKeyEnum.OPPORTUNITY_QUOTATION;
    activeSourceId.value = id;
    needInitDetail.value = true;
    otherSaveParams.value.id = id;
    linkFormInfo.value = undefined;
    formCreateDrawerVisible.value = true;
  }
  function handleVoid(row: QuotationItem) {
    openModal({
      type: 'error',
      title: t('opportunity.quotation.voidTitleTip', { name: characterLimit(row.name) }),
      content: t('opportunity.quotation.invalidContentTip'),
      positiveText: t('common.confirmVoid'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await voidQuotation(row.id);
          Message.success(t('common.voidSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  const showDetailDrawer = ref(false);
  const activeRow = ref<Partial<QuotationItem>>();
  function handleDelete(row: QuotationItem) {
    openModal({
      type: 'error',
      title: t('opportunity.quotation.deleteTitleTip', { name: characterLimit(row.name) }),
      content: t('opportunity.quotation.deleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteQuotation(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  async function handleRevoke(row: QuotationItem) {
    try {
      await revokeQuotation(row.id);
      Message.success(t('common.revokeSuccess'));
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function handleDownload(id: string) {
    openNewPage(FullPageEnum.FULL_PAGE_EXPORT_QUOTATION, { id });
  }

  function handleActionSelect(row: QuotationItem, actionKey: string, done?: () => void) {
    switch (actionKey) {
      case 'edit':
        handleEdit(row.id);
        break;
      case 'voided':
        handleVoid(row);
        break;
      case 'revoke':
        handleRevoke(row);
        break;
      case 'delete':
        handleDelete(row);
        break;
      case 'download':
        handleDownload(row.id);
        break;
      default:
        break;
    }
  }

  const groupList = [
    {
      label: t('common.edit'),
      key: 'edit',
      permission: ['OPPORTUNITY_QUOTATION:UPDATE'],
    },
    {
      label: t('common.voided'),
      key: 'voided',
      permission: ['OPPORTUNITY_QUOTATION:VOIDED'],
    },
    {
      label: t('common.delete'),
      key: 'delete',
      permission: ['OPPORTUNITY_QUOTATION:DELETE'],
    },
  ];

  const moreGroupList = [
    {
      label: t('common.download'),
      key: 'download',
      permission: ['OPPORTUNITY_QUOTATION:DOWNLOAD'],
    },
    {
      label: t('common.revoke'),
      key: 'revoke',
    },
  ];

  function getOperationGroupList(row: QuotationItem) {
    const allGroups = [...groupList, ...moreGroupList];
    const getGroups = (keys: string[]) => {
      return keys.map((key) => allGroups.find((e) => e.key === key)).filter(Boolean) as typeof groupList;
    };
    const commonGroups = ['voided', 'delete'];

    switch (row.approvalStatus) {
      case QuotationStatusEnum.APPROVED:
        return getGroups(['download', ...commonGroups]);
      case QuotationStatusEnum.UNAPPROVED:
      case QuotationStatusEnum.REVOKED:
        return getGroups(['edit', ...commonGroups]);
      case QuotationStatusEnum.APPROVING:
        const operationGroups = row.createUser === useStore.userInfo.id ? ['revoke', ...commonGroups] : commonGroups;
        return getGroups(operationGroups);
      case QuotationStatusEnum.VOIDED:
        return getGroups(['delete']);
      default:
        return [];
    }
  }
  const showOverviewDrawer = ref<boolean>(false);
  const activeOpportunity = ref();
  function showOpportunityDrawer(row: QuotationItem) {
    showOverviewDrawer.value = true;
    activeOpportunity.value = {
      id: row.opportunityId,
      name: row.opportunityName,
    };
  }
  const showCustomerOverviewDrawer = ref(false);
  const showCustomerOpenseaOverviewDrawer = ref(false);
  const poolId = ref<string>('');
  const activeSourceCustomerId = ref<string>('');
  const isCustomerReadonly = ref(false);

  const openSeaOptions = ref<CluePoolItem[]>([]);
  function handleOpenCustomerDrawer(
    params: { customerId: string; inCustomerPool: boolean; poolId: string },
    readonly = false
  ) {
    activeSourceCustomerId.value = params.customerId;
    if (params.inCustomerPool) {
      if (hasAnyPermission(['CUSTOMER_MANAGEMENT_POOL:READ'])) {
        showCustomerOpenseaOverviewDrawer.value = true;
        poolId.value = params.poolId;
      } else {
        Message.warning(t('opportunity.noOpenSeaPermission'));
      }
    } else {
      showCustomerOverviewDrawer.value = true;
    }
    isCustomerReadonly.value = readonly;
  }

  function handleShowCustomerDrawer(params: { customerId: string; inCustomerPool: boolean; poolId: string }) {
    handleOpenCustomerDrawer(params, true);
  }

  async function initOpenSeaOptions() {
    if (hasAnyPermission(['CUSTOMER_MANAGEMENT_POOL:READ'])) {
      const res = await getOpenSeaOptions();
      openSeaOptions.value = res;
    }
  }

  const hiddenColumns = computed<string[]>(() => {
    const openSeaSetting = openSeaOptions.value.find((item) => item.id === poolId.value);
    return openSeaSetting?.fieldConfigs.filter((item) => !item.enable).map((item) => item.fieldId) || [];
  });

  onBeforeMount(() => {
    initOpenSeaOptions();
  });

  const { useTableRes, customFieldsFilterConfig, fieldList } = await useFormCreateTable({
    formKey: props.formKey,
    containerClass: `.crm-quotation-table-${props.formKey}`,
    operationColumn: props.readonly
      ? undefined
      : {
          key: 'operation',
          width: currentLocale.value === 'zh-CN' ? 140 : 200,
          fixed: 'right',
          render: (row: QuotationItem) =>
            getOperationGroupList(row).length
              ? h(CrmOperationButton, {
                  groupList: getOperationGroupList(row),
                  onSelect: (key: string, done?: () => void) => handleActionSelect(row, key, done),
                })
              : '-',
        },
    specialRender: {
      name: (row: QuotationItem) => {
        const createNameButton = () =>
          h(
            CrmTableButton,
            {
              onClick: () => {
                activeRow.value = row;
                activeQuotationId.value = row.id;
                showDetailDrawer.value = true;
              },
            },
            { default: () => row.name, trigger: () => row.name }
          );
        return props.readonly ? h(CrmNameTooltip, { text: row.name }) : createNameButton();
      },
      opportunityId: (row: QuotationItem) => {
        return hasAnyPermission(['OPPORTUNITY_MANAGEMENT:READ'])
          ? h(
              CrmTableButton,
              {
                onClick: () => {
                  showOpportunityDrawer(row);
                },
              },
              { default: () => row.opportunityName, trigger: () => row.opportunityName }
            )
          : h(CrmNameTooltip, { text: row.opportunityName });
      },
      approvalStatus: (row: QuotationItem) =>
        h(quotationStatus, {
          status: row.approvalStatus,
        }),
      amount: (row: QuotationItem) => {
        return formatNumberValue(row.amount ?? 0, {
          ...inputNumberDefaultFieldConfig,
          showThousandsSeparator: true,
        });
      },
    },
    permission: ['OPPORTUNITY_QUOTATION:APPROVAL', 'OPPORTUNITY_QUOTATION:VOIDED'],
    readonly: props.readonly,
  });
  const { propsRes, propsEvent, loadList, setLoadListParams, setAdvanceFilter, filterItem, advanceFilter } =
    useTableRes;

  const isAdvancedSearchMode = ref(false);
  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

  const filterConfigList = computed<FilterFormItem[]>(() => {
    return [
      {
        title: t('common.status'),
        dataIndex: 'approvalStatus',
        type: FieldTypeEnum.SELECT_MULTIPLE,
        selectProps: {
          options: quotationStatusOptions,
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
        title: t('opportunity.quotation.amount'),
        dataIndex: 'amount',
        type: FieldTypeEnum.INPUT_NUMBER,
      },
      ...baseFilterConfigList,
    ] as FilterFormItem[];
  });

  function handleAdvSearch(filter: FilterResult, isAdvancedMode: boolean, originalForm?: FilterForm) {
    keyword.value = '';
    isAdvancedSearchMode.value = isAdvancedMode;
    setAdvanceFilter(filter);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function searchData(_keyword?: string) {
    setLoadListParams({
      keyword: _keyword ?? keyword.value,
      viewId: props.sourceId ? 'ALL' : activeTab.value,
      opportunityId: props.sourceId,
    });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function searchByKeyword(val: string) {
    keyword.value = val;
    nextTick(() => {
      searchData();
    });
  }

  onBeforeMount(async () => {
    if (props.sourceId) {
      searchData();
    }
  });

  watch(
    () => activeTab.value,
    async (val) => {
      if (val) {
        checkedRowKeys.value = [];
        setLoadListParams({
          keyword: keyword.value,
          viewId: props.sourceId ? 'ALL' : activeTab.value,
          opportunityId: props.sourceId,
        });
        crmTableRef.value?.setColumnSort(val);
      }
    },
    { immediate: true }
  );

  watch([() => tableRefreshId.value, () => props.refreshKey], () => {
    checkedRowKeys.value = [];
    searchData();
  });
</script>

<style scoped></style>
