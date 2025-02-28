<template>
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
            {{ t('opportunity.createOpportunity') }}
          </n-button>
          <n-button type="primary" ghost class="n-btn-outline-primary">
            {{ t('opportunity.importOpportunity') }}
          </n-button>
        </div>
      </template>
      <template #actionRight>
        <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
      </template>
    </CrmTable>
    <TransferModal
      v-model:show="showTransferModal"
      :opt-ids="checkedRowKeys"
      :module-type="ModuleConfigEnum.BUSINESS_MANAGEMENT"
    />
    <DetailDrawer v-model:show="showDetailModal" />
  </CrmCard>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { DataTableRowKey, NButton, NSwitch, useMessage } from 'naive-ui';

  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig, CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmEditableText from '@/components/business/crm-editable-text/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import DetailDrawer from './components/detailDrawer.vue';
  import TransForm from './components/transferForm.vue';
  import TransferModal from './components/transferModal.vue';

  import { getUserList } from '@/api/modules/system/org';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import { characterLimit } from '@/utils';

  const Message = useMessage();
  const { openModal } = useModal();

  const { t } = useI18n();

  const checkedRowKeys = ref<DataTableRowKey[]>([]);

  const keyword = ref('');

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.batchTransfer'),
        key: 'batchTransfer',
      },
      {
        label: t('common.batchDelete'),
        key: 'batchDelete',
      },
    ],
  };

  const tableRefreshId = ref(0);

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
      case 'batchTransfer':
        handleBatchTransfer();
        break;
      case 'batchDelete':
        handleBatchDelete();
        break;
      default:
        break;
    }
  }
  // TODO 等待联调
  async function updateOpportunityName(row: any, newVal: string) {
    try {
      return Promise.resolve(true);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      return Promise.resolve(false);
    }
  }
  const showDetailModal = ref<boolean>(false);
  function showDetail(id: string) {
    showDetailModal.value = true;
  }

  function showCustomerDetail(id: string) {}

  // TODO 等待联调
  async function handleDisable(row: any) {
    openModal({
      type: 'warning',
      title: t('common.confirmDisabledTitle', { name: characterLimit(row.name) }),
      content: t('opportunity.disabledContentTip'),
      positiveText: t('common.confirmDisable'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          tableRefreshId.value += 1;
          Message.success(t('common.disabled'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }
  // TODO 等待联调
  function handleEnable(row: any) {
    try {
      Message.success(t('common.opened'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  // 编辑
  function handleEdit() {}

  // 跟进
  function handleFollowUp(row: any) {}

  // 删除
  function handleDelete(row: any) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.userName) }),
      content: t('opportunity.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          tableRefreshId.value += 1;
          Message.success(t('common.deleteSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  // TODO 类型
  const form = ref<any>({
    head: null,
  });

  const transferFormRef = ref<InstanceType<typeof TransForm>>();
  const transferLoading = ref(false);

  // 转移
  function handleTransfer(row: any) {
    transferFormRef.value?.formRef?.validate(async (error) => {
      if (!error) {
        try {
          transferLoading.value = true;
          Message.success(t('common.transferSuccess'));
          form.value.head = null;
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        }
      }
    });
  }
  // TODO 等待联调
  function handleActionSelect(row: any, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        handleEdit();
        break;
      case 'followUp':
        handleFollowUp(row);
        break;
      case 'pop-transfer':
        handleTransfer(row);
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  const columns: CrmDataTableColumn[] = [
    {
      type: 'selection',
    },
    {
      title: t('opportunity.name'),
      key: 'name',
      width: 200,
      sortOrder: false,
      sorter: true,
      render: (row: any) => {
        return h(
          CrmEditableText,
          {
            value: row.userName,
            onHandleEdit: (val: string) => {
              updateOpportunityName(row, val);
              row.name = val;
            },
          },
          {
            default: () => {
              return h(
                NButton,
                {
                  text: true,
                  type: 'primary',
                  onClick: () => showDetail(row.id),
                },
                { default: () => row.userName }
              );
            },
          }
        );
      },
    },
    {
      title: t('opportunity.customerName'),
      key: 'customerName',
      width: 200,
      sortOrder: false,
      sorter: true,
      render: (row: any) => {
        return h(
          NButton,
          {
            text: true,
            type: 'primary',
            onClick: () => showCustomerDetail(row.customerId),
          },
          { default: () => row.name }
        );
      },
    },
    {
      title: t('common.status'),
      key: 'enable',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      filterOptions: [
        {
          label: t('common.enable'),
          value: 1,
        },
        {
          label: t('common.disable'),
          value: 0,
        },
      ],
      filter: true,
      render: (row: any) => {
        return h(NSwitch, {
          value: row.enable,
          onClick: () => {
            if (row.enable) {
              handleDisable(row);
            } else {
              handleEnable(row);
            }
          },
        });
      },
    },
    // TODO 自定义
    // {
    //   title: t('opportunity.stage'),
    //   key: 'stage',
    //   ellipsis: {
    //     tooltip: true,
    //   },
    //   width: 100,
    //   showInTable: false,
    // },
    // {
    //   title: t('opportunity.source'),
    //   key: 'source',
    //   ellipsis: {
    //     tooltip: true,
    //   },
    //   width: 100,
    // },
    {
      title: t('opportunity.contact'),
      key: 'source',
      ellipsis: {
        tooltip: true,
      },
      width: 100,
    },
    {
      title: t('common.phoneNumber'),
      key: 'source',
      ellipsis: {
        tooltip: true,
      },
      width: 100,
    },
    {
      title: t('common.head'),
      key: 'head',
      ellipsis: {
        tooltip: true,
      },
      width: 100,
    },
    {
      title: t('opportunity.department'),
      key: 'departmentName',
      ellipsis: {
        tooltip: true,
      },
      width: 100,
    },

    // TODO 自定义
    // {
    //   title: t('opportunity.region'),
    //   key: 'region',
    //   ellipsis: {
    //     tooltip: true,
    //   },
    //   width: 100,
    // },
    {
      title: t('opportunity.belongDays'),
      key: 'belongDays',
      ellipsis: {
        tooltip: true,
      },
      width: 100,
    },
    {
      title: t('opportunity.remainingBelong'),
      key: 'remainingBelong',
      ellipsis: {
        tooltip: true,
      },
      width: 100,
      showInTable: false,
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    {
      title: t('common.creator'),
      key: 'createUser',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      render: (row: any) => {
        return h(CrmNameTooltip, { text: row.createUserName });
      },
      showInTable: false,
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUser',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      render: (row: any) => {
        return h(CrmNameTooltip, { text: row.updateUserName });
      },
    },
    {
      key: 'operation',
      width: 180,
      fixed: 'right',
      render: (row: any) =>
        h(
          CrmOperationButton,
          {
            groupList: [
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
                label: t('common.delete'),
                key: 'delete',
              },
            ],
            onSelect: (key: string) => handleActionSelect(row, key),
            onCancel: () => {
              form.value.head = null;
            },
          },
          {
            transferPopContent: () => {
              return h(TransForm, {
                class: 'w-[320px] mt-[16px]',
                form: form.value,
                ref: transferFormRef,
                moduleType: ModuleConfigEnum.BUSINESS_MANAGEMENT,
              });
            },
          }
        ),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(
    getUserList,
    {
      tableKey: TableKeyEnum.OPPORTUNITY_LIST,
      showSetting: true,
      columns,
      scrollX: 2000,
    },
    (row: any) => {
      return {
        ...row,
        departmentName: row.departmentName || '-',
        phone: row.phone || '-',
      };
    }
  );

  function initData() {
    // TODO 等待联调
    setLoadListParams({
      departmentIds: ['101256012006162432'],
    });
    loadList();
  }

  function searchData(val: string) {
    keyword.value = val;
    initData();
  }

  onBeforeMount(() => {
    initData();
  });
</script>

<style scoped></style>
