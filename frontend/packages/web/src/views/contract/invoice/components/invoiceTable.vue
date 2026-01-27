<template>
  <CrmTable
    ref="crmTableRef"
    v-model:checked-row-keys="checkedRowKeys"
    v-bind="propsRes"
    :fullscreen-target-ref="props.fullscreenTargetRef"
    :class="`crm-contract-payment-table-${FormDesignKeyEnum.INVOICE}`"
    :not-show-table-filter="isAdvancedSearchMode"
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
          v-if="!props.readonly"
          v-permission="['CONTRACT_INVOICE:ADD']"
          :loading="createLoading"
          type="primary"
          @click="handleNewClick"
        >
          {{ t('invoice.new') }}
        </n-button>
        <n-button
          v-permission="['CONTRACT_INVOICE:EXPORT']"
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
        v-if="!props.isContractTab"
        v-model:active-tab="activeTab"
        :type="FormDesignKeyEnum.INVOICE"
        :custom-fields-config-list="customFieldsFilterConfig"
        :filter-config-list="filterConfigList"
        :advanced-original-form="advancedOriginalForm"
        :route-name="ContractRouteEnum.CONTRACT_INVOICE"
        @refresh-table-data="searchData"
      />
    </template>
  </CrmTable>

  <CrmFormCreateDrawer
    v-model:visible="formCreateDrawerVisible"
    :form-key="FormDesignKeyEnum.INVOICE"
    :source-id="activeSourceId"
    :need-init-detail="needInitDetail"
    :initial-source-name="initialSourceName"
    :link-form-key="FormDesignKeyEnum.CONTRACT"
    :link-form-info="linkFormInfo"
    @saved="() => searchData()"
  />
  <CrmTableExportModal
    v-model:show="showExportModal"
    :params="exportParams"
    :export-columns="exportColumns"
    :is-export-all="isExportAll"
    type="invoice"
    @create-success="handleExportCreateSuccess"
  />

  <DetailDrawer
    v-model:visible="showDetailDrawer"
    :sourceId="activeSourceId"
    :readonly="props.readonly"
    @refresh="searchData"
    @open-contract-drawer="showContractDrawer"
    @open-customer-drawer="showCustomerDrawer"
  />
</template>

