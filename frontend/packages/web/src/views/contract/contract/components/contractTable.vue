<template>
  <CrmTable
    ref="crmTableRef"
    v-model:checked-row-keys="checkedRowKeys"
    v-bind="propsRes"
    class="crm-contract-table"
    :not-show-table="activeShowType === 'billboard'"
    :not-show-table-filter="isAdvancedSearchMode"
    :action-config="actionConfig"
    :fullscreen-target-ref="props.fullscreenTargetRef"
    :hiddenBackToTop="activeShowType === 'billboard'"
    :customTotal="activeShowType === 'billboard'"
    @page-change="propsEvent.pageChange"
    @page-size-change="propsEvent.pageSizeChange"
    @sorter-change="propsEvent.sorterChange"
    @filter-change="filterChange"
    @batch-action="handleBatchAction"
    @refresh="searchData"
  >
    <template #actionLeft>
      <div class="flex items-center gap-[12px]">
        <n-button v-permission="['CONTRACT:ADD']" type="primary" @click="handleNewClick">
          {{ t('contract.new') }}
        </n-button>
        <n-button
          v-permission="['CONTRACT:EXPORT']"
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
      <n-tabs v-model:value="activeShowType" type="segment" size="large" class="show-type-tabs">
        <n-tab-pane name="table" class="hidden">
          <template #tab><CrmIcon type="iconicon_list" /></template>
        </n-tab-pane>
        <n-tab-pane name="billboard" class="hidden">
          <template #tab><CrmIcon type="iconicon_waterfalls" /></template>
        </n-tab-pane>
      </n-tabs>
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
      />
    </template>
    <template v-if="activeShowType === 'billboard'" #other>
      <billboard
        ref="billboardRef"
        :keyword="keyword"
        :view-id="activeTab"
        :advance-filter="advanceFilter"
        :enable-approval="enableApproval"
        :has-stage-permission="hasContractStagePermission"
        @change="getStatistic()"
        @open-detail="handleOpenDetail"
        @init="handleBillboardInit"
      />
    </template>
    <template v-if="showStatisticInfo" #totalRight>
      <div v-if="activeShowType === 'billboard'">
        {{ t('crmPagination.total', { count: billboardTotalCount }) }}
      </div>
      <div class="ml-[24px]">
        {{ t('opportunity.averageAmount') }}
        <span class="ml-[4px]">
          {{ abbreviateNumber(totalAmountInfo?.averageAmount, '').value }}
          <span class="unit">
            {{ abbreviateNumber(totalAmountInfo?.averageAmount, '').unit }}
          </span>
        </span>
      </div>
      <div class="ml-[24px]">
        {{ t('opportunity.totalAmount') }}
        <span class="ml-[4px]">
          {{ abbreviateNumber(totalAmountInfo?.amount, '').value }}
          <span class="unit">
            {{ abbreviateNumber(totalAmountInfo?.amount, '').unit }}
          </span>
        </span>
      </div>
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
    @saved="handleFormCreateSaved"
    @review="handleFormReview"
  />

  <CrmTableExportModal
    v-model:show="showExportModal"
    :params="exportParams"
    :export-columns="exportColumns"
    :is-export-all="isExportAll"
    :show-approval-tip="exportApprovalTip"
    type="contract"
    @create-success="handleExportCreateSuccess"
  />

  <VoidReasonModal
    v-model:visible="showVoidReasonModal"
    :name="activeSourceName"
    :sourceId="activeSourceId"
    @refresh="searchData(undefined, activeSourceId)"
  />

  <DetailDrawer
    v-model:visible="showDetailDrawer"
    :sourceId="activeSourceId"
    isContractTableDetail
    @refresh="searchData(undefined, activeSourceId)"
    @delete="removeItemFromList(activeSourceId)"
    @showCustomerDrawer="showCustomerDrawer"
    @open-business-title-drawer="handleOpenBusinessTitleDrawer"
  />

  <ApprovalModal
    v-model:show="showApprovalModal"
    :quotationIds="checkedRowKeys"
    :approval-api="batchApproveContract"
    @refresh="handleApprovalSuccess"
  />

  <batchOperationResultModal v-model:visible="resultVisible" :result="batchResult" :name="batchOperationName" />

  <businessTitleDrawer v-model:visible="showBusinessTitleDetailDrawer" :source-id="activeBusinessTitleSourceId" />
  <CrmBatchEditModal
    v-model:visible="showEditModal"
    v-model:field-list="editFieldList"
    :ids="checkedRowKeys"
    :form-key="FormDesignKeyEnum.CONTRACT"
    :show-approval-tip="batchEditApprovalTip"
    @refresh="handleRefresh"
  />
  <CrmStatusFlowModal
    v-model:show="flowModalShow"
    :from="{ id: currentStageConfig?.id, name: currentStageConfig?.name }"
    :to="{ id: targetStageConfig?.id, name: targetStageConfig?.name }"
    :form-key="FormDesignKeyEnum.CONTRACT"
    :circulationFieldValues="circulationFieldValues"
    :source-id="activeSourceId"
    @success="handleFlowSuccess"
  />
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import { DataTableRowKey, NButton, NTabPane, NTabs, useMessage } from 'naive-ui';

  import { ContractStatusEnum } from '@lib/shared/enums/contractEnum';
  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { CirculationTypeEnum } from '@lib/shared/enums/opportunityEnum.js';
  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import useLocale from '@lib/shared/locale/useLocale';
  import { abbreviateNumber, characterLimit } from '@lib/shared/method';
  import { ExportTableColumnItem } from '@lib/shared/models/common';
  import type { ContractItem } from '@lib/shared/models/contract';
  import {
    BatchOperationResult,
    type CirculationFieldValueItem,
    OpportunityStageConfig,
    type StageConfigItem,
  } from '@lib/shared/models/opportunity';

  import { COMMON_SELECTION_OPERATORS } from '@/components/pure/crm-advance-filter/index';
  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmApprovalPopover from '@/components/business/crm-approval/components/crm-approval-popover.vue';
  import batchOperationResultModal from '@/components/business/crm-batch-edit-modal/components/batchOperationResultModal.vue';
  import CrmBatchEditModal from '@/components/business/crm-batch-edit-modal/index.vue';
  import StatusTagSelect from '@/components/business/crm-follow-detail/statusTagSelect.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import { OpenDetailType } from '@/components/business/crm-stage-board/types';
  import CrmStatusFlowModal from '@/components/business/crm-status-flow-modal/index.vue';
  import CrmTableExportModal from '@/components/business/crm-table-export-modal/index.vue';
  import CrmViewSelect from '@/components/business/crm-view-select/index.vue';
  import businessTitleDrawer from '../../businessTitle/components/detail.vue';
  import billboard from './billboard/index.vue';
  import DetailDrawer from './detail.vue';
  import VoidReasonModal from './voidReasonModal.vue';
  import ApprovalModal from '@/views/opportunity/components/quotation/approvalModal.vue';

  import {
    batchApproveContract,
    changeContractStatus,
    deleteContract,
    getContractStatistic,
    getContractStatusConfig,
  } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import { processStatusOptions } from '@/config/process';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useApprovalResourceAction from '@/hooks/useApprovalResourceAction';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useLocalForage from '@/hooks/useLocalForage';
  import useModal from '@/hooks/useModal';
  // import useViewChartParams, { STORAGE_VIEW_CHART_KEY, ViewChartResult } from '@/hooks/useViewChartParams';
  import { getExportColumns } from '@/utils/export';
  import { hasAnyPermission } from '@/utils/permission';

  import { ContractRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    fullscreenTargetRef?: HTMLElement | null;
  }>();
  const emit = defineEmits<{
    (
      e: 'openCustomerDrawer',
      params: { customerId: string; inCustomerPool: boolean; poolId: string },
      readonly: boolean
    ): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();
  const { getItem, setItem } = useLocalForage();
  const route = useRoute();

  const activeShowType = ref<'table' | 'billboard'>();
  const activeTab = ref();
  const keyword = ref('');

  // 操作
  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const tableRefreshId = ref(0);
  const billboardTotalCount = ref(0);
  const tableRemoveRefreshId = ref('');
  const tableItemRefreshId = ref('');

  const formCreateDrawerVisible = ref(false);
  const activeSourceId = ref('');
  const activeSourceName = ref('');
  const needInitDetail = ref(false);
  const activeFormKey = ref(FormDesignKeyEnum.CONTRACT);

  function handleNewClick() {
    needInitDetail.value = false;
    activeFormKey.value = FormDesignKeyEnum.CONTRACT;
    activeSourceId.value = '';
    activeSourceName.value = '';
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

  const showEditModal = ref(false);
  const { initFormConfig: initEditFormConfig, fieldList: editFieldList } = useFormCreateApi({
    formKey: ref(FormDesignKeyEnum.CONTRACT),
  });
  function handleBatchEdit() {
    initEditFormConfig();
    showEditModal.value = true;
  }

  function handleRefresh() {
    checkedRowKeys.value = [];
    tableRefreshId.value += 1;
  }

  const showApprovalModal = ref(false);
  const batchOperationName = ref('');
  const batchResult = ref<BatchOperationResult>({
    success: 0,
    fail: 0,
    errorMessages: '',
  });
  const resultVisible = ref(false);
  function handleApprovalSuccess(val: BatchOperationResult) {
    batchResult.value = val;
    resultVisible.value = true;
    tableRefreshId.value += 1;
  }

  function handleBatchAction(item: ActionsItem) {
    switch (item.key) {
      case 'exportChecked':
        isExportAll.value = false;
        showExportModal.value = true;
        break;
      case 'approval':
        showApprovalModal.value = true;
        batchOperationName.value = t('common.batchApproval');
        break;
      case 'batchEdit':
        handleBatchEdit();
        break;
      default:
        break;
    }
  }

  const showDetailDrawer = ref(false);

  function handleEdit(id: string) {
    activeFormKey.value = FormDesignKeyEnum.CONTRACT;
    activeSourceId.value = id;
    needInitDetail.value = true;
    formCreateDrawerVisible.value = true;
  }

  const showVoidReasonModal = ref(false);
  function handleVoided(row: ContractItem) {
    activeSourceName.value = row.name;
    activeSourceId.value = row.id;
    showVoidReasonModal.value = true;
  }

  // 回款
  const initialSourceName = ref('');
  const linkFormInfo = ref();
  const { initFormDetail, initFormConfig, linkFormFieldMap } = useFormCreateApi({
    formKey: ref(FormDesignKeyEnum.CONTRACT),
    sourceId: activeSourceId,
  });
  async function handlePaymentRecord(row: ContractItem) {
    activeSourceId.value = row.id;
    initialSourceName.value = row.name;
    needInitDetail.value = false;
    activeFormKey.value = FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD;
    await initFormConfig();
    await initFormDetail(false, true);
    linkFormInfo.value = linkFormFieldMap.value;
    formCreateDrawerVisible.value = true;
  }

  function createContractDataActionMap(row: ContractItem) {
    return {
      edit: {
        label: t('common.edit'),
        key: 'edit',
        permission: ['CONTRACT:UPDATE'],
      },
      paymentRecord: {
        label: t('contract.payment'),
        key: 'paymentRecord',
        permission: ['CONTRACT:PAYMENT'],
        disabled: !row.amount || row.alreadyPayAmount >= row.amount,
        tooltipContent: row.alreadyPayAmount >= row.amount ? t('contract.noPaymentRequired') : undefined,
      },
      delete: {
        label: t('common.delete'),
        key: 'delete',
        permission: ['CONTRACT:DELETE'],
      },
    };
  }

  const {
    initApprovalPermission,
    resolveRowOperation,
    enableApproval,
    deleteExecute,
    hasApprovalScopedPermission,
    getApprovalActionTip,
  } = useApprovalOperation<ContractItem>({
    formType: FormDesignKeyEnum.CONTRACT,
    dataActionMap: createContractDataActionMap,
    specialActionFilter: (row, actionKeys) => {
      if (row.stage !== ContractStatusEnum.VOID) {
        return actionKeys;
      }

      return actionKeys.filter((key) => {
        if (key === 'paymentRecord') {
          return false;
        }

        if (!enableApproval.value && key === 'edit') {
          return false;
        }

        return true;
      });
    },
  });

  const { reviewByFormResult, reviewByResourceId, revokeByResourceId } = useApprovalResourceAction({
    formKey: FormDesignKeyEnum.CONTRACT,
  });

  function handleDelete(row: ContractItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('common.deleteConfirmContent'),
      positiveText: deleteExecute.value ? t('crm.approval.confirmAndSubmitReview') : t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteContract(row.id);
          Message.success(deleteExecute.value ? t('common.reviewSuccess') : t('common.deleteSuccess'));
          tableRemoveRefreshId.value = row.id;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleRevoke(row: ContractItem) {
    revokeByResourceId(row.id, {
      onSuccess: (resourceId) => {
        tableItemRefreshId.value = resourceId;
      },
    });
  }

  function handleApproval(row: ContractItem) {
    if (!hasApprovalScopedPermission(row, ['CONTRACT:READ'])) {
      return;
    }
    activeSourceId.value = row.id;
    showDetailDrawer.value = true;
  }

  function handleReview(row: ContractItem) {
    reviewByResourceId(row.id, {
      onSuccess: (resourceId) => {
        tableItemRefreshId.value = resourceId;
      },
    });
  }

  async function handleActionSelect(row: ContractItem, actionKey: string) {
    switch (actionKey) {
      case 'review':
        handleReview(row);
        break;
      case 'paymentRecord':
        handlePaymentRecord(row);
        break;
      case 'revoke':
        handleRevoke(row);
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

  function showCustomerDrawer(params: { customerId: string; inCustomerPool: boolean; poolId: string }) {
    emit(
      'openCustomerDrawer',
      {
        customerId: params.customerId,
        inCustomerPool: params.inCustomerPool,
        poolId: params.poolId || '',
      },
      false
    );
  }

  function handleOpenDetail(type: OpenDetailType, item: ContractItem) {
    if (type === 'customer') {
      showCustomerDrawer(item);
      return;
    }
    if (!hasApprovalScopedPermission(item, ['CONTRACT:READ'])) {
      return;
    }
    activeSourceId.value = item.id;
    showDetailDrawer.value = true;
  }

  function hasContractStagePermission(row: ContractItem) {
    return hasApprovalScopedPermission(row, ['CONTRACT:STAGE']);
  }

  const stageConfig = ref<OpportunityStageConfig>();
  const contractStageList = computed(() => stageConfig.value?.stageConfigList || []);
  const contractEndStages = computed(() =>
    contractStageList.value.filter((item) => item.type === 'END').map((i) => i.id)
  );

  function isContractStageOptionDisabled(row: ContractItem, targetStage: string) {
    const currentStage = row.stage;
    const isSameStage = currentStage === targetStage;
    const currentIndex = contractStageList.value.findIndex((item) => item.id === currentStage);
    const targetIndex = contractStageList.value.findIndex((item) => item.id === targetStage);
    const isCurrentEndStage = contractEndStages.value.includes(currentStage);

    if (isCurrentEndStage) {
      return isSameStage || !stageConfig.value?.endRollBack;
    }

    if (stageConfig.value?.afootRollBack) {
      return isSameStage;
    }

    return isSameStage || targetIndex < currentIndex;
  }

  async function initStageConfig() {
    try {
      stageConfig.value = await getContractStatusConfig();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  await Promise.all([initStageConfig(), initApprovalPermission()]);

  const flowModalShow = ref(false);
  const targetStageConfig = ref<StageConfigItem>();
  const circulationFieldValues = ref<CirculationFieldValueItem[]>([]);
  const currentStageConfig = ref<StageConfigItem>();

  function getContractStageOptions(row: ContractItem) {
    const currentStageAdvanceConfig = stageConfig.value?.advancedConfigs?.find((e) => e.originId === row.stage);
    return contractStageList.value.map((item) => ({
      label: item.name,
      value: item.id,
      disabled:
        stageConfig.value?.circulationType === CirculationTypeEnum.ADVANCED
          ? currentStageAdvanceConfig?.targets.find((e) => e.targetId === item.id)?.enable
          : isContractStageOptionDisabled(row, item.id),
    }));
  }

  async function changeStatus(id: string, stage: string) {
    try {
      await changeContractStatus({
        id,
        stage,
      });
      Message.success(t('common.updateSuccess'));
      return true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  const { useTableRes, customFieldsFilterConfig, fieldList } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.CONTRACT,
    operationColumn: {
      key: 'operation',
      width: 180,
      fixed: 'right',
      render: (row: ContractItem) => {
        const operation = resolveRowOperation(row);
        return operation.groupList.length
          ? h(CrmOperationButton, {
              groupList: operation.groupList,
              moreList: operation.moreList,
              onSelect: (key: string) => handleActionSelect(row, key),
            })
          : '-';
      },
    },
    specialRender: {
      name: (row: ContractItem) => {
        return hasApprovalScopedPermission(row, ['CONTRACT:READ'])
          ? h(
              CrmTableButton,
              {
                onClick: () => {
                  activeSourceId.value = row.id;
                  showDetailDrawer.value = true;
                },
              },
              { default: () => row.name, trigger: () => row.name }
            )
          : h(CrmNameTooltip, { text: row.name });
      },
      customerId: (row: ContractItem) => {
        return !row.customerName ||
          (!row.inCustomerPool && !hasAnyPermission(['CUSTOMER_MANAGEMENT:READ'])) ||
          (row.inCustomerPool && !hasAnyPermission(['CUSTOMER_MANAGEMENT_POOL:READ']))
          ? h(
              CrmNameTooltip,
              { text: row.customerName },
              {
                default: () => row.customerName,
              }
            )
          : h(
              CrmTableButton,
              {
                onClick: () => {
                  showCustomerDrawer(row);
                },
              },
              { default: () => row.customerName, trigger: () => row.customerName }
            );
      },
      stage: (row: ContractItem) => {
        const canEditStage = hasApprovalScopedPermission(row, ['CONTRACT:STAGE']);
        return h(StatusTagSelect, {
          'status': row.stage as ContractStatusEnum,
          'noRender': true,
          'disabled': !canEditStage,
          'onUpdate:status': async (val) => {
            circulationFieldValues.value =
              stageConfig.value?.advancedConfigs
                .find((e) => e.originId === row.stage)
                ?.targets.find((e) => e.targetId === val)?.circulationFieldValues || [];
            if (
              stageConfig.value?.circulationType === CirculationTypeEnum.ADVANCED &&
              circulationFieldValues.value.length
            ) {
              activeSourceId.value = row.id;
              targetStageConfig.value = stageConfig.value?.stageConfigList.find((e) => e.id === val);
              currentStageConfig.value = stageConfig.value?.stageConfigList.find((e) => e.id === row.stage);
              flowModalShow.value = true;
            } else if (val === ContractStatusEnum.VOID) {
              handleVoided(row);
            } else {
              const res = await changeStatus(row.id, val);
              if (res) row.stage = val;
            }
          },
          'statusOptions': getContractStageOptions(row),
        });
      },
      approvalStatus: (row: ContractItem) =>
        h(CrmApprovalPopover, {
          status: row.approvalStatus,
          formKey: FormDesignKeyEnum.CONTRACT,
          sourceId: row.id,
          showMore: hasApprovalScopedPermission(row, ['CONTRACT:READ']),
          disabled: row.approvalStatus !== ProcessStatusEnum.UNAPPROVED,
          onMore: () => {
            handleApproval(row);
          },
        }),
    },
    permission: ['CONTRACT:EXPORT', 'CONTRACT:UPDATE'],
    containerClass: '.crm-contract-table',
    contractStage: stageConfig.value?.stageConfigList || [],
    enableApproval,
  });
  const {
    propsRes,
    propsEvent,
    tableQueryParams,
    filterItem,
    advanceFilter,
    loadList,
    setLoadListParams,
    setAdvanceFilter,
  } = useTableRes;
  const billboardRef = ref<InstanceType<typeof billboard>>();

  function handleFlowSuccess() {
    tableRefreshId.value += 1;
  }

  const statisticInfo = ref({ amount: 0, averageAmount: 0 });
  async function getStatistic(_keyword?: string) {
    try {
      const res = await getContractStatistic({
        keyword: _keyword ?? keyword.value,
        viewId: activeTab.value,
        combineSearch: advanceFilter,
        filters: filterItem.value,
      });
      statisticInfo.value = res;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  const totalAmountInfo = computed(() => {
    if (checkedRowKeys.value.length > 0) {
      const amount = propsRes.value.data
        .filter((item: ContractItem) => checkedRowKeys.value.includes(item.id))
        .reduce((total: number, item: ContractItem) => total + (item.amount || 0), 0);
      return {
        averageAmount: amount / checkedRowKeys.value.length,
        amount,
      };
    }
    return {
      averageAmount: statisticInfo.value?.averageAmount ?? 0,
      amount: statisticInfo.value?.amount ?? 0,
    };
  });

  function filterChange(val: any) {
    propsEvent.value.filterChange(val);
    getStatistic();
  }

  const showStatisticInfo = computed(
    () => propsRes.value.columns.find((item) => item.key === 'amount') || activeShowType.value === 'billboard'
  );

  const exportColumns = computed<ExportTableColumnItem[]>(() =>
    getExportColumns(propsRes.value.columns, customFieldsFilterConfig.value as FilterFormItem[], fieldList.value, true)
  );
  const exportParams = computed(() => {
    return {
      ...tableQueryParams.value,
      ids: checkedRowKeys.value,
    };
  });

  const exportApprovalTip = computed(() => getApprovalActionTip(['CONTRACT:EXPORT'], 'common.exportApprovalTip'));
  const batchEditApprovalTip = computed(() => getApprovalActionTip(['CONTRACT:UPDATE'], 'common.batchEditApprovalTip'));

  const actionConfig = computed(() => {
    return {
      baseAction: [
        {
          label: t('common.exportChecked'),
          key: 'exportChecked',
          permission: ['CONTRACT:EXPORT'],
        },
        {
          label: t('common.batchEdit'),
          key: 'batchEdit',
          permission: ['CONTRACT:UPDATE'],
        },
      ],
    };
  });

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
    {
      title: t('contract.status'),
      dataIndex: 'stage',
      type: FieldTypeEnum.SELECT_MULTIPLE,
      operatorOption: COMMON_SELECTION_OPERATORS,
      selectProps: {
        options:
          stageConfig.value?.stageConfigList.map((e: any) => ({
            label: e.name,
            value: e.id,
          })) || [],
      },
    },
    {
      title: t('contract.voidReason'),
      dataIndex: 'voidReason',
      type: FieldTypeEnum.INPUT,
    },
    // 先去掉
    // {
    //   title: t('contract.alreadyPayAmount'),
    //   dataIndex: 'alreadyPayAmount',
    //   type: FieldTypeEnum.INPUT_NUMBER,
    // },
    {
      title: t('contract.approvalStatus'),
      dataIndex: 'approvalStatus',
      operatorOption: COMMON_SELECTION_OPERATORS,
      type: FieldTypeEnum.SELECT_MULTIPLE,
      selectProps: {
        options: processStatusOptions,
      },
    },
    ...baseFilterConfigList,
  ]);

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

  const isAdvancedSearchMode = ref(false);
  const advancedOriginalForm = ref<FilterForm | undefined>();
  function handleAdvSearch(filter: FilterResult, isAdvancedMode: boolean, originalForm?: FilterForm) {
    keyword.value = '';
    advancedOriginalForm.value = originalForm;
    isAdvancedSearchMode.value = isAdvancedMode;
    setAdvanceFilter(filter);
    if (activeShowType.value === 'billboard') {
      billboardRef.value?.refresh();
      getStatistic();
    } else {
      loadList();
      getStatistic();
      crmTableRef.value?.scrollTo({ top: 0 });
    }
  }

  function searchData(val?: string, refreshId?: string) {
    if (!activeTab.value) return;
    setLoadListParams({ keyword: val ?? keyword.value, viewId: activeTab.value });
    if (activeShowType.value === 'billboard') {
      billboardRef.value?.refresh();
      getStatistic(val);
    } else {
      loadList(false, refreshId);
      getStatistic(val);
      if (!refreshId) {
        crmTableRef.value?.scrollTo({ top: 0 });
      }
    }
  }

  watch(
    () => activeShowType.value,
    async (val) => {
      if (val) {
        await setItem('contract-active-show-type', activeShowType.value as 'table' | 'billboard');
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

  const showBusinessTitleDetailDrawer = ref(false);
  const activeBusinessTitleSourceId = ref<string>('');
  function handleOpenBusinessTitleDrawer(params: { id: string }) {
    activeBusinessTitleSourceId.value = params.id;
    showBusinessTitleDetailDrawer.value = true;
  }

  function handleFormCreateSaved(res: any) {
    if (needInitDetail.value || activeFormKey.value === FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD) {
      searchData(undefined, res.id);
    } else {
      searchData();
    }
  }

  function handleFormReview(res: any) {
    reviewByFormResult(res, {
      onSuccess: () => {
        handleFormCreateSaved(res);
      },
    });
  }

  function removeItemFromList(id: string) {
    if (deleteExecute.value) {
      searchData();
      return;
    }
    if (activeShowType.value === 'billboard') {
      billboardRef.value?.refresh();
      getStatistic();
      return;
    }
    propsRes.value.data = propsRes.value.data.filter((item) => item.id !== id);
    propsRes.value.crmPagination = {
      ...propsRes.value.crmPagination,
      itemCount: (propsRes.value.crmPagination?.itemCount ?? 1) - 1,
    };
    getStatistic();
  }

  watch(
    () => tableRemoveRefreshId.value,
    (val) => {
      if (val) {
        removeItemFromList(val);
        getStatistic();
      }
    }
  );

  watch(
    () => tableItemRefreshId.value,
    (val) => {
      if (val) {
        searchData(undefined, val);
        tableItemRefreshId.value = '';
      }
    }
  );

  // 先不上
  // function handleGeneratedChart(res: FilterResult, form: FilterForm) {
  //   advancedOriginalForm.value = form;
  //   setAdvanceFilter(res);
  //   tableAdvanceFilterRef.value?.setAdvancedFilter(res, true);
  //   searchData();
  // }

  // const { initTableViewChartParams, getChartViewId } = useViewChartParams();

  // function viewChartCallBack(params: ViewChartResult) {
  //   const { viewId, formModel, filterResult } = params;
  //   tableAdvanceFilterRef.value?.initFormModal(formModel, true);
  //   setAdvanceFilter(filterResult);
  //   activeTab.value = viewId;
  // }

  watch(
    () => activeTab.value,
    (val) => {
      if (val) {
        checkedRowKeys.value = [];
        setLoadListParams({ keyword: keyword.value, viewId: activeTab.value });
        // initTableViewChartParams(viewChartCallBack);
        crmTableRef.value?.setColumnSort(val);
        getStatistic();
      }
    },
    { immediate: true }
  );

  onMounted(async () => {
    activeShowType.value = (await getItem<'billboard' | 'table'>('contract-active-show-type')) ?? 'table';
    if (route.query.id) {
      activeSourceId.value = route.query.id as string;
      showDetailDrawer.value = true;
    }
  });

  function handleBillboardInit(total: number) {
    billboardTotalCount.value = total;
  }

  // onBeforeUnmount(() => {
  //   sessionStorage.removeItem(STORAGE_VIEW_CHART_KEY);
  // });

  watch(
    () => showExportModal.value,
    (val) => {
      if (val) {
        initApprovalPermission();
      }
    }
  );
</script>

<style lang="less" scoped>
  .show-type-tabs {
    :deep(.n-tabs-tab) {
      padding: 6px;
    }
  }
</style>
