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
      :source-ids="checkedRowKeys"
      :module-type="ModuleConfigEnum.BUSINESS_MANAGEMENT"
    />
    <OptOverviewDrawer v-model:show="showOverviewDrawer" />
    <CrmFormCreateDrawer
      v-model:visible="formCreateDrawerVisible"
      :title="t('opportunity.new')"
      :form-key="FormDesignKeyEnum.BUSINESS"
      :source-id="activeOpportunityId"
    />
  </CrmCard>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { DataTableRowKey, NButton, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import type { OpportunityItem } from '@lib/shared/models/opportunity';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmImportButton from '@/components/business/crm-import-button/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import TransferModal from '@/components/business/crm-transfer-modal/index.vue';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import OptOverviewDrawer from './components/optOverviewDrawer.vue';

  import { importUserPreCheck, importUsers } from '@/api/modules/system/org';
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

  const showOverviewDrawer = ref<boolean>(false);
  const activeOpportunityId = ref('');
  const formCreateDrawerVisible = ref(false);

  // TODO
  function showCustomerDetail(id: string) {
    activeOpportunityId.value = id;
  }

  // 编辑
  function handleEdit() {}

  // 跟进
  function handleFollowUp(row: OpportunityItem) {}

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
  const transferForm = ref<any>({
    head: null,
  });

  const transferFormRef = ref<InstanceType<typeof TransferForm>>();
  const transferLoading = ref(false);

  // 转移
  function handleTransfer(row: OpportunityItem) {
    transferFormRef.value?.formRef?.validate(async (error) => {
      if (!error) {
        try {
          transferLoading.value = true;
          Message.success(t('common.transferSuccess'));
          transferForm.value.head = null;
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        }
      }
    });
  }

  // TODO 等待联调
  function handleActionSelect(row: OpportunityItem, actionKey: string) {
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
        popSlotName: 'transferPopTitle',
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
              transferForm.value.head = null;
            },
          },
          {
            transferPopContent: () => {
              return h(TransferForm, {
                class: 'w-[320px] mt-[16px]',
                form: transferForm.value,
                ref: transferFormRef,
                moduleType: ModuleConfigEnum.BUSINESS_MANAGEMENT,
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
