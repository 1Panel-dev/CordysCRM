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
    :source-id="activeSourceId"
    @edit="handleEdit"
    @refresh="() => searchData(undefined, activeSourceId)"
    @remove="removeItemFromList(activeSourceId)"
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
    @saved="handleFormCreateSaved"
  />
  <batchOperationResultModal v-model:visible="resultVisible" :result="batchResult" :name="batchOperationName" />
  <CrmBatchEditModal
    v-model:visible="showEditModal"
    v-model:field-list="editFieldList"
    :ids="checkedRowKeys"
    :form-key="FormDesignKeyEnum.OPPORTUNITY_QUOTATION"
    @refresh="handleRefresh"
  />

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
  import { useRoute } from 'vue-router';
  import { DataTableRowKey, NButton, useMessage } from 'naive-ui';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { QuotationStatusEnum } from '@lib/shared/enums/opportunityEnum';
  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { BatchOperationResult, QuotationItem } from '@lib/shared/models/opportunity';
  import { CluePoolItem } from '@lib/shared/models/system/module';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmApprovalPopover from '@/components/business/crm-approval/components/crm-approval-popover.vue';
  import CrmBatchEditModal from '@/components/business/crm-batch-edit-modal/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmViewSelect from '@/components/business/crm-view-select/index.vue';
  import OptOverviewDrawer from '../optOverviewDrawer.vue';
  import approvalModal from './approvalModal.vue';
  import batchOperationResultModal from './batchOperationResultModal.vue';
  import detailDrawer from './detail.vue';
  import customerOverviewDrawer from '@/views/customer/components/customerOverviewDrawer.vue';
  import openSeaOverviewDrawer from '@/views/customer/components/openSeaOverviewDrawer.vue';

  import { batchVoided, deleteQuotation, getOpenSeaOptions, revokeQuotation, voidQuotation } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import { quotationDataActionMap } from '@/config/opportunity';
  import { processStatusOptions } from '@/config/process';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';
  import useOpenNewPage from '@/hooks/useOpenNewPage';
  import { hasAnyPermission } from '@/utils/permission';

  import { FullPageEnum } from '@/enums/routeEnum';

  const { openModal } = useModal();
  const { t } = useI18n();
  const Message = useMessage();

  const { openNewPage } = useOpenNewPage();

  const props = defineProps<{
    formKey: FormDesignKeyEnum.OPPORTUNITY_QUOTATION;
    sourceName?: string;
    sourceId?: string;
    readonly?: boolean;
    openseaHiddenColumns?: string[];
    refreshKey?: number;
  }>();

  const route = useRoute();
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
  const tableRefreshId = ref(0);
  const tableRemoveRefreshId = ref('');
  const tableRefreshItemId = ref('');

  const showApprovalModal = ref(false);
  const showEditModal = ref(false);
  function handleBatchApproval() {
    showApprovalModal.value = true;
    batchOperationName.value = t('common.batchApproval');
  }

  const { initFormConfig: initEditFormConfig, fieldList: editFieldList } = useFormCreateApi({
    formKey: ref(FormDesignKeyEnum.OPPORTUNITY_QUOTATION),
  });
  function handleBatchEdit() {
    initEditFormConfig();
    showEditModal.value = true;
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
          // todo xinxinwu 作废状态 status 等待调整
          const result = await batchVoided({
            ids: checkedRowKeys.value,
            status: QuotationStatusEnum.VOIDED,
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
      case 'batchEdit':
        handleBatchEdit();
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
          tableRefreshItemId.value = row.id;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  const showDetailDrawer = ref(false);
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
          tableRemoveRefreshId.value = row.id;
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
      tableRefreshItemId.value = row.id;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function handleDownload(id: string) {
    openNewPage(FullPageEnum.FULL_PAGE_EXPORT_QUOTATION, { id });
  }

  const { initApprovalPermission, resolveRowOperation, enableApproval, canOpenListDetail } =
    useApprovalOperation<QuotationItem>({
      formType: FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
      detailReadPermissions: ['OPPORTUNITY_QUOTATION:READ'],
      dataActionMap: quotationDataActionMap,
      specialActionFilter: (row, actionKeys) => {
        // todo 状态为作废历史数据 xinxinwu
        if (row.status === QuotationStatusEnum.VOIDED) {
          return actionKeys.filter((key) => key === 'delete');
        }
        return actionKeys;
      },
    });

  function handleApproval(row: QuotationItem) {
    if (!canOpenListDetail(row)) {
      return;
    }
    activeSourceId.value = row.id;
    showDetailDrawer.value = true;
  }

  function handleActionSelect(row: QuotationItem, actionKey: string, done?: () => void) {
    switch (actionKey) {
      case 'edit':
        handleEdit(row.id);
        break;
      case 'review':
        // todo 提审
        handleApproval(row);
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

  await initApprovalPermission();

  const { useTableRes, customFieldsFilterConfig } = await useFormCreateTable({
    formKey: props.formKey,
    containerClass: `.crm-quotation-table-${props.formKey}`,
    operationColumn: props.readonly
      ? undefined
      : {
          key: 'operation',
          width: 180,
          fixed: 'right',
          render: (row: QuotationItem) => {
            const operation = resolveRowOperation(row);
            return operation.groupList.length
              ? h(CrmOperationButton, {
                  groupList: operation.groupList,
                  moreList: operation.moreList,
                  onSelect: (key: string, done?: () => void) => handleActionSelect(row, key, done),
                })
              : '-';
          },
        },
    specialRender: {
      name: (row: QuotationItem) => {
        const canOpenDetail = !props.readonly && canOpenListDetail(row);
        const createNameButton = () =>
          h(
            CrmTableButton,
            {
              onClick: () => {
                activeSourceId.value = row.id;
                showDetailDrawer.value = true;
              },
            },
            { default: () => row.name, trigger: () => row.name }
          );
        return canOpenDetail ? createNameButton() : h(CrmNameTooltip, { text: row.name });
      },
      opportunityId: (row: QuotationItem) => {
        return hasAnyPermission(['OPPORTUNITY_MANAGEMENT:READ']) && row.opportunityName
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
      status: (row: QuotationItem) => {
        return h(
          CrmTag,
          {
            type: row.status === QuotationStatusEnum.VOIDED ? 'default' : 'info',
            theme: 'light',
          },
          {
            default: () => (row.status === QuotationStatusEnum.VOIDED ? t('common.voided') : t('common.normal')),
          }
        );
      },
      approvalStatus: (row: QuotationItem) => {
        const canOpenDetail = canOpenListDetail(row);
        return row.status === QuotationStatusEnum.VOIDED
          ? '-'
          : h(CrmApprovalPopover, {
              status: row.approvalStatus,
              formKey: props.formKey,
              sourceId: row.id,
              showMore: canOpenDetail,
              disabled: row.approvalStatus !== ProcessStatusEnum.UNAPPROVED,
              onMore: () => {
                handleApproval(row);
              },
            });
      },
    },
    permission: ['OPPORTUNITY_QUOTATION:APPROVAL', 'OPPORTUNITY_QUOTATION:VOIDED'],
    readonly: props.readonly,
    enableApproval,
  });

  const actionConfig = computed(<BatchActionConfig>() => {
    return {
      baseAction: [
        ...(enableApproval.value
          ? [
              {
                label: t('common.batchApproval'),
                key: 'approval',
                permission: ['OPPORTUNITY_QUOTATION:APPROVAL'],
              },
            ]
          : []),
        {
          label: t('common.batchEdit'),
          key: 'batchEdit',
          permission: ['OPPORTUNITY_QUOTATION:UPDATE'],
        },
        {
          label: t('common.batchVoid'),
          key: 'voided',
          permission: ['OPPORTUNITY_QUOTATION:VOIDED'],
        },
      ],
    };
  });
  const { propsRes, propsEvent, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;

  const isAdvancedSearchMode = ref(false);
  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

  const filterConfigList = computed<FilterFormItem[]>(() => {
    return [
      ...(enableApproval.value
        ? [
            {
              title: t('common.approvalStatus'),
              dataIndex: 'approvalStatus',
              type: FieldTypeEnum.SELECT_MULTIPLE,
              selectProps: {
                options: processStatusOptions,
              },
            },
          ]
        : []),
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

  function searchData(_keyword?: string, refreshId?: string) {
    setLoadListParams({
      keyword: _keyword ?? keyword.value,
      viewId: props.sourceId ? 'ALL' : activeTab.value,
      opportunityId: props.sourceId,
    });
    loadList(false, refreshId);
    if (!refreshId) {
      crmTableRef.value?.scrollTo({ top: 0 });
    }
  }

  function searchByKeyword(val: string) {
    keyword.value = val;
    nextTick(() => {
      searchData();
    });
  }

  function handleFormCreateSaved() {
    if (needInitDetail.value) {
      searchData(undefined, activeSourceId.value);
    } else {
      searchData();
    }
  }

  function removeItemFromList(id: string) {
    propsRes.value.data = propsRes.value.data.filter((item) => item.id !== id);
    propsRes.value.crmPagination = {
      ...propsRes.value.crmPagination,
      itemCount: (propsRes.value.crmPagination?.itemCount ?? 1) - 1,
    };
  }

  watch(
    () => tableRemoveRefreshId.value,
    (val) => {
      if (val) {
        removeItemFromList(val);
      }
    }
  );

  watch(
    () => tableRefreshItemId.value,
    (val) => {
      if (val) {
        searchData(undefined, val);
        tableRefreshItemId.value = '';
      }
    }
  );

  onBeforeMount(() => {
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

  onMounted(() => {
    if (route.query.id && !props.sourceId) {
      activeSourceId.value = route.query.id as string;
      showDetailDrawer.value = true;
    }
  });
</script>

<style scoped></style>
