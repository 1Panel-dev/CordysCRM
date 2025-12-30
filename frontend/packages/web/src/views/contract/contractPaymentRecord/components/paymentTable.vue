<template>
  <CrmTable
    ref="crmTableRef"
    v-model:checked-row-keys="checkedRowKeys"
    v-bind="propsRes"
    :fullscreen-target-ref="props.fullscreenTargetRef"
    :class="`crm-contract-payment-table-${props.formKey}`"
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
        <n-button
          v-if="!props.readonly"
          v-permission="['CONTRACT_PAYMENT_PLAN:ADD']"
          :loading="createLoading"
          type="primary"
          @click="handleNewClick"
        >
          {{ t('contract.paymentRecord.new') }}
        </n-button>
        <!-- TODO lmy 导入 -->
        <n-button
          v-permission="['CONTRACT_PAYMENT_PLAN:EXPORT']"
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
        v-if="!props.isContractTab"
        v-model:active-tab="activeTab"
        :type="FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD"
        :custom-fields-config-list="customFieldsFilterConfig"
        :filter-config-list="filterConfigList"
        :advanced-original-form="advancedOriginalForm"
        :route-name="ContractRouteEnum.CONTRACT_PAYMENT_RECORD"
        @refresh-table-data="searchData"
      />
    </template>
  </CrmTable>

  <CrmFormCreateDrawer
    v-model:visible="formCreateDrawerVisible"
    :form-key="activeFormKey"
    :source-id="activeSourceId"
    :need-init-detail="needInitDetail"
    :initial-source-name="initialSourceName"
    :link-form-key="FormDesignKeyEnum.CONTRACT"
    :link-form-info="linkFormInfo"
    @saved="() => searchData()"
  />
  <CrmTableExportModal
    v-model:show="showExportModal"
    :params="exportParams"
    :export-columns="exportColumns"
    :is-export-all="isExportAll"
    type="contractPaymentRecord"
    @create-success="handleExportCreateSuccess"
  />

  <DetailDrawer
    v-model:visible="showDetailDrawer"
    :sourceId="activeSourceId"
    :readonly="props.readonly"
    @refresh="searchData"
    @open-contract-drawer="showContractDrawer"
  />
</template>

