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
        <!-- 先不上 -->
        <!-- <CrmImportButton
          :validate-api="importUserPreCheck"
          :import-save-api="importUsers"
          :title="t('clue.importClues')"
          :button-text="t('clue.importClues')"
          @import-success="() => searchData()"
        /> -->
      </template>
      <template #actionRight>
        <div class="flex gap-[12px]">
          <n-select
            v-model:value="poolId"
            :options="cluePoolOptions"
            value-field="id"
            :render-option="renderOption"
            :show-checkmark="false"
            label-field="name"
            class="w-[240px]"
            @update-value="(e) => searchData(undefined, e)"
          />
          <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
          <CrmAdvanceFilter
            ref="msAdvanceFilterRef"
            v-model:keyword="keyword"
            :custom-fields-config-list="filterConfigList"
            :filter-config-list="customFieldsFilterConfig"
            @adv-search="handleAdvSearch"
          />
        </div>
      </template>
    </CrmTable>
  </CrmCard>
  <TransferModal
    v-model:show="showDistributeModal"
    :source-ids="checkedRowKeys"
    :title="t('common.batchDistribute')"
    :positive-text="t('common.distribute')"
    @confirm="handleBatchAssign"
  />
  <CluePoolOverviewDrawer
    v-model:show="showOverviewDrawer"
    :pool-id="poolId"
    :detail="activeClue"
    @refresh="handleRefresh"
  />
  <addOrEditPoolDrawer
    v-model:visible="drawerVisible"
    :type="ModuleConfigEnum.CLUE_MANAGEMENT"
    :row="cluePoolRow"
    @refresh="init"
  />
</template>

