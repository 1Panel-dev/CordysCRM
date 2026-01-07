<template>
  <CrmTable
    ref="crmTableRef"
    v-model:checked-row-keys="checkedRowKeys"
    v-bind="propsRes"
    class="crm-business-title-table"
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
          :api-type="ImportTypeExcludeFormDesignEnum.CONTRACT_BUSINESS_TITLE_IMPORT"
          :title="t('module.businessTitle')"
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
  <businessTitleDrawer
    v-model:visible="businessNameDrawerVisible"
    :source-id="activeSourceId"
    @load="() => searchData()"
    @cancel="handleCancel"
  />
  <detail v-model:visible="showDetailDrawer" :source-id="activeSourceId" @edit="handleEdit" @cancel="handleCancel" />
  <CrmTableExportModal
    v-model:show="showExportModal"
    :params="exportParams"
    :export-columns="exportColumns"
    :is-export-all="isExportAll"
    type="businessTitle"
    @create-success="() => (checkedRowKeys = [])"
  />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { DataTableRowKey, NButton, useMessage } from 'naive-ui';

  import { ImportTypeExcludeFormDesignEnum } from '@lib/shared/enums/commonEnum';
  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { ExportTableColumnItem } from '@lib/shared/models/common';
  import type { BusinessTitleItem } from '@lib/shared/models/contract';

  import { COMMON_SELECTION_OPERATORS } from '@/components/pure/crm-advance-filter/index';
  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig, CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmImportButton from '@/components/business/crm-import-button/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmTableExportModal from '@/components/business/crm-table-export-modal/index.vue';
  import businessTitleDrawer from './businessTitleDrawer.vue';
  import detail from './detail.vue';

  import {
    deleteBusinessTitle,
    getBusinessTitleInvoiceCheck,
    getBusinessTitleList,
    revokeBusinessTitle,
  } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import useModal from '@/hooks/useModal';
  import { getExportColumns } from '@/utils/export';

  const props = defineProps<{
    sourceId?: string;
    readonly?: boolean;
  }>();

  const { t } = useI18n();
  const Message = useMessage();

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

  async function deleteHandler(row: BusinessTitleItem) {
    try {
      const isInvoiceChecked = await getBusinessTitleInvoiceCheck(row.id);
      const content = isInvoiceChecked
        ? t('contract.businessTitle.deleteInvoiceContent')
        : t('contract.businessTitle.deleteContent');
      const positiveText = isInvoiceChecked ? t('common.gotIt') : t('common.confirmDelete');
      const type = isInvoiceChecked ? 'default' : 'error';
      openModal({
        type,
        title: t('common.deleteConfirmTitle', { name: characterLimit(row.businessName) }),
        content,
        positiveText,
        negativeText: t('common.cancel'),
        onPositiveClick: async () => {
          try {
            if (isInvoiceChecked) return;
            await deleteBusinessTitle(row.id);
            Message.success(t('common.deleteSuccess'));
            tableRefreshId.value += 1;
          } catch (error) {
            // eslint-disable-next-line no-console
            console.error(error);
          }
        },
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function handleRevoke(row: BusinessTitleItem) {
    try {
      await revokeBusinessTitle(row.id);
      Message.success(t('common.revokeSuccess'));
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  const showDetailDrawer = ref(false);
  function showDetail(row: BusinessTitleItem) {
    activeSourceId.value = row.id;
    showDetailDrawer.value = true;
  }

  function handleCancel() {
    activeSourceId.value = '';
  }

  function handleActionSelect(row: BusinessTitleItem, actionKey: string) {
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
      render: (row: BusinessTitleItem, rowIndex: number) => rowIndex + 1,
    },
    {
      title: t('contract.businessTitle.companyName'),
      key: 'businessName',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
      columnSelectorDisabled: true,
      render: (row: BusinessTitleItem) => {
        const createNameButton = () => [
          // todo xinxinwu
          row.type === 'thirdParty'
            ? h(CrmIcon, {
                type: 'iconicon_enterprise',
                size: 16,
                class: 'mr-[8px] text-[var(--primary-8)]',
              })
            : null,
          h(
            CrmTableButton,
            {
              onClick: () => showDetail(row),
            },
            { default: () => row.businessName, trigger: () => row.businessName }
          ),
        ];

        return props.readonly ? h(CrmNameTooltip, { text: row.businessName }) : createNameButton();
      },
    },
    {
      title: t('contract.businessTitle.bank'),
      key: 'openingBank',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
    },
    {
      title: t('contract.businessTitle.dataSource'),
      key: 'type',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
      render: (row: BusinessTitleItem) => {
        return row.type === 'thirdParty'
          ? t('contract.businessTitle.addMethodThird')
          : t('contract.businessTitle.addMethodCustom');
      },
    },
    {
      title: t('contract.businessTitle.taxpayerNumber'),
      key: 'identificationNumber',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
    },
    {
      title: t('contract.businessTitle.bankAccount'),
      key: 'bankAccount',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
    },
    {
      title: t('contract.businessTitle.phone'),
      key: 'phoneNumber',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
    },
    {
      title: t('contract.businessTitle.capital'),
      key: 'registeredCapital',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
    },
    {
      title: t('contract.businessTitle.address'),
      key: 'registrationAddress',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
    },
    {
      title: t('contract.businessTitle.companyScale'),
      key: 'companySize',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
    },
    {
      title: t('contract.businessTitle.registrationAccount'),
      key: 'registrationNumber',
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
      render: (row: BusinessTitleItem) => {
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
      render: (row: BusinessTitleItem) => {
        return h(CrmNameTooltip, { text: row.updateUserName });
      },
    },
    {
      key: 'operation',
      width: 100,
      fixed: 'right',
      render: (row: BusinessTitleItem) =>
        h(CrmOperationButton, {
          groupList: [
            { label: t('common.edit'), key: 'edit', permission: [] },
            { label: t('common.delete'), key: 'delete', permission: [] },
          ],
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
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

  const { propsRes, propsEvent, tableQueryParams, loadList, setLoadListParams, setAdvanceFilter } = useTable(
    getBusinessTitleList,
    {
      tableKey: TableKeyEnum.CONTRACT_BUSINESS_NAME,
      showSetting: true,
      columns,
      containerClass: '.crm-business-title-list-table',
    }
  );

  const exportColumns = computed<ExportTableColumnItem[]>(() => getExportColumns(propsRes.value.columns));
  const exportParams = computed(() => {
    return {
      ...tableQueryParams.value,
      ids: checkedRowKeys.value,
    };
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
      title: t('contract.businessTitle.companyName'),
      dataIndex: 'companyName',
      type: FieldTypeEnum.INPUT,
    },
    {
      title: t('contract.businessTitle.bank'),
      dataIndex: 'openingBank',
      type: FieldTypeEnum.INPUT,
    },
    {
      title: t('contract.businessTitle.dataSource'),
      dataIndex: 'type',
      type: FieldTypeEnum.SELECT_MULTIPLE,
      operatorOption: COMMON_SELECTION_OPERATORS,
      selectProps: {
        options: [
          { label: t('contract.businessTitle.addMethodThird'), value: 'thirdParty' },
          { label: t('contract.businessTitle.addMethodCustom'), value: 'custom' },
        ],
      },
    },
    {
      title: t('contract.businessTitle.bankAccount'),
      dataIndex: 'identificationNumber',
      type: FieldTypeEnum.INPUT,
    },
    {
      title: t('contract.businessTitle.bankAccount'),
      dataIndex: 'bankAccount',
      type: FieldTypeEnum.INPUT,
    },
    {
      title: t('contract.businessTitle.phone'),
      dataIndex: 'phoneNumber',
      type: FieldTypeEnum.INPUT,
    },
    {
      title: t('contract.businessTitle.capital'),
      dataIndex: 'registeredCapital',
      type: FieldTypeEnum.INPUT_NUMBER,
    },
    {
      title: t('contract.businessTitle.address'),
      dataIndex: 'registrationAddress',
      type: FieldTypeEnum.INPUT,
    },
    {
      title: t('contract.businessTitle.companyScale'),
      dataIndex: 'companySize',
      type: FieldTypeEnum.INPUT,
    },
    {
      title: t('contract.businessTitle.registrationAccount'),
      dataIndex: 'registrationNumber',
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
