<template>
  <CrmTable
    ref="crmTableRef"
    v-model:checked-row-keys="checkedRowKeys"
    v-bind="propsRes"
    :class="`crm-opportunity-table-${props.formKey}`"
    :action-config="actionConfig"
    @page-change="propsEvent.pageChange"
    @page-size-change="propsEvent.pageSizeChange"
    @sorter-change="propsEvent.sorterChange"
    @batch-action="handleBatchAction"
  >
    <template #actionLeft>
      <div class="flex items-center gap-[12px]">
        <n-button
          v-if="!props.readonly && hasAnyPermission(['OPPORTUNITY_QUOTATION:ADD'])"
          type="primary"
          @click="handleCreate"
        >
          {{ t('opportunity.quotation.new') }}
        </n-button>
      </div>
    </template>
    <template #actionRight>
      <CrmAdvanceFilter
        ref="tableAdvanceFilterRef"
        v-model:keyword="keyword"
        :search-placeholder="t('opportunity.quotation.searchPlaceholder')"
        :custom-fields-config-list="customFieldsFilterConfig"
        :filter-config-list="[]"
        @adv-search="handleAdvSearch"
        @keyword-search="searchByKeyword"
      />
    </template>

    <template #view>
      <CrmViewSelect
        v-if="!props.sourceId"
        v-model:active-tab="activeTab"
        :type="FormDesignKeyEnum.OPPORTUNITY_QUOTATION"
        :custom-fields-config-list="customFieldsFilterConfig"
        :filter-config-list="filterConfigList"
        @refresh-table-data="searchData"
      />
    </template>
  </CrmTable>
  <approvalModal v-model:show="showApprovalModal" :quotationIds="checkedRowKeys" @refresh="handleApprovalSuccess" />
  <detailDrawer
    v-model:visible="showDetailDrawer"
    :detail="activeRow"
    :refresh-id="tableRefreshId"
    @edit="handleEdit"
    @refresh="handleRefresh"
  />
  <CrmFormCreateDrawer
    v-model:visible="formCreateDrawerVisible"
    :form-key="activeFormKey"
    :source-id="activeSourceId"
    :need-init-detail="needInitDetail"
    :initial-source-name="initialSourceName"
    :other-save-params="otherSaveParams"
    :link-form-key="FormDesignKeyEnum.CONTRACT"
    @saved="searchData"
  />
  <batchOperationResultModal v-model:visible="resultVisible" :result="batchResult" :name="t('common.batchVoid')" />
</template>

