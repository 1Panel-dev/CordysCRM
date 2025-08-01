<template>
  <CrmCard hide-footer no-content-bottom-padding>
    <CrmTable
      ref="crmTableRef"
      v-model:checked-row-keys="checkedRowKeys"
      v-bind="propsRes"
      class="crm-open-sea-table"
      :not-show-table-filter="isAdvancedSearchMode"
      :action-config="actionConfig"
      :columns="filterColumns"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
      @batch-action="handleBatchAction"
    >
      <template #actionLeft>
        <n-select
          v-model:value="openSea"
          :options="openSeaOptions"
          :render-option="renderOption"
          :show-checkmark="false"
          value-field="id"
          label-field="name"
          class="w-[240px]"
          @update-value="(e) => searchData(undefined, e)"
        />
      </template>
      <template #actionRight>
        <CrmAdvanceFilter
          ref="tableAdvanceFilterRef"
          v-model:keyword="keyword"
          :custom-fields-config-list="filterConfigList"
          :filter-config-list="customFieldsFilterConfig"
          @adv-search="handleAdvSearch"
          @keyword-search="searchData"
        />
      </template>
    </CrmTable>
  </CrmCard>
  <addOrEditPoolDrawer
    v-model:visible="drawerVisible"
    :type="ModuleConfigEnum.CUSTOMER_MANAGEMENT"
    :row="openSeaRow"
    quick
    @refresh="init"
  />
  <openSeaOverviewDrawer
    v-model:show="showOverviewDrawer"
    :source-id="activeCustomerId"
    :pool-id="openSea"
    :hidden-columns="hiddenColumns"
    @change="searchData"
  />
  <TransferModal
    v-model:show="showDistributeModal"
    :source-ids="checkedRowKeys"
    :title="t('common.batchDistribute')"
    :positive-text="t('common.distribute')"
    @confirm="handleBatchAssign"
  />
</template>

