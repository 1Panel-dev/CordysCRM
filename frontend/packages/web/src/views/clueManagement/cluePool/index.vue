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
        <n-button class="mr-[12px]" type="primary" @click="formCreateDrawerVisible = true">
          {{ t('clueManagement.newClue') }}
        </n-button>
        <!-- TODO 联调换接口 -->
        <CrmImportButton
          :validate-api="importUserPreCheck"
          :import-save-api="importUsers"
          :title="t('clue.importClues')"
          :button-text="t('clue.importClues')"
          @import-success="() => searchData()"
        />
      </template>
      <template #actionRight>
        <div class="flex gap-[12px]">
          <n-select v-model:value="cluePool" :options="cluePoolOptions" class="w-[240px]" />
          <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
        </div>
      </template>
    </CrmTable>
  </CrmCard>
  <CrmFormCreateDrawer
    v-model:visible="formCreateDrawerVisible"
    :form-key="FormDesignKeyEnum.CLUE"
    :source-id="activeClueId"
  />
  <!-- TODO 联调换接口 -->
  <TransferModal
    v-model:show="showDistributeModal"
    :source-ids="checkedRowKeys"
    :title="t('common.batchDistribute')"
    :save-api="batchTransferClue"
  />
  <CluePoolOverviewDrawer v-model:show="showOverviewDrawer" :detail="activeClue" @refresh="handleRefresh" />
</template>

<script setup lang="ts">
  import { DataTableRowKey, NButton, NSelect, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import type { TransferParams } from '@lib/shared/models/customer/index';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmImportButton from '@/components/business/crm-import-button/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import TransferModal from '@/components/business/crm-transfer-modal/index.vue';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import CluePoolOverviewDrawer from './components/cluePoolOverviewDrawer.vue';

  import { batchTransferClue } from '@/api/modules/clue';
  import { importUserPreCheck, importUsers } from '@/api/modules/system/org';
  import { defaultTransferForm } from '@/config/opportunity';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';

  import { SelectMixedOption } from 'naive-ui/es/select/src/interface';

  const { t } = useI18n();
  const { openModal } = useModal();
  const Message = useMessage();

  const cluePool = ref('');
  // TODO
  const cluePoolOptions = ref<SelectMixedOption[]>([
    {
      label: 'GGBOND',
      value: '1',
      id: '1',
      createUser: '1',
      updateUser: 'string;',
      updateUserName: 'string;',
      createTime: 'number;',
      updateTime: 'number;',
      name: 'string;',
      scopeId: ' string;',
      organizationId: 'string;',
      ownerId: 'string;',
      enable: 'boolean;',
      auto: 'boolean;',
      members: [],
      owners: [],
      pickRule: {}, // 领取规则
      recycleRule: {}, // 回收规则
    },
  ]);

  const keyword = ref('');
  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const showOverviewDrawer = ref(true);

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.batchClaim'),
        key: 'batchClaim',
      },
      {
        label: t('customer.batchDistribute'),
        key: 'batchDistribute',
      },
      {
        label: t('common.batchDelete'),
        key: 'batchDelete',
      },
    ],
  };

  const tableRefreshId = ref(0);

  function handleRefresh() {
    checkedRowKeys.value = [];
    tableRefreshId.value += 1;
  }

  // 批量领取
  function handleBatchClaim() {
    openModal({
      type: 'info',
      title: t('clue.batchClaimTip', { number: checkedRowKeys.value.length }),
      positiveText: t('common.confirmClaim'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          // TODO  联调
          handleRefresh();
          Message.success(t('common.claimSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 批量分配
  const showDistributeModal = ref<boolean>(false);

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
          // TODO  联调
          handleRefresh();
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
      case 'batchClaim':
        handleBatchClaim();
        break;
      case 'batchDistribute':
        showDistributeModal.value = true;
        break;
      case 'batchDelete':
        handleBatchDelete();
        break;
      default:
        break;
    }
  }

  const claimLoading = ref(false);
  const distributeLoading = ref(false);

  const operationGroupList = computed<ActionsItem[]>(() => {
    return [
      {
        label: t('common.edit'),
        key: 'edit',
      },
      {
        label: t('common.claim'),
        key: 'claim',
        popConfirmProps: {
          loading: claimLoading.value,
          title: t('clue.claimTip'),
          positiveText: t('common.claim'),
          iconType: 'primary',
        },
      },
      {
        label: t('common.distribute'),
        key: 'distribute',
        popConfirmProps: {
          loading: distributeLoading.value,
          title: t('common.distribute'),
          positiveText: t('common.confirm'),
          iconType: 'primary',
        },
        popSlotContent: 'distributePopContent',
      },
      {
        label: t('common.delete'),
        key: 'delete',
      },
    ];
  });

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
          // TODO 联调
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  const distributeFormRef = ref<InstanceType<typeof TransferForm>>();
  const distributeForm = ref<TransferParams>({
    ...defaultTransferForm,
  });
  function handleDistribute(row: any) {
    distributeFormRef.value?.formRef?.validate(async (error) => {
      if (!error) {
        try {
          distributeLoading.value = true;
          // TODO 联调换接口
          await batchTransferClue({
            ...distributeForm.value,
            ids: [row.id],
          });
          Message.success(t('common.distributeSuccess'));
          distributeForm.value = { ...defaultTransferForm };
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          distributeLoading.value = false;
        }
      }
    });
  }

  const activeClueId = ref('');
  const formCreateDrawerVisible = ref(false);

  function handleActionSelect(row: any, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        activeClueId.value = row.id;
        formCreateDrawerVisible.value = true;
        break;
      case 'pop-claim':
        tableRefreshId.value += 1;
        break;
      case 'pop-distribute':
        handleDistribute(row);
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  const activeClue = ref<any>();
  const { useTableRes } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.CLUE,
    operationColumn: {
      key: 'operation',
      width: 200,
      fixed: 'right',
      render: (row: any) =>
        h(
          CrmOperationButton,
          {
            groupList: operationGroupList.value,
            onSelect: (key: string) => handleActionSelect(row, key),
            onCancel: () => {
              distributeForm.value = { ...defaultTransferForm };
            },
          },
          {
            distributePopContent: () => {
              return h(TransferForm, {
                class: 'w-[320px] mt-[16px]',
                form: distributeForm.value,
                ref: distributeFormRef,
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
              activeClue.value = row;
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