<script setup lang="ts">
  import { DataTableRowKey, NButton, useMessage } from 'naive-ui';

  import { FieldTypeEnum, FormDesignKeyEnum, FormLinkScenarioEnum } from '@lib/shared/enums/formDesignEnum';
  import { QuotationStatusEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import useLocale from '@lib/shared/locale/useLocale';
  import { characterLimit } from '@lib/shared/method';
  import { BatchOperationResult, QuotationItem } from '@lib/shared/models/opportunity';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmViewSelect from '@/components/business/crm-view-select/index.vue';
  import approvalModal from './approvalModal.vue';
  import batchOperationResultModal from './batchOperationResultModal.vue';
  import detailDrawer from './detail.vue';
  import quotationStatus from './quotationStatus.vue';

  import { batchVoided, deleteQuotation, revokeQuotation, voidQuotation } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';
  import { useUserStore } from '@/store';
  import { hasAnyPermission } from '@/utils/permission';

  const { openModal } = useModal();
  const { t } = useI18n();
  const Message = useMessage();
  const { currentLocale } = useLocale(Message.loading);

  const useStore = useUserStore();

  const props = defineProps<{
    formKey: FormDesignKeyEnum.OPPORTUNITY_QUOTATION;
    sourceId?: string;
    readonly?: boolean;
    openseaHiddenColumns?: string[];
  }>();

  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const resultVisible = ref(false);
  const batchResult = ref<BatchOperationResult>({
    success: 0,
    fail: 0,
    errorMessage: '',
  });
  const batchOperationName = ref(t('common.batchVoid'));
  const formCreateDrawerVisible = ref(false);
  const activeSourceId = ref('');
  const initialSourceName = ref('');
  const needInitDetail = ref(false);
  const activeFormKey = ref(FormDesignKeyEnum.OPPORTUNITY_QUOTATION);

  const activeTab = ref();
  const keyword = ref('');
  const activeQuotationId = ref('');
  const tableRefreshId = ref(0);
  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.batchApproval'),
        key: 'approval',
        permission: ['OPPORTUNITY_QUOTATION:APPROVAL'],
      },
      {
        label: t('common.batchVoid'),
        key: 'voided',
        permission: ['OPPORTUNITY_QUOTATION:VOIDED'],
      },
    ],
  };

  const showApprovalModal = ref(false);
  function handleBatchApproval() {
    showApprovalModal.value = true;
    batchOperationName.value = t('common.batchApproval');
  }

  function handleRefresh() {
    checkedRowKeys.value = [];
    tableRefreshId.value += 1;
  }

  function handleApprovalSuccess(val: BatchOperationResult) {
    if (val.success > 0 || val.fail > 0) {
      resultVisible.value = true;
    }
    batchResult.value = val;
    handleRefresh();
  }

  // 批量作废
  function handleBatchVoid() {
    batchOperationName.value = t('common.batchVoid');
    openModal({
      type: 'error',
      title: t('opportunity.quotation.batchInvalidTitleTip', { number: checkedRowKeys.value.length }),
      content: t('opportunity.quotation.invalidContentTip'),
      positiveText: t('common.confirmVoid'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          const result = await batchVoided(checkedRowKeys.value);
          if (result.success > 0 || result.fail > 0) {
            resultVisible.value = true;
          }
          handleRefresh();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleBatchAction(item: ActionsItem) {
    switch (item.key) {
      case 'approval':
        handleBatchApproval();
        break;
      case 'voided':
        handleBatchVoid();
        break;
      default:
        break;
    }
  }

  const otherSaveParams = ref({
    type: 'QUOTATION',
    id: '',
  });

  async function handleCreate() {
    needInitDetail.value = false;
    activeFormKey.value = FormDesignKeyEnum.OPPORTUNITY_QUOTATION;
    activeSourceId.value = '';
    formCreateDrawerVisible.value = true;
  }

  function handleEdit(id: string) {
    activeFormKey.value = FormDesignKeyEnum.OPPORTUNITY_QUOTATION;
    activeSourceId.value = id;
    needInitDetail.value = true;
    otherSaveParams.value.id = id;
    formCreateDrawerVisible.value = true;
  }
  function handleVoid(row: QuotationItem) {
    const { hasContract } = row;
    const content = hasContract
      ? t('opportunity.quotation.invalidHasContractContentTip')
      : t('opportunity.quotation.invalidContentTip');

    const positiveText = hasContract ? t('common.gotIt') : t('common.confirmVoid');
    openModal({
      type: hasContract ? 'default' : 'error',
      title: t('opportunity.quotation.voidTitleTip', { name: characterLimit(row.name) }),
      content,
      positiveText,
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        if (hasContract) {
          return;
        }
        try {
          await voidQuotation(row.id);
          Message.success(t('common.voidSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  const showDetailDrawer = ref(false);
  const activeRow = ref<Partial<QuotationItem>>();
  function handleDelete(row: QuotationItem) {
    const { hasContract } = row;
    const content = hasContract
      ? t('opportunity.quotation.deleteHasContractContentTip')
      : t('opportunity.quotation.deleteContentTip');

    const positiveText = hasContract ? t('common.gotIt') : t('common.confirmVoid');
    openModal({
      type: hasContract ? 'default' : 'error',
      title: t('opportunity.quotation.deleteTitleTip', { name: characterLimit(row.name) }),
      content,
      positiveText,
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        if (hasContract) {
          return;
        }
        try {
          await deleteQuotation(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  async function handleRevoke(row: QuotationItem) {
    try {
      await revokeQuotation(row.id);
      Message.success(t('common.revokeSuccess'));
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function handleActionSelect(row: QuotationItem, actionKey: string, done?: () => void) {
    switch (actionKey) {
      case 'edit':
        handleEdit(row.id);
        break;
      case 'voided':
        handleVoid(row);
        break;
      case 'revoke':
        handleRevoke(row);
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  const groupList = [
    {
      label: t('common.edit'),
      key: 'edit',
      permission: ['OPPORTUNITY_QUOTATION:UPDATE'],
    },
    {
      label: t('common.voided'),
      key: 'voided',
      permission: ['OPPORTUNITY_QUOTATION:VOIDED'],
    },
    {
      label: t('common.delete'),
      key: 'delete',
      permission: ['OPPORTUNITY_QUOTATION:DELETE'],
    },
  ];

  const moreGroupList = [
    {
      label: t('common.download'),
      key: 'download',
      permission: ['OPPORTUNITY_QUOTATION:EXPORT'],
    },
    {
      label: t('common.revoke'),
      key: 'revoke',
    },
  ];

  function getOperationGroupList(row: QuotationItem) {
    const allGroups = [...groupList, ...moreGroupList];
    const getGroups = (keys: string[]) => allGroups.filter((e) => keys.includes(e.key));
    const commonGroups = ['voided', 'delete'];

    switch (row.approvalStatus) {
      case QuotationStatusEnum.APPROVED:
        return getGroups(['download', ...commonGroups]);
      case QuotationStatusEnum.UNAPPROVED:
      case QuotationStatusEnum.REVOKE:
        return getGroups(['edit', ...commonGroups]);
      case QuotationStatusEnum.APPROVING:
        const operationGroups = row.createUser === useStore.userInfo.id ? ['revoke', ...commonGroups] : commonGroups;
        return getGroups(operationGroups);
      case QuotationStatusEnum.VOIDED:
        return getGroups(['delete']);
      default:
        return [];
    }
  }

  const { useTableRes, customFieldsFilterConfig, fieldList } = await useFormCreateTable({
    formKey: props.formKey,
    containerClass: `.crm-quotation-table-${props.formKey}`,
    operationColumn: props.readonly
      ? undefined
      : {
          key: 'operation',
          width: 200,
          fixed: 'right',
          render: (row: QuotationItem) =>
            getOperationGroupList(row).length
              ? h(CrmOperationButton, {
                  groupList: getOperationGroupList(row),
                  onSelect: (key: string, done?: () => void) => handleActionSelect(row, key, done),
                })
              : '-',
        },
    specialRender: {
      name: (row: QuotationItem) => {
        const createNameButton = () =>
          h(
            CrmTableButton,
            {
              onClick: () => {
                // TODO:  xinxinwu 🏷
                activeRow.value = {
                  ...row,
                  approvalStatus: QuotationStatusEnum.APPROVING,
                };
                activeQuotationId.value = row.id;
                showDetailDrawer.value = true;
              },
            },
            { default: () => row.name, trigger: () => row.name }
          );
        return props.readonly ? h(CrmNameTooltip, { text: row.name }) : createNameButton();
      },
      status: (row: QuotationItem) =>
        h(quotationStatus, {
          status: row.approvalStatus,
        }),
    },
    permission: [
      'OPPORTUNITY_QUOTATION:UPDATE',
      'OPPORTUNITY_QUOTATION:DELETE',
      'OPPORTUNITY_QUOTATION:EXPORT',
      'OPPORTUNITY_QUOTATION:VOIDED',
    ],
    readonly: props.readonly,
  });
  const { propsRes, propsEvent, loadList, setLoadListParams, setAdvanceFilter, filterItem, advanceFilter } =
    useTableRes;

  const isAdvancedSearchMode = ref(false);
  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

  const statusOptions = [
    {
      value: QuotationStatusEnum.APPROVED,
      label: t('common.pass'),
    },
    {
      value: QuotationStatusEnum.UNAPPROVED,
      label: t('common.unPass'),
    },
    {
      value: QuotationStatusEnum.APPROVING,
      label: t('common.review'),
    },
    {
      value: QuotationStatusEnum.VOIDED,
      label: t('common.voided'),
    },
    {
      value: QuotationStatusEnum.REVOKE,
      label: t('common.revoke'),
    },
  ];

  const filterConfigList = computed<FilterFormItem[]>(() => {
    return [
      {
        title: t('common.status'),
        dataIndex: 'approvalStatus',
        type: FieldTypeEnum.SELECT_MULTIPLE,
        selectProps: {
          options: statusOptions,
        },
      },
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
      ...baseFilterConfigList,
    ] as FilterFormItem[];
  });

  function handleAdvSearch(filter: FilterResult, isAdvancedMode: boolean, originalForm?: FilterForm) {
    keyword.value = '';
    isAdvancedSearchMode.value = isAdvancedMode;
    setAdvanceFilter(filter);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function searchData(_keyword?: string) {
    setLoadListParams({
      keyword: _keyword ?? keyword.value,
      viewId: activeTab.value,
      opportunityId: props.sourceId,
    });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function searchByKeyword(val: string) {
    keyword.value = val;
    nextTick(() => {
      searchData();
    });
  }

  onBeforeMount(async () => {
    if (props.sourceId) {
      searchData();
    }
  });

  watch(
    () => activeTab.value,
    async (val) => {
      if (val) {
        checkedRowKeys.value = [];
        setLoadListParams({
          keyword: keyword.value,
          viewId: activeTab.value,
          opportunityId: props.sourceId,
        });
        crmTableRef.value?.setColumnSort(val);
      }
    },
    { immediate: true }
  );

  watch(
    () => tableRefreshId.value,
    () => {
      checkedRowKeys.value = [];
      searchData();
    }
  );
</script>

<style scoped></style>