<script setup lang="ts">
  import { DataTableRowKey, NButton, useMessage } from 'naive-ui';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import useLocale from '@lib/shared/locale/useLocale';
  import { ExportTableColumnItem } from '@lib/shared/models/common';
  import type { PaymentRecordItem } from '@lib/shared/models/contract';

  import { COMMON_SELECTION_OPERATORS } from '@/components/pure/crm-advance-filter/index';
  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmTableExportModal from '@/components/business/crm-table-export-modal/index.vue';
  import CrmViewSelect from '@/components/business/crm-view-select/index.vue';
  import DetailDrawer from './detail.vue';

  import { deletePaymentRecord, updatePaymentRecord } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';
  import { getExportColumns } from '@/utils/export';
  import { hasAnyPermission } from '@/utils/permission';

  import { ContractRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();
  const Message = useMessage();
  const { currentLocale } = useLocale(Message.loading);
  const { openModal } = useModal();

  const props = defineProps<{
    fullscreenTargetRef?: HTMLElement | null;
    isContractTab?: boolean;
    sourceId?: string; // 合同详情下
    sourceName?: string;
    readonly?: boolean;
    formKey: FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD;
  }>();
  const emit = defineEmits<{
    (e: 'openContractDrawer', params: { id: string }): void;
  }>();

  const activeTab = ref();
  const keyword = ref('');

  // 操作
  const checkedRowKeys = ref<DataTableRowKey[]>([]);

  const formCreateDrawerVisible = ref(false);
  const activeSourceId = ref(props.sourceId || '');
  const initialSourceName = ref('');
  const needInitDetail = ref(false);
  const activeFormKey = ref(FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD);

  const createLoading = ref(false);
  const linkFormKey = ref(FormDesignKeyEnum.CONTRACT);
  const linkFormInfo = ref();
  const { initFormDetail, initFormConfig, linkFormFieldMap } = useFormCreateApi({
    formKey: linkFormKey,
    sourceId: activeSourceId,
  });

  async function handleNewClick() {
    try {
      createLoading.value = true;
      activeSourceId.value = props.isContractTab ? props.sourceId || '' : '';
      initialSourceName.value = props.isContractTab ? props.sourceName || '' : '';
      needInitDetail.value = false;
      activeFormKey.value = FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD;
      if (props.isContractTab) {
        linkFormKey.value = FormDesignKeyEnum.CONTRACT;
        await initFormConfig();
        await initFormDetail(false, true);
      }
      linkFormInfo.value = linkFormFieldMap.value;
      formCreateDrawerVisible.value = true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      createLoading.value = false;
    }
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
        permission: ['CONTRACT_PAYMENT_PLAN:EXPORT'], // TODO lmy permission
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

  const operationGroupList = computed<ActionsItem[]>(() => {
    return [
      {
        label: t('common.detail'),
        key: 'detail',
      },
      ...(!props.readonly
        ? [
            {
              label: t('common.edit'),
              key: 'edit',
              permission: ['CONTRACT_PAYMENT_PLAN:UPDATE'], // TODO lmy permission
            },
            {
              label: t('common.delete'),
              key: 'delete',
              permission: ['CONTRACT_PAYMENT_PLAN:DELETE'], // TODO lmy permission
            },
          ]
        : []),
    ];
  });

  const tableRefreshId = ref(0);
  const showDetailDrawer = ref(false);

  function handleDelete(row: PaymentRecordItem) {
    openModal({
      type: 'error',
      title: t('system.personal.confirmDelete'),
      content: t('common.deleteConfirmContent'), // TODO lmy 删除以后需要扣除已经款金额；
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deletePaymentRecord(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleEdit(id: string) {
    activeFormKey.value = FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD;
    activeSourceId.value = id;
    needInitDetail.value = true;
    formCreateDrawerVisible.value = true;
  }

  function showDetail(id: string) {
    activeSourceId.value = id;
    showDetailDrawer.value = true;
  }

  async function handleActionSelect(row: PaymentRecordItem, actionKey: string) {
    switch (actionKey) {
      case 'detail':
        showDetail(row.id);
        break;
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

  function showContractDrawer(params: { id: string }) {
    if (props.isContractTab) {
      showDetailDrawer.value = false;
    } else {
      emit('openContractDrawer', {
        id: params.id,
      });
    }
  }

  const { useTableRes, customFieldsFilterConfig } = await useFormCreateTable({
    formKey: props.formKey,
    excludeFieldIds: ['contractId'],
    operationColumn: {
      key: 'operation',
      width: currentLocale.value === 'en-US' ? 180 : 150,
      fixed: 'right',
      render: (row: PaymentRecordItem) =>
        h(CrmOperationButton, {
          groupList: operationGroupList.value,
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
    specialRender: {
      contractId: (row: PaymentRecordItem) => {
        return props.isContractTab || !hasAnyPermission(['CONTRACT:READ'])
          ? h(
              CrmNameTooltip,
              { text: row.contractName },
              {
                default: () => row.contractName,
              }
            )
          : h(
              CrmTableButton,
              {
                onClick: () => {
                  showContractDrawer({ id: row.contractId });
                },
              },
              { default: () => row.contractName, trigger: () => row.contractName }
            );
      },
    },
    permission: ['CONTRACT_PAYMENT_PLAN:EXPORT'], // TODO lmy permission
    containerClass: `.crm-contract-payment-table-${props.formKey}`,
  });
  const { propsRes, propsEvent, tableQueryParams, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;

  const exportColumns = computed<ExportTableColumnItem[]>(() =>
    getExportColumns(propsRes.value.columns, customFieldsFilterConfig.value as FilterFormItem[])
  );

  const exportParams = computed(() => {
    return {
      ...tableQueryParams.value,
      ids: checkedRowKeys.value,
      contractId: props.sourceId,
    };
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

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
    setLoadListParams({ keyword: val ?? keyword.value, viewId: activeTab.value, contractId: props.sourceId });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  watch(
    () => tableRefreshId.value,
    () => {
      checkedRowKeys.value = [];
      searchData();
    }
  );

  onBeforeMount(() => {
    if (props.isContractTab) {
      searchData();
    }
  });

  watch(
    () => activeTab.value,
    (val) => {
      if (val) {
        checkedRowKeys.value = [];
        setLoadListParams({ keyword: keyword.value, viewId: activeTab.value, contractId: props.sourceId });
        crmTableRef.value?.setColumnSort(val);
      }
    }
  );
</script>
