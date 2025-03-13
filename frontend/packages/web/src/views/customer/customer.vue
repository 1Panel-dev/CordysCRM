<template>
  <CrmCard no-content-padding hide-footer auto-height class="mb-[16px]">
    <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line" />
  </CrmCard>

  <CrmCard :special-height="64" hide-footer>
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
          <n-button class="mr-[12px]" type="primary" @click="handleNewClick">
            {{ t('customer.new') }}
          </n-button>
        </div>
      </template>
      <template #actionRight>
        <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
      </template>
    </CrmTable>
  </CrmCard>
  <TransferModal
    v-model:show="showTransferModal"
    :source-ids="checkedRowKeys"
    :save-api="batchTransferCustomer"
    @load-list="loadList"
  />
  <customerOverviewDrawer v-model:show="showOverviewDrawer" :source-id="activeSourceId" />
  <CrmFormCreateDrawer
    v-model:visible="formCreateDrawerVisible"
    :form-key="activeFormKey"
    :source-id="activeSourceId"
    :other-save-params="otherFollowRecordSaveParams"
    @saved="loadList"
  />
</template>

<script setup lang="ts">
  import { DataTableRowKey, NButton, TabPaneProps, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import TransferModal from '@/components/business/crm-transfer-modal/index.vue';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import customerOverviewDrawer from './components/customerOverviewDrawer.vue';

  import { batchDeleteCustomer, batchTransferCustomer, deleteCustomer, updateCustomer } from '@/api/modules/customer';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();

  const activeTab = ref('all');
  const tabList = computed<TabPaneProps[]>(() => {
    return [
      {
        name: 'all',
        tab: t('customer.all'),
      },
      {
        name: 'mine',
        tab: t('customer.mine'),
      },
      {
        name: 'deptCustomer',
        tab: t('customer.deptCustomer'),
      },
      {
        name: 'cooperationCustomer',
        tab: t('customer.cooperationCustomer'),
      },
    ];
  });

  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const keyword = ref('');
  const formCreateDrawerVisible = ref(false);
  const activeSourceId = ref('');
  const activeFormKey = ref(FormDesignKeyEnum.CUSTOMER);
  const otherFollowRecordSaveParams = ref({
    type: 'CUSTOMER',
    customerId: '',
    id: '',
  });

  function handleNewClick() {
    activeFormKey.value = FormDesignKeyEnum.CUSTOMER;
    formCreateDrawerVisible.value = true;
  }

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.batchTransfer'),
        key: 'batchTransfer',
      },
      {
        label: t('customer.moveToOpenSea'),
        key: 'moveToOpenSea',
      },
      {
        label: t('common.batchDelete'),
        key: 'batchDelete',
      },
    ],
  };

  const tableRefreshId = ref(0);

  // 批量删除
  function handleBatchDelete() {
    openModal({
      type: 'error',
      title: t('customer.batchDeleteTitleTip', { number: checkedRowKeys.value.length }),
      content: t('customer.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await batchDeleteCustomer(checkedRowKeys.value);
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
      content: t('customer.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteCustomer(row.id);
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
    owner: null,
  });

  async function transferCustomer() {
    try {
      transferLoading.value = true;
      await updateCustomer({
        id: activeSourceId.value,
        owner: transferForm.value.owner,
      });
      Message.success(t('common.transferSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      transferLoading.value = false;
    }
  }

  function handleActionSelect(row: any, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        activeFormKey.value = FormDesignKeyEnum.CUSTOMER;
        activeSourceId.value = row.id;
        otherFollowRecordSaveParams.value.id = row.id;
        formCreateDrawerVisible.value = true;
        break;
      case 'followUp':
        activeFormKey.value = FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER;
        activeSourceId.value = '';
        otherFollowRecordSaveParams.value.customerId = row.id;
        formCreateDrawerVisible.value = true;
        break;
      case 'pop-transfer':
        activeSourceId.value = row.id;
        transferCustomer();
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
        label: t('opportunity.followUp'),
        key: 'followUp',
      },
      {
        label: t('common.edit'),
        key: 'edit',
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
      ...(activeTab.value === 'all'
        ? [
            {
              label: t('common.delete'),
              key: 'delete',
            },
          ]
        : []),
    ];
  });

  // 概览
  const showOverviewDrawer = ref(false);

  const { useTableRes } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.CUSTOMER,
    operationColumn: {
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
                  transferForm.value.owner = null; // TODO lmy 字段
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
    specialRender: {
      name: (row: any) => {
        return h(
          NButton,
          {
            text: true,
            type: 'primary',
            onClick: () => {
              activeFormKey.value = FormDesignKeyEnum.CUSTOMER;
              activeSourceId.value = row.id;
              showOverviewDrawer.value = true;
            },
          },
          { default: () => row.name }
        );
      },
    },
  });
  const { propsRes, propsEvent, loadList, setLoadListParams } = useTableRes;

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
