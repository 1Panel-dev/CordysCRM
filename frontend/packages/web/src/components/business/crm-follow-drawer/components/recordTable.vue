<template>
  <CrmCard hide-footer no-content-bottom-padding>
    <CrmTable
      ref="crmTableRef"
      v-model:checked-row-keys="checkedRowKeys"
      v-bind="propsRes"
      class="crm-record-table"
      :not-show-table="activeShowType === 'timeline'"
      :hiddenBackToTop="activeShowType === 'timeline'"
      :action-config="props.detailTabResourceId ? { baseAction: [] } : actionConfig"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="handleSorterChange"
      @filter-change="handleFilterChange"
      @batch-action="handleBatchAction"
      @refresh="handleCurrentViewRefresh"
    >
      <template #actionLeft>
        <div class="flex items-center gap-[12px]">
          <n-button v-if="canCreate" type="primary" @click="handleAdd">
            {{ t('crmFollowRecord.writeRecord') }}
          </n-button>
          <CrmImportButton
            v-if="!props.detailTabResourceId && !props.readonly && hasAnyPermission(['FOLLOW_RECORD:IMPORT'])"
            :api-type="FormDesignKeyEnum.FOLLOW_RECORD"
            :title="t('eventDrawer.record')"
            @import-success="searchData"
          />
          <n-button
            v-if="!props.detailTabResourceId && hasAnyPermission(['FOLLOW_RECORD:EXPORT'])"
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
          ref="tableAdvanceFilterRef"
          v-model:keyword="keyword"
          :search-placeholder="t('common.searchByName')"
          :custom-fields-config-list="filterConfigList"
          :filter-config-list="customFieldsFilterConfig"
          @adv-search="handleAdvSearch"
          @keyword-search="searchByKeyword"
        />
        <n-tabs v-model:value="activeShowType" type="segment" size="large" class="show-type-tabs">
          <n-tab-pane name="table" class="hidden">
            <template #tab><CrmIcon type="iconicon_list" /></template>
          </n-tab-pane>
          <n-tab-pane name="timeline" class="hidden">
            <template #tab><CrmIcon type="iconicon_timeline" /></template>
          </n-tab-pane>
        </n-tabs>
      </template>
      <template #view>
        <CrmViewSelect
          v-if="!props.detailTabResourceId"
          v-model:active-tab="activeTab"
          :type="FormDesignKeyEnum.FOLLOW_RECORD"
          :custom-fields-config-list="filterConfigList"
          :filter-config-list="customFieldsFilterConfig"
          :advanced-original-form="advancedOriginalForm"
          @refresh-table-data="searchData"
          @generated-chart="handleGeneratedChart"
        />
      </template>
      <template v-if="activeShowType === 'timeline'" #other>
        <div class="flex-1">
          <n-spin v-if="timelineLoading && !timelineData.length" class="flex justify-center py-[32px]" />
          <FollowRecord
            v-else
            v-model:data="timelineData"
            :virtual-scroll-height="isFullScreen ? 'calc(100vh - 143px)' : 'calc(100vh - 289px)'"
            :get-description-fun="getDescriptionFun"
            key-field="id"
            :disabled-open-detail="false"
            :readonly="props.readonly"
            type="followRecord"
            :empty-text="t('crmFollowRecord.noFollowRecord')"
            @reach-bottom="handleTimelineReachBottom"
          >
            <template #titleLeft="{ item }">
              <CrmTag type="primary" theme="light"> {{ item.type }} </CrmTag>
            </template>
            <template #headerAction="{ item }">
              <div class="flex items-center gap-[4px]">
                <n-button type="primary" class="text-btn-primary" quaternary @click="handleDetail(item)">
                  {{ t('common.detail') }}
                </n-button>
                <n-button v-if="canEdit" type="primary" class="text-btn-primary" quaternary @click="handleEdit(item)">
                  {{ t('common.edit') }}
                </n-button>
                <n-button
                  v-if="canDelete"
                  type="error"
                  class="text-btn-error"
                  quaternary
                  @click="handleDelete(item.id)"
                >
                  {{ t('common.delete') }}
                </n-button>
              </div>
            </template>
            <template #createTime="{ descItem }">
              <div class="flex items-center gap-[8px]">
                {{ dayjs(descItem.value).format('YYYY-MM-DD HH:mm:ss') }}
              </div>
            </template>
            <template #updateTime="{ descItem }">
              <div class="flex items-center gap-[8px]">
                {{ dayjs(descItem.value).format('YYYY-MM-DD HH:mm:ss') }}
              </div>
            </template>
          </FollowRecord>
        </div>
      </template>
    </CrmTable>

    <CrmFormCreateDrawer
      v-model:visible="formDrawerVisible"
      :form-key="FormDesignKeyEnum.FOLLOW_RECORD"
      :source-id="realFollowSourceId"
      :need-init-detail="needInitDetail"
      @saved="handleAfterSave"
    />
    <DetailDrawer
      v-model:show="showDetailDrawer"
      :form-key="FormDesignKeyEnum.FOLLOW_RECORD"
      :source-id="sourceId"
      :refresh-key="refreshKey"
      :source-name="sourceName"
      :readonly="props.independentDetail ? false : props.readonly"
      :detail="activeItem"
      @delete="handleDelete(sourceId)"
      @edit="handleEdit(activeItem)"
      @detail-init="handleDetailInit"
      @count-change="handleCommentCountChange"
    />
    <CustomerOverviewDrawer
      v-model:show="showCustomerDrawer"
      :source-id="customerId"
      @refresh="handleRefresh"
      @saved="handleRefresh"
      @deleted="handleRefresh"
    />
    <OptOverviewDrawer
      v-model:show="showOpportunityDrawer"
      :detail="activeOpportunity"
      @refresh="handleRefresh"
      @remove="handleRefresh"
      @open-customer-drawer="handleShowCustomerDrawer"
    />
    <ClueOverviewDrawer
      v-model:show="showClueDrawer"
      :detail="activeClue"
      @refresh="handleRefresh"
      @saved="handleRefresh"
      @remove="handleRefresh"
      @open-customer-drawer="handleShowCustomerDrawer"
    />
    <OpenSeaOverviewDrawer
      v-model:show="showOpenSeaDrawer"
      :source-id="customerId"
      :pool-id="poolId"
      :hidden-columns="[]"
      @change="handleRefresh"
    />
    <CluePoolOverviewDrawer
      v-model:show="showCluePoolDrawer"
      :detail="activeCluePool"
      :pool-id="poolId"
      :hidden-columns="[]"
      @refresh="handleRefresh"
      @remove="handleRefresh"
    />
  </CrmCard>
  <CrmTableExportModal
    v-model:show="showExportModal"
    :params="exportParams"
    :export-columns="exportColumns"
    :is-export-all="isExportAll"
    type="followRecord"
    @create-success="handleExportCreateSuccess"
  />
