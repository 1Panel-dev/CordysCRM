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
          <n-button class="mr-[12px]" type="primary" @click="formCreateDrawerVisible = true">
            {{ t('opportunity.createOpportunity') }}
          </n-button>
          <!-- TODO 等待联调 -->
          <CrmImportButton
            :validate-api="importUserPreCheck"
            :import-save-api="importUsers"
            :title="t('opportunity.importOpportunity')"
            :button-text="t('opportunity.importOpportunity')"
            @import-success="() => searchData()"
          />
        </div>
      </template>
      <template #actionRight>
        <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
      </template>
    </CrmTable>
    <TransferModal
      v-model:show="showTransferModal"
      :is-batch="true"
      :source-ids="checkedRowKeys"
      :save-api="transferOpt"
    />
    <OptOverviewDrawer v-model:show="showOverviewDrawer" :source-id="activeOpportunityId" />
    <CrmFormCreateDrawer
      v-model:visible="formCreateDrawerVisible"
      :form-key="realFormKey"
      :source-id="activeOpportunityId"
    />
  </CrmCard>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { DataTableRowKey, NButton, TabPaneProps, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import type { TransferParams } from '@lib/shared/models/customer/index';
  import type { OpportunityItem } from '@lib/shared/models/opportunity';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmImportButton from '@/components/business/crm-import-button/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import TransferModal from '@/components/business/crm-transfer-modal/index.vue';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import OptOverviewDrawer from './components/optOverviewDrawer.vue';

  import { batchDeleteOpt, deleteOpt, transferOpt } from '@/api/modules/opportunity';
  import { importUserPreCheck, importUsers } from '@/api/modules/system/org';
  import { defaultTransferForm } from '@/config/opportunity';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
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
  const activeTab = ref('all');

  const tabList = computed<TabPaneProps[]>(() => {
    // TODO 根据不同的用户展示tab
    return [
      {
        name: 'all',
        tab: t('opportunity.allOpportunities'),
      },
      {
        name: 'my',
        tab: t('opportunity.myOpportunities'),
      },
      {
        name: 'department',
        tab: t('opportunity.departmentOpportunities'),
      },
      {
        name: 'converted',
        tab: t('opportunity.convertedOpportunities'),
      },
    ];
  });

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
          tableRefreshId.value += 1;
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

  const showOverviewDrawer = ref<boolean>(false);
  const activeOpportunityId = ref('');
  const formCreateDrawerVisible = ref(false);
  const realFormKey = ref<FormDesignKeyEnum>(FormDesignKeyEnum.BUSINESS);

  // TODO
  function showCustomerDetail(id: string) {
    activeOpportunityId.value = id;
  }

  // 编辑
  function handleEdit(id: string) {
    activeOpportunityId.value = id;
    realFormKey.value = FormDesignKeyEnum.BUSINESS;
    formCreateDrawerVisible.value = true;
  }

  // 跟进
  function handleFollowUp() {
    activeOpportunityId.value = '';
    realFormKey.value = FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS;
    formCreateDrawerVisible.value = true;
  }

  // 删除
  function handleDelete(row: OpportunityItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.opportunityName) }),
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
  function handleTransfer(row: OpportunityItem) {
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
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          transferLoading.value = false;
        }
      }
    });
  }

  function handleActionSelect(row: OpportunityItem, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        handleEdit(row.id);
        break;
      case 'followUp':
        handleFollowUp();
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

  // TODO :
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
        popSlotContent: 'transferPopContent',
      },
      {
        label: t('common.delete'),
        key: 'delete',
      },
    ];
  });

  const { useTableRes } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.BUSINESS,
    operationColumn: {
      key: 'operation',
      width: 200,
      fixed: 'right',
      render: (row: OpportunityItem) =>
        h(
          CrmOperationButton,
          {
            groupList: operationGroupList.value,
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
      name: (row: OpportunityItem) => {
        return h(
          NButton,
          {
            text: true,
            type: 'primary',
            onClick: () => {
              activeOpportunityId.value = row.id;
              showOverviewDrawer.value = true;
            },
          },
          { default: () => row.opportunityName }
        );
      },
      customerName: (row: OpportunityItem) => {
        return h(
          NButton,
          {
            text: true,
            type: 'primary',
            onClick: () => showCustomerDetail(row.id),
          },
          { default: () => row.customerName }
        );
      },
    },
  });
  const { propsRes, propsEvent, loadList, setLoadListParams } = useTableRes;

  function searchData() {
    setLoadListParams({
      keyword: keyword.value,
    });
    loadList();
  }

  watch(
    () => tableRefreshId.value,
    () => {
      searchData();
    }
  );

  onBeforeMount(() => {
    searchData();
  });
</script>

<style scoped></style>
