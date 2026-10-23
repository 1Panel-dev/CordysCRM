<template>
  <CrmDrawer
    v-model:show="show"
    :title="t('workbench.dataOverview.myTasks')"
    :width="1200"
    :footer="false"
    show-auto-open-next
    no-padding
  >
    <div class="task-wrapper">
      <div class="task-class">
        <n-scrollbar class="p-[24px]">
          <n-collapse class="pl-[16px]" :default-expanded-names="[props.type || 'pending']">
            <n-collapse-item
              v-for="item in collapseItems"
              :title="item.title"
              :name="item.name"
              :class="{ 'collapse-item--active': activeTaskType.split('-')[0] === item.name }"
            >
              <div
                v-for="child in item.children"
                class="task-item"
                :class="{ 'task-item--active': activeTaskType === child.name }"
                @click="
                  () => {
                    activeTaskType = child.name;
                    selectedKeys = [];
                  }
                "
              >
                {{ child.title }}
                <div v-if="['pending'].includes(item.name)" class="task-count">{{ child.count }}</div>
              </div>
              <template #arrow>
                <CrmIcon type="iconicon_right" :size="12" color="var(--text-n2)" class="mr-[4px]" />
              </template>
              <template #header-extra>
                <div v-if="['pending'].includes(item.name)" class="task-count mr-[16px]">
                  {{ item.count }}
                </div>
              </template>
            </n-collapse-item>
          </n-collapse>
        </n-scrollbar>
      </div>
      <div class="task-content p-[24px]">
        <div class="mb-[16px] flex w-full items-center justify-between">
          <div class="flex flex-1 items-center gap-[8px] font-semibold">
            <div
              v-if="activeTaskType.includes('pending') && approvalConfigDetail?.allowBatchProcess"
              class="flex items-center"
            >
              <n-checkbox
                v-model:checked="allSelect"
                :label="activeModuleTitle"
                :indeterminate="indeterminate"
                @update-checked="handleSelectAll"
              />
              <div v-if="selectedKeys.length > 0" class="flex items-center gap-[4px] font-normal leading-[24px]">
                <div>{{ t('crmPagination.checked') }}</div>
                <div class="text-[var(--primary-8)]">{{ selectedKeys.length }}</div>
                <div>{{ t('crmPagination.item') }}</div>
                <n-button text type="primary" size="small" @click="() => (selectedKeys = [])">
                  <div class="text-[14px] leading-[24px]">{{ t('common.clear') }}</div>
                </n-button>
              </div>
            </div>
            <div v-else>{{ activeModuleTitle }}</div>
            <div
              v-if="activeTaskType.startsWith('pending-') && (listTotal > 0 || processedCount > 0)"
              class="text-[14px] leading-[24px] text-[var(--text-n4)]"
            >
              {{
                listTotal > 0
                  ? t('taskDrawer.pendingCount', { count: listTotal })
                  : t('taskDrawer.processedCount', { count: processedCount })
              }}
            </div>
            <div v-if="activeTaskType.includes('pending') && selectedKeys.length > 0">
              <n-button type="primary" ghost class="mr-[8px]" @click="handleReject">
                {{ t('common.reject') }}
              </n-button>
              <n-button type="primary" ghost @click="handleApprove">{{ t('common.approve') }}</n-button>
            </div>
          </div>
          <div class="ml-auto flex items-center gap-[12px]">
            <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
            <n-button type="default" class="outline--secondary px-[8px]" @click="refresh">
              <CrmIcon class="text-[var(--text-n1)]" type="iconicon_refresh" :size="16" />
            </n-button>
          </div>
        </div>
        <taskList
          v-if="show && activeTaskType"
          ref="taskListRef"
          v-model:selected-keys="selectedKeys"
          key-field="id"
          virtualScrollHeight="100%"
          :activeTaskType="activeTaskType"
          :load-params="{ keyword }"
          :approval-config-detail="approvalConfigDetail"
          :empty-text="pendingEmptyText"
          @list-init="handleListInit"
          @open-detail="handleOpenDetail"
          @approval-success="handleTaskListApprovalSuccess"
        />
      </div>
    </div>
  </CrmDrawer>
  <approvalModal
    v-model:show="approvalVisible"
    :approval-type="approvalType"
    :approval-item-keys="selectedKeys"
    :resource-type="activeResourceType"
    @approval-success="handleBatchApprovalSuccess"
  />
  <ContractDetailDrawer
    v-model:visible="contractDetailVisible"
    :source-id="activeResourceId"
    :approvalTaskId="approvalTaskId"
    @show-customer-drawer="handleOpenCustomerDetail"
    @refresh="handleDetailRefresh"
    @approval-success="handleDetailApprovalSuccess"
  />
  <QuotationDetailDrawer
    v-model:visible="quotationDetailVisible"
    :source-id="activeResourceId"
    :approvalTaskId="approvalTaskId"
    @refresh="handleDetailRefresh"
    @approval-success="handleDetailApprovalSuccess"
  />
  <OrderDetailDrawer
    v-model:visible="orderDetailVisible"
    :source-id="activeResourceId"
    :approvalTaskId="approvalTaskId"
    @refresh="handleDetailRefresh"
    @approval-success="handleDetailApprovalSuccess"
  />
  <InvoiceDetailDrawer
    v-model:visible="invoiceDetailVisible"
    :source-id="activeResourceId"
    :approvalTaskId="approvalTaskId"
    @refresh="handleDetailRefresh"
    @approval-success="handleDetailApprovalSuccess"
    @open-contract-drawer="handleOpenContractDetail"
    @open-customer-drawer="handleOpenCustomerDetail"
  />
  <CustomerOverviewDrawer
    v-model:show="customerDetailVisible"
    :source-id="activeResourceId"
    :approvalTaskId="approvalTaskId"
    @saved="handleDetailRefresh"
    @deleted="handleApproveSuccess"
    @refresh="handleDetailRefresh"
    @approval-success="handleDetailApprovalSuccess"
  />
  <OpenSeaOverviewDrawer
    v-model:show="openSeaDetailVisible"
    :source-id="activeResourceId"
    :pool-id="activePoolId"
    :hidden-columns="openSeaHiddenColumns"
    :approvalTaskId="approvalTaskId"
    @change="handleDetailRefresh"
    @approval-success="handleDetailApprovalSuccess"
    @delete="handleApproveSuccess"
  />
  <ClueOverviewDrawer
    v-model:show="clueDetailVisible"
    :detail="activeResourceDetail"
    :approvalTaskId="approvalTaskId"
    @saved="handleDetailRefresh"
    @remove="handleApproveSuccess"
    @refresh="handleDetailRefresh"
    @approval-success="handleDetailApprovalSuccess"
    @open-customer-drawer="handleOpenCustomerDetail"
  />
  <CluePoolOverviewDrawer
    v-model:show="cluePoolDetailVisible"
    :detail="activeResourceDetail"
    :pool-id="activePoolId"
    :hidden-columns="cluePoolHiddenColumns"
    :approvalTaskId="approvalTaskId"
    @remove="handleApproveSuccess"
    @refresh="handleDetailRefresh"
    @approval-success="handleDetailApprovalSuccess"
  />
  <OptOverviewDrawer
    v-model:show="opportunityDetailVisible"
    :detail="activeResourceDetail"
    :approvalTaskId="approvalTaskId"
    @remove="handleApproveSuccess"
    @refresh="handleDetailRefresh"
    @approval-success="handleDetailApprovalSuccess"
    @open-customer-drawer="handleOpenCustomerDetail"
  />
  <CustomFormDetailDrawer
    v-model:visible="customFormDetailVisible"
    :source-id="activeResourceId"
    :customFormId="resourceType"
    :approvalTaskId="approvalTaskId"
    @edit="handleCustomFormEdit"
    @refresh="handleDetailRefresh"
    @approval-success="handleDetailApprovalSuccess"
  />
  <CrmFormCreateDrawer
    v-model:visible="customFormEditVisible"
    :form-key="FormDesignKeyEnum.CUSTOM_FORM"
    :source-id="activeResourceId"
    :need-init-detail="true"
    :custom-form-id="resourceType"
    @saved="handleDetailRefresh"
    @review="handleCustomFormReview"
  />
