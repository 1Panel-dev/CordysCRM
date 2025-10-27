<template>
  <CrmTable
    ref="crmTableRef"
    v-model:checked-row-keys="checkedRowKeys"
    v-bind="propsRes"
    :class="`crm-open-sea-table-${props.formKey}`"
    :not-show-table-filter="isAdvancedSearchMode"
    :action-config="props.readonly ? undefined : actionConfig"
    :columns="filterColumns"
    @page-change="propsEvent.pageChange"
    @page-size-change="propsEvent.pageSizeChange"
    @sorter-change="propsEvent.sorterChange"
    @filter-change="propsEvent.filterChange"
    @batch-action="handleBatchAction"
    @refresh="searchData"
  >
    <template v-if="props.readonly" #tableTop>
      <slot name="searchTableTotal" :total="propsRes.crmPagination?.itemCount || 0"></slot>
    </template>
    <template #actionLeft>
      <div v-if="!props.hiddenPoolSelect" class="flex items-center gap-[12px]">
        <n-select
          v-model:value="openSea"
          :options="openSeaOptions"
          :render-option="renderOption"
          :show-checkmark="false"
          value-field="id"
          label-field="name"
          class="w-[200px]"
          @update-value="(e) => searchData(undefined, e)"
        />
        <n-button
          v-if="hasAnyPermission(['CUSTOMER_MANAGEMENT_POOL:EXPORT']) && !props.readonly"
          type="primary"
          ghost
          class="n-btn-outline-primary"
          :disabled="propsRes.data.length === 0"
          @click="handleExportAllClick"
        >
          {{ t('common.exportAll') }}
        </n-button>
      </div>
    </template>
    <template #actionRight>
      <CrmAdvanceFilter
        v-if="!props.hiddenAdvanceFilter"
        ref="tableAdvanceFilterRef"
        v-model:keyword="keyword"
        :custom-fields-config-list="filterConfigList"
        :filter-config-list="customFieldsFilterConfig"
        @adv-search="handleAdvSearch"
        @keyword-search="searchData"
      />
    </template>
    <template #view>
      <CrmViewSelect
        v-if="!props.hiddenAdvanceFilter"
        v-model:active-tab="activeTab"
        :type="FormDesignKeyEnum.CUSTOMER_OPEN_SEA"
        :custom-fields-config-list="filterConfigList"
        :filter-config-list="customFieldsFilterConfig"
        :advanced-original-form="advancedOriginalForm"
        @refresh-table-data="searchData"
        @generated-chart="handleGeneratedChart"
      />
    </template>
  </CrmTable>
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
  <CrmTableExportModal
    v-model:show="showExportModal"
    :params="exportParams"
    :export-columns="exportColumns"
    :is-export-all="isExportAll"
    type="openSea"
    @create-success="handleExportCreateSuccess"
  />
  <CrmBatchEditModal
    v-model:visible="showEditModal"
    v-model:field-list="fieldList"
    :ids="checkedRowKeys"
    :form-key="FormDesignKeyEnum.CUSTOMER_OPEN_SEA"
    @refresh="handleRefresh"
  />
</template>