</template>

<script setup lang="ts">
  import { DataTableRowKey, NButton, NSpin, NTabPane, NTabs, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { transformData } from '@lib/shared/method/formCreate';
  import type { ExportTableColumnItem, FilterConditionItem, SortParams } from '@lib/shared/models/common';
  import { FormDetailTabQuery } from '@lib/shared/models/system/module';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { Description } from '@/components/pure/crm-detail-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import { descriptionList } from '@/components/business/crm-follow-detail/config';
  import FollowRecord from '@/components/business/crm-follow-detail/followRecord.vue';
  import { getFormListApiMap } from '@/components/business/crm-form-create/config';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmImportButton from '@/components/business/crm-import-button/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmTableExportModal from '@/components/business/crm-table-export-modal/index.vue';
  import CrmViewSelect from '@/components/business/crm-view-select/index.vue';
  import DetailDrawer from './detailDrawer.vue';
  import ClueOverviewDrawer from '@/views/clueManagement/clue/components/clueOverviewDrawer.vue';
  import CluePoolOverviewDrawer from '@/views/clueManagement/cluePool/components/cluePoolOverviewDrawer.vue';
  import CustomerOverviewDrawer from '@/views/customer/components/customerOverviewDrawer.vue';
  import OpenSeaOverviewDrawer from '@/views/customer/components/openSeaOverviewDrawer.vue';
  import OptOverviewDrawer from '@/views/opportunity/components/optOverviewDrawer.vue';

  import { deleteFollowRecord } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useLocalForage from '@/hooks/useLocalForage';
  import useModal from '@/hooks/useModal';
  import { useRelatedTableChanges } from '@/hooks/useRelatedDataRefresh';
  import { getExportColumns, splitFollowResourceNameColumns } from '@/utils/export';
  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    readonly?: boolean;
    independentDetail?: boolean;
    detailTabResourceId?: string;
    detailTabQuery?: FormDetailTabQuery;
    detailTabPageFormId?: string;
    tableKey?: string;
    hideOperationColumn?: boolean;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { setItem, getItem } = useLocalForage();
  const { openModal } = useModal();

  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const isExportAll = ref(false);
  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.exportChecked'),
        key: 'exportChecked',
        permission: ['FOLLOW_RECORD:EXPORT'],
      },
    ],
  };

  const activeTab = ref('');

  const keyword = ref('');

  const filterConfigList = computed<FilterFormItem[]>(() => {
    return [
      {
        title: t('opportunity.department'),
        dataIndex: 'departmentId',
        type: FieldTypeEnum.TREE_SELECT,
        treeSelectProps: {
          labelField: 'name',
          keyField: 'id',
          multiple: true,
          clearFilterAfterSelect: false,
          checkable: true,
          showContainChildModule: true,
          type: 'department',
        },
      },
      {
        dataIndex: 'phone',
        title: t('common.phoneNumber'),
        type: FieldTypeEnum.PHONE,
      },
      ...baseFilterConfigList,
    ] as FilterFormItem[];
  });

  const operationGroupList: ActionsItem[] = [
    {
      label: t('common.detail'),
      key: 'detail',
    },
    {
      label: t('common.edit'),
      key: 'edit',
    },
    {
      label: t('common.delete'),
      key: 'delete',
    },
  ];

  const tableRefreshId = ref(0);
  function handleRefresh() {
    checkedRowKeys.value = [];
    tableRefreshId.value += 1;
  }

  const refreshKey = ref(0);
  const activeItem = ref<any>();
  const sourceId = ref('');
  const sourceName = ref('');
  const showDetailDrawer = ref(false);
  const canRead = computed(() => hasAnyPermission(['FOLLOW_RECORD:READ']));
  const canManage = (permission: string) => !props.readonly && hasAnyPermission([permission]);
  const canCreate = computed(() => canManage('FOLLOW_RECORD:ADD'));
  const canEdit = computed(() => canManage('FOLLOW_RECORD:UPDATE'));
  const canDelete = computed(() => canManage('FOLLOW_RECORD:DELETE'));

  const showCustomerDrawer = ref(false);
  const showOpportunityDrawer = ref(false);
  const showClueDrawer = ref(false);
  const showOpenSeaDrawer = ref(false);
  const showCluePoolDrawer = ref(false);
  const customerId = ref('');
  const poolId = ref('');
  const activeOpportunity = ref();
  const activeClue = ref();
  const activeCluePool = ref();
  function handleShowCustomerDrawer(params: { customerId: string; inCustomerPool: boolean; poolId: string }) {
    customerId.value = params.customerId;
    if (params.inCustomerPool) {
      poolId.value = params.poolId;
      showOpenSeaDrawer.value = true;
    } else {
      showCustomerDrawer.value = true;
    }
  }
  function openCustomerDrawer(row: any) {
    customerId.value = row.customerId;
    if (row.poolId) {
      poolId.value = row.poolId;
      showOpenSeaDrawer.value = true;
    } else {
      showCustomerDrawer.value = true;
    }
  }
  function openSourceDrawer(row: any) {
    if (row.resourceType === 'CLUE') {
      if (row.poolId) {
        poolId.value = row.poolId;
        activeCluePool.value = { id: row.clueId, name: row.clueName };
        showCluePoolDrawer.value = true;
      } else {
        activeClue.value = { id: row.clueId, name: row.clueName };
        showClueDrawer.value = true;
      }
    } else {
      openCustomerDrawer(row);
    }
  }
  function openOpportunityDrawer(row: any) {
    activeOpportunity.value = { id: row.opportunityId, name: row.opportunityName };
    showOpportunityDrawer.value = true;
  }
  function handleDetail(row: any) {
    sourceId.value = row.id;
    activeItem.value = row;
    sourceName.value = row.resourceType === 'CLUE' ? row.clueName : row.customerName;
    showDetailDrawer.value = true;
  }

  // 删除
  async function handleDelete(id: string) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirm'),
      positiveText: t('common.confirmDelete'),
      content: t('common.deleteConfirmContent'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteFollowRecord(id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
          if (showDetailDrawer.value) {
            showDetailDrawer.value = false;
          }
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  const formDrawerVisible = ref(false);
  const realFollowSourceId = ref<string | undefined>('');
  const needInitDetail = ref(false);
  function handleAdd() {
    realFollowSourceId.value = '';
    needInitDetail.value = false;
    formDrawerVisible.value = true;
  }
  function handleEdit(item: any) {
    realFollowSourceId.value = item.id;
    needInitDetail.value = true;
    formDrawerVisible.value = true;
  }

  function handleAfterSave() {
    if (showDetailDrawer.value) {
      refreshKey.value += 1;
    }
    tableRefreshId.value += 1;
  }

  function handleActionSelect(row: any, actionKey: string) {
    switch (actionKey) {
      case 'detail':
        handleDetail(row);
        break;
      case 'delete':
        handleDelete(row.id);
        break;
      case 'edit':
        handleEdit(row);
        break;
      default:
        break;
    }
  }

  const { useTableRes, customFieldsFilterConfig, fieldList } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.FOLLOW_RECORD,
    containerClass: '.crm-record-table',
    readonly: props.readonly,
    detailTabResourceId: props.detailTabResourceId,
    detailTabPageFormId: props.detailTabPageFormId,
    detailTabQuery: props.detailTabQuery,
    tableKey: props.tableKey,
    hideOperationColumn: props.hideOperationColumn,
    permission: props.detailTabResourceId ? [] : ['FOLLOW_RECORD:EXPORT'],
    operationColumn: {
      key: 'operation',
      width: 140,
      fixed: 'right',
      render: (row: any) =>
        h(CrmOperationButton, {
          groupList: operationGroupList.filter((item) => {
            if (item.key === 'detail') return canRead.value;
            if (item.key === 'edit') return canEdit.value;
            return canDelete.value;
          }),
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
    specialRender: {
      name: (row: any) => {
        return h(
          CrmTableButton,
          {
            onClick: () => openSourceDrawer(row),
          },
          {
            default: () => (row.resourceType === 'CLUE' ? row.clueName : row.customerName),
            trigger: () => (row.resourceType === 'CLUE' ? row.clueName : row.customerName),
          }
        );
      },
      customerName: (row: any) =>
        h(CrmTableButton, { onClick: () => openCustomerDrawer(row) }, { default: () => row.customerName }),
      opportunityId: (row: any) =>
        hasAnyPermission(['OPPORTUNITY_MANAGEMENT:READ']) && row.opportunityName
          ? h(
              CrmTableButton,
              { onClick: () => openOpportunityDrawer(row) },
              { default: () => row.opportunityName, trigger: () => row.opportunityName }
            )
          : h(CrmNameTooltip, { text: row.opportunityName }),
    },
  });
  const { propsRes, propsEvent, tableQueryParams, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;

  const showExportModal = ref(false);

  function handleBatchAction(item: ActionsItem) {
    if (item.key === 'exportChecked') {
      isExportAll.value = false;
      showExportModal.value = true;
    }
  }

  const exportParams = computed(() => ({
    ...tableQueryParams.value,
    ids: checkedRowKeys.value,
  }));
  const exportColumns = computed<ExportTableColumnItem[]>(() =>
    // 列表合并展示客户名称、公司名称, 导出仍按两个字段处理
    splitFollowResourceNameColumns(
      getExportColumns(
        propsRes.value.columns,
        customFieldsFilterConfig.value as FilterFormItem[],
        fieldList.value,
        true
      ),
      fieldList.value
    )
  );

  function handleExportAllClick() {
    isExportAll.value = true;
    showExportModal.value = true;
  }

  function handleExportCreateSuccess() {
    checkedRowKeys.value = [];
  }

  const timelineData = ref<any[]>([]);
  const timelineLoading = ref(false);
  const timelinePagination = reactive({ current: 0, total: 0, pageSize: 0 });
  let timelineRequestId = 0;
  let useTableResultAsTimelineFirstPage = false;
  let hasTableLoadedOnce = false;
  const activeShowType = ref<'table' | 'timeline'>();

  function syncTimelineWithTableResult() {
    timelineRequestId += 1;
    timelineData.value = propsRes.value.data.map((item) => ({ ...item }));
    timelinePagination.current = propsRes.value.crmPagination?.page ?? 1;
    timelinePagination.total = propsRes.value.crmPagination?.itemCount ?? 0;
    timelinePagination.pageSize = propsRes.value.crmPagination?.pageSize ?? 0;
    timelineLoading.value = false;
    useTableResultAsTimelineFirstPage = false;
  }

  async function loadTimelineData(reset = false) {
    if (!reset && timelineLoading.value) {
      return;
    }

    const requestId = ++timelineRequestId;
    const current = reset ? 1 : timelinePagination.current + 1;
    if (reset) {
      timelineData.value = [];
      timelinePagination.current = 0;
      timelinePagination.total = 0;
      timelinePagination.pageSize = 0;
    }

    timelineLoading.value = true;
    try {
      const response = await getFormListApiMap[FormDesignKeyEnum.FOLLOW_RECORD]!({
        ...tableQueryParams.value,
        current,
        pageSize: tableQueryParams.value.pageSize ?? propsRes.value.crmPagination?.pageSize,
      });
      if (requestId !== timelineRequestId) {
        return;
      }

      const data = response.list.map((item: any) => {
        if (item.updateTime) item.updateTime = dayjs(item.updateTime).format('YYYY-MM-DD HH:mm:ss');
        if (item.createTime) item.createTime = dayjs(item.createTime).format('YYYY-MM-DD HH:mm:ss');
        return transformData({ item, originalData: response, fields: fieldList.value });
      });
      timelineData.value = reset
        ? data
        : [
            ...timelineData.value,
            ...data.filter((item) => !timelineData.value.some((dataItem) => dataItem.id === item.id)),
          ];
      timelinePagination.current = response.current;
      timelinePagination.total = response.total;
      timelinePagination.pageSize = response.pageSize;
    } finally {
      if (requestId === timelineRequestId) {
        timelineLoading.value = false;
      }
    }
  }

  function refreshTimeline() {
    if (activeShowType.value === 'timeline') {
      useTableResultAsTimelineFirstPage = true;
      timelineLoading.value = true;
      if (!propsRes.value.loading) {
        syncTimelineWithTableResult();
      }
    }
  }

  function handleTimelineReachBottom() {
    if (
      !timelineLoading.value &&
      timelinePagination.pageSize > 0 &&
      timelinePagination.total > timelinePagination.current * timelinePagination.pageSize
    ) {
      loadTimelineData();
    }
  }
  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const isFullScreen = computed(() => crmTableRef.value?.isFullScreen);
  const isAdvancedSearchMode = ref(false);
  const advancedOriginalForm = ref<FilterForm | undefined>();
  async function handleAdvSearch(filter: FilterResult, isAdvancedMode: boolean, originalForm?: FilterForm) {
    keyword.value = '';
    advancedOriginalForm.value = originalForm;
    isAdvancedSearchMode.value = isAdvancedMode;
    setAdvanceFilter(filter);
    await loadList();
    refreshTimeline();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  async function searchData(_keyword?: string) {
    setLoadListParams({
      keyword: _keyword ?? keyword.value,
      ...(props.detailTabResourceId ? {} : { viewId: activeTab.value }),
    });
    await loadList();
    refreshTimeline();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  async function handleCurrentViewRefresh() {
    if (activeShowType.value === 'timeline') {
      await loadTimelineData(true);
      return;
    }
    await searchData();
  }

  function handleDetailInit(detail?: Record<string, any>) {
    const commentCount = detail?.commentCount;
    if (!activeItem.value || typeof commentCount !== 'number') {
      return;
    }

    activeItem.value.commentCount = commentCount;
  }

  function handleCommentCountChange(commentCount: number) {
    if (activeItem.value) {
      activeItem.value.commentCount = commentCount;
    }
  }

  const tableAdvanceFilterRef = ref<InstanceType<typeof CrmAdvanceFilter>>();
  async function handleGeneratedChart(res: FilterResult, form: FilterForm) {
    advancedOriginalForm.value = form;
    setAdvanceFilter(res);
    tableAdvanceFilterRef.value?.setAdvancedFilter(res, true);
    await searchData();
  }

  function searchByKeyword(val: string) {
    keyword.value = val;
    nextTick(() => {
      searchData();
    });
  }

  watch(
    () => activeTab.value,
    async (viewId) => {
      if (viewId) {
        setLoadListParams({
          keyword: keyword.value,
          viewId,
        });
        crmTableRef.value?.setColumnSort(viewId);
      } else {
        await searchData();
      }
    }
  );

  watch(
    () => tableRefreshId.value,
    async () => {
      await searchData();
    }
  );

  watch(
    () => propsRes.value.loading,
    (loading, previousLoading) => {
      if (!loading && previousLoading) {
        hasTableLoadedOnce = true;
        if (useTableResultAsTimelineFirstPage && activeShowType.value === 'timeline') {
          syncTimelineWithTableResult();
        }
      }
    }
  );

  watch(
    () => activeShowType.value,
    async (val) => {
      if (val) {
        await setItem(`record-active-show-type`, activeShowType.value as 'table' | 'timeline');
      }
      if (val === 'timeline') {
        if (useTableResultAsTimelineFirstPage) {
          timelineLoading.value = true;
          if (hasTableLoadedOnce && !propsRes.value.loading) {
            syncTimelineWithTableResult();
          }
          return;
        }
        await loadTimelineData(true);
      }
    }
  );
  onMounted(async () => {
    if (props.detailTabResourceId) {
      activeShowType.value = 'table';
      searchData();
      return;
    }
    const showType = (await getItem<'timeline' | 'table'>(`record-active-show-type`)) ?? 'table';
    useTableResultAsTimelineFirstPage = showType === 'timeline';
    activeShowType.value = showType;
  });

  function getDescriptionFun(item: any) {
    const isClue = item.resourceType === 'CLUE' && item.clueId?.length;
    const customerNameKey = isClue ? 'clueName' : 'customerName';
    let lastDescriptionList = [
      ...[
        {
          key: customerNameKey,
          label: isClue ? t('crmFollowRecord.companyName') : t('opportunity.customerName'),
          value: customerNameKey,
        },
      ],
      ...descriptionList.map((descriptionItem) => {
        if (!descriptionItem.formConfigField) {
          return descriptionItem;
        }
        const label = fieldList.value.find((field) => field.businessKey === descriptionItem.formConfigField)?.name;
        return {
          ...descriptionItem,
          label,
        };
      }),
    ];

    if (isClue) {
      lastDescriptionList = lastDescriptionList.filter((e) => !['contactName', 'phone'].includes(e.key));
    }

    return (lastDescriptionList.map((desc) => ({
      ...desc,
      value: item[desc.key as keyof any],
    })) || []) as Description[];
  }

  async function handleSorterChange(sortObj: SortParams) {
    await propsEvent.value.sorterChange(sortObj);
    await refreshTimeline();
  }

  async function handleFilterChange(filters: FilterConditionItem[]) {
    await propsEvent.value.filterChange(filters);
    await refreshTimeline();
  }
  useRelatedTableChanges(props, [() => tableRefreshId.value]);
</script>

<style lang="less" scoped>
  .show-type-tabs {
    :deep(.n-tabs-tab) {
      padding: 6px;
    }
  }
</style>