</template>

<script setup lang="ts">
  import { NButton, NCheckbox, NCollapse, NCollapseItem, NScrollbar } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { type ApprovalListTypeEnum, ApprovalResourceTypeEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { OptionDTO } from '@lib/shared/models/system/business';
  import type { CluePoolItem } from '@lib/shared/models/system/module';
  import type { ApprovalProcessDetail, ApprovalTodoItem } from '@lib/shared/models/system/process';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import approvalModal from './approvalModal.vue';
  import taskList from './taskList.vue';
  import ClueOverviewDrawer from '@/views/clueManagement/clue/components/clueOverviewDrawer.vue';
  import CluePoolOverviewDrawer from '@/views/clueManagement/cluePool/components/cluePoolOverviewDrawer.vue';
  import ContractDetailDrawer from '@/views/contract/contract/components/detail.vue';
  import InvoiceDetailDrawer from '@/views/contract/invoice/components/detail.vue';
  import CustomerOverviewDrawer from '@/views/customer/components/customerOverviewDrawer.vue';
  import OpenSeaOverviewDrawer from '@/views/customer/components/openSeaOverviewDrawer.vue';
  import CustomFormDetailDrawer from '@/views/customForm/components/detail.vue';
  import OptOverviewDrawer from '@/views/opportunity/components/optOverviewDrawer.vue';
  import QuotationDetailDrawer from '@/views/opportunity/components/quotation/detail.vue';
  import OrderDetailDrawer from '@/views/order/order/components/detail.vue';

  import {
    getApprovalConfigDetail,
    getApprovalFlowFormOptions,
    getOpenSeaOptions,
    getPoolOptions,
    getTodoStatistic,
  } from '@/api/modules';
  import useApprovalResourceAction from '@/hooks/useApprovalResourceAction';
  import useOpenNewPage from '@/hooks/useOpenNewPage.js';
  import { hasAnyPermission } from '@/utils/permission';

  import { ContractRouteEnum, CustomerRouteEnum } from '@/enums/routeEnum.js';

  const props = defineProps<{
    type?: ApprovalListTypeEnum;
  }>();

  const { t } = useI18n();
  const { openNewPage } = useOpenNewPage();

  const show = defineModel<boolean>('show', {
    required: true,
    default: false,
  });
  const autoOpenNextStorageKey = 'crm-task-auto-open-next';
  const autoOpenNext = ref(localStorage.getItem(autoOpenNextStorageKey) !== 'false');
  watch(autoOpenNext, (value) => localStorage.setItem(autoOpenNextStorageKey, String(value)));
  provide('crmTaskAutoOpenNext', autoOpenNext);

  const keyword = ref('');
  const activeTaskType = ref<string>('');

  function getTaskResourceType(taskType: string) {
    const separatorIndex = taskType.indexOf('-');
    return separatorIndex === -1 ? '' : taskType.slice(separatorIndex + 1);
  }

  const statistic = ref<Record<string, any>>({
    total: 0,
    contract: 0,
    quotation: 0,
    order: 0,
    invoice: 0,
  });

  const approvalFormOptions = ref<OptionDTO[]>([]);
  const moduleItems = computed(() =>
    approvalFormOptions.value.map((item) => ({
      name: String(item.id),
      title: item.name,
      count: 0,
    }))
  );
  const allItems = computed(() => [
    {
      name: 'pending',
      title: t('workbench.dataOverview.pendingApproval'),
      count: 0,
      children: moduleItems.value.map((e) => ({ ...e, name: `pending-${e.name}` })),
    },
    {
      name: 'approved',
      title: t('workbench.dataOverview.approvedByMe'),
      count: 0,
      children: moduleItems.value.map((e) => ({ ...e, name: `approved-${e.name}` })),
    },
    {
      name: 'initiated',
      title: t('workbench.dataOverview.initiatedByMe'),
      count: 0,
      children: moduleItems.value.map((e) => ({ ...e, name: `initiated-${e.name}` })),
    },
    {
      name: 'copied',
      title: t('workbench.dataOverview.copiedToMe'),
      count: 0,
      children: moduleItems.value.map((e) => ({ ...e, name: `copied-${e.name}` })),
    },
    {
      name: 'mentioned',
      title: t('taskDrawer.mentionedMe'),
      count: 0,
      children: moduleItems.value.map((e) => ({ ...e, name: `mentioned-${e.name}` })),
    },
  ]);

  function getStatisticCount(resourceType: string) {
    return statistic.value[resourceType] ?? statistic.value[resourceType.toLowerCase()] ?? 0;
  }

  const collapseItems = computed(() => {
    return allItems.value.map((e) => {
      if (e.name === 'pending') {
        e.count = statistic.value.total;
        e.children = e.children.map((child) => {
          const resourceType = getTaskResourceType(child.name);
          return {
            ...child,
            count: getStatisticCount(resourceType),
          };
        });
      }
      return e;
    });
  });

  const activeModuleTitle = computed(() => {
    const resourceType = getTaskResourceType(activeTaskType.value);
    const module = moduleItems.value.find((item) => item.name === resourceType);
    return module ? module.title : '';
  });
  const activeResourceType = computed(() => getTaskResourceType(activeTaskType.value));
  const allSelect = ref<boolean>(false);
  const selectedKeys = ref<string[]>([]);
  const listTotal = ref(0);
  const listAllKeys = ref<string[]>([]);
  const processedCount = ref(0);
  const pendingEmptyText = computed(() =>
    processedCount.value > 0 && activeTaskType.value.startsWith('pending-')
      ? t('taskDrawer.categoryComplete', { count: processedCount.value })
      : ''
  );
  const indeterminate = computed(() => {
    if (selectedKeys.value.length === 0 || selectedKeys.value.length === listTotal.value) {
      return false;
    }
    return selectedKeys.value.length > 0;
  });

  function handleSelectAll() {
    if (allSelect.value === true) {
      selectedKeys.value = [...listAllKeys.value];
    } else {
      selectedKeys.value = [];
    }
  }

  watch(
    () => selectedKeys.value,
    (val) => {
      if (val.length === 0) {
        allSelect.value = false;
      } else if (val.length === listTotal.value) {
        allSelect.value = true;
      } else {
        allSelect.value = false;
      }
    }
  );

  async function initStatistic() {
    try {
      statistic.value = await getTodoStatistic();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function initApprovalFormOptions() {
    try {
      approvalFormOptions.value = await getApprovalFlowFormOptions();
      const targetListType = props.type || 'pending';
      const resourceType = getTaskResourceType(activeTaskType.value);

      if (!resourceType || !moduleItems.value.some((item) => item.name === resourceType)) {
        activeTaskType.value = moduleItems.value.length ? `${targetListType}-${moduleItems.value[0].name}` : '';
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      approvalFormOptions.value = [];
      activeTaskType.value = '';
    }
  }

  watch(
    () => show.value,
    (val) => {
      if (val) {
        initApprovalFormOptions();
        initStatistic();
      }
    },
    {
      immediate: true,
    }
  );

  function handleListInit(total: number, keys: string[]) {
    listTotal.value = total;
    listAllKeys.value = keys;
  }

  const taskListRef = ref<InstanceType<typeof taskList>>();

  watch(
    () => activeTaskType.value,
    () => {
      processedCount.value = 0;
    }
  );

  async function handleSuccessfulProcessing(count = 1, currentApprovalTaskId?: string) {
    if (!activeTaskType.value.startsWith('pending-')) return;
    processedCount.value += count;
    await initStatistic();
    if (autoOpenNext.value && currentApprovalTaskId) {
      await taskListRef.value?.openNextPending(currentApprovalTaskId);
    } else {
      await taskListRef.value?.loadTaskList(true);
    }
  }

  function handleTaskListApprovalSuccess(
    _singleItem: boolean,
    _currentApprovalTaskId: string | undefined,
    count: number
  ) {
    if (activeTaskType.value.startsWith('pending-')) {
      processedCount.value += count;
    }
    initStatistic();
    taskListRef.value?.loadTaskList(true);
  }

  function handleBatchApprovalSuccess() {
    const count = selectedKeys.value.length;
    allSelect.value = false;
    selectedKeys.value = [];
    if (count > 0) {
      handleSuccessfulProcessing(count);
    }
  }

  function searchData(val?: string) {
    taskListRef.value?.loadTaskList(true, val);
  }

  function refresh() {
    searchData();
    initStatistic();
  }

  const approvalVisible = ref(false);
  const approvalType = ref<'approve' | 'reject'>('approve');

  function handleReject() {
    approvalType.value = 'reject';
    approvalVisible.value = true;
  }

  function handleApprove() {
    approvalType.value = 'approve';
    approvalVisible.value = true;
  }

  watch(
    () => show.value,
    (val) => {
      if (!val) {
        keyword.value = '';
        activeTaskType.value = '';
      } else if (props.type) {
        const firstModule = moduleItems.value[0]?.name;
        activeTaskType.value = firstModule ? `${props.type}-${firstModule}` : '';
      }
    }
  );

  const approvalConfigDetail = ref<ApprovalProcessDetail>();

  async function initApprovalConfigDetail() {
    try {
      const [_, resourceType] = activeTaskType.value.split('-');
      if (!resourceType) {
        approvalConfigDetail.value = undefined;
        return;
      }
      approvalConfigDetail.value = await getApprovalConfigDetail(resourceType);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => activeTaskType.value,
    () => {
      initApprovalConfigDetail();
    },
    {
      immediate: true,
    }
  );

  const activeResourceId = ref('');
  const contractDetailVisible = ref(false);
  const quotationDetailVisible = ref(false);
  const orderDetailVisible = ref(false);
  const invoiceDetailVisible = ref(false);
  const customerDetailVisible = ref(false);
  const openSeaDetailVisible = ref(false);
  const clueDetailVisible = ref(false);
  const cluePoolDetailVisible = ref(false);
  const opportunityDetailVisible = ref(false);
  const customFormDetailVisible = ref(false);
  const customFormEditVisible = ref(false);
  const approvalTaskId = ref('');
  const resourceType = ref('');
  const activeResourceName = ref('');
  const activePoolId = ref('');
  const activeResourceDetail = computed(() => ({
    id: activeResourceId.value,
    name: activeResourceName.value,
  }));

  type ApprovalTodoItemWithPool = ApprovalTodoItem & {
    inSharedPool?: boolean;
    poolId?: string | number;
  };

  function getHiddenColumns(poolOptions: CluePoolItem[], poolId: string) {
    const pool = poolOptions.find((item) => item.id === poolId);
    return pool?.fieldConfigs.filter((item) => !item.enable).map((item) => item.fieldId) || [];
  }

  const openSeaOptions = ref<CluePoolItem[]>([]);
  const cluePoolOptions = ref<CluePoolItem[]>([]);
  const openSeaHiddenColumns = computed(() => getHiddenColumns(openSeaOptions.value, activePoolId.value));
  const cluePoolHiddenColumns = computed(() => getHiddenColumns(cluePoolOptions.value, activePoolId.value));
  const { reviewByFormResult } = useApprovalResourceAction({
    formKey: resourceType,
  });

  async function initOpenSeaOptions() {
    if (openSeaOptions.value.length || !hasAnyPermission(['CUSTOMER_MANAGEMENT_POOL:READ'])) {
      return;
    }
    try {
      openSeaOptions.value = await getOpenSeaOptions();
    } catch (error) {
      // 池配置不可用时仍允许审批待办进入详情，使用详情接口的默认字段展示。
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function initCluePoolOptions() {
    if (cluePoolOptions.value.length || !hasAnyPermission(['CLUE_MANAGEMENT_POOL:READ'])) {
      return;
    }
    try {
      cluePoolOptions.value = await getPoolOptions();
    } catch (error) {
      // 池配置不可用时仍允许审批待办进入详情，使用详情接口的默认字段展示。
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function handleOpenDetail(
    resourceId: string,
    _resourceType: string,
    _approvalTaskId: string,
    item: ApprovalTodoItemWithPool
  ) {
    activeResourceId.value = resourceId;
    approvalTaskId.value = _approvalTaskId;
    resourceType.value = _resourceType;
    activeResourceName.value = item.resourceName;
    activePoolId.value = item.poolId ? String(item.poolId) : '';

    switch (_resourceType) {
      case ApprovalResourceTypeEnum.CLUE:
        if (item.poolId) {
          await initCluePoolOptions();
          cluePoolDetailVisible.value = true;
          break;
        }
        clueDetailVisible.value = true;
        break;
      case ApprovalResourceTypeEnum.CUSTOMER:
        if (item.inSharedPool) {
          await initOpenSeaOptions();
          openSeaDetailVisible.value = true;
          break;
        }
        customerDetailVisible.value = true;
        break;
      case ApprovalResourceTypeEnum.OPPORTUNITY:
        opportunityDetailVisible.value = true;
        break;
      case ApprovalResourceTypeEnum.CONTRACT:
        contractDetailVisible.value = true;
        break;
      case ApprovalResourceTypeEnum.QUOTATION:
        quotationDetailVisible.value = true;
        break;
      case ApprovalResourceTypeEnum.ORDER:
        orderDetailVisible.value = true;
        break;
      case ApprovalResourceTypeEnum.INVOICE:
        invoiceDetailVisible.value = true;
        break;
      default:
        customFormDetailVisible.value = true;
        break;
    }
  }

  function closeDetailDrawers() {
    contractDetailVisible.value = false;
    quotationDetailVisible.value = false;
    orderDetailVisible.value = false;
    invoiceDetailVisible.value = false;
    customerDetailVisible.value = false;
    openSeaDetailVisible.value = false;
    clueDetailVisible.value = false;
    cluePoolDetailVisible.value = false;
    opportunityDetailVisible.value = false;
    customFormDetailVisible.value = false;
  }

  const skipNextDetailRefresh = ref(false);

  function handleDetailRefresh() {
    if (skipNextDetailRefresh.value) {
      skipNextDetailRefresh.value = false;
      return;
    }
    initStatistic();
    taskListRef.value?.loadTaskList(true);
  }

  function handleApproveSuccess() {
    closeDetailDrawers();
    handleDetailRefresh();
    allSelect.value = false;
    selectedKeys.value = [];
  }

  function handleDetailApprovalSuccess() {
    skipNextDetailRefresh.value = true;
    closeDetailDrawers();
    allSelect.value = false;
    selectedKeys.value = [];
    handleSuccessfulProcessing(1, approvalTaskId.value);
  }

  function handleCustomFormEdit(sourceId: string) {
    activeResourceId.value = sourceId;
    customFormEditVisible.value = true;
  }

  function handleCustomFormReview(res: any) {
    reviewByFormResult(res, {
      onSuccess: handleApproveSuccess,
    });
  }

  function handleOpenCustomerDetail(params: { customerId: string; inCustomerPool: boolean; poolId: string }) {
    if (params.inCustomerPool) {
      openNewPage(CustomerRouteEnum.CUSTOMER_OPEN_SEA, {
        id: params.customerId,
        poolId: params.poolId,
      });
    } else {
      openNewPage(CustomerRouteEnum.CUSTOMER_INDEX, {
        id: params.customerId,
      });
    }
  }

  function handleOpenContractDetail(params: { id: string }) {
    openNewPage(ContractRouteEnum.CONTRACT_INDEX, {
      id: params.id,
    });
  }
</script>

<style lang="less" scoped>
  .task-wrapper {
    @apply flex h-full;
    .task-class {
      width: 268px;
      border-right: 1px solid var(--text-n8);
      @apply h-full overflow-hidden;
      .task-count {
        color: var(--text-n4);
      }
      .task-item {
        @apply flex w-full cursor-pointer items-center justify-between;

        padding: 6px 16px;
        padding-left: 20px;
        &--active {
          color: var(--primary-8);
          background-color: var(--primary-7);
        }
      }
      .n-collapse-item {
        border-top: none !important;
        &:not(:first-child) {
          margin-top: 8px;
        }
        :deep(.n-collapse-item__header) {
          padding: 4px 0;
          .n-collapse-item__header-main {
            line-height: 26px;
          }
        }
        :deep(.n-collapse-item__content-inner) {
          padding: 0;
        }
      }
      .collapse-item--active {
        :deep(.n-collapse-item__header-main) {
          color: var(--primary-8);
        }
      }
    }
    .task-footer {
      position: absolute;
      bottom: 0;
      width: 268px;
      height: 56px;
    }
    .task-content {
      width: calc(100% - 268px);
      @apply h-full overflow-hidden;
      :deep(.n-checkbox__label) {
        font-weight: 600;
      }
    }
  }
</style>
