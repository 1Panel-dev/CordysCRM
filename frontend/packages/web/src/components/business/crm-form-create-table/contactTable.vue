<template>
  <CrmCard hide-footer no-content-bottom-padding :special-height="props.specialHeight">
    <CrmTable
      ref="crmTableRef"
      v-model:checked-row-keys="checkedRowKeys"
      v-bind="propsRes"
      class="crm-contact-table"
      :not-show-table-filter="isAdvancedSearchMode"
      :action-config="props.readonly ? undefined : actionConfig"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
      @batch-action="handleBatchAction"
      @refresh="searchData"
    >
      <template v-if="props.readonly" #tableTop>
        <slot name="searchTableTotal" :total="propsRes.crmPagination?.itemCount || 0"></slot>
        <CrmSearchInput
          v-if="props.sourceId"
          v-model:value="keyword"
          class="!w-[240px]"
          :placeholder="t('common.searchByNamePhone')"
          @search="searchData"
        />
      </template>
      <template #actionLeft>
        <div v-if="!props.readonly" class="flex items-center gap-[12px]">
          <n-button v-permission="['CUSTOMER_MANAGEMENT_CONTACT:ADD']" type="primary" @click="handleCreate">
            {{ t('overviewDrawer.addContract') }}
          </n-button>
          <CrmImportButton
            v-if="hasAnyPermission(['CUSTOMER_MANAGEMENT_CONTACT:IMPORT']) && !props.sourceId"
            :api-type="FormDesignKeyEnum.CONTACT"
            :title="t('customer.contact')"
            @import-success="() => searchData()"
          />
          <n-button
            v-if="!props.sourceId"
            v-permission="['CUSTOMER_MANAGEMENT_CONTACT:EXPORT']"
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
        <CrmSearchInput
          v-if="props.sourceId"
          v-model:value="keyword"
          class="!w-[240px]"
          :placeholder="t('common.searchByNamePhone')"
          @search="searchData"
        />
        <CrmAdvanceFilter
          v-if="!props.hiddenAdvanceFilter && !props.sourceId"
          ref="tableAdvanceFilterRef"
          v-model:keyword="keyword"
          :custom-fields-config-list="filterConfigList"
          :filter-config-list="customFieldsFilterConfig"
          :search-placeholder="t('common.searchByNamePhone')"
          @adv-search="handleAdvSearch"
          @keyword-search="searchData"
        />
      </template>
      <template #view>
        <CrmViewSelect
          v-if="props.formKey === FormDesignKeyEnum.CONTACT"
          v-model:active-tab="activeTab"
          :type="FormDesignKeyEnum.CONTACT"
          :custom-fields-config-list="filterConfigList"
          :filter-config-list="customFieldsFilterConfig"
          :advanced-original-form="advancedOriginalForm"
          @refresh-table-data="searchData"
          @generated-chart="handleGeneratedChart"
        />
      </template>
    </CrmTable>
    <CrmFormCreateDrawer
      v-model:visible="formCreateDrawerVisible"
      :form-key="FormDesignKeyEnum.CONTACT"
      :source-id="activeContactId"
      :need-init-detail="needInitDetail"
      :initial-source-name="props.initialSourceName"
      @saved="() => searchData()"
    />
    <!-- 停用 -->
    <CrmModal
      v-model:show="deactivateModalVisible"
      size="small"
      :ok-loading="deactivateLoading"
      :positive-text="t('common.deactivate')"
      @confirm="handleDeactivate"
    >
      <template #title>
        <div class="flex gap-[4px] overflow-hidden">
          <div class="text-[var(--text-n1)]">{{ t('common.deactivationReason') }}</div>
          <div class="flex text-[var(--text-n4)]">
            (
            <div class="one-line-text max-w-[300px]">{{ activeContactName }}</div>
            )
          </div>
        </div>
      </template>
      <n-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-placement="left"
        label-width="auto"
        require-mark-placement="right"
      >
        <n-form-item path="reason" :label="t('common.deactivationReason')">
          <n-input
            v-model:value="form.reason"
            type="textarea"
            :placeholder="t('common.pleaseInput')"
            allow-clear
            maxlength="200"
            show-count
          />
        </n-form-item>
      </n-form>
    </CrmModal>

    <CrmTableExportModal
      v-model:show="showExportModal"
      :params="exportParams"
      :export-columns="exportColumns"
      :is-export-all="isExportAll"
      type="contact"
      @create-success="handleExportCreateSuccess"
    />

    <CrmBatchEditModal
      v-model:visible="showEditModal"
      v-model:field-list="fieldList"
      :ids="checkedRowKeys"
      :form-key="FormDesignKeyEnum.CUSTOMER_CONTACT"
      @refresh="handleRefresh"
    />
  </CrmCard>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import {
    DataTableRowKey,
    FormInst,
    FormRules,
    NButton,
    NForm,
    NFormItem,
    NInput,
    NSwitch,
    useMessage,
  } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { ExportTableColumnItem } from '@lib/shared/models/common';
  import type { CustomerContractListItem } from '@lib/shared/models/customer';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmBatchEditModal from '@/components/business/crm-batch-edit-modal/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmImportButton from '@/components/business/crm-import-button/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmTableExportModal from '@/components/business/crm-table-export-modal/index.vue';
  import CrmViewSelect from '@/components/business/crm-view-select/index.vue';

  import {
    checkOpportunity,
    deleteCustomerContact,
    disableCustomerContact,
    enableCustomerContact,
  } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';
  import { getExportColumns } from '@/utils/export';
  import { hasAnyPermission } from '@/utils/permission';

  import { AppRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    sourceId?: string;
    refreshKey?: number;
    initialSourceName?: string;
    readonly?: boolean;
    formKey:
      | FormDesignKeyEnum.CONTACT
      | FormDesignKeyEnum.CUSTOMER_CONTACT
      | FormDesignKeyEnum.BUSINESS_CONTACT
      | FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT;
    specialHeight?: number;
    hiddenAdvanceFilter?: boolean;
    hiddenTotal?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'init', val: { filterConfigList: FilterFormItem[]; customFieldsFilterConfig: FilterFormItem[] }): void;
  }>();

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();
  const router = useRouter();

  const keyword = ref('');
  const formCreateDrawerVisible = ref(false);
  const activeContactId = ref('');
  const activeContactName = ref('');
  const needInitDetail = ref(false);
  const tableRefreshId = ref(0);

  const activeTab = ref();

  const operationGroupList: ActionsItem[] = [
    {
      label: t('common.edit'),
      key: 'edit',
      permission: ['CUSTOMER_MANAGEMENT_CONTACT:UPDATE'],
    },
    {
      label: t('common.delete'),
      key: 'delete',
      permission: ['CUSTOMER_MANAGEMENT_CONTACT:DELETE'],
    },
  ];

  const actionConfig = computed<BatchActionConfig>(() => ({
    baseAction: !props.sourceId
      ? [
          {
            label: t('common.exportChecked'),
            key: 'exportChecked',
            permission: ['CUSTOMER_MANAGEMENT_CONTACT:EXPORT'],
          },
          {
            label: t('common.batchEdit'),
            key: 'batchEdit',
            permission: ['CUSTOMER_MANAGEMENT_CONTACT:UPDATE'],
          },
        ]
      : [],
  }));

  function handleCreate() {
    if (props.sourceId) {
      activeContactId.value = props.sourceId;
    } else {
      activeContactId.value = '';
    }
    needInitDetail.value = false;
    formCreateDrawerVisible.value = true;
  }

  async function bindOpportunity(id: string) {
    try {
      return await checkOpportunity(id);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  // 删除
  async function handleDelete(row: CustomerContractListItem) {
    const hasData = await bindOpportunity(row.id);
    const title = hasData
      ? t('customer.contact.deleteTitle')
      : t('common.deleteConfirmTitle', { name: characterLimit(row.name) });
    const content = hasData ? '' : t('customer.contact.deleteContentTip');
    const positiveText = t(hasData ? 'opportunity.gotIt' : 'common.confirm');
    const negativeText = t(hasData ? 'opportunity.goMove' : 'common.cancel');

    openModal({
      type: 'error',
      title,
      content,
      positiveText,
      negativeText,
      positiveButtonProps: {
        type: hasData ? 'primary' : 'error',
        size: 'medium',
      },
      onPositiveClick: async () => {
        if (!hasData) {
          try {
            await deleteCustomerContact(row.id);
            Message.success(t('common.deleteSuccess'));
            tableRefreshId.value += 1;
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
          }
        }
      },
      onNegativeClick: () => {
        if (hasData) {
          router.push({
            name: AppRouteEnum.OPPORTUNITY,
          });
        }
      },
    });
  }

  // 切换状态
  const deactivateModalVisible = ref(false);
  const deactivateLoading = ref(false);
  const formRef = ref<FormInst | null>(null);
  const form = ref<{ reason: string }>({ reason: '' });
  const rules: FormRules = {
    reason: [{ required: true, message: t('common.notNull', { value: t('common.deactivationReason') }) }],
  };

  async function handleToggleStatus(row: CustomerContractListItem) {
    if (row.enable) {
      deactivateModalVisible.value = true;
      form.value.reason = '';
      activeContactName.value = row.name;
      activeContactId.value = row.id;
    } else {
      openModal({
        type: 'default',
        title: t('common.confirmEnableTitle', { name: characterLimit(row.name) }),
        positiveText: t('common.enable'),
        negativeText: t('common.cancel'),
        onPositiveClick: async () => {
          try {
            await enableCustomerContact(row.id);
            Message.success(t('common.activated'));
            tableRefreshId.value += 1;
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
          }
        },
      });
    }
  }

  async function handleDeactivate() {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          deactivateLoading.value = true;
          await disableCustomerContact(activeContactId.value, form.value.reason);
          Message.success(t('common.deactivated'));
          deactivateModalVisible.value = false;
          tableRefreshId.value += 1;
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          deactivateLoading.value = false;
        }
      }
    });
  }

  function handleActionSelect(row: CustomerContractListItem, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        activeContactId.value = row.id;
        activeContactName.value = row.name;
        needInitDetail.value = true;
        formCreateDrawerVisible.value = true;
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }
  const handleAdvanceFilter = ref<null | ((...args: any[]) => void)>(null);
  const handleSearchData = ref<null | ((...args: any[]) => void)>(null);

  defineExpose({
    handleAdvanceFilter,
    handleSearchData,
  });
  const { useTableRes, customFieldsFilterConfig, fieldList } = await useFormCreateTable({
    formKey: props.formKey,
    showPagination: !props.sourceId,
    readonly: props.readonly,
    containerClass: '.crm-contact-table',
    hiddenTotal: !!props.hiddenTotal,
    operationColumn: props.readonly
      ? undefined
      : {
          key: 'operation',
          width: 100,
          fixed: 'right',
          render: (row: CustomerContractListItem) =>
            h(CrmOperationButton, {
              groupList: operationGroupList,
              onSelect: (key: string) => handleActionSelect(row, key),
            }),
        },
    specialRender: {
      status: (row: CustomerContractListItem) => {
        return h(NSwitch, {
          value: row.enable,
          disabled: !hasAnyPermission(['CUSTOMER_MANAGEMENT_CONTACT:UPDATE']) || props.readonly,
          onClick: () => {
            if (!hasAnyPermission(['CUSTOMER_MANAGEMENT_CONTACT:UPDATE']) || props.readonly) return;
            handleToggleStatus(row);
          },
        });
      },
      customerId: (row: CustomerContractListItem) => {
        return h(CrmNameTooltip, { text: row.customerName });
      },
      name: (row: CustomerContractListItem) => {
        return h(CrmNameTooltip, { text: row.name });
      },
      owner: (row: CustomerContractListItem) => row.ownerName ?? '-',
    },
  });

  const { propsRes, propsEvent, loadList, setLoadListParams, setAdvanceFilter, tableQueryParams } = useTableRes;
  const backupData = ref<CustomerContractListItem[]>([]);

  const filterConfigList = computed(() => [
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
    ...baseFilterConfigList,
  ]);

  const checkedRowKeys = ref<DataTableRowKey[]>([]);

  const exportColumns = computed<ExportTableColumnItem[]>(() =>
    getExportColumns(propsRes.value.columns, customFieldsFilterConfig.value as FilterFormItem[])
  );

  const exportParams = computed(() => {
    return {
      ...tableQueryParams.value,
      ids: checkedRowKeys.value,
    };
  });

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
      case 'exportChecked':
        isExportAll.value = false;
        showExportModal.value = true;
        break;
      case 'batchEdit':
        handleBatchEdit();
        break;
      default:
        break;
    }
  }

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  async function searchData(val?: string) {
    if (props.sourceId) {
      if (val) {
        const lowerCaseVal = val.toLowerCase();
        propsRes.value.data = backupData.value.filter((item: CustomerContractListItem) => {
          return item.name.toLowerCase().includes(lowerCaseVal) || item.phone.toLowerCase().includes(lowerCaseVal);
        });
      } else {
        setLoadListParams({ id: props.sourceId });
        await loadList(true);
        backupData.value = cloneDeep(propsRes.value.data);
      }
    } else {
      setLoadListParams({ keyword: val ?? keyword.value, viewId: activeTab.value });
      await loadList();
      backupData.value = cloneDeep(propsRes.value.data);
    }
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function handleRefresh() {
    checkedRowKeys.value = [];
    searchData();
  }

  handleSearchData.value = searchData;

  const tableAdvanceFilterRef = ref<InstanceType<typeof CrmAdvanceFilter>>();
  const isAdvancedSearchMode = ref(false);
  const advancedOriginalForm = ref<FilterForm | undefined>();
  function handleAdvSearch(filter: FilterResult, isAdvancedMode: boolean, originalForm?: FilterForm) {
    advancedOriginalForm.value = originalForm;
    keyword.value = '';
    isAdvancedSearchMode.value = isAdvancedMode;
    setAdvanceFilter(filter);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function handleGeneratedChart(res: FilterResult, _form: FilterForm) {
    advancedOriginalForm.value = _form;
    setAdvanceFilter(res);
    tableAdvanceFilterRef.value?.setAdvancedFilter(res, true);
    searchData();
  }

  handleAdvanceFilter.value = handleAdvSearch;

  watch(
    () => tableRefreshId.value,
    () => {
      crmTableRef.value?.clearCheckedRowKeys();
      searchData();
    }
  );

  watch(
    () => props.refreshKey,
    (val) => {
      if (val) {
        crmTableRef.value?.clearCheckedRowKeys();
        searchData();
      }
    }
  );

  onMounted(() => {
    if (props.hiddenAdvanceFilter) {
      emit('init', {
        filterConfigList: filterConfigList.value,
        customFieldsFilterConfig: customFieldsFilterConfig.value as FilterFormItem[],
      });
    }
    if (!props.sourceId) return;
    searchData();
  });

  watch(
    () => activeTab.value,
    (val) => {
      if (val) {
        setLoadListParams({ keyword: keyword.value, viewId: activeTab.value });
        crmTableRef.value?.setColumnSort(val);
      }
    },
    { immediate: true }
  );
</script>
