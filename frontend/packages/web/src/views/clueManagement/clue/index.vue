<template>
  <CrmCard no-content-padding hide-footer auto-height class="mb-[16px]">
    <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line" />
  </CrmCard>

  <CrmCard hide-footer>
    <CrmTable
      v-model:checked-row-keys="checkedRowKeys"
      v-bind="propsRes"
      :action-config="actionConfig"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
      @batch-action="handleBatchAction"
    >
      <template #actionLeft>
        <div class="flex items-center">
          <n-button class="mr-[12px]" type="primary">
            {{ t('clueManagement.newClue') }}
          </n-button>
        </div>
      </template>
      <template #actionRight>
        <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
      </template>
    </CrmTable>
    <TransferModal v-model:show="showTransferModal" :source-ids="checkedRowKeys" />
    <ClueOverviewDrawer v-model:show="showOverviewDrawer" />
  </CrmCard>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { DataTableRowKey, NButton, TabPaneProps, useMessage } from 'naive-ui';

  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig, CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import TransferModal from '@/components/business/crm-transfer-modal/index.vue';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import ClueOverviewDrawer from './components/clueOverviewDrawer.vue';

  import { getCluePoolPage } from '@/api/modules/system/module';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();

  const activeTab = ref('allClues');
  const tabList = computed<TabPaneProps[]>(() => {
    // TODO lmy 根据不同的用户展示tab
    return [
      {
        name: 'allClues',
        tab: t('clue.allClues'),
      },
      {
        name: 'myClues',
        tab: t('clue.myClues'),
      },
      {
        name: 'departmentClues',
        tab: t('clue.departmentClues'),
      },
      {
        name: 'convertedToCustomer',
        tab: t('clue.convertedToCustomer'),
      },
      {
        name: 'convertedToOpportunity',
        tab: t('clue.convertedToOpportunity'),
      },
    ];
  });

  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const keyword = ref('');

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.batchTransfer'),
        key: 'batchTransfer',
      },
      {
        label: t('clue.moveIntoCluePool'),
        key: 'moveIntoCluePool',
      },
      {
        label: t('common.batchDelete'),
        key: 'batchDelete',
      },
    ],
  };

  const tableRefreshId = ref(0);

  // 批量移入线索池
  function handleBatchMoveIntoCluePool() {
    openModal({
      type: 'warning',
      title: t('clue.batchMoveIntoCluePoolTitleTip', { number: checkedRowKeys.value.length }),
      content: t('clue.batchMoveIntoCluePoolContentTip'),
      positiveText: t('common.confirmMoveIn'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          // TODO lmy 联调
          tableRefreshId.value += 1;
          Message.success(t('common.moveInSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 批量删除
  function handleBatchDelete() {
    openModal({
      type: 'error',
      title: t('clue.batchDeleteTitleTip', { number: checkedRowKeys.value.length }),
      content: t('clue.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          // TODO lmy 联调
          tableRefreshId.value += 1;
          Message.success(t('common.deleteSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 批量转移
  const showTransferModal = ref<boolean>(false);

  function handleBatchAction(item: ActionsItem) {
    switch (item.key) {
      case 'batchTransfer':
        showTransferModal.value = true;
        break;
      case 'moveIntoCluePool':
        handleBatchMoveIntoCluePool();
        break;
      case 'batchDelete':
        handleBatchDelete();
        break;
      default:
        break;
    }
  }

  // 删除
  function handleDelete(row: any) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: row.name }),
      content: t('clue.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          // TODO lmy 联调
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 转移
  const transferFormRef = ref<InstanceType<typeof TransferForm>>();
  const transferLoading = ref(false);
  const transferForm = ref<any>({
    head: null, // TODO lmy 字段
    belongToPublicPool: null,
  });

  function handleActionSelect(row: any, actionKey: string) {
    // TODO lmy
    switch (actionKey) {
      case 'edit':
        break;
      case 'followUp':
        break;
      case 'pop-transfer':
        break;
      case 'convertToCustomer':
        break;
      case 'convertToOpportunity':
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  const operationGroupList = computed<ActionsItem[]>(() => {
    return [
      {
        label: t('common.edit'),
        key: 'edit',
      },
      {
        label: t('opportunity.followUp'),
        key: 'followUp',
      },
      {
        label: t('common.transfer'),
        key: 'transfer',
        popConfirmProps: {
          loading: transferLoading.value,
          title: t('common.transfer'),
          positiveText: t('common.confirm'),
          iconType: 'primary',
        },
        popSlotName: 'transferPopTitle',
        popSlotContent: 'transferPopContent',
      },
      {
        label: t('clue.convertToCustomer'),
        key: 'convertToCustomer',
      },
      {
        label: t('clue.convertToOpportunity'),
        key: 'convertToOpportunity',
      },
      ...(['departmentClues', 'myClues'].includes(activeTab.value)
        ? []
        : [
            {
              label: t('common.delete'),
              key: 'delete',
            },
          ]),
    ];
  });

  // 概览
  const showOverviewDrawer = ref(true);

  const columns: CrmDataTableColumn[] = [
    {
      type: 'selection',
      // TODO lmy 已转【客户】【商机】复选框为禁用态
      disabled(row: any) {
        return ['客户', '商机'].includes(row.convertedTo);
      },
    },
    {
      title: t('opportunity.customerName'),
      key: 'name',
      width: 100,
      sortOrder: false,
      sorter: true,
      render: (row: any) => {
        return h(
          NButton,
          {
            text: true,
            type: 'primary',
            onClick: () => {
              // TODO lmy 概览
            },
          },
          { default: () => row.name }
        );
      },
    },
    // TODO lmy 有是自定义字段
    {
      title: t('clue.clueProgress'),
      key: 'clueProgress',
      width: 100,
      filterOptions: [],
      filter: true,
    },
    {
      title: t('clue.convertedTo'),
      key: 'convertedTo',
      width: 100,
      filterOptions: [],
      filter: true,
    },
    {
      title: t('clue.clueSource'),
      key: 'clueSource',
      width: 100,
      filterOptions: [],
      filter: true,
    },
    {
      title: t('common.head'),
      key: 'head',
      ellipsis: {
        tooltip: true,
      },
      width: 100,
      filterOptions: [],
      filter: true,
    },
    {
      title: t('role.department'),
      key: 'departmentName',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      filterOptions: [],
      filter: true,
    },
    {
      title: t('opportunity.region'),
      key: 'region',
      ellipsis: {
        tooltip: true,
      },
      width: 100,
      filterOptions: [],
      filter: true,
    },
    {
      title: t('clue.remainingDays'),
      key: 'remainingDays',
      width: 100,
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUser',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      filterOptions: [],
      filter: true,
      render: (row: any) => {
        return h(CrmNameTooltip, { text: row.updateUserName });
      },
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
    {
      key: 'operation',
      width: 200,
      fixed: 'right',
      render: (row: any) =>
        ['convertedToCustomer', 'convertedToOpportunity'].includes(activeTab.value)
          ? '-'
          : h(
              CrmOperationButton,
              {
                groupList: operationGroupList.value,
                onSelect: (key: string) => handleActionSelect(row, key),
                onCancel: () => {
                  transferForm.value.head = null; // TODO lmy 字段
                  transferForm.value.belongToPublicPool = null;
                },
              },
              {
                transferPopContent: () => {
                  return h(TransferForm, {
                    class: 'w-[320px] mt-[16px]',
                    form: transferForm.value,
                    ref: transferFormRef,
                    moduleType: ModuleConfigEnum.CLUE_MANAGEMENT,
                  });
                },
              }
            ),
    },
  ];

  // TODO 联调
  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(getCluePoolPage, {
    tableKey: TableKeyEnum.AUTH,
    showSetting: true,
    columns,
    scrollX: 2000,
  });

  function searchData() {
    setLoadListParams({ keyword: keyword.value });
    loadList();
  }

  watch(
    () => tableRefreshId.value,
    () => {
      loadList();
    }
  );

  onMounted(() => {
    searchData();
  });
</script>

<style lang="less" scoped>
  :deep(.n-tabs-scroll-padding) {
    width: 16px !important;
  }
</style>