<script setup lang="ts">
  import { VNodeChild } from 'vue';
  import { DataTableRowKey, NSelect, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CluePoolListItem } from '@lib/shared/models/clue';
  import type { TransferParams } from '@lib/shared/models/customer/index';
  import type { CluePoolItem } from '@lib/shared/models/system/module';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  // import CrmImportButton from '@/components/business/crm-import-button/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import TransferModal from '@/components/business/crm-transfer-modal/index.vue';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import CluePoolOverviewDrawer from './components/cluePoolOverviewDrawer.vue';
  import addOrEditPoolDrawer from '@/views/system/module/components/addOrEditPoolDrawer.vue';

  import {
    assignClue,
    batchAssignClue,
    batchDeleteCluePool,
    batchPickClue,
    deleteCluePool,
    getPoolOptions,
    pickClue,
  } from '@/api/modules';
  import { filterConfigList } from '@/config/clue';
  import { defaultTransferForm } from '@/config/opportunity';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';

  import { SelectOption } from 'naive-ui/es/select/src/interface';

  const { t } = useI18n();
  const { openModal } = useModal();
  const Message = useMessage();

  const poolId = ref('');
  const cluePoolOptions = ref<CluePoolItem[]>([]);
  const cluePoolRow = ref<any>({});
  const drawerVisible = ref(false);

  function renderOption({ node, option }: { node: VNode; option: SelectOption }): VNodeChild {
    if (option.editable) {
      (node.children as Array<VNode>)?.push(
        h(CrmIcon, {
          type: 'iconicon_set_up',
          class: 'openSea-setting-icon',
          onClick: (e: Event) => {
            e.stopPropagation();
            cluePoolRow.value = { ...option };
            drawerVisible.value = true;
          },
        })
      );
    }
    return h(node);
  }

  async function getCluePoolOptions() {
    try {
      cluePoolOptions.value = await getPoolOptions();
      poolId.value = cluePoolOptions.value[0]?.id || '';
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  const keyword = ref('');
  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const showOverviewDrawer = ref(false);

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.batchClaim'),
        key: 'batchClaim',
        permission: ['CLUE_MANAGEMENT_POOL:PICK'],
      },
      {
        label: t('common.batchDistribute'),
        key: 'batchDistribute',
        permission: ['CLUE_MANAGEMENT_POOL:ASSIGN'],
      },
      {
        label: t('common.batchDelete'),
        key: 'batchDelete',
        permission: ['CLUE_MANAGEMENT_POOL:DELETE'],
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
      type: 'default',
      title: t('clue.batchClaimTip', { count: checkedRowKeys.value.length }),
      positiveText: t('common.confirmClaim'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await batchPickClue({
            batchIds: checkedRowKeys.value,
            poolId: poolId.value,
          });
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
  async function handleBatchAssign(owner: string | null) {
    try {
      await batchAssignClue({
        batchIds: checkedRowKeys.value,
        assignUserId: owner || '',
      });
      Message.success(t('common.distributeSuccess'));
      handleRefresh();
      showDistributeModal.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
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
          await batchDeleteCluePool(checkedRowKeys.value as string[]);
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

  // 删除
  function handleDelete(row: CluePoolListItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: row.name }),
      content: t('clue.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteCluePool(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 领取
  async function handleClaim(id: string) {
    try {
      claimLoading.value = true;
      await pickClue({
        clueId: id,
        poolId: poolId.value,
      });
      Message.success(t('common.claimSuccess'));
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      claimLoading.value = false;
    }
  }

  // 分配
  const distributeFormRef = ref<InstanceType<typeof TransferForm>>();
  const distributeForm = ref<TransferParams>({
    ...defaultTransferForm,
  });
  function handleDistribute(id: string) {
    distributeFormRef.value?.formRef?.validate(async (error) => {
      if (!error) {
        try {
          distributeLoading.value = true;
          await assignClue({
            assignUserId: distributeForm.value.owner || '',
            clueId: id,
          });
          Message.success(t('common.distributeSuccess'));
          distributeForm.value = { ...defaultTransferForm };
          tableRefreshId.value += 1;
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          distributeLoading.value = false;
        }
      }
    });
  }

  function handleActionSelect(row: CluePoolListItem, actionKey: string) {
    switch (actionKey) {
      case 'pop-claim':
        handleClaim(row.id);
        break;
      case 'pop-distribute':
        handleDistribute(row.id);
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  const activeClue = ref<CluePoolListItem>();
  const { useTableRes, customFieldsFilterConfig } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.CLUE_POOL,
    operationColumn: {
      key: 'operation',
      width: 200,
      fixed: 'right',
      render: (row: CluePoolListItem) =>
        h(
          CrmOperationButton,
          {
            groupList: [
              {
                label: t('common.claim'),
                key: 'claim',
                permission: ['CLUE_MANAGEMENT_POOL:PICK'],
                popConfirmProps: {
                  loading: claimLoading.value,
                  title: t('clue.claimTip', { name: row.name }),
                  positiveText: t('common.claim'),
                  iconType: 'primary',
                },
              },
              {
                label: t('common.distribute'),
                key: 'distribute',
                permission: ['CLUE_MANAGEMENT_POOL:ASSIGN'],
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
                permission: ['CLUE_MANAGEMENT_POOL:DELETE'],
              },
            ],
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
      name: (row: CluePoolListItem) => {
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
    permission: ['CLUE_MANAGEMENT_POOL:PICK', 'CLUE_MANAGEMENT_POOL:ASSIGN', 'CLUE_MANAGEMENT_POOL:DELETE'],
  });
  const { propsRes, propsEvent, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;

  function handleAdvSearch(filter: FilterResult) {
    keyword.value = '';
    setAdvanceFilter(filter);
    loadList();
  }

  function searchData(_keyword?: string, id?: string) {
    setLoadListParams({ keyword: _keyword ?? keyword.value, poolId: id || poolId.value });
    loadList();
  }

  watch(
    () => tableRefreshId.value,
    () => {
      loadList();
    }
  );

  async function init() {
    await getCluePoolOptions();
    searchData();
  }

  onBeforeMount(() => {
    init();
  });
</script>

<style lang="less">
  .n-base-select-option {
    @apply justify-between;
  }
  .n-base-select-option:hover {
    .openSea-setting-icon {
      @apply visible;
    }
  }
  .openSea-setting-icon {
    @apply invisible cursor-pointer;

    color: var(--text-n4);
    &:hover {
      color: var(--primary-8);
    }
  }
</style>
