<template>
  <CrmTable
    ref="crmTableRef"
    v-model:checked-row-keys="checkedRowKeys"
    v-bind="propsRes"
    class="crm-business-name-table"
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
        <n-button type="primary" @click="handleNewClick">
          {{ t('common.newCreate') }}
        </n-button>
        <CrmImportButton
          :api-type="ImportTypeExcludeFormDesignEnum.CONTRACT_BUSINESS_NAME_IMPORT"
          :title="t('module.businessName')"
          @import-success="() => searchData()"
        />
        <n-button
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
        :filter-config-list="filterConfigList"
        @adv-search="handleAdvSearch"
        @keyword-search="searchData"
      />
    </template>
  </CrmTable>
  <businessNameDrawer
    v-model:visible="businessNameDrawerVisible"
    :source-id="activeSourceId"
    @load="() => searchData()"
    @edit="handleEdit"
  />
  <detail v-model:visible="showDetailDrawer" :source-id="activeSourceId" />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { DataTableRowKey, NButton, useMessage } from 'naive-ui';

  import { ImportTypeExcludeFormDesignEnum } from '@lib/shared/enums/commonEnum';
  import { ContractBusinessNameStatusEnum } from '@lib/shared/enums/contractEnum';
  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { QuotationStatusEnum } from '@lib/shared/enums/opportunityEnum';
  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { ExportTableColumnItem } from '@lib/shared/models/common';
  import type { BusinessNameItem, ContractItem } from '@lib/shared/models/contract';

  import { COMMON_SELECTION_OPERATORS } from '@/components/pure/crm-advance-filter/index';
  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig, CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmImportButton from '@/components/business/crm-import-button/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmTableExportModal from '@/components/business/crm-table-export-modal/index.vue';
  import businessNameDrawer from './businessNameDrawer.vue';
  import businessNameStatus from './businessNameStatus.vue';
  import detail from './detail.vue';

  import { deleteBusinessName, getBusinessNameList, revokeBusinessName } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import { contractBusinessNameStatusOptions } from '@/config/contract';
  import useModal from '@/hooks/useModal';
  import { useUserStore } from '@/store';
  import { getExportColumns } from '@/utils/export';
  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    sourceId?: string;
    readonly?: boolean;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const useStore = useUserStore();

  const keyword = ref('');
  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const activeSourceId = ref('');
  const { openModal } = useModal();
  const tableRefreshId = ref(0);

  const businessNameDrawerVisible = ref(false);
  function handleNewClick() {
    activeSourceId.value = '';
    businessNameDrawerVisible.value = true;
  }
  function handleEdit(id: string) {
    activeSourceId.value = id;
    businessNameDrawerVisible.value = true;
  }

  function deleteHandler(row: BusinessNameItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('contract.businessName.deleteContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteBusinessName(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  async function handleRevoke(row: BusinessNameItem) {
    try {
      await revokeBusinessName(row.id);
      Message.success(t('common.revokeSuccess'));
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  const showDetailDrawer = ref(false);
  function showDetail(row: BusinessNameItem) {
    activeSourceId.value = row.id;
    showDetailDrawer.value = true;
  }

  function handleActionSelect(row: BusinessNameItem, actionKey: string) {
    switch (actionKey) {
      case 'approval':
        showDetail(row);
        break;
      case 'revoke':
        handleRevoke(row);
        break;
      case 'edit':
        handleEdit(row.id);
        break;
      case 'delete':
        deleteHandler(row);
        break;
      default:
        break;
    }
  }

  function getOperationGroupList(row: BusinessNameItem) {
    switch (row.status) {
      case ContractBusinessNameStatusEnum.APPROVING:
        return [
          { label: t('common.approval'), key: 'approval', permission: [] },
          ...(useStore.userInfo.id === row.createUser
            ? [{ label: t('common.revoke'), key: 'revoke', permission: [] }]
            : []),
          { label: t('common.delete'), key: 'delete', permission: [] },
        ];
      case ContractBusinessNameStatusEnum.APPROVED:
        return [{ label: t('common.delete'), key: 'delete', permission: [] }];
      default:
        return [
          { label: t('common.edit'), key: 'edit', permission: [] },
          { label: t('common.delete'), key: 'delete', permission: [] },
        ];
    }
  }

  const columns: CrmDataTableColumn[] = [
    {
      type: 'selection',
      fixed: 'left',
      width: 46,
    },
    {
      fixed: 'left',
      title: t('crmTable.order'),
      width: 50,
      key: SpecialColumnEnum.ORDER,
      resizable: false,
      columnSelectorDisabled: true,
      render: (row: BusinessNameItem, rowIndex: number) => rowIndex + 1,
    },
    {
      title: t('contract.businessName.id'),
      key: 'id',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      fixed: 'left',
      columnSelectorDisabled: true,
      render: (row: BusinessNameItem) => {
        const createNameButton = () =>
          h(
            CrmTableButton,
            {
              onClick: () => showDetail(row),
            },
            { default: () => row.id, trigger: () => row.id }
          );
        return props.readonly ? h(CrmNameTooltip, { text: row.id }) : createNameButton();
      },
    },
    {
      title: t('contract.businessName.companyName'),
      key: 'companyName',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
    },
    {
      title: t('contract.businessName.status'),
      key: 'status',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      filter: true,
      filterOptions: contractBusinessNameStatusOptions,
      width: 200,
      render: (row: BusinessNameItem) => {
        return row.status ? h(businessNameStatus, { status: row.status }) : '-';
      },
    },
    {
      title: t('contract.businessName.bank'),
      key: 'bank',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
    },

    {
      title: t('contract.businessName.dataSource'),
      key: 'dataSource',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
    },
    {
      title: t('contract.businessName.bankAccount'),
      key: 'bankAccount',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
    },
    {
      title: t('contract.businessName.address'),
      key: 'address',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
    },
    {
      title: t('contract.businessName.customerScale'),
      key: 'customerScale',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 200,
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.creator'),
      key: 'createUser',
      sortOrder: false,
      sorter: true,
      width: 200,
      render: (row: BusinessNameItem) => {
        return h(CrmNameTooltip, { text: row.createUserName });
      },
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 150,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUser',
      width: 200,
      sortOrder: false,
      sorter: true,
      render: (row: BusinessNameItem) => {
        return h(CrmNameTooltip, { text: row.updateUserName });
      },
    },
    {
      key: 'operation',
      width: 140,
      fixed: 'right',
      render: (row: BusinessNameItem) =>
        getOperationGroupList(row).length
          ? h(CrmOperationButton, {
              groupList: getOperationGroupList(row),
              onSelect: (key: string) => handleActionSelect(row, key),
            })
          : '-',
    },
  ];

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.exportChecked'),
        key: 'exportChecked',
        permission: [],
      },
    ],
  };

  const isExportAll = ref(false);
  const showExportModal = ref<boolean>(false);
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

  const { propsRes, propsEvent, loadList, setLoadListParams, setAdvanceFilter } = useTable(getBusinessNameList, {
    tableKey: TableKeyEnum.CONTRACT_BUSINESS_NAME,
    showSetting: true,
    columns,
    containerClass: '.crm-business-name-list-table',
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const isAdvancedSearchMode = ref(false);

  function handleAdvSearch(filter: FilterResult, isAdvancedMode: boolean, originalForm?: FilterForm) {
    keyword.value = '';
    isAdvancedSearchMode.value = isAdvancedMode;
    setAdvanceFilter(filter);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function searchData(val?: string) {
    setLoadListParams({ keyword: val ?? keyword.value });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function handleExportAllClick() {
    isExportAll.value = true;
    showExportModal.value = true;
  }

  const filterConfigList = computed<FilterFormItem[]>(() => [
    {
      title: t('contract.businessName.id'),
      dataIndex: 'id',
      type: FieldTypeEnum.INPUT,
    },
    {
      title: t('contract.businessName.companyName'),
      dataIndex: 'companyName',
      type: FieldTypeEnum.INPUT,
    },
    {
      title: t('contract.businessName.status'),
      dataIndex: 'status',
      type: FieldTypeEnum.SELECT_MULTIPLE,
      operatorOption: COMMON_SELECTION_OPERATORS,
      selectProps: {
        options: contractBusinessNameStatusOptions,
      },
    },
    {
      title: t('contract.businessName.bank'),
      dataIndex: 'bank',
      type: FieldTypeEnum.INPUT,
    },
    {
      title: t('contract.businessName.dataSource'),
      dataIndex: 'dataSource',
      type: FieldTypeEnum.SELECT_MULTIPLE,
      operatorOption: COMMON_SELECTION_OPERATORS,
      selectProps: {
        options: [
          { label: t('contract.businessName.addMethodThird'), value: 'third' },
          { label: t('contract.businessName.addMethodCustom'), value: 'custom' },
        ],
      },
    },
    {
      title: t('contract.businessName.bankAccount'),
      dataIndex: 'bankAccount',
      type: FieldTypeEnum.INPUT,
    },
    {
      title: t('contract.businessName.address'),
      dataIndex: 'address',
      type: FieldTypeEnum.INPUT,
    },
    {
      title: t('contract.businessName.customerScale'),
      dataIndex: 'customerScale',
      type: FieldTypeEnum.INPUT,
    },
    ...baseFilterConfigList,
  ]);

  watch(
    () => tableRefreshId.value,
    () => {
      checkedRowKeys.value = [];
      searchData();
    }
  );

  onBeforeMount(() => {
    searchData();
  });
</script>

<style scoped></style>