<script setup lang="ts">
  import { DataTableRowKey, NButton, useMessage } from 'naive-ui';

  import { ContractInvoiceStatusEnum } from '@lib/shared/enums/contractEnum';
  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import useLocale from '@lib/shared/locale/useLocale';
  import { ExportTableColumnItem } from '@lib/shared/models/common';
  import type { ContractInvoiceItem } from '@lib/shared/models/contract';

  import { COMMON_SELECTION_OPERATORS } from '@/components/pure/crm-advance-filter/index';
  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmTableExportModal from '@/components/business/crm-table-export-modal/index.vue';
  import CrmViewSelect from '@/components/business/crm-view-select/index.vue';
  import contractInvoiceStatus from './contractInvoiceStatus.vue';
  import DetailDrawer from './detail.vue';

  import { batchDeleteInvoiced, deleteInvoiced, revokeInvoiced } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import { contractInvoiceStatusOptions, deleteInvoiceContentMap } from '@/config/contract';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';
  import useUserStore from '@/store/modules/user';
  import { getExportColumns } from '@/utils/export';
  import { hasAnyPermission } from '@/utils/permission';

  import { ContractRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    fullscreenTargetRef?: HTMLElement | null;
    isContractTab?: boolean;
    sourceId?: string; // 合同详情下
    sourceName?: string;
    readonly?: boolean;
  }>();
  const emit = defineEmits<{
    (e: 'openBusinessTitleDrawer', params: { id: string }): void;
    (e: 'openContractDrawer', params: { id: string }): void;
    (e: 'openCustomerDrawer', params: { customerId: string; inCustomerPool: boolean; poolId: string }): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { currentLocale } = useLocale(Message.loading);
  const { openModal } = useModal();
  const useStore = useUserStore();

  const activeTab = ref();
  const keyword = ref('');
  const tableRefreshId = ref(0);

  // 操作
  const checkedRowKeys = ref<DataTableRowKey[]>([]);

  const formCreateDrawerVisible = ref(false);
  const activeSourceId = ref(props.sourceId || '');
  const initialSourceName = ref('');
  const needInitDetail = ref(false);

  const createLoading = ref(false);
  const linkFormKey = ref(FormDesignKeyEnum.CONTRACT);
  const linkFormInfo = ref();
  const { initFormDetail, initFormConfig, linkFormFieldMap } = useFormCreateApi({
    formKey: linkFormKey,
    sourceId: activeSourceId,
  });

  async function handleNewClick() {
    try {
      createLoading.value = true;
      activeSourceId.value = props.isContractTab ? props.sourceId || '' : '';
      initialSourceName.value = props.isContractTab ? props.sourceName || '' : '';
      needInitDetail.value = false;
      if (props.isContractTab) {
        linkFormKey.value = FormDesignKeyEnum.CONTRACT;
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
        permission: ['CONTRACT_INVOICE:EXPORT'],
      },
      {
        label: t('common.batchDelete'),
        key: 'batchDelete',
        permission: ['CONTRACT_INVOICE:DELETE'],
      },
    ],
  };

  // 批量删除
  function handleBatchDelete() {
    openModal({
      type: 'error',
      title: t('invoice.batchDeleteTitle', { count: checkedRowKeys.value.length }),
      content: t('invoice.batchDelete'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await batchDeleteInvoiced(checkedRowKeys.value as string[]);
          tableRefreshId.value += 1;
          Message.success(t('common.deleteSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleBatchAction(item: ActionsItem) {
    switch (item.key) {
      case 'exportChecked':
        isExportAll.value = false;
        showExportModal.value = true;
        break;
      case 'batchDelete':
        handleBatchDelete();
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
      title: t('contract.approvalStatus'),
      dataIndex: 'approvalStatus',
      type: FieldTypeEnum.SELECT_MULTIPLE,
      operatorOption: COMMON_SELECTION_OPERATORS,
      selectProps: {
        options: contractInvoiceStatusOptions,
      },
    },
    ...baseFilterConfigList,
  ]);

  function getOperationGroupList(row: ContractInvoiceItem) {
    if (props.readonly) {
      return [];
    }
    if (row.approvalStatus === ContractInvoiceStatusEnum.APPROVING) {
      return [
        {
          label: t('common.approval'),
          key: 'approval',
          permission: ['CONTRACT_INVOICE:APPROVAL'],
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
          permission: ['CONTRACT_INVOICE:DELETE'],
        },
      ];
    }
    if (row.approvalStatus === ContractInvoiceStatusEnum.APPROVED) {
      return [
        {
          label: t('common.delete'),
          key: 'delete',
          permission: ['CONTRACT_INVOICE:DELETE'],
        },
      ];
    }
    return [
      {
        label: t('common.edit'),
        key: 'edit',
        permission: ['CONTRACT_INVOICE:UPDATE'],
      },
      {
        label: t('common.delete'),
        key: 'delete',
        permission: ['CONTRACT_INVOICE:DELETE'],
      },
    ];
  }

  const showDetailDrawer = ref(false);

  function handleDelete(row: any) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: row.name }),
      content: deleteInvoiceContentMap[row.approvalStatus as ContractInvoiceStatusEnum],
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteInvoiced(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleEdit(id: string) {
    activeSourceId.value = id;
    needInitDetail.value = true;
    formCreateDrawerVisible.value = true;
  }

  function showDetail(id: string) {
    activeSourceId.value = id;
    showDetailDrawer.value = true;
  }

  async function handleRevoke(row: ContractInvoiceItem) {
    try {
      await revokeInvoiced(row.id);
      Message.success(t('common.revokeSuccess'));
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function handleActionSelect(row: ContractInvoiceItem, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        handleEdit(row.id);
        break;
      case 'revoke':
        handleRevoke(row);
        break;
      case 'delete':
        handleDelete(row);
        break;
      case 'approval':
        showDetail(row.id);
        break;
      default:
        break;
    }
  }

  function showContractDrawer(params: { id: string }) {
    if (props.isContractTab) {
      showDetailDrawer.value = false;
    } else {
      emit('openContractDrawer', {
        id: params.id,
      });
    }
  }

  function showCustomerDrawer(params: { customerId: string; inCustomerPool: boolean; poolId: string }) {
    emit('openCustomerDrawer', {
      customerId: params.customerId,
      inCustomerPool: params.inCustomerPool,
      poolId: params.poolId,
    });
  }

  function showBusinessTitleDetail(businessTitleId: string) {
    emit('openBusinessTitleDrawer', {
      id: businessTitleId,
    });
  }

  const { useTableRes, customFieldsFilterConfig } = await useFormCreateTable({
    formKey: props.isContractTab ? FormDesignKeyEnum.CONTRACT_INVOICE : FormDesignKeyEnum.INVOICE,
    operationColumn: {
      key: 'operation',
      width: currentLocale.value === 'en-US' ? 180 : 150,
      fixed: 'right',
      render: (row: ContractInvoiceItem) =>
        h(CrmOperationButton, {
          groupList: getOperationGroupList(row),
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
    specialRender: {
      name: (row: ContractInvoiceItem) => {
        return h(
          CrmTableButton,
          {
            onClick: () => {
              showDetail(row.id);
            },
          },
          { default: () => row.name, trigger: () => row.name }
        );
      },
      contractId: (row: ContractInvoiceItem) => {
        return props.isContractTab || !hasAnyPermission(['CONTRACT:READ'])
          ? h(
              CrmNameTooltip,
              { text: row.contractName },
              {
                default: () => row.contractName,
              }
            )
          : h(
              CrmTableButton,
              {
                onClick: () => {
                  showContractDrawer({ id: row.contractId });
                },
              },
              { default: () => row.contractName, trigger: () => row.contractName }
            );
      },
      approvalStatus: (row: ContractInvoiceItem) =>
        h(contractInvoiceStatus, {
          status: row.approvalStatus,
        }),
      businessTitleId: (row: ContractInvoiceItem) =>
        hasAnyPermission(['CONTRACT_BUSINESS_TITLE:READ'])
          ? h(
              CrmTableButton,
              {
                onClick: () => {
                  showBusinessTitleDetail(row.businessTitleId);
                },
              },
              { default: () => row.businessTitleName, trigger: () => row.businessTitleName }
            )
          : h(
              CrmNameTooltip,
              { text: row.businessTitleName },
              {
                default: () => row.businessTitleName,
              }
            ),
    },
    permission: ['CONTRACT_INVOICE:EXPORT'],
    containerClass: `.crm-contract-payment-table-${FormDesignKeyEnum.INVOICE}`,
  });
  const { propsRes, propsEvent, tableQueryParams, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;

  const exportColumns = computed<ExportTableColumnItem[]>(() =>
    getExportColumns(propsRes.value.columns, customFieldsFilterConfig.value as FilterFormItem[])
  );

  const exportParams = computed(() => {
    return {
      ...tableQueryParams.value,
      ids: checkedRowKeys.value,
      contractId: props.sourceId,
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
    setLoadListParams({ keyword: val ?? keyword.value, viewId: activeTab.value, contractId: props.sourceId });
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

  onBeforeMount(() => {
    if (props.isContractTab) {
      searchData();
    }
  });

  watch(
    () => activeTab.value,
    (val) => {
      if (val) {
        checkedRowKeys.value = [];
        setLoadListParams({ keyword: keyword.value, viewId: activeTab.value, contractId: props.sourceId });
        crmTableRef.value?.setColumnSort(val);
      }
    }
  );
</script>
