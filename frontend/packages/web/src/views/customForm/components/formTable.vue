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
        <CrmImportButton
          v-if="!props.readonly"
          :api-type="FormDesignKeyEnum.CUSTOM_FORM"
          :custom-form-id="props.formKey"
          @import-success="() => searchData()"
        />
        <n-button
          v-if="!props.readonly"
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
    @review="handleFormReview"
  />
  <detail
    v-model:visible="showOverviewDrawer"
    :source-id="activeSourceId"
    :customFormId="props.formKey"
    :refreshId="refreshKey"
    @edit="handleEdit"
    @refresh="searchData(undefined, activeSourceId)"
    @delete="removeItemFromList(activeSourceId)"
  />

  <CrmTableExportModal
    v-model:show="showExportModal"
    :params="exportParams"
    :export-columns="exportColumns"
    :is-export-all="isExportAll"
    type="customForm"
    :custom-form-id="props.formKey"
    :custom-form-type-string="formKeyName"
    @create-success="handleExportCreateSuccess"
  />
</template>

<script setup lang="ts">
  import { type DataTableRowKey, NButton, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { ExportTableColumnItem } from '@lib/shared/models/common';
  import type { CustomFormPageItem } from '@lib/shared/models/customForm.js';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { type FilterForm, FilterFormItem, type FilterResult } from '@/components/pure/crm-advance-filter/type';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { BatchActionConfig, CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmApprovalPopover from '@/components/business/crm-approval/components/crm-approval-popover.vue';
  import CrmBatchEditModal from '@/components/business/crm-batch-edit-modal/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmImportButton from '@/components/business/crm-import-button/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmTableExportModal from '@/components/business/crm-table-export-modal/index.vue';
  import detail from './detail.vue';

  import { batchDeleteCustomFormData, deleteCustomFormData } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useApprovalResourceAction from '@/hooks/useApprovalResourceAction';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';
  import { getExportColumns } from '@/utils/export';

  import type { InternalRowData } from 'naive-ui/es/data-table/src/interface';

  const props = defineProps<{
    formKey: string;
    formKeyName: string;
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
  const handleSearchData = ref<null | ((val?: string, refreshId?: string) => void)>(null);
  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const tableRefreshId = ref(0);
  const tableRemoveRefreshId = ref('');
  const formCreateDrawerVisible = ref(false);
  const activeSourceId = ref('');
  const initialSourceName = ref('');
  const needInitDetail = ref(false);
  const refreshKey = ref(0);

  function handleNewClick() {
    needInitDetail.value = false;
    activeSourceId.value = '';
    formCreateDrawerVisible.value = true;
  }

  const tableAdvanceFilterRef = ref<InstanceType<typeof CrmAdvanceFilter>>();
  function handleEdit(id: string) {
    activeSourceId.value = id;
    needInitDetail.value = true;
    formCreateDrawerVisible.value = true;
  }

  const customFormId = computed(() => props.formKey);
  const customFormActionMap: Record<string, ActionsItem> = {
    edit: {
      label: t('common.edit'),
      key: 'edit',
      permission: [],
    },
    delete: {
      label: t('common.delete'),
      key: 'delete',
      permission: [],
    },
  };
  const { initApprovalPermission, resolveRowOperation, enableApproval, deleteExecute } =
    useApprovalOperation<CustomFormPageItem>({
      formType: customFormId,
      dataActionMap: customFormActionMap,
      specialActionFilter: (row, actionKeys) => {
        if (row.isAdmin) {
          return actionKeys;
        }

        return actionKeys.filter((key) => !['edit', 'delete'].includes(key));
      },
      shouldUseRolePermissionOnly: () => true,
    });
  const { reviewByFormResult, reviewByResourceId, revokeByResourceId } = useApprovalResourceAction({
    formKey: customFormId,
  });
  // 删除
  function handleDelete(row: CustomFormPageItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('common.deleteConfirmContent'),
      positiveText: deleteExecute.value ? t('crm.approval.confirmAndSubmitReview') : t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteCustomFormData(row.id);
          Message.success(deleteExecute.value ? t('common.reviewSuccess') : t('common.deleteSuccess'));
          tableRemoveRefreshId.value = row.id;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }
  async function handleActionSelect(row: CustomFormPageItem, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        handleEdit(row.id);
        break;
      case 'delete':
        handleDelete(row);
        break;
      case 'review':
        reviewByResourceId(row.id, {
          onSuccess: () => handleSearchData.value?.(undefined, row.id),
        });
        break;
      case 'revoke':
        revokeByResourceId(row.id, {
          onSuccess: () => handleSearchData.value?.(undefined, row.id),
        });
        break;
      default:
        break;
    }
  }

  const showOverviewDrawer = ref(false);
  const operationColumn = computed<CrmDataTableColumn | undefined>(() => {
    if (props.readonly) {
      return undefined;
    }
    return {
      key: 'operation',
      width: 180,
      fixed: 'right',
      render: (row: CustomFormPageItem) => {
        const actions = resolveRowOperation(row);

        if (!actions.groupList.length && !actions.moreList.length) {
          return '-';
        }

        return h(CrmOperationButton, {
          groupList: actions.groupList,
          moreList: actions.moreList,
          onSelect: (key: string) => handleActionSelect(row, key),
        });
      },
    };
  });

  const { useTableRes, customFieldsFilterConfig, initFormConfig, columns, fieldList } = await useFormCreateTable({
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
      approvalStatus: (row: CustomFormPageItem) => {
        if (!row.approvalStatus) {
          return '-';
        }

        return h(CrmApprovalPopover, {
          status: row.approvalStatus,
          formKey: customFormId.value,
          sourceId: row.id,
          disabled: row.approvalStatus !== ProcessStatusEnum.UNAPPROVED,
          onMore: () => {
            activeSourceId.value = row.id;
            showOverviewDrawer.value = true;
          },
        });
      },
    },
    permission: [],
    containerClass: '.crm-customForm-table',
    readonly: props.readonly,
  });

  const { propsRes, propsEvent, loadList, setLoadListParams, tableQueryParams, setAdvanceFilter } = useTableRes;

  const formColumns = computed(() => {
    if (enableApproval.value) {
      return columns.value;
    }

    return columns.value.filter((column) => column.key !== 'approvalStatus');
  });
  function searchData(val?: string, refreshId?: string) {
    setLoadListParams({ keyword: val ?? keyword.value, customFormId: customFormId.value });
    loadList(false, refreshId, customFormId.value);
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
    loadList(false, undefined, customFormId.value);
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  handleAdvanceFilter.value = handleAdvSearch;

  const exportColumns = computed<ExportTableColumnItem[]>(() =>
    getExportColumns(formColumns.value, customFieldsFilterConfig.value as FilterFormItem[], fieldList.value, true)
  );
  const exportParams = computed(() => {
    return {
      ...tableQueryParams.value,
      ids: checkedRowKeys.value,
    };
  });

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.exportChecked'),
        key: 'exportChecked',
        permission: [],
      },
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

  const showExportModal = ref<boolean>(false);
  const isExportAll = ref(false);
  function handleExportAllClick() {
    isExportAll.value = true;
    showExportModal.value = true;
  }

  function handleExportCreateSuccess() {
    checkedRowKeys.value = [];
  }

  // 批量删除
  function handleBatchDelete() {
    openModal({
      type: 'error',
      title: t('common.batchDeleteTitle', { count: checkedRowKeys.value.length }),
      content: t('common.deleteConfirmContent'),
      positiveText: deleteExecute.value ? t('crm.approval.confirmAndSubmitReview') : t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          tableRefreshId.value += 1;
          await batchDeleteCustomFormData(checkedRowKeys.value as string[]);
          Message.success(deleteExecute.value ? t('common.reviewSuccess') : t('common.deleteSuccess'));
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
      case 'exportChecked':
        isExportAll.value = false;
        showExportModal.value = true;
        break;
      default:
        break;
    }
  }

  function handleFormCreateSaved(res?: any) {
    refreshKey.value += 1;
    if (needInitDetail.value) {
      searchData(undefined);
    } else {
      searchData();
    }
  }

  function handleFormReview(res: any) {
    reviewByFormResult(res, {
      onSuccess: () => {
        if (needInitDetail.value) {
          searchData(undefined, res?.id);
        } else {
          searchData();
        }
      },
    });
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
    propsRes.value.tableKey = val;
    await initApprovalPermission();
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
