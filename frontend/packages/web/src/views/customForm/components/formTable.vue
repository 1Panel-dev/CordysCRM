<template>
  <CrmTable
    ref="crmTableRef"
    v-model:checked-row-keys="checkedRowKeys"
    v-bind="propsRes"
    class="crm-customForm-table"
    :not-show-table-filter="isAdvancedSearchMode"
    :action-config="props.readonly ? undefined : actionConfig"
    :columns="formColumns"
    :table-key="customFormId"
    @row-key-change="handleRowKeyChange"
    @page-change="propsEvent.pageChange"
    @page-size-change="propsEvent.pageSizeChange"
    @sorter-change="propsEvent.sorterChange"
    @filter-change="propsEvent.filterChange"
    @batch-action="handleBatchAction"
    @refresh="searchData"
  >
    <template #actionLeft>
      <div class="flex items-center gap-[12px]">
        <n-button v-if="!props.readonly" type="primary" @click="handleNewClick">
          {{ t('common.add') }}
        </n-button>
      </div>
    </template>
    <template #actionRight>
      <CrmAdvanceFilter
        ref="tableAdvanceFilterRef"
        v-model:keyword="keyword"
        :custom-fields-config-list="customFieldsFilterConfig"
        :filter-config-list="baseFilterConfigList"
        @adv-search="handleAdvSearch"
        @keyword-search="searchData"
      />
    </template>
  </CrmTable>

  <CrmBatchEditModal
    v-model:visible="showEditModal"
    v-model:field-list="editFieldList"
    :ids="checkedRowKeys"
    :form-key="FormDesignKeyEnum.CUSTOM_FORM"
    :otherSaveParams="{
      customFormId: props.formKey,
    }"
    @refresh="() => (tableRefreshId += 1)"
  />
  <CrmFormCreateDrawer
    v-model:visible="formCreateDrawerVisible"
    :form-key="FormDesignKeyEnum.CUSTOM_FORM"
    :source-id="activeSourceId"
    :need-init-detail="needInitDetail"
    :initial-source-name="initialSourceName"
    :custom-form-id="props.formKey"
    @saved="handleFormCreateSaved"
  />
  <detail
    v-model:visible="showOverviewDrawer"
    :source-id="activeSourceId"
    :customFormId="props.formKey"
    @edit="handleEdit"
    @refresh="removeItemFromList(activeSourceId)"
  />
</template>

