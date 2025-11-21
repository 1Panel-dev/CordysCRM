<template>
  <div ref="contractCardRef" class="h-full">
    <CrmCard no-content-padding hide-footer>
      <div class="h-full px-[16px] pt-[16px]">
        <CrmTable
          ref="crmTableRef"
          v-model:checked-row-keys="checkedRowKeys"
          v-bind="propsRes"
          class="crm-contract-table"
          :not-show-table-filter="isAdvancedSearchMode"
          :action-config="actionConfig"
          @page-change="propsEvent.pageChange"
          @page-size-change="propsEvent.pageSizeChange"
          @sorter-change="propsEvent.sorterChange"
          @filter-change="propsEvent.filterChange"
          @batch-action="handleBatchAction"
          @refresh="searchData"
        >
          <template #actionLeft>
            <div class="flex items-center gap-[12px]">
              <!-- TODO lmy permission -->
              <n-button v-permission="['CUSTOMER_MANAGEMENT:ADD']" type="primary" @click="handleNewClick">
                {{ t('contract.new') }}
              </n-button>
              <!-- TODO lmy permission -->
              <n-button
                v-permission="['CUSTOMER_MANAGEMENT:ADD']"
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
              :custom-fields-config-list="customFieldsFilterConfig"
              :filter-config-list="filterConfigList"
              @adv-search="handleAdvSearch"
              @keyword-search="searchData"
            />
          </template>
          <template #view>
            <CrmViewSelect
              v-model:active-tab="activeTab"
              :type="FormDesignKeyEnum.CONTRACT"
              :custom-fields-config-list="customFieldsFilterConfig"
              :filter-config-list="filterConfigList"
              :advanced-original-form="advancedOriginalForm"
              :route-name="ContractRouteEnum.CONTRACT_INDEX"
              @refresh-table-data="searchData"
              @generated-chart="handleGeneratedChart"
            />
          </template>
        </CrmTable>

        <CrmFormCreateDrawer
          v-model:visible="formCreateDrawerVisible"
          :form-key="activeFormKey"
          :source-id="activeSourceId"
          :need-init-detail="needInitDetail"
          :initial-source-name="initialSourceName"
          :other-save-params="otherFollowRecordSaveParams"
          :link-form-key="FormDesignKeyEnum.CONTRACT"
          @saved="searchData"
        />
        <CrmTableExportModal
          v-model:show="showExportModal"
          :params="exportParams"
          :export-columns="exportColumns"
          :is-export-all="isExportAll"
          type="contract"
          @create-success="handleExportCreateSuccess"
        />
      </div>
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import { DataTableRowKey, NButton, useMessage } from 'naive-ui';

  import { FieldTypeEnum, FormDesignKeyEnum, FormLinkScenarioEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import useLocale from '@lib/shared/locale/useLocale';
  import { ExportTableColumnItem } from '@lib/shared/models/common';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmTableExportModal from '@/components/business/crm-table-export-modal/index.vue';
  import CrmViewSelect from '@/components/business/crm-view-select/index.vue';

  import { baseFilterConfigList } from '@/config/clue';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useViewChartParams, { STORAGE_VIEW_CHART_KEY, ViewChartResult } from '@/hooks/useViewChartParams';
  import { getExportColumns } from '@/utils/export';

  import { ContractRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();
  const Message = useMessage();
  const { currentLocale } = useLocale(Message.loading);

  const activeTab = ref();
  const keyword = ref('');

  // 操作
  const checkedRowKeys = ref<DataTableRowKey[]>([]);

  const formCreateDrawerVisible = ref(false);
  const activeSourceId = ref('');
  const initialSourceName = ref('');
  const needInitDetail = ref(false);
  const activeFormKey = ref(FormDesignKeyEnum.CONTRACT);
  const otherFollowRecordSaveParams = ref({
    type: 'CONTRACT',
    id: '',
  });

  function handleNewClick() {
    needInitDetail.value = false;
    activeFormKey.value = FormDesignKeyEnum.CONTRACT;
    activeSourceId.value = '';
    formCreateDrawerVisible.value = true;
  }

  const showExportModal = ref<boolean>(false);
  const isExportAll = ref(false);

  function handleExportAllClick() {
    isExportAll.value = true;
    showExportModal.value = true;
  }
  function handleExportCreateSuccess() {
    checkedRowKeys.value = [];
  }

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.exportChecked'),
        key: 'exportChecked',
        permission: ['CUSTOMER_MANAGEMENT:EXPORT'], // TODO lmy
      },
    ],
  };

  function handleBatchAction(item: ActionsItem) {
    switch (item.key) {
      case 'exportChecked':
        isExportAll.value = false;
        showExportModal.value = true;
        break;
      default:
        break;
    }
  }

  // 表格
  const filterConfigList = computed<FilterFormItem[]>(() => [
    // TODO lmy 部门 已认款金额 归档状态 归档时间 作废原因 作废时间
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
    ...baseFilterConfigList,
  ]);

  const operationGroupList: ActionsItem[] = [
    {
      label: t('common.detail'),
      key: 'detail',
    },
    {
      label: t('common.edit'),
      key: 'edit',
      permission: ['CUSTOMER_MANAGEMENT_CONTACT:UPDATE'], // TODO lmy
    },
    {
      label: 'more',
      key: 'more',
      slotName: 'more',
    },
  ];

  // TODO lmy
  async function handleActionSelect(row: any, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        activeFormKey.value = FormDesignKeyEnum.CONTRACT;
        activeSourceId.value = row.id;
        needInitDetail.value = true;
        otherFollowRecordSaveParams.value.id = row.id;
        formCreateDrawerVisible.value = true;
        break;
      case 'delete':
        break;
      default:
        break;
    }
  }

  const { useTableRes, customFieldsFilterConfig } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.CONTRACT,
    operationColumn: {
      key: 'operation',
      width: currentLocale.value === 'en-US' ? 250 : 200,
      fixed: 'right',
      render: (row: any) =>
        h(CrmOperationButton, {
          groupList: operationGroupList,
          moreList: [
            // TODO lmy 根据状态显示操作？ 【查看 编辑 更多（回款计划 回款 归档 取消归档 作废 取消作废 开票 删除）】
            {
              label: t('module.paymentPlan'),
              key: 'paymentPlan',
              permission: ['CUSTOMER_MANAGEMENT:RECYCLE'],
            },
            {
              label: t('customer.moveToOpenSea'),
              key: 'moveToOpenSea',
              permission: ['CUSTOMER_MANAGEMENT:RECYCLE'],
            },
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
          ],
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
    specialRender: {
      // TODO lmy 状态？
    },
    permission: ['CUSTOMER_MANAGEMENT:RECYCLE', 'CUSTOMER_MANAGEMENT:UPDATE', 'CUSTOMER_MANAGEMENT:DELETE'], // TODO lmy
    containerClass: '.crm-contract-table',
  });
  const { propsRes, propsEvent, tableQueryParams, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;

  const exportColumns = computed<ExportTableColumnItem[]>(() =>
    getExportColumns(propsRes.value.columns, customFieldsFilterConfig.value as FilterFormItem[])
  );
  const exportParams = computed(() => {
    return {
      ...tableQueryParams.value,
      ids: checkedRowKeys.value,
    };
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const tableAdvanceFilterRef = ref<InstanceType<typeof CrmAdvanceFilter>>();

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

  function searchData(val?: string) {
    setLoadListParams({ keyword: val ?? keyword.value, viewId: activeTab.value });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
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

  onBeforeUnmount(() => {
    sessionStorage.removeItem(STORAGE_VIEW_CHART_KEY);
  });
</script>
