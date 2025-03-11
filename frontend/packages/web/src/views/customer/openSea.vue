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
        <n-select v-model:value="openSea" :options="openSeaOptions" :render-option="renderOption" class="w-[240px]" />
      </template>
      <template #actionRight>
        <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
      </template>
    </CrmTable>
  </CrmCard>
  <addOrEditPoolDrawer v-model:visible="drawerVisible" :type="ModuleConfigEnum.CUSTOMER_MANAGEMENT" :row="openSeaRow" />
  <openSeaOverviewDrawer v-model:show="showOverviewDrawer" :source-id="activeCustomerId" />
  <TransferModal v-model:show="showDistributeModal" :source-ids="checkedRowKeys" :title="t('common.batchDistribute')" />
</template>

<script setup lang="ts">
  import { VNodeChild } from 'vue';
  import { DataTableRowKey, NButton, NSelect, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import TransferModal from '@/components/business/crm-transfer-modal/index.vue';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import openSeaOverviewDrawer from './components/openSeaOverviewDrawer.vue';
  import addOrEditPoolDrawer from '@/views/system/module/components/addOrEditPoolDrawer.vue';

  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';

  import { SelectMixedOption, SelectOption } from 'naive-ui/es/select/src/interface';

  const { t } = useI18n();
  const { openModal } = useModal();
  const Message = useMessage();

  const openSea = ref('');
  const openSeaOptions = ref<SelectMixedOption[]>([
    {
      label: 'GGBOND',
      value: '1',
    },
  ]);
  const keyword = ref('');
  const drawerVisible = ref(false);
  const openSeaRow = ref<any>({});
  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const activeCustomerId = ref('');
  const showOverviewDrawer = ref(true);

  function renderOption({ node, option }: { node: VNode; option: SelectOption }): VNodeChild {
    (node.children as Array<VNode>)?.push(
      h(CrmIcon, {
        type: 'iconicon_set_up',
        class: 'cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-8)]',
        onClick: (e: Event) => {
          e.stopPropagation();
        },
      })
    );
    return h(node);
  }

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

  // 批量领取
  function handleBatchClaim() {
    openModal({
      type: 'info',
      title: t('customer.batchClaimTip', { number: checkedRowKeys.value.length }),
      content: t('customer.claimTipContent'),
      positiveText: t('common.confirmClaim'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          // TODO  联调
          tableRefreshId.value += 1;
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
      title: t('customer.batchDeleteTitleTip', { number: checkedRowKeys.value.length }),
      content: t('customer.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          // TODO  联调
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
  const distributeFormRef = ref<InstanceType<typeof TransferForm>>();
  const distributeForm = ref<any>({
    head: null,
  });

  const operationGroupList = computed<ActionsItem[]>(() => {
    return [
      {
        label: t('common.claim'),
        key: 'claim',
        popConfirmProps: {
          loading: claimLoading.value,
          title: t('customer.claimTip'),
          content: t('customer.claimTipContent'),
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
      content: t('customer.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          // TODO: 联调
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleActionSelect(row: any, actionKey: string) {
    // TODO:
    switch (actionKey) {
      case 'claim':
        break;
      case 'distribute':
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  const { useTableRes } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.CUSTOMER,
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
              distributeForm.value.head = null;
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
              activeCustomerId.value = row.id;
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

<style lang="less">
  .n-base-select-option {
    @apply justify-between;
  }
</style>