<script setup lang="ts">
  import { VNodeChild } from 'vue';
  import { DataTableRowKey, NSelect, NTooltip, useMessage } from 'naive-ui';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { TableQueryParams } from '@lib/shared/models/common';
  import { CluePoolItem } from '@lib/shared/models/system/module';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import TransferModal from '@/components/business/crm-transfer-modal/index.vue';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import openSeaOverviewDrawer from './components/openSeaOverviewDrawer.vue';
  import addOrEditPoolDrawer from '@/views/system/module/components/addOrEditPoolDrawer.vue';

  import {
    assignOpenSeaCustomer,
    batchAssignOpenSeaCustomer,
    batchDeleteOpenSeaCustomer,
    batchPickOpenSeaCustomer,
    deleteOpenSeaCustomer,
    getOpenSeaOptions,
    pickOpenSeaCustomer,
  } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';

  import { SelectOption } from 'naive-ui/es/select/src/interface';

  const { t } = useI18n();
  const { openModal } = useModal();
  const Message = useMessage();

  const openSea = ref<string | number>('');
  const openSeaOptions = ref<CluePoolItem[]>([]);
  const keyword = ref('');
  const drawerVisible = ref(false);
  const openSeaRow = ref<any>({});
  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const activeCustomerId = ref('');
  const showOverviewDrawer = ref(false);
  const batchTableQueryParams = ref<TableQueryParams>({});

  function renderOption({ node, option }: { node: VNode; option: SelectOption }): VNodeChild {
    if (option.editable) {
      (node.children as Array<VNode>)?.push(
        h(CrmIcon, {
          type: 'iconicon_set_up',
          class: 'openSea-setting-icon',
          onClick: (e: Event) => {
            e.stopPropagation();
            openSeaRow.value = { ...option };
            drawerVisible.value = true;
          },
        })
      );
    }
    return h(NTooltip, null, {
      trigger: () => node,
      default: () => option.name,
    });
  }

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.batchClaim'),
        key: 'batchClaim',
        permission: ['CUSTOMER_MANAGEMENT_POOL:PICK'],
      },
      {
        label: t('common.batchDistribute'),
        key: 'batchDistribute',
        permission: ['CUSTOMER_MANAGEMENT_POOL:ASSIGN'],
      },
      {
        label: t('common.batchDelete'),
        key: 'batchDelete',
        permission: ['CUSTOMER_MANAGEMENT_POOL:DELETE'],
      },
    ],
  };

  const tableRefreshId = ref(0);

  // 批量领取
  function handleBatchClaim() {
    openModal({
      type: 'default',
      title: t('customer.batchClaimTip', { count: checkedRowKeys.value.length }),
      content: t('customer.claimTipContent'),
      positiveText: t('common.confirmClaim'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await batchPickOpenSeaCustomer({
            ...batchTableQueryParams.value,
            batchIds: checkedRowKeys.value,
            poolId: openSea.value,
          });
          tableRefreshId.value += 1;
          checkedRowKeys.value = [];
          Message.success(t('common.claimSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 批量分配
  const distributeLoading = ref(false);
  const distributeFormRef = ref<InstanceType<typeof TransferForm>>();
  const distributeForm = ref<any>({
    owner: null,
  });
  const showDistributeModal = ref<boolean>(false);
  async function handleBatchAssign(owner: string | null) {
    try {
      distributeLoading.value = true;
      await batchAssignOpenSeaCustomer({
        ...batchTableQueryParams.value,
        batchIds: checkedRowKeys.value,
        assignUserId: owner || '',
      });
      checkedRowKeys.value = [];
      Message.success(t('common.distributeSuccess'));
      showDistributeModal.value = false;
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      distributeLoading.value = false;
    }
  }

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
          await batchDeleteOpenSeaCustomer({
            ...batchTableQueryParams.value,
            batchIds: checkedRowKeys.value,
            poolId: openSea.value,
          });
          checkedRowKeys.value = [];
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

  const operationGroupList = computed<ActionsItem[]>(() => {
    return [
      {
        label: t('common.claim'),
        key: 'claim',
        permission: ['CUSTOMER_MANAGEMENT_POOL:PICK'],
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
        permission: ['CUSTOMER_MANAGEMENT_POOL:ASSIGN'],
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
        permission: ['CUSTOMER_MANAGEMENT_POOL:DELETE'],
      },
    ];
  });

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
          await deleteOpenSeaCustomer(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  async function handleClaim(id: string) {
    try {
      claimLoading.value = true;
      await pickOpenSeaCustomer({
        customerId: id,
        poolId: openSea.value,
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

  async function handleDistribute(id: string) {
    try {
      distributeLoading.value = true;
      await assignOpenSeaCustomer({
        customerId: id,
        assignUserId: distributeForm.value.owner,
      });
      Message.success(t('common.distributeSuccess'));
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      distributeLoading.value = false;
    }
  }

  function handleActionSelect(row: any, actionKey: string) {
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

  const hiddenColumns = computed<string[]>(() => {
    const openSeaSetting = openSeaOptions.value.find((item) => item.id === openSea.value);
    return openSeaSetting?.fieldConfigs.filter((item) => !item.enable).map((item) => item.fieldId) || [];
  });
  const { useTableRes, customFieldsFilterConfig, reasonOptions } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.CUSTOMER_OPEN_SEA,
    containerClass: '.crm-open-sea-table',
    operationColumn: {
      key: 'operation',
      width: 150,
      fixed: 'right',
      render: (row: any) =>
        h(
          CrmOperationButton,
          {
            groupList: operationGroupList.value,
            onSelect: (key: string) => handleActionSelect(row, key),
            onCancel: () => {
              distributeForm.value.owner = null;
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
          CrmTableButton,
          {
            onClick: () => {
              activeCustomerId.value = row.id;
              showOverviewDrawer.value = true;
            },
          },
          { default: () => row.name, trigger: () => row.name }
        );
      },
    },
    permission: ['CUSTOMER_MANAGEMENT_POOL:PICK', 'CUSTOMER_MANAGEMENT_POOL:ASSIGN', 'CUSTOMER_MANAGEMENT_POOL:DELETE'],
  });
  const { propsRes, propsEvent, tableQueryParams, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;
  batchTableQueryParams.value = tableQueryParams;
  const filterColumns = computed(() => {
    return propsRes.value.columns.filter((item) => !hiddenColumns.value.includes(item.key as string));
  });
  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

  function handleAdvSearch(filter: FilterResult) {
    keyword.value = '';
    setAdvanceFilter(filter);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  const tableAdvanceFilterRef = ref<InstanceType<typeof CrmAdvanceFilter>>();
  const isAdvancedSearchMode = computed(() => tableAdvanceFilterRef.value?.isAdvancedSearchMode);
  function searchData(_keyword?: string, poolId?: string) {
    setLoadListParams({ keyword: _keyword ?? keyword.value, poolId: poolId || openSea.value });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  watch(
    () => tableRefreshId.value,
    () => {
      crmTableRef.value?.clearCheckedRowKeys();
      searchData();
    }
  );

  async function initOpenSeaOptions() {
    const res = await getOpenSeaOptions();
    openSeaOptions.value = res;
    openSea.value = openSeaOptions.value[0]?.id || '';
  }

  const filterConfigList = computed(() => [
    {
      title: t('customer.recycleReason'),
      dataIndex: 'reasonId',
      type: FieldTypeEnum.SELECT_MULTIPLE,
      selectProps: {
        options: reasonOptions.value,
      },
    },
    ...baseFilterConfigList,
  ]);

  async function init() {
    await initOpenSeaOptions();
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