<script setup lang="ts">
  import { VNodeChild } from 'vue';
  import { useRoute } from 'vue-router';
  import { DataTableRowKey, NButton, NSelect, NTooltip, useMessage } from 'naive-ui';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import useLocale from '@lib/shared/locale/useLocale';
  import { characterLimit } from '@lib/shared/method';
  import { ExportTableColumnItem, TableQueryParams } from '@lib/shared/models/common';
  import { CluePoolItem } from '@lib/shared/models/system/module';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmBatchEditModal from '@/components/business/crm-batch-edit-modal/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmTableExportModal from '@/components/business/crm-table-export-modal/index.vue';
  import TransferModal from '@/components/business/crm-transfer-modal/index.vue';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import CrmViewSelect from '@/components/business/crm-view-select/index.vue';
  import openSeaOverviewDrawer from './openSeaOverviewDrawer.vue';
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
  import useOpenNewPage from '@/hooks/useOpenNewPage';
  import useViewChartParams, { STORAGE_VIEW_CHART_KEY, ViewChartResult } from '@/hooks/useViewChartParams';
  import { getExportColumns } from '@/utils/export';
  import { hasAnyPermission } from '@/utils/permission';

  import { CustomerRouteEnum } from '@/enums/routeEnum';

  import { SelectOption } from 'naive-ui/es/select/src/interface';

  const props = defineProps<{
    formKey: FormDesignKeyEnum.CUSTOMER_OPEN_SEA | FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC;
    hiddenAdvanceFilter?: boolean;
    hiddenPoolSelect?: boolean;
    readonly?: boolean;
    isLimitShowDetail?: boolean; // 是否根据权限限查看详情
    hiddenTotal?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'init', val: { filterConfigList: FilterFormItem[]; customFieldsFilterConfig: FilterFormItem[] }): void;
  }>();

  const { t } = useI18n();
  const { openModal } = useModal();
  const Message = useMessage();
  const route = useRoute();
  const { currentLocale } = useLocale(Message.loading);
  const { openNewPage } = useOpenNewPage();

  const openSea = ref<string | number>('');
  const openSeaOptions = ref<CluePoolItem[]>([]);
  const keyword = ref('');
  const drawerVisible = ref(false);
  const openSeaRow = ref<any>({});
  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const activeCustomerId = ref('');
  const showOverviewDrawer = ref(false);
  const batchTableQueryParams = ref<TableQueryParams>({});
  const activeTab = ref();

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
        label: t('common.exportChecked'),
        key: 'exportChecked',
        permission: ['CUSTOMER_MANAGEMENT_POOL:EXPORT'],
      },
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
        label: t('common.batchEdit'),
        key: 'batchEdit',
        permission: ['CUSTOMER_MANAGEMENT_POOL:UPDATE'],
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

  async function handleClaim(row: any) {
    try {
      claimLoading.value = true;
      await pickOpenSeaCustomer({
        customerId: row.id,
        poolId: openSea.value,
      });
      Message.success(t('common.claimSuccess'));
      tableRefreshId.value += 1;
      openNewPage(CustomerRouteEnum.CUSTOMER_INDEX, {
        id: row.id,
      });
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

  const isExportAll = ref(false);
  const showExportModal = ref<boolean>(false);

  function handleExportAllClick() {
    isExportAll.value = true;
    showExportModal.value = true;
  }

  function handleExportCreateSuccess() {
    checkedRowKeys.value = [];
  }

  const showEditModal = ref(false);
  function handleBatchEdit() {
    showEditModal.value = true;
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
      case 'batchEdit':
        handleBatchEdit();
        break;
      case 'exportChecked':
        isExportAll.value = false;
        showExportModal.value = true;
        break;
      default:
        break;
    }
  }

  function handleActionSelect(row: any, actionKey: string) {
    switch (actionKey) {
      case 'pop-claim':
        handleClaim(row);
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

  const handleAdvanceFilter = ref<null | ((...args: any[]) => void)>(null);
  const handleSearchData = ref<null | ((...args: any[]) => void)>(null);

  defineExpose({
    handleAdvanceFilter,
    handleSearchData,
  });

  const { useTableRes, customFieldsFilterConfig, reasonOptions, fieldList } = await useFormCreateTable({
    formKey: props.formKey,
    containerClass: `.crm-open-sea-table-${props.formKey}`,
    operationColumn: props.readonly
      ? undefined
      : {
          key: 'operation',
          width: currentLocale.value === 'en-US' ? 200 : 150,
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
        return props.isLimitShowDetail && row.hasPermission === false
          ? h(CrmNameTooltip, { text: row.name })
          : h(
              CrmTableButton,
              {
                onClick: () => {
                  activeCustomerId.value = row.id;
                  openSea.value = row.poolId ?? openSea.value;
                  showOverviewDrawer.value = true;
                },
              },
              { default: () => row.name, trigger: () => row.name }
            );
      },
    },
    permission: ['CUSTOMER_MANAGEMENT_POOL:PICK', 'CUSTOMER_MANAGEMENT_POOL:ASSIGN', 'CUSTOMER_MANAGEMENT_POOL:DELETE'],
    hiddenTotal: ref(!!props.hiddenTotal),
    readonly: props.readonly,
  });
  const { propsRes, propsEvent, tableQueryParams, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;
  batchTableQueryParams.value = tableQueryParams.value;
  const filterColumns = computed(() => {
    return propsRes.value.columns.filter((item) => !hiddenColumns.value.includes(item.key as string));
  });

  const exportParams = computed(() => {
    return {
      ...tableQueryParams.value,
      ids: checkedRowKeys.value,
      poolId: openSea.value,
    };
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const isAdvancedSearchMode = ref(false);
  const advancedOriginalForm = ref<FilterForm | undefined>();
  function handleAdvSearch(filter: FilterResult, isAdvancedMode: boolean, originalForm?: FilterForm) {
    keyword.value = '';
    isAdvancedSearchMode.value = isAdvancedMode;
    setAdvanceFilter(filter);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
    advancedOriginalForm.value = originalForm;
  }
  handleAdvanceFilter.value = handleAdvSearch;

  const tableAdvanceFilterRef = ref<InstanceType<typeof CrmAdvanceFilter>>();
  function searchData(_keyword?: string, poolId?: string) {
    setLoadListParams({
      keyword: _keyword ?? keyword.value,
      poolId: props.hiddenPoolSelect ? undefined : poolId || openSea.value,
      viewId: activeTab.value,
    });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function handleGeneratedChart(res: FilterResult, form: FilterForm) {
    advancedOriginalForm.value = form;
    setAdvanceFilter(res);
    tableAdvanceFilterRef.value?.setAdvancedFilter(res, true);
    searchData();
  }

  handleSearchData.value = searchData;

  watch(
    () => tableRefreshId.value,
    () => {
      crmTableRef.value?.clearCheckedRowKeys();
      searchData();
    }
  );

  const { initTableViewChartParams, getChartViewId } = useViewChartParams();
  function viewChartCallBack(params: ViewChartResult) {
    const { viewId, formModal, filterResult } = params;
    tableAdvanceFilterRef.value?.initFormModal(formModal, true);
    setAdvanceFilter(filterResult);
    activeTab.value = viewId;
  }
  watch(
    () => activeTab.value,
    (val) => {
      if (val) {
        checkedRowKeys.value = [];
        setLoadListParams({
          keyword: keyword.value,
          viewId: getChartViewId() ?? activeTab.value,
          poolId: props.hiddenPoolSelect ? undefined : openSea.value,
        });
        initTableViewChartParams(viewChartCallBack);
        crmTableRef.value?.setColumnSort(val);
      }
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

  const exportColumns = computed<ExportTableColumnItem[]>(() =>
    getExportColumns(propsRes.value.columns, customFieldsFilterConfig.value as FilterFormItem[])
  );

  async function init() {
    await initOpenSeaOptions();
    searchData();
  }

  function handleRefresh() {
    checkedRowKeys.value = [];
    searchData();
  }

  onBeforeMount(() => {
    if (!props.hiddenPoolSelect) {
      init();
    }
  });

  onMounted(() => {
    emit('init', {
      filterConfigList: filterConfigList.value,
      customFieldsFilterConfig: customFieldsFilterConfig.value as FilterFormItem[],
    });

    if (route.query.id) {
      activeCustomerId.value = route.query.id as string;
      openSea.value = route.query.poolId as string;
      showOverviewDrawer.value = true;
    }
  });

  onBeforeUnmount(() => {
    sessionStorage.removeItem(STORAGE_VIEW_CHART_KEY);
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