<script setup lang="ts">
  import { type DataTableRowKey, NButton, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { CustomFormPageItem } from '@lib/shared/models/customForm.js';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { type FilterForm, type FilterResult } from '@/components/pure/crm-advance-filter/type';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { BatchActionConfig, CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmBatchEditModal from '@/components/business/crm-batch-edit-modal/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import detail from './detail.vue';

  import { batchDeleteCustomFormData, deleteCustomFormData } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';

  import type { InternalRowData } from 'naive-ui/es/data-table/src/interface';

  const props = defineProps<{
    formKey: string;
    readonly?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'init'): void;
    (e: 'showCountDetail', row: Record<string, any>, type: 'opportunity' | 'clue'): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const keyword = ref('');
  const isAdvancedSearchMode = ref(false);
  const advancedOriginalForm = ref<FilterForm | undefined>();
  const handleAdvanceFilter = ref<null | ((...args: any[]) => void)>(null);
  const handleSearchData = ref<null | ((...args: any[]) => void)>(null);
  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const tableRefreshId = ref(0);
  const tableRemoveRefreshId = ref('');
  const formCreateDrawerVisible = ref(false);
  const activeSourceId = ref('');
  const initialSourceName = ref('');
  const needInitDetail = ref(false);

  function handleNewClick() {
    needInitDetail.value = false;
    activeSourceId.value = '';
    formCreateDrawerVisible.value = true;
  }

  const tableAdvanceFilterRef = ref<InstanceType<typeof CrmAdvanceFilter>>();
  const operationGroupList = [
    {
      label: t('common.edit'),
      key: 'edit',
      permission: [],
    },
    {
      label: t('common.delete'),
      key: 'delete',
      permission: [],
    },
  ];

  // 删除
  function handleDelete(row: CustomFormPageItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('common.deleteConfirmContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteCustomFormData(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRemoveRefreshId.value = row.id;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleEdit(id: string) {
    activeSourceId.value = id;
    needInitDetail.value = true;
    formCreateDrawerVisible.value = true;
  }

  async function handleActionSelect(row: any, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        handleEdit(row.id);
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  const showOverviewDrawer = ref(false);
  const customFormId = computed(() => props.formKey);
  const operationColumn = computed<CrmDataTableColumn | undefined>(() => {
    if (props.readonly) {
      return undefined;
    }
    return {
      key: 'operation',
      width: 120,
      fixed: 'right',
      render: (row: CustomFormPageItem) =>
        row.isAdmin
          ? h(CrmOperationButton, {
              groupList: row.isAdmin ? operationGroupList : [],
              onSelect: (key: string) => handleActionSelect(row, key),
            })
          : '-',
    };
  });

  const { useTableRes, customFieldsFilterConfig, initFormConfig, columns } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.CUSTOM_FORM,
    customFormId,
    disabledSelection: (row: CustomFormPageItem) => {
      return !row.isAdmin || props.readonly;
    },
    operationColumn: operationColumn.value,
    specialRender: {
      name: (row: CustomFormPageItem) => {
        return h(
          CrmTableButton,
          {
            onClick: () => {
              activeSourceId.value = row.id;
              showOverviewDrawer.value = true;
            },
          },
          { trigger: () => row.name, default: () => row.name }
        );
      },
    },
    permission: [],
    containerClass: '.crm-customForm-table',
    readonly: props.readonly,
  });

  const { propsRes, propsEvent, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;

  const formColumns = computed(() => columns.value);
  function searchData(val?: string, refreshId?: string) {
    setLoadListParams({ keyword: val ?? keyword.value, customFormId: customFormId.value });
    loadList(false, refreshId);
    if (!refreshId) {
      crmTableRef.value?.scrollTo({ top: 0 });
    }
  }
  handleSearchData.value = searchData;

  function handleAdvSearch(filter: FilterResult, isAdvancedMode: boolean, originalForm?: FilterForm) {
    keyword.value = '';
    advancedOriginalForm.value = originalForm;
    isAdvancedSearchMode.value = isAdvancedMode;
    setAdvanceFilter(filter);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  handleAdvanceFilter.value = handleAdvSearch;

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.batchEdit'),
        key: 'batchEdit',
        permission: [],
      },
      {
        label: t('common.batchDelete'),
        key: 'batchDelete',
        permission: [],
      },
    ],
  };

  const selectedRows = ref<InternalRowData[]>([]);
  function handleRowKeyChange(keys: DataTableRowKey[], _rows: InternalRowData[]) {
    selectedRows.value = _rows;
  }

  const showEditModal = ref(false);
  const { initFormConfig: initEditFormConfig, fieldList: editFieldList } = useFormCreateApi({
    formKey: ref(FormDesignKeyEnum.CUSTOM_FORM),
    customFormId,
  });
  function handleBatchEdit() {
    initEditFormConfig();
    showEditModal.value = true;
  }

  // 批量删除
  function handleBatchDelete() {
    openModal({
      type: 'error',
      title: t('common.batchDeleteTitle', { count: checkedRowKeys.value.length }),
      content: t('common.deleteConfirmContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          tableRefreshId.value += 1;
          await batchDeleteCustomFormData(checkedRowKeys.value as string[]);
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
      case 'batchEdit':
        handleBatchEdit();
        break;
      case 'batchDelete':
        handleBatchDelete();
        break;
      default:
        break;
    }
  }

  function handleFormCreateSaved(res?: any) {
    if (needInitDetail.value) {
      searchData(undefined);
    } else {
      searchData();
    }
  }

  function removeItemFromList(id: string) {
    propsRes.value.data = propsRes.value.data.filter((item) => item.id !== id);
    propsRes.value.crmPagination = {
      ...propsRes.value.crmPagination,
      itemCount: (propsRes.value.crmPagination?.itemCount ?? 1) - 1,
    };
  }

  watch(
    () => tableRemoveRefreshId.value,
    (val) => {
      if (val) {
        removeItemFromList(val);
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

  async function init(val: string) {
    checkedRowKeys.value = [];
    keyword.value = '';
    await initFormConfig(props.readonly, operationColumn.value);
    tableAdvanceFilterRef.value?.clearFilter();
    setLoadListParams({ customFormId: val });
    searchData();
  }

  watch(
    () => props.formKey,
    (val) => {
      init(val);
    },
    {
      immediate: true,
    }
  );

  defineExpose({
    init,
  });
</script>

<style lang="less" scoped></style>
