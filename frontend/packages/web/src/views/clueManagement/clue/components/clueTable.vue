<template>
  <CrmTable
    ref="crmTableRef"
    v-model:checked-row-keys="checkedRowKeys"
    v-bind="propsRes"
    class="crm-clue-table"
    :not-show-table-filter="isAdvancedSearchMode"
    :action-config="props.readonly ? undefined : actionConfig"
    :fullscreen-target-ref="leadCardRef"
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
      <div v-if="!props.readonly" class="flex items-center gap-[12px]">
        <n-button v-permission="['CLUE_MANAGEMENT:ADD']" type="primary" @click="handleAdd">
          {{ t('clueManagement.newClue') }}
        </n-button>
        <CrmImportButton
          v-if="hasAnyPermission(['CLUE_MANAGEMENT:IMPORT'])"
          :validate-api="preCheckImportLead"
          :import-save-api="importLead"
          :download-template-api="downloadLeadTemplate"
          :title="t('module.clueManagement')"
          @import-success="() => searchData()"
        />
        <n-button
          v-if="
            hasAnyPermission(['CLUE_MANAGEMENT:EXPORT']) && activeTab !== CustomerSearchTypeEnum.CUSTOMER_COLLABORATION
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
        :search-placeholder="t('common.searchByNamePhone')"
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
        :type="FormDesignKeyEnum.CLUE"
        :internal-list="tabList"
        :custom-fields-config-list="filterConfigList"
        :filter-config-list="customFieldsFilterConfig"
        @refresh-table-data="searchData"
      />
    </template>
  </CrmTable>

  <TransferModal
    v-model:show="showTransferModal"
    :source-ids="checkedRowKeys"
    :save-api="batchTransferClue"
    @load-list="handleRefresh"
  />
  <ClueOverviewDrawer
    v-if="isInitOverviewDrawer"
    v-model:show="showOverviewDrawer"
    :detail="activeClue"
    @refresh="handleRefresh"
    @convert-to-customer="() => handleConvertToCustomer(activeClue)"
    @open-customer-drawer="handleOpenCustomerDetail"
  />
  <CrmFormCreateDrawer
    v-if="isInitFormCreateDrawer"
    v-model:visible="formCreateDrawerVisible"
    :form-key="formKey"
    :source-id="activeClueId"
    :need-init-detail="needInitDetail"
    :initial-source-name="activeRowName"
    :other-save-params="otherFollowRecordSaveParams"
    :link-form-info="linkFormInfo"
    @saved="handleFormSaved"
  />
  <CrmTableExportModal
    v-model:show="showExportModal"
    :params="exportParams"
    :export-columns="exportColumns"
    :is-export-all="isExportAll"
    type="clue"
    @create-success="handleExportCreateSuccess"
  />
  <convertToCustomerDrawer
    v-if="isInitConvertDrawer"
    v-model:show="showConvertToCustomerDrawer"
    :clue-id="otherFollowRecordSaveParams.clueId"
    @new="handleNewCustomer"
    @finish="handleRefresh"
  />
  <CrmMoveModal
    v-model:show="showMoveModal"
    :reason-key="ReasonTypeEnum.CLUE_POOL_RS"
    :source-id="moveIds"
    :name="activeRowName"
    @refresh="() => handleRefresh()"
  />
  <customerOverviewDrawer
    v-if="isInitCustomerDrawer"
    v-model:show="showCustomerDrawer"
    :source-id="customerId"
    readonly
  />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { DataTableRowKey, NButton, TabPaneProps, useMessage } from 'naive-ui';

  import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  // import { StageResultEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { ClueListItem } from '@lib/shared/models/clue';
  import { ExportTableColumnItem } from '@lib/shared/models/common';
  import type { TransferParams } from '@lib/shared/models/customer/index';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmImportButton from '@/components/business/crm-import-button/index.vue';
  import CrmMoveModal from '@/components/business/crm-move-modal/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmTableExportModal from '@/components/business/crm-table-export-modal/index.vue';
  import TransferModal from '@/components/business/crm-transfer-modal/index.vue';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import CrmViewSelect from '@/components/business/crm-view-select/index.vue';

  import {
    batchDeleteClue,
    batchTransferClue,
    deleteClue,
    downloadLeadTemplate,
    importLead,
    preCheckImportLead,
    reTransitionCustomer,
  } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import { defaultTransferForm } from '@/config/opportunity';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useHiddenTab from '@/hooks/useHiddenTab';
  import useModal from '@/hooks/useModal';
  import { getExportColumns } from '@/utils/export';
  import { hasAnyPermission } from '@/utils/permission';

  const convertToCustomerDrawer = defineAsyncComponent(() => import('./convertToCustomerDrawer.vue'));
  const ClueOverviewDrawer = defineAsyncComponent(() => import('./clueOverviewDrawer.vue'));
  const customerOverviewDrawer = defineAsyncComponent(
    () => import('@/views/customer/components/customerOverviewDrawer.vue')
  );

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();

  const props = defineProps<{
    tableFormKey: FormDesignKeyEnum.CLUE | FormDesignKeyEnum.SEARCH_GLOBAL_CLUE;
    hiddenAdvanceFilter?: boolean;
    readonly?: boolean;
    isLimitShowDetail?: boolean; // 是否根据权限限查看详情
    hiddenTotal?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'init', val: { filterConfigList: FilterFormItem[]; customFieldsFilterConfig: FilterFormItem[] }): void;
  }>();

  const allTabList: TabPaneProps[] = [
    {
      name: CustomerSearchTypeEnum.ALL,
      tab: t('clue.allClues'),
    },
    {
      name: CustomerSearchTypeEnum.SELF,
      tab: t('clue.myClues'),
    },
    {
      name: CustomerSearchTypeEnum.DEPARTMENT,
      tab: t('clue.departmentClues'),
    },
  ];

  const leadCardRef = ref<HTMLElement | null>(null);

  const { tabList, activeTab } = useHiddenTab(
    allTabList,
    props.tableFormKey === FormDesignKeyEnum.CLUE ? FormDesignKeyEnum.CLUE : undefined
  );

  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const keyword = ref('');
  const activeClueId = ref('');
  const needInitDetail = ref(false);
  const activeClue = ref<ClueListItem>();
  const formKey = ref(FormDesignKeyEnum.CLUE);
  const isInitFormCreateDrawer = ref(false);
  const formCreateDrawerVisible = ref(false);

  const actionConfig = computed<BatchActionConfig>(() => {
    return {
      baseAction: [
        {
          label: t('common.exportChecked'),
          key: 'exportChecked',
          permission: ['CLUE_MANAGEMENT:EXPORT'],
        },
        ...(activeTab.value !== CustomerSearchTypeEnum.CUSTOMER_TRANSITION
          ? [
              {
                label: t('common.batchTransfer'),
                key: 'batchTransfer',
                permission: ['CLUE_MANAGEMENT:UPDATE'],
              },
              {
                label: t('clue.moveIntoCluePool'),
                key: 'moveIntoCluePool',
                permission: ['CLUE_MANAGEMENT:RECYCLE'],
              },
              {
                label: t('common.batchDelete'),
                key: 'batchDelete',
                permission: ['CLUE_MANAGEMENT:DELETE'],
              },
            ]
          : []),
      ],
    };
  });

  const tableRefreshId = ref(0);

  function handleRefresh() {
    checkedRowKeys.value = [];
    tableRefreshId.value += 1;
  }

  const showExportModal = ref<boolean>(false);
  const activeRowName = ref('');
  // 移入线索池
  const showMoveModal = ref(false);
  const moveIds = ref<(string | number) | (string | number)[]>('');
  function handleMoveToLeadPool(row?: ClueListItem) {
    activeRowName.value = row?.name ?? '';
    moveIds.value = row?.id ? row.id : checkedRowKeys.value;
    showMoveModal.value = true;
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
          await batchDeleteClue(checkedRowKeys.value as string[]);
          handleRefresh();
          Message.success(t('common.deleteSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 批量转移
  const showTransferModal = ref<boolean>(false);
  const isExportAll = ref(false);

  function handleBatchAction(item: ActionsItem) {
    switch (item.key) {
      case 'batchTransfer':
        showTransferModal.value = true;
        break;
      case 'moveIntoCluePool':
        handleMoveToLeadPool();
        break;
      case 'batchDelete':
        handleBatchDelete();
        break;
      case 'exportChecked':
        isExportAll.value = false;
        showExportModal.value = true;
        break;
      default:
        break;
    }
  }

  function handleExportAllClick() {
    isExportAll.value = true;
    showExportModal.value = true;
  }

  function handleExportCreateSuccess() {
    checkedRowKeys.value = [];
  }

  // 删除
  function handleDelete(row: ClueListItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('clue.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteClue(row.id);
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
  const transferForm = ref<TransferParams>({
    ...defaultTransferForm,
  });

  function handleTransfer(row: ClueListItem) {
    transferFormRef.value?.formRef?.validate(async (error) => {
      if (!error) {
        try {
          transferLoading.value = true;
          await batchTransferClue({
            ...transferForm.value,
            ids: [row.id],
          });
          Message.success(t('common.transferSuccess'));
          transferForm.value = { ...defaultTransferForm };
          tableRefreshId.value += 1;
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          transferLoading.value = false;
        }
      }
    });
  }

  const linkFormInfo = ref<Record<string, any> | undefined>({});
  // 新增
  function handleAdd() {
    isInitFormCreateDrawer.value = true;
    formKey.value = FormDesignKeyEnum.CLUE;
    activeClueId.value = '';
    linkFormInfo.value = undefined;
    formCreateDrawerVisible.value = true;
  }

  function handleNewCustomer(formInfo: Record<string, any>) {
    isInitFormCreateDrawer.value = true;
    linkFormInfo.value = formInfo;
    formKey.value = FormDesignKeyEnum.CUSTOMER;
    activeClueId.value = '';
    formCreateDrawerVisible.value = true;
  }

  const isInitConvertDrawer = ref(false);
  const showConvertToCustomerDrawer = ref(false);

  const otherFollowRecordSaveParams = ref({
    type: 'CLUE',
    clueId: '',
    id: '',
  });

  function handleConvertToCustomer(row?: ClueListItem) {
    isInitConvertDrawer.value = true;
    activeClueId.value = '';
    formKey.value = FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER;
    activeRowName.value = row?.name || '';
    otherFollowRecordSaveParams.value.clueId = row?.id || '';
    needInitDetail.value = false;
    showConvertToCustomerDrawer.value = true;
  }

  function handleActionSelect(row: ClueListItem, actionKey: string) {
    activeClueId.value = row.id;
    switch (actionKey) {
      case 'edit':
        isInitFormCreateDrawer.value = true;
        otherFollowRecordSaveParams.value.id = row.id;
        needInitDetail.value = true;
        formKey.value = FormDesignKeyEnum.CLUE;
        linkFormInfo.value = undefined;
        formCreateDrawerVisible.value = true;
        break;
      case 'followUp':
        isInitFormCreateDrawer.value = true;
        formKey.value = FormDesignKeyEnum.FOLLOW_RECORD_CLUE;
        otherFollowRecordSaveParams.value.clueId = row.id;
        activeRowName.value = row.name;
        needInitDetail.value = false;
        linkFormInfo.value = undefined;
        formCreateDrawerVisible.value = true;
        break;
      case 'pop-transfer':
        handleTransfer(row);
        break;
      case 'convertToCustomer':
        activeClueId.value = '';
        handleConvertToCustomer(row);
        break;
      case 'moveIntoCluePool':
        handleMoveToLeadPool(row);
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  // 概览
  const isInitOverviewDrawer = ref(false);
  const showOverviewDrawer = ref(false);

  const handleAdvanceFilter = ref<null | ((...args: any[]) => void)>(null);
  const handleSearchData = ref<null | ((...args: any[]) => void)>(null);
  defineExpose({
    handleAdvanceFilter,
    handleSearchData,
  });

  const { useTableRes, customFieldsFilterConfig } = await useFormCreateTable({
    formKey: props.tableFormKey,
    containerClass: '.crm-clue-table',
    operationColumn: props.readonly
      ? undefined
      : {
          key: 'operation',
          width: 200,
          fixed: 'right',
          render: (row: ClueListItem) =>
            row.transitionType && ['CUSTOMER', 'OPPORTUNITY'].includes(row.transitionType)
              ? '-'
              : h(
                  CrmOperationButton,
                  {
                    groupList: [
                      {
                        label: t('common.edit'),
                        key: 'edit',
                        permission: ['CLUE_MANAGEMENT:UPDATE'],
                      },
                      {
                        label: t('opportunity.followUp'),
                        key: 'followUp',
                        permission: ['CLUE_MANAGEMENT:UPDATE'],
                      },
                      {
                        label: t('common.transfer'),
                        key: 'transfer',
                        permission: ['CLUE_MANAGEMENT:UPDATE'],
                        popConfirmProps: {
                          loading: transferLoading.value,
                          title: t('common.transfer'),
                          positiveText: t('common.confirm'),
                          iconType: 'primary',
                        },
                        popSlotContent: 'transferPopContent',
                      },
                      {
                        label: 'more',
                        key: 'more',
                        slotName: 'more',
                      },
                    ],
                    moreList: [
                      {
                        label: t('clue.moveIntoCluePool'),
                        key: 'moveIntoCluePool',
                        permission: ['CLUE_MANAGEMENT:RECYCLE'],
                      },
                      {
                        label: t('clue.convertToCustomer'),
                        key: 'convertToCustomer',
                        permission: ['CLUE_MANAGEMENT:READ', 'CUSTOMER_MANAGEMENT:ADD'],
                        allPermission: true,
                      },
                      {
                        label: t('common.delete'),
                        key: 'delete',
                        danger: true,
                        permission: ['CLUE_MANAGEMENT:DELETE'],
                      },
                    ],
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
      name: (row: ClueListItem) => {
        return props.isLimitShowDetail && row.hasPermission === false
          ? h(CrmNameTooltip, { text: row.name })
          : h(
              CrmTableButton,
              {
                onClick: () => {
                  activeClue.value = row;
                  isInitOverviewDrawer.value = true;
                  showOverviewDrawer.value = true;
                },
              },
              { default: () => row.name, trigger: () => row.name }
            );
      },
    },
    permission: ['CLUE_MANAGEMENT:RECYCLE', 'CLUE_MANAGEMENT:DELETE', 'CLUE_MANAGEMENT:UPDATE'],
    hiddenTotal: !!props.hiddenTotal,
    readonly: props.readonly,
  });
  const { propsRes, propsEvent, tableQueryParams, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;

  const exportParams = computed(() => {
    return {
      ...tableQueryParams.value,
      ids: checkedRowKeys.value,
    };
  });

  async function handleFormSaved(res: any) {
    if (linkFormInfo.value) {
      await reTransitionCustomer({
        clueId: otherFollowRecordSaveParams.value.clueId,
        customerId: res.id,
      });
    }
    loadList();
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
        checkable: true,
        showContainChildModule: true,
        type: 'department',
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
    {
      title: t('customer.collectionTime'),
      dataIndex: 'collectionTime',
      type: FieldTypeEnum.TIME_RANGE_PICKER,
    },
    ...baseFilterConfigList,
  ]);

  const exportColumns = computed<ExportTableColumnItem[]>(() =>
    getExportColumns(filterConfigList.value, customFieldsFilterConfig.value as FilterFormItem[])
  );

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const isAdvancedSearchMode = ref(false);
  function handleAdvSearch(filter: FilterResult, isAdvancedMode: boolean) {
    keyword.value = '';
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

  const showCustomerDrawer = ref(false);
  const customerId = ref('');
  const isInitCustomerDrawer = ref(false);

  function handleOpenCustomerDetail(params: { customerId: string; inCustomerPool: boolean; poolId: string }) {
    customerId.value = params.customerId;
    isInitCustomerDrawer.value = true;
    showCustomerDrawer.value = true;
  }

  watch(
    () => activeTab.value,
    (val) => {
      if (val) {
        checkedRowKeys.value = [];
        searchData();
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
  });
</script>

<style lang="less" scoped>
  :deep(.n-tabs-scroll-padding) {
    width: 16px !important;
  }
</style>
