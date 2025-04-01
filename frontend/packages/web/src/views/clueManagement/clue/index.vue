<template>
  <CrmCard no-content-padding hide-footer auto-height class="mb-[16px]">
    <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line" @change="searchData()" />
  </CrmCard>

  <CrmCard hide-footer :special-height="64">
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
          <n-button v-permission="['CLUE_MANAGEMENT:ADD']" class="mr-[12px]" type="primary" @click="handleAddOrEdit">
            {{ t('clueManagement.newClue') }}
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
    <TransferModal
      v-model:show="showTransferModal"
      :source-ids="checkedRowKeys"
      :save-api="batchTransferClue"
      @load-list="handleRefresh"
    />
    <ClueOverviewDrawer v-model:show="showOverviewDrawer" :detail="activeClue" @refresh="handleRefresh" />
    <CrmFormCreateDrawer
      v-model:visible="formCreateDrawerVisible"
      :form-key="formKey"
      :source-id="activeClueId"
      :need-init-detail="needInitDetail"
      :initial-source-name="activeRowName"
      :other-save-params="otherFollowRecordSaveParams"
      @saved="loadList"
    />
    <ToCluePoolResultModel
      v-model:show="showToCluePoolResultModel"
      :fail-count="failCount"
      :success-count="successCount"
    />
  </CrmCard>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { DataTableRowKey, NButton, TabPaneProps, useMessage } from 'naive-ui';

  import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { StageResultEnum } from '@lib/shared/enums/opportunityEnum';
  import type { ClueListItem } from '@lib/shared/models/clue';
  import type { TransferParams } from '@lib/shared/models/customer/index';

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
  import ClueOverviewDrawer from './components/clueOverviewDrawer.vue';
  import ToCluePoolResultModel from './components/toCluePoolResultModel.vue';

  import { batchDeleteClue, batchToCluePool, batchTransferClue, deleteClue, updateClue } from '@/api/modules/clue';
  import { filterConfigList } from '@/config/clue';
  import { defaultTransferForm } from '@/config/opportunity';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();

  const activeTab = ref(CustomerSearchTypeEnum.ALL);
  const tabList = computed<TabPaneProps[]>(() => {
    // TODO lmy 根据不同的用户展示tab
    return [
      {
        name: CustomerSearchTypeEnum.ALL,
        tab: t('clue.allClues'),
      },
      {
        name: CustomerSearchTypeEnum.SELF,
        tab: t('clue.myClues'),
      },
      {
        name: CustomerSearchTypeEnum.DEPARTMENT,
        tab: t('clue.departmentClues'),
      },
      {
        name: CustomerSearchTypeEnum.CUSTOMER_TRANSITION,
        tab: t('clue.convertedToCustomer'),
      },
      {
        name: CustomerSearchTypeEnum.OPPORTUNITY_TRANSITION,
        tab: t('clue.convertedToOpportunity'),
      },
    ];
  });

  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const keyword = ref('');
  const activeClueId = ref('');
  const needInitDetail = ref(false);
  const activeClue = ref<ClueListItem>();
  const formKey = ref(FormDesignKeyEnum.CLUE);
  const formCreateDrawerVisible = ref(false);

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.batchTransfer'),
        key: 'batchTransfer',
        permission: ['CLUE_MANAGEMENT:UPDATE'],
      },
      {
        label: t('clue.moveIntoCluePool'),
        key: 'moveIntoCluePool',
        permission: ['CLUE_MANAGEMENT:RECYCLE'],
      },
      {
        label: t('common.batchDelete'),
        key: 'batchDelete',
        permission: ['CLUE_MANAGEMENT:DELETE'],
      },
    ],
  };

  const tableRefreshId = ref(0);

  function handleRefresh() {
    checkedRowKeys.value = [];
    tableRefreshId.value += 1;
  }

  // 批量移入线索池
  const showToCluePoolResultModel = ref(false);
  const successCount = ref<number>(0);
  const failCount = ref<number>(0);
  function handleBatchMoveIntoCluePool() {
    openModal({
      type: 'default',
      title: t('clue.batchMoveIntoCluePoolTitleTip', { number: checkedRowKeys.value.length }),
      content: t('clue.batchMoveIntoCluePoolContentTip'),
      positiveText: t('common.confirmMoveIn'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          const { success, fail } = await batchToCluePool(checkedRowKeys.value as string[]);
          successCount.value = success;
          failCount.value = fail;
          showToCluePoolResultModel.value = true;
          handleRefresh();
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
          await batchDeleteClue(checkedRowKeys.value as string[]);
          handleRefresh();
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
  function handleDelete(row: ClueListItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: row.name }),
      content: t('clue.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteClue(row.id);
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
  const transferForm = ref<TransferParams>({
    ...defaultTransferForm,
  });

  function handleTransfer(row: ClueListItem) {
    transferFormRef.value?.formRef?.validate(async (error) => {
      if (!error) {
        try {
          transferLoading.value = true;
          await updateClue({
            id: row.id,
            owner: transferForm.value.owner as string,
          });
          Message.success(t('common.transferSuccess'));
          transferForm.value = { ...defaultTransferForm };
          tableRefreshId.value += 1;
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          transferLoading.value = false;
        }
      }
    });
  }

  // 新增
  function handleAddOrEdit() {
    formKey.value = FormDesignKeyEnum.CLUE;
    formCreateDrawerVisible.value = true;
  }

  const otherFollowRecordSaveParams = ref({
    type: 'CLUE',
    clueId: '',
    id: '',
  });
  const activeRowName = ref('');

  function handleActionSelect(row: ClueListItem, actionKey: string) {
    activeClueId.value = row.id;
    switch (actionKey) {
      case 'edit':
        otherFollowRecordSaveParams.value.id = row.id;
        needInitDetail.value = true;
        handleAddOrEdit();
        break;
      case 'followUp':
        formKey.value = FormDesignKeyEnum.FOLLOW_RECORD_CLUE;
        otherFollowRecordSaveParams.value.clueId = row.id;
        activeRowName.value = row.name;
        needInitDetail.value = false;
        formCreateDrawerVisible.value = true;
        break;
      case 'pop-transfer':
        handleTransfer(row);
        break;
      case 'convertToCustomer':
        activeClueId.value = '';
        formKey.value = FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER;
        otherFollowRecordSaveParams.value.clueId = row.id;
        needInitDetail.value = false;
        formCreateDrawerVisible.value = true;
        break;
      case 'convertToOpportunity':
        activeClueId.value = '';
        formKey.value = FormDesignKeyEnum.CLUE_TRANSITION_BUSINESS;
        otherFollowRecordSaveParams.value.clueId = row.id;
        needInitDetail.value = false;
        formCreateDrawerVisible.value = true;
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  // 概览
  const showOverviewDrawer = ref(false);

  const { useTableRes, customFieldsFilterConfig } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.CLUE,
    operationColumn: {
      key: 'operation',
      width: 330,
      fixed: 'right',
      render: (row: ClueListItem) =>
        row.transitionType && ['CUSTOMER', 'OPPORTUNITY'].includes(row.transitionType)
          ? '-'
          : h(
              CrmOperationButton,
              {
                groupList: [
                  ...([StageResultEnum.FAIL, StageResultEnum.SUCCESS].includes(row.stage as StageResultEnum)
                    ? []
                    : [
                        {
                          label: t('common.edit'),
                          key: 'edit',
                          permission: ['CLUE_MANAGEMENT:UPDATE'],
                        },
                        {
                          label: t('opportunity.followUp'),
                          key: 'followUp',
                          permission: ['CLUE_MANAGEMENT:UPDATE'],
                        },
                      ]),
                  {
                    label: t('common.transfer'),
                    key: 'transfer',
                    permission: ['CLUE_MANAGEMENT:UPDATE'],
                    popConfirmProps: {
                      loading: transferLoading.value,
                      title: t('common.transfer'),
                      positiveText: t('common.confirm'),
                      iconType: 'primary',
                    },
                    popSlotContent: 'transferPopContent',
                  },
                  {
                    label: t('clue.convertToCustomer'),
                    key: 'convertToCustomer',
                    permission: ['CLUE_MANAGEMENT:READ', 'CUSTOMER_MANAGEMENT:ADD'],
                  },
                  {
                    label: t('clue.convertToOpportunity'),
                    key: 'convertToOpportunity',
                    permission: ['CLUE_MANAGEMENT:READ', 'OPPORTUNITY_MANAGEMENT:ADD'],
                  },
                  ...([CustomerSearchTypeEnum.DEPARTMENT, CustomerSearchTypeEnum.SELF].includes(activeTab.value) ||
                  [StageResultEnum.FAIL, StageResultEnum.SUCCESS].includes(row.stage as StageResultEnum)
                    ? []
                    : [
                        {
                          label: t('common.delete'),
                          key: 'delete',
                          permission: ['CLUE_MANAGEMENT:DELETE'],
                        },
                      ]),
                ],
                onSelect: (key: string) => handleActionSelect(row, key),
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
      name: (row: ClueListItem) => {
        return h(
          CrmTableButton,
          {
            onClick: () => {
              activeClue.value = row;
              showOverviewDrawer.value = true;
            },
          },
          { default: () => row.name, trigger: () => row.name }
        );
      },
    },
  });
  const { propsRes, propsEvent, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;

  function handleAdvSearch(filter: FilterResult) {
    keyword.value = '';
    setAdvanceFilter(filter);
    loadList();
  }

  function searchData(val?: string) {
    setLoadListParams({ keyword: val ?? keyword.value, searchType: activeTab.value });
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
