<template>
  <CrmTable
    ref="crmTableRef"
    v-model:checked-row-keys="checkedRowKeys"
    v-bind="propsRes"
    class="crm-contract-table"
    :not-show-table-filter="isAdvancedSearchMode"
    :action-config="actionConfig"
    :fullscreen-target-ref="props.fullscreenTargetRef"
    @page-change="propsEvent.pageChange"
    @page-size-change="propsEvent.pageSizeChange"
    @sorter-change="propsEvent.sorterChange"
    @filter-change="propsEvent.filterChange"
    @batch-action="handleBatchAction"
    @refresh="searchData"
  >
    <template #actionLeft>
      <div class="flex items-center gap-[12px]">
        <n-button v-permission="['CONTRACT:ADD']" type="primary" @click="handleNewClick">
          {{ t('contract.new') }}
        </n-button>
        <n-button
          v-permission="['CONTRACT:EXPORT']"
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
        ref="tableAdvanceFilterRef"
        v-model:keyword="keyword"
        :custom-fields-config-list="customFieldsFilterConfig"
        :filter-config-list="filterConfigList"
        @adv-search="handleAdvSearch"
        @keyword-search="searchData"
      />
    </template>
    <template #view>
      <CrmViewSelect
        v-model:active-tab="activeTab"
        :type="FormDesignKeyEnum.CONTRACT"
        :custom-fields-config-list="customFieldsFilterConfig"
        :filter-config-list="filterConfigList"
        :advanced-original-form="advancedOriginalForm"
        :route-name="ContractRouteEnum.CONTRACT_INDEX"
        @refresh-table-data="searchData"
      />
    </template>
  </CrmTable>

  <CrmFormCreateDrawer
    v-model:visible="formCreateDrawerVisible"
    :form-key="activeFormKey"
    :source-id="activeSourceId"
    :need-init-detail="needInitDetail"
    :link-form-key="FormDesignKeyEnum.CONTRACT"
    @saved="() => searchData()"
  />
  <CrmTableExportModal
    v-model:show="showExportModal"
    :params="exportParams"
    :export-columns="exportColumns"
    :is-export-all="isExportAll"
    type="contract"
    @create-success="handleExportCreateSuccess"
  />
  <VoidReasonModal
    v-model:visible="showVoidReasonModal"
    :name="activeSourceName"
    :sourceId="activeSourceId"
    @refresh="searchData"
  />
  <DetailDrawer
    v-model:visible="showDetailDrawer"
    :sourceId="activeSourceId"
    @refresh="searchData"
    @showCustomerDrawer="showCustomerDrawer"
  />
  <ApprovalModal
    v-model:show="showApprovalModal"
    :quotationIds="checkedRowKeys"
    :approval-api="batchApproveContract"
    @refresh="handleApprovalSuccess"
  />
  <batchOperationResultModal v-model:visible="resultVisible" :result="batchResult" :name="batchOperationName" />
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import { DataTableRowKey, NButton, NTooltip, useMessage } from 'naive-ui';

  import { ContractStatusEnum } from '@lib/shared/enums/contractEnum';
  import { FieldTypeEnum, FormDesignKeyEnum, FormLinkScenarioEnum } from '@lib/shared/enums/formDesignEnum';
  import { QuotationStatusEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import useLocale from '@lib/shared/locale/useLocale';
  import { characterLimit } from '@lib/shared/method';
  import { ExportTableColumnItem } from '@lib/shared/models/common';
  import type { ContractItem } from '@lib/shared/models/contract';
  import { BatchOperationResult } from '@lib/shared/models/opportunity';

  import { COMMON_SELECTION_OPERATORS } from '@/components/pure/crm-advance-filter/index';
  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import StatusTagSelect from '@/components/business/crm-follow-detail/statusTagSelect.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmTableExportModal from '@/components/business/crm-table-export-modal/index.vue';
  import CrmViewSelect from '@/components/business/crm-view-select/index.vue';
  import DetailDrawer from './detail.vue';
  import VoidReasonModal from './voidReasonModal.vue';
  import ApprovalModal from '@/views/opportunity/components/quotation/approvalModal.vue';
  import batchOperationResultModal from '@/views/opportunity/components/quotation/batchOperationResultModal.vue';
  import QuotationStatus from '@/views/opportunity/components/quotation/quotationStatus.vue';

  import { batchApproveContract, changeContractStatus, deleteContract, revokeContract } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import { contractStatusOptions } from '@/config/contract';
  import { quotationStatusOptions } from '@/config/opportunity';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';
  import { useUserStore } from '@/store';
  // import useViewChartParams, { STORAGE_VIEW_CHART_KEY, ViewChartResult } from '@/hooks/useViewChartParams';
  import { getExportColumns } from '@/utils/export';
  import { hasAnyPermission } from '@/utils/permission';

  import { ContractRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    fullscreenTargetRef?: HTMLElement | null;
  }>();
  const emit = defineEmits<{
    (
      e: 'openCustomerDrawer',
      params: { customerId: string; inCustomerPool: boolean; poolId: string },
      readonly: boolean
    ): void;
  }>();

  const useStore = useUserStore();
  const { t } = useI18n();
  const Message = useMessage();
  const { currentLocale } = useLocale(Message.loading);
  const { openModal } = useModal();
  const route = useRoute();

  const activeTab = ref();
  const keyword = ref('');

  // 操作
  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const tableRefreshId = ref(0);

  const formCreateDrawerVisible = ref(false);
  const activeSourceId = ref('');
  const activeSourceName = ref('');
  const needInitDetail = ref(false);
  const activeFormKey = ref(FormDesignKeyEnum.CONTRACT);

  function handleNewClick() {
    needInitDetail.value = false;
    activeFormKey.value = FormDesignKeyEnum.CONTRACT;
    activeSourceId.value = '';
    activeSourceName.value = '';
    formCreateDrawerVisible.value = true;
  }

  const showExportModal = ref<boolean>(false);
  const isExportAll = ref(false);

  function handleExportAllClick() {
    isExportAll.value = true;
    showExportModal.value = true;
  }
  function handleExportCreateSuccess() {
    checkedRowKeys.value = [];
  }

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.exportChecked'),
        key: 'exportChecked',
        permission: ['CONTRACT:EXPORT'],
      },
      {
        label: t('common.batchApproval'),
        key: 'approval',
        permission: ['CONTRACT:APPROVAL'],
      },
    ],
  };

  const showApprovalModal = ref(false);
  const batchOperationName = ref('');
  const batchResult = ref<BatchOperationResult>({
    success: 0,
    fail: 0,
    errorMessages: '',
  });
  const resultVisible = ref(false);
  function handleApprovalSuccess(val: BatchOperationResult) {
    batchResult.value = val;
    resultVisible.value = true;
    tableRefreshId.value += 1;
  }

  function handleBatchAction(item: ActionsItem) {
    switch (item.key) {
      case 'exportChecked':
        isExportAll.value = false;
        showExportModal.value = true;
        break;
      case 'approval':
        showApprovalModal.value = true;
        batchOperationName.value = t('common.batchApproval');
        break;
      default:
        break;
    }
  }

  // 表格
  const filterConfigList = computed<FilterFormItem[]>(() => [
    {
      title: t('opportunity.department'),
      dataIndex: 'departmentId',
      type: FieldTypeEnum.TREE_SELECT,
      treeSelectProps: {
        labelField: 'name',
        keyField: 'id',
        multiple: true,
        clearFilterAfterSelect: false,
        type: 'department',
        checkable: true,
        showContainChildModule: true,
        containChildIds: [],
      },
    },
    {
      title: t('contract.status'),
      dataIndex: 'stage',
      type: FieldTypeEnum.SELECT_MULTIPLE,
      operatorOption: COMMON_SELECTION_OPERATORS,
      selectProps: {
        options: contractStatusOptions,
      },
    },
    {
      title: t('contract.voidReason'),
      dataIndex: 'voidReason',
      type: FieldTypeEnum.INPUT,
    },
    {
      title: t('opportunity.quotation.amount'),
      dataIndex: 'amount',
      type: FieldTypeEnum.INPUT_NUMBER,
    },
    {
      title: t('contract.approvalStatus'),
      dataIndex: 'approvalStatus',
      operatorOption: COMMON_SELECTION_OPERATORS,
      type: FieldTypeEnum.SELECT_MULTIPLE,
      selectProps: {
        options: quotationStatusOptions.filter((item) => ![QuotationStatusEnum.VOIDED].includes(item.value)),
      },
    },
    ...baseFilterConfigList,
  ]);

  function getOperationGroupList(row: ContractItem) {
    if (row.approvalStatus === QuotationStatusEnum.APPROVING) {
      return [
        {
          label: t('common.approval'),
          key: 'approval',
          permission: ['CONTRACT:APPROVAL'],
        },
        ...(row.createUser === useStore.userInfo.id
          ? [
              {
                label: t('common.revoke'),
                key: 'revoke',
              },
            ]
          : []),
        {
          label: t('common.delete'),
          key: 'delete',
          permission: ['CONTRACT:DELETE'],
        },
      ];
    }
    if (row.approvalStatus === QuotationStatusEnum.APPROVED) {
      return [
        // TODO lmy 回款
        {
          label: t('common.delete'),
          key: 'paymentRecord',
          permission: ['CONTRACT:DELETE'],
        },
        {
          label: t('common.delete'),
          key: 'delete',
          permission: ['CONTRACT:DELETE'],
        },
      ];
    }
    return [
      {
        label: t('common.edit'),
        key: 'edit',
        permission: ['CONTRACT:UPDATE'],
      },
      {
        label: t('common.delete'),
        key: 'delete',
        permission: ['CONTRACT:DELETE'],
      },
    ];
  }

  const showDetailDrawer = ref(false);

  function handleEdit(id: string) {
    activeFormKey.value = FormDesignKeyEnum.CONTRACT;
    activeSourceId.value = id;
    needInitDetail.value = true;
    formCreateDrawerVisible.value = true;
  }

  function handleDelete(row: ContractItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('common.deleteConfirmContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteContract(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  const showVoidReasonModal = ref(false);
  function handleVoided(row: ContractItem) {
    activeSourceName.value = row.name;
    activeSourceId.value = row.id;
    showVoidReasonModal.value = true;
  }

  async function handleRevoke(row: ContractItem) {
    try {
      await revokeContract(row.id);
      Message.success(t('common.revokeSuccess'));
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function handleActionSelect(row: ContractItem, actionKey: string) {
    switch (actionKey) {
      case 'approval':
        activeSourceId.value = row.id;
        showDetailDrawer.value = true;
        break;
      // TODO lmy 回款
      case 'revoke':
        handleRevoke(row);
        break;
      case 'edit':
        handleEdit(row.id);
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  function showCustomerDrawer(params: { customerId: string; inCustomerPool: boolean; poolId: string }) {
    activeSourceId.value = params.customerId;
    emit(
      'openCustomerDrawer',
      {
        customerId: params.customerId,
        inCustomerPool: params.inCustomerPool,
        poolId: params.poolId || '',
      },
      false
    );
  }

  async function changeStatus(id: string, stage: string) {
    try {
      await changeContractStatus(id, stage);
      Message.success(t('common.updateSuccess'));
      return true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  const { useTableRes, customFieldsFilterConfig, fieldList } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.CONTRACT,
    operationColumn: {
      key: 'operation',
      width: currentLocale.value === 'en-US' ? 180 : 150,
      fixed: 'right',
      render: (row: ContractItem) =>
        getOperationGroupList(row).length
          ? h(CrmOperationButton, {
              groupList: getOperationGroupList(row),
              onSelect: (key: string) => handleActionSelect(row, key),
            })
          : '-',
    },
    specialRender: {
      name: (row: ContractItem) => {
        return h(
          CrmTableButton,
          {
            onClick: () => {
              activeSourceId.value = row.id;
              showDetailDrawer.value = true;
            },
          },
          { default: () => row.name, trigger: () => row.name }
        );
      },
      customerId: (row: ContractItem) => {
        return (!row.inCustomerPool && !hasAnyPermission(['CUSTOMER_MANAGEMENT:READ'])) ||
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
      stage: (row: ContractItem) => {
        const disabled = row.approvalStatus !== QuotationStatusEnum.APPROVED || !hasAnyPermission(['CONTRACT:STAGE']);
        if (disabled) {
          return h(
            NTooltip,
            { delay: 300 },
            {
              trigger: () =>
                h(
                  'div',
                  { class: 'cursor-not-allowed' },
                  {
                    default: () => contractStatusOptions.find((item) => item.value === row?.stage)?.label,
                  }
                ),
              default: () => t('contract.changeStageTip'),
            }
          );
        }
        return h(StatusTagSelect, {
          'status': row.stage as ContractStatusEnum,
          'noRender': true,
          'disabled': disabled,
          'onUpdate:status': async (val) => {
            // 修改为作废的时候需要填写原因
            if (val === ContractStatusEnum.VOID) {
              handleVoided(row);
            } else {
              const res = await changeStatus(row.id, val);
              if (res) row.stage = val;
            }
          },
          'statusOptions': contractStatusOptions,
        });
      },
      approvalStatus: (row: ContractItem) =>
        h(QuotationStatus, {
          status: row.approvalStatus,
        }),
    },
    permission: ['CONTRACT:EXPORT', 'CONTRACT:APPROVAL'],
    containerClass: '.crm-contract-table',
  });
  const { propsRes, propsEvent, tableQueryParams, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;

  const exportColumns = computed<ExportTableColumnItem[]>(() =>
    getExportColumns(propsRes.value.columns, customFieldsFilterConfig.value as FilterFormItem[], fieldList.value)
  );
  const exportParams = computed(() => {
    return {
      ...tableQueryParams.value,
      ids: checkedRowKeys.value,
    };
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const tableAdvanceFilterRef = ref<InstanceType<typeof CrmAdvanceFilter>>();

  const isAdvancedSearchMode = ref(false);
  const advancedOriginalForm = ref<FilterForm | undefined>();
  function handleAdvSearch(filter: FilterResult, isAdvancedMode: boolean, originalForm?: FilterForm) {
    keyword.value = '';
    advancedOriginalForm.value = originalForm;
    isAdvancedSearchMode.value = isAdvancedMode;
    setAdvanceFilter(filter);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function searchData(val?: string) {
    setLoadListParams({ keyword: val ?? keyword.value, viewId: activeTab.value });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  watch(
    () => tableRefreshId.value,
    () => {
      checkedRowKeys.value = [];
      searchData();
    }
  );

  // 先不上
  // function handleGeneratedChart(res: FilterResult, form: FilterForm) {
  //   advancedOriginalForm.value = form;
  //   setAdvanceFilter(res);
  //   tableAdvanceFilterRef.value?.setAdvancedFilter(res, true);
  //   searchData();
  // }

  // const { initTableViewChartParams, getChartViewId } = useViewChartParams();

  // function viewChartCallBack(params: ViewChartResult) {
  //   const { viewId, formModel, filterResult } = params;
  //   tableAdvanceFilterRef.value?.initFormModal(formModel, true);
  //   setAdvanceFilter(filterResult);
  //   activeTab.value = viewId;
  // }

  watch(
    () => activeTab.value,
    (val) => {
      if (val) {
        checkedRowKeys.value = [];
        setLoadListParams({ keyword: keyword.value, viewId: activeTab.value });
        // initTableViewChartParams(viewChartCallBack);
        crmTableRef.value?.setColumnSort(val);
      }
    }
  );

  onMounted(async () => {
    if (route.query.id) {
      activeSourceId.value = route.query.id as string;
      showDetailDrawer.value = true;
    }
  });

  // onBeforeUnmount(() => {
  //   sessionStorage.removeItem(STORAGE_VIEW_CHART_KEY);
  // });
</script>
