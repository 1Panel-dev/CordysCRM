<template>
  <CrmCard no-content-padding hide-footer auto-height class="mb-[16px]">
    <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line" @change="handleTabChange" />
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
          <n-button v-permission="['CUSTOMER_MANAGEMENT:ADD']" class="mr-[12px]" type="primary" @click="handleNewClick">
            {{ t('customer.new') }}
          </n-button>
        </div>
      </template>
      <template #actionRight>
        <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
        <CrmAdvanceFilter
          ref="msAdvanceFilterRef"
          v-model:keyword="keyword"
          :custom-fields-config-list="filterConfigList"
          :filter-config-list="customFieldsFilterConfig"
          @adv-search="handleAdvSearch"
        />
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
    :need-init-detail="needInitDetail"
    :initial-source-name="initialSourceName"
    :other-save-params="otherFollowRecordSaveParams"
    @saved="loadList"
  />
</template>

<script setup lang="ts">
  import { DataTableRowKey, NButton, TabPaneProps, useMessage } from 'naive-ui';

  import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import TransferModal from '@/components/business/crm-transfer-modal/index.vue';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import customerOverviewDrawer from './components/customerOverviewDrawer.vue';

  import {
    batchDeleteCustomer,
    batchMoveCustomer,
    batchTransferCustomer,
    deleteCustomer,
    updateCustomer,
  } from '@/api/modules';
  import { filterConfigList as clueFilterConfigList } from '@/config/clue';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();

  const activeTab = ref(CustomerSearchTypeEnum.ALL);
  const tabList = computed<TabPaneProps[]>(() => {
    return [
      {
        name: CustomerSearchTypeEnum.ALL,
        tab: t('customer.all'),
      },
      {
        name: CustomerSearchTypeEnum.SELF,
        tab: t('customer.mine'),
      },
      {
        name: CustomerSearchTypeEnum.DEPARTMENT,
        tab: t('customer.deptCustomer'),
      },
      {
        name: CustomerSearchTypeEnum.VISIBLE,
        tab: t('customer.cooperationCustomer'),
      },
    ];
  });

  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const keyword = ref('');
  const formCreateDrawerVisible = ref(false);
  const activeSourceId = ref('');
  const initialSourceName = ref('');
  const needInitDetail = ref(false);
  const activeFormKey = ref(FormDesignKeyEnum.CUSTOMER);
  const otherFollowRecordSaveParams = ref({
    type: 'CUSTOMER',
    customerId: '',
    id: '',
  });

  function handleNewClick() {
    activeFormKey.value = FormDesignKeyEnum.CUSTOMER;
    activeSourceId.value = '';
    formCreateDrawerVisible.value = true;
  }

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.batchTransfer'),
        key: 'batchTransfer',
        permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
      },
      {
        label: t('customer.moveToOpenSea'),
        key: 'moveToOpenSea',
        permission: ['CUSTOMER_MANAGEMENT:RECYCLE'],
      },
      {
        label: t('common.batchDelete'),
        key: 'batchDelete',
        permission: ['CUSTOMER_MANAGEMENT:DELETE'],
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
      case 'moveToOpenSea':
        openModal({
          type: 'warning',
          title: t('customer.batchMoveTitleTip', { number: checkedRowKeys.value.length }),
          content: t('customer.batchMoveContentTip'),
          positiveText: t('common.confirmMoveIn'),
          negativeText: t('common.cancel'),
          onPositiveClick: async () => {
            try {
              await batchMoveCustomer(checkedRowKeys.value);
              checkedRowKeys.value = [];
              tableRefreshId.value += 1;
              Message.success(t('common.moveInSuccess'));
            } catch (error) {
              // eslint-disable-next-line no-console
              console.error(error);
            }
          },
        });
        break;
      default:
        break;
    }
  }

  // 删除
  function handleDelete(row: any) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
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
      transferForm.value.owner = null;
      tableRefreshId.value += 1;
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
        needInitDetail.value = true;
        otherFollowRecordSaveParams.value.id = row.id;
        formCreateDrawerVisible.value = true;
        break;
      case 'followUp':
        activeFormKey.value = FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER;
        activeSourceId.value = row.id;
        needInitDetail.value = false;
        initialSourceName.value = row.name;
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
        permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
      },
      {
        label: t('common.edit'),
        key: 'edit',
        permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
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
        permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
      },
      ...(activeTab.value === CustomerSearchTypeEnum.ALL
        ? [
            {
              label: t('common.delete'),
              key: 'delete',
              permission: ['CUSTOMER_MANAGEMENT:DELETE'],
            },
          ]
        : []),
    ];
  });

  // 概览
  const showOverviewDrawer = ref(false);

  const { useTableRes, customFieldsFilterConfig } = await useFormCreateTable({
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
                  transferForm.value.owner = null;
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
          CrmTableButton,
          {
            onClick: () => {
              activeFormKey.value = FormDesignKeyEnum.CUSTOMER;
              activeSourceId.value = row.id;
              showOverviewDrawer.value = true;
            },
          },
          { trigger: () => row.name, default: () => row.name }
        );
      },
    },
  });
  const { propsRes, propsEvent, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;

  const filterConfigList = [
    {
      title: t('common.head'),
      dataIndex: 'ownerName',
      type: FieldTypeEnum.USER_SELECT,
    },
    ...clueFilterConfigList,
  ];
  function handleAdvSearch(filter: FilterResult) {
    keyword.value = '';
    setAdvanceFilter(filter);
    loadList();
  }

  function searchData() {
    setLoadListParams({ keyword: keyword.value, searchType: activeTab.value });
    loadList();
  }

  function handleTabChange() {
    searchData();
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
