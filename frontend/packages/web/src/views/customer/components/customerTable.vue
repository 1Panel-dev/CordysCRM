<template>
  <CrmTable
    ref="crmTableRef"
    v-model:checked-row-keys="checkedRowKeys"
    v-bind="propsRes"
    class="crm-customer-table"
    :columns="tableColumns"
    :not-show-table-filter="isAdvancedSearchMode"
    :action-config="props.readonly ? undefined : actionConfig"
    @row-key-change="handleRowKeyChange"
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
      <div class="flex items-center gap-[12px]">
        <n-button
          v-if="
            activeTab !== CustomerSearchTypeEnum.CUSTOMER_COLLABORATION &&
            hasAnyPermission(['CUSTOMER_MANAGEMENT:ADD']) &&
            !props.readonly
          "
          type="primary"
          @click="handleNewClick"
        >
          {{ t('customer.new') }}
        </n-button>
        <CrmImportButton
          v-if="hasAnyPermission(['CUSTOMER_MANAGEMENT:IMPORT'])"
          :api-type="FormDesignKeyEnum.CUSTOMER"
          :title="t('module.customerManagement')"
          @import-success="() => searchData()"
        />
        <n-button
          v-if="
            hasAnyPermission(['CUSTOMER_MANAGEMENT:EXPORT']) &&
            activeTab !== CustomerSearchTypeEnum.CUSTOMER_COLLABORATION &&
            !props.readonly
          "
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
        :custom-fields-config-list="customFieldsFilterConfig"
        :filter-config-list="filterConfigList"
        @adv-search="handleAdvSearch"
        @keyword-search="searchData"
      />
    </template>
    <template #view>
      <CrmViewSelect
        v-if="!props.hiddenAdvanceFilter"
        v-model:active-tab="activeTab"
        :type="FormDesignKeyEnum.CUSTOMER"
        :custom-fields-config-list="customFieldsFilterConfig"
        :filter-config-list="filterConfigList"
        :advanced-original-form="advancedOriginalForm"
        :route-name="CustomerRouteEnum.CUSTOMER_INDEX"
        @refresh-table-data="searchData"
        @generated-chart="handleGeneratedChart"
      />
    </template>
  </CrmTable>

  <TransferModal
    v-model:show="showTransferModal"
    :source-ids="checkedRowKeys"
    :save-api="batchTransferCustomer"
    @load-list="searchData"
  />
  <customerOverviewDrawer v-model:show="showOverviewDrawer" :source-id="activeSourceId" @saved="searchData" />
  <CrmFormCreateDrawer
    v-model:visible="formCreateDrawerVisible"
    :form-key="activeFormKey"
    :source-id="activeSourceId"
    :need-init-detail="needInitDetail"
    :initial-source-name="initialSourceName"
    :other-save-params="otherFollowRecordSaveParams"
    @saved="handleFormCreateSaved"
  />
  <CrmTableExportModal
    v-model:show="showExportModal"
    :params="exportParams"
    :export-columns="exportColumns"
    :is-export-all="isExportAll"
    type="customer"
    @create-success="handleExportCreateSuccess"
  />
  <CrmMoveModal
    v-model:show="showMoveModal"
    :reason-key="ReasonTypeEnum.CUSTOMER_POOL_RS"
    :source-id="moveIds"
    :name="initialSourceName"
    type="warning"
    @refresh="() => (tableRefreshId += 1)"
  />

  <CrmBatchEditModal
    v-model:visible="showEditModal"
    v-model:field-list="fieldList"
    :ids="checkedRowKeys"
    :form-key="FormDesignKeyEnum.CUSTOMER"
    @refresh="() => (tableRefreshId += 1)"
  />
  <mergeAccountModal v-model:show="showMergeModal" :selected-rows="selectedRows" @saved="() => (tableRefreshId += 1)" />
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import { DataTableRowKey, NButton, useMessage } from 'naive-ui';

  import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ModuleConfigEnum, ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import useLocale from '@lib/shared/locale/useLocale';
  import { characterLimit } from '@lib/shared/method';
  import { ExportTableColumnItem } from '@lib/shared/models/common';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import { CrmPopConfirmIconType } from '@/components/pure/crm-pop-confirm/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmBatchEditModal from '@/components/business/crm-batch-edit-modal/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmImportButton from '@/components/business/crm-import-button/index.vue';
  import CrmMoveModal from '@/components/business/crm-move-modal/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmTableExportModal from '@/components/business/crm-table-export-modal/index.vue';
  import TransferModal from '@/components/business/crm-transfer-modal/index.vue';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import CrmViewSelect from '@/components/business/crm-view-select/index.vue';
  import customerOverviewDrawer from './customerOverviewDrawer.vue';
  import mergeAccountModal from './mergeAccountModal.vue';

  import { batchDeleteCustomer, batchTransferCustomer, deleteCustomer, updateCustomer } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';
  import useViewChartParams, { STORAGE_VIEW_CHART_KEY, ViewChartResult } from '@/hooks/useViewChartParams';
  import { getExportColumns } from '@/utils/export';
  import { hasAnyPermission } from '@/utils/permission';

  import { CustomerRouteEnum } from '@/enums/routeEnum';

  import { InternalRowData } from 'naive-ui/es/data-table/src/interface';

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();
  const route = useRoute();
  const { currentLocale } = useLocale(Message.loading);

  const props = defineProps<{
    formKey: FormDesignKeyEnum.CUSTOMER | FormDesignKeyEnum.SEARCH_ADVANCED_CUSTOMER;
    hiddenAdvanceFilter?: boolean;
    readonly?: boolean;
    isLimitShowDetail?: boolean; // 是否根据权限限查看详情
    hiddenTotal?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'init', val: { filterConfigList: FilterFormItem[]; customFieldsFilterConfig: FilterFormItem[] }): void;
    (e: 'showCountDetail', row: Record<string, any>, type: 'opportunity' | 'clue'): void;
  }>();

  const activeTab = ref();

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
    needInitDetail.value = false;
    activeFormKey.value = FormDesignKeyEnum.CUSTOMER;
    activeSourceId.value = '';
    formCreateDrawerVisible.value = true;
  }

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.exportChecked'),
        key: 'exportChecked',
        permission: ['CUSTOMER_MANAGEMENT:EXPORT'],
      },
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
    ],
    moreAction: [
      {
        label: t('common.batchEdit'),
        key: 'batchEdit',
        permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
      },
      {
        label: t('customer.mergeAccount'),
        key: 'merge',
        permission: ['CUSTOMER_MANAGEMENT:MERGE'],
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

  const showMoveModal = ref(false);
  const moveIds = ref<(string | number) | (string | number)[]>('');

  function handleMoveToOpenSea(row?: any) {
    initialSourceName.value = row?.name ?? '';
    moveIds.value = row?.id ? row.id : checkedRowKeys.value;
    showMoveModal.value = true;
  }

  const showEditModal = ref(false);
  function handleBatchEdit() {
    showEditModal.value = true;
  }

  const selectedRows = ref<InternalRowData[]>([]);
  function handleRowKeyChange(keys: DataTableRowKey[], _rows: InternalRowData[]) {
    selectedRows.value = _rows;
  }

  const showMergeModal = ref(false);
  function handleMergeAccount() {
    openModal({
      type: 'error',
      icon: () => {
        return h(CrmIcon, {
          type: 'iconicon_error_circle_filled',
          size: 20,
          class: 'mr-[8px] text-[var(--error-red)]',
        });
      },
      title: t('customer.mergeConfirmTitle'),
      content: t('customer.mergeConfirmContent'),
      positiveText: t('customer.confirmMerge'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        showMergeModal.value = true;
      },
    });
  }

  // 批量转移
  const showTransferModal = ref<boolean>(false);
  const showExportModal = ref<boolean>(false);

  const isExportAll = ref(false);
  function handleBatchAction(item: ActionsItem) {
    switch (item.key) {
      case 'batchTransfer':
        showTransferModal.value = true;
        break;
      case 'batchEdit':
        handleBatchEdit();
        break;
      case 'batchDelete':
        handleBatchDelete();
        break;
      case 'moveToOpenSea':
        handleMoveToOpenSea();
        break;
      case 'merge':
        handleMergeAccount();
        break;
      case 'exportChecked':
        isExportAll.value = false;
        showExportModal.value = true;
        break;
      default:
        break;
    }
  }

  function handleExportCreateSuccess() {
    checkedRowKeys.value = [];
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
      case 'moveToOpenSea':
        handleMoveToOpenSea(row);
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
      ...(activeTab.value !== CustomerSearchTypeEnum.CUSTOMER_COLLABORATION
        ? [
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
                iconType: 'primary' as CrmPopConfirmIconType,
              },
              popSlotName: 'transferPopTitle',
              popSlotContent: 'transferPopContent',
              permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
            },
            {
              label: 'more',
              key: 'more',
              slotName: 'more',
            },
          ]
        : []),
    ];
  });

  // 概览
  const showOverviewDrawer = ref(false);
  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const handleAdvanceFilter = ref<null | ((...args: any[]) => void)>(null);
  const handleSearchData = ref<null | ((...args: any[]) => void)>(null);

  defineExpose({
    handleAdvanceFilter,
    handleSearchData,
  });
  const { useTableRes, customFieldsFilterConfig, fieldList } = await useFormCreateTable({
    formKey: props.formKey,
    disabledSelection: (row: any) => {
      return row.collaborationType === 'READ_ONLY';
    },
    operationColumn: props.readonly
      ? undefined
      : {
          key: 'operation',
          width: currentLocale.value === 'en-US' ? 250 : 200,
          fixed: 'right',
          render: (row: any) =>
            ['convertedToCustomer', 'convertedToOpportunity'].includes(activeTab.value) ||
            row.collaborationType === 'READ_ONLY'
              ? '-'
              : h(
                  CrmOperationButton,
                  {
                    groupList: operationGroupList.value,
                    moreList: [
                      ...(activeTab.value !== CustomerSearchTypeEnum.CUSTOMER_COLLABORATION
                        ? [
                            {
                              label: t('customer.moveToOpenSea'),
                              key: 'moveToOpenSea',
                              permission: ['CUSTOMER_MANAGEMENT:RECYCLE'],
                            },
                            {
                              label: t('common.delete'),
                              key: 'delete',
                              danger: true,
                              permission: ['CUSTOMER_MANAGEMENT:DELETE'],
                            },
                          ]
                        : []),
                    ],
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
        return props.isLimitShowDetail && row.hasPermission === false
          ? h(CrmNameTooltip, { text: row.name })
          : h(
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
      opportunityCount: (row: any) => {
        return !row.opportunityCount
          ? row.opportunityCount ?? '-'
          : h(
              NButton,
              {
                text: true,
                type: 'primary',
                disabled: !row.opportunityModuleEnable || !hasAnyPermission(['OPPORTUNITY_MANAGEMENT:READ']),
                onClick: () => {
                  emit('showCountDetail', row, 'opportunity');
                },
              },
              { default: () => row.opportunityCount }
            );
      },
      clueCount: (row: any) => {
        return !row.clueCount
          ? row.clueCount ?? '-'
          : h(
              NButton,
              {
                text: true,
                type: 'primary',
                disabled:
                  !row.clueModuleEnable || !hasAnyPermission(['CLUE_MANAGEMENT:READ', 'CLUE_MANAGEMENT_POOL:READ']),
                onClick: () => {
                  emit('showCountDetail', row, 'clue');
                },
              },
              { default: () => row.clueCount }
            );
      },
    },
    permission: ['CUSTOMER_MANAGEMENT:RECYCLE', 'CUSTOMER_MANAGEMENT:UPDATE', 'CUSTOMER_MANAGEMENT:DELETE'],
    containerClass: '.crm-customer-table',
    hiddenTotal: ref(!!props.hiddenTotal),
    readonly: props.readonly,
  });
  const { propsRes, propsEvent, tableQueryParams, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;
  const tableColumns = computed(() => {
    if (activeTab.value === CustomerSearchTypeEnum.CUSTOMER_COLLABORATION) {
      return propsRes.value.columns
        .filter((item: any) => item.type !== 'selection')
        .map((e) => {
          if (e.key === 'operation') {
            return {
              ...e,
              width: 80,
            };
          }
          return e;
        });
    }
    return propsRes.value.columns;
  });

  const exportParams = computed(() => {
    return {
      ...tableQueryParams.value,
      ids: checkedRowKeys.value,
    };
  });

  function handleExportAllClick() {
    isExportAll.value = true;
    showExportModal.value = true;
  }

  const filterConfigList = computed<FilterFormItem[]>(() => [
    {
      title: t('opportunity.department'),
      dataIndex: 'departmentId',
      type: FieldTypeEnum.TREE_SELECT,
      treeSelectProps: {
        labelField: 'name',
        keyField: 'id',
        multiple: true,
        clearFilterAfterSelect: false,
        type: 'department',
        checkable: true,
        showContainChildModule: true,
        containChildIds: [],
      },
    },
    {
      title: t('customer.lastFollowUps'),
      dataIndex: 'follower',
      type: FieldTypeEnum.USER_SELECT,
    },
    {
      title: t('customer.lastFollowUpDate'),
      dataIndex: 'followTime',
      type: FieldTypeEnum.TIME_RANGE_PICKER,
    },
    ...baseFilterConfigList,
  ]);

  const exportColumns = computed<ExportTableColumnItem[]>(() =>
    getExportColumns(propsRes.value.columns, customFieldsFilterConfig.value as FilterFormItem[])
  );

  const isAdvancedSearchMode = ref(false);
  const advancedOriginalForm = ref<FilterForm | undefined>();
  function handleAdvSearch(filter: FilterResult, isAdvancedMode: boolean, originalForm?: FilterForm) {
    keyword.value = '';
    advancedOriginalForm.value = originalForm;
    isAdvancedSearchMode.value = isAdvancedMode;
    setAdvanceFilter(filter);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  handleAdvanceFilter.value = handleAdvSearch;

  const tableAdvanceFilterRef = ref<InstanceType<typeof CrmAdvanceFilter>>();

  function searchData(val?: string) {
    setLoadListParams({ keyword: val ?? keyword.value, viewId: activeTab.value });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }
  handleSearchData.value = searchData;

  function handleFormCreateSaved(res: any) {
    searchData();
    if (activeFormKey.value === FormDesignKeyEnum.CUSTOMER && !needInitDetail.value) {
      // 新建客户后打开新建联系人
      activeSourceId.value = res.id;
      initialSourceName.value = res.name;
      activeFormKey.value = FormDesignKeyEnum.CONTACT;
      nextTick(() => {
        formCreateDrawerVisible.value = true;
      });
    }
  }

  function handleGeneratedChart(res: FilterResult, form: FilterForm) {
    advancedOriginalForm.value = form;
    setAdvanceFilter(res);
    tableAdvanceFilterRef.value?.setAdvancedFilter(res, true);
    searchData();
  }

  const { initTableViewChartParams, getChartViewId } = useViewChartParams();

  function viewChartCallBack(params: ViewChartResult) {
    const { viewId, formModel, filterResult } = params;
    tableAdvanceFilterRef.value?.initFormModal(formModel, true);
    setAdvanceFilter(filterResult);
    activeTab.value = viewId;
  }

  watch(
    () => activeTab.value,
    (val) => {
      if (val) {
        checkedRowKeys.value = [];
        setLoadListParams({ keyword: keyword.value, viewId: getChartViewId() ?? activeTab.value });
        initTableViewChartParams(viewChartCallBack);
        crmTableRef.value?.setColumnSort(val);
      }
    }
  );

  watch(
    () => tableRefreshId.value,
    () => {
      checkedRowKeys.value = [];
      searchData();
    }
  );

  onMounted(() => {
    emit('init', {
      filterConfigList: filterConfigList.value,
      customFieldsFilterConfig: customFieldsFilterConfig.value as FilterFormItem[],
    });
    if (route.query.id) {
      activeFormKey.value = FormDesignKeyEnum.CUSTOMER;
      activeSourceId.value = route.query.id as string;
      showOverviewDrawer.value = true;
    }
  });

  onBeforeUnmount(() => {
    sessionStorage.removeItem(STORAGE_VIEW_CHART_KEY);
  });
</script>

<style lang="less" scoped>
  :deep(.n-tabs-scroll-padding) {
    width: 16px !important;
  }
</style>
