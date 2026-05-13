<template>
  <CrmDrawer v-model:show="show" :title="t('workbench.dataOverview.myTasks')" :width="1200" :footer="false" no-padding>
    <div class="task-wrapper">
      <div class="task-class">
        <div class="p-[24px]">
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
                @click="activeTaskType = child.name"
              >
                {{ child.title }}
                <div v-if="item.name === 'pending'" class="task-count">{{ item.count }}</div>
              </div>
              <template #arrow>
                <CrmIcon type="iconicon_right" :size="12" color="var(--text-n2)" class="mr-[4px]" />
              </template>
              <template #header-extra>
                <div v-if="item.name === 'pending'" class="task-count mr-[16px]">{{ item.count }}</div>
              </template>
            </n-collapse-item>
          </n-collapse>
        </div>
      </div>
      <div class="task-content p-[24px]">
        <div class="mb-[16px] flex w-full items-center justify-between">
          <div class="flex flex-1 items-center gap-[8px]">
            <div class="font-semibold">
              <n-checkbox
                v-if="activeTaskType.includes('pending') && approvalConfigDetail?.allowBatchProcess"
                :value="allSelect"
                :label="activeModuleTitle"
                :indeterminate="indeterminate"
                @update-checked="handleSelectAll"
              />
              <div v-else>{{ activeModuleTitle }}</div>
              <template v-if="activeTaskType.includes('pending') && selectedKeys.length > 0">
                <n-button type="primary" ghost class="mr-[8px]" @click="handleReject">
                  {{ t('common.reject') }}
                </n-button>
                <n-button type="primary" ghost @click="handleApprove">{{ t('common.approve') }}</n-button>
              </template>
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
          v-if="show"
          ref="taskListRef"
          v-model:selected-keys="selectedKeys"
          key-field="id"
          virtualScrollHeight="100%"
          :activeTaskType="activeTaskType"
          :load-params="{ keyword }"
          :approval-config-detail="approvalConfigDetail"
          @list-init="handleListInit"
          @open-detail="handleOpenDetail"
        />
      </div>
    </div>
  </CrmDrawer>
  <approvalModal
    v-model:show="approvalVisible"
    :approval-type="approvalType"
    :approval-item-keys="selectedKeys"
    module="WORKBENCH"
  />
  <ContractDetailDrawer v-model:visible="contractDetailVisible" :source-id="activeResourceId" />
  <QuotationDetailDrawer v-model:visible="quotationDetailVisible" :source-id="activeResourceId" />
  <OrderDetailDrawer v-model:visible="orderDetailVisible" :source-id="activeResourceId" />
  <InvoiceDetailDrawer v-model:visible="invoiceDetailVisible" :source-id="activeResourceId" />
</template>

<script setup lang="ts">
  import { NButton, NCheckbox, NCollapse, NCollapseItem } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { type ApprovalListTypeEnum, ApprovalResourceTypeEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ApprovalProcessDetail } from '@lib/shared/models/system/process';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import approvalModal from './approvalModal.vue';
  import taskList from './taskList.vue';
  import ContractDetailDrawer from '@/views/contract/contract/components/detail.vue';
  import InvoiceDetailDrawer from '@/views/contract/invoice/components/detail.vue';
  import QuotationDetailDrawer from '@/views/opportunity/components/quotation/detail.vue';
  import OrderDetailDrawer from '@/views/order/order/components/detail.vue';

  import { getApprovalConfigDetail, getTodoStatistic } from '@/api/modules';

  const props = defineProps<{
    type?: ApprovalListTypeEnum;
  }>();

  const { t } = useI18n();

  const show = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const keyword = ref('');
  const activeTaskType = ref<string>('pending-QUOTATION');

  const moduleItems = [
    {
      name: ApprovalResourceTypeEnum.QUOTATION,
      title: t('menu.quotation'),
      count: 0,
    },
    {
      name: ApprovalResourceTypeEnum.CONTRACT,
      title: t('module.contract'),
      count: 0,
    },
    {
      name: ApprovalResourceTypeEnum.ORDER,
      title: t('module.order'),
      count: 0,
    },
    {
      name: ApprovalResourceTypeEnum.INVOICE,
      title: t('module.invoiceApproval'),
      count: 0,
    },
  ];
  const collapseItems = ref([
    {
      name: 'pending',
      title: t('workbench.dataOverview.pendingApproval'),
      count: 0,
      children: cloneDeep(moduleItems).map((e) => ({ ...e, name: `pending-${e.name}` })),
    },
    {
      name: 'approved',
      title: t('workbench.dataOverview.approvedByMe'),
      count: 0,
      children: cloneDeep(moduleItems).map((e) => ({ ...e, name: `approved-${e.name}` })),
    },
    {
      name: 'initiated',
      title: t('workbench.dataOverview.initiatedByMe'),
      count: 0,
      children: cloneDeep(moduleItems).map((e) => ({ ...e, name: `initiated-${e.name}` })),
    },
    {
      name: 'copied',
      title: t('workbench.dataOverview.copiedToMe'),
      count: 0,
      children: cloneDeep(moduleItems).map((e) => ({ ...e, name: `copied-${e.name}` })),
    },
  ]);

  const activeModuleTitle = computed(() => {
    const [_, moduleName] = activeTaskType.value.split('-');
    const module = moduleItems.find((item) => item.name === moduleName);
    return module ? module.title : '';
  });
  const allSelect = ref<string>('N');
  const selectedKeys = ref<string[]>([]);
  const listTotal = ref(0);
  const listAllKeys = ref<string[]>([]);
  const indeterminate = computed(() => {
    if (selectedKeys.value.length === 0 || selectedKeys.value.length === listTotal.value) {
      return false;
    }
    return selectedKeys.value.length > 0;
  });

  function handleSelectAll() {
    if (allSelect.value === 'Y') {
      selectedKeys.value = [];
      allSelect.value = 'N';
    } else {
      allSelect.value = 'Y';
      selectedKeys.value = [...listAllKeys.value];
    }
  }

  const statistic = ref<Record<string, any>>({
    total: 0,
    contract: 0,
    quotation: 0,
    order: 0,
    invoice: 0,
  });
  async function initStatistic() {
    try {
      statistic.value = await getTodoStatistic();
      collapseItems.value = collapseItems.value.map((e) => {
        if (e.name === 'pending') {
          e.children = e.children.map((child) => {
            const [_, name] = child.name.split('-');
            return {
              ...child,
              count: statistic.value[name.toLowerCase()],
            };
          });
        }
        return e;
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => show.value,
    (val) => {
      if (val) {
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
  function searchData(val?: string) {
    taskListRef.value?.loadTaskList(true, val);
  }

  function refresh() {
    searchData();
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
        activeTaskType.value = 'pending-QUOTATION';
      } else if (props.type) {
        activeTaskType.value = `${props.type}-QUOTATION`;
      }
    }
  );

  const approvalConfigDetail = ref<ApprovalProcessDetail>();

  async function initApprovalConfigDetail() {
    try {
      const [_, resourceType] = activeTaskType.value.split('-');
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
  function handleOpenDetail(resourceId: string) {
    activeResourceId.value = resourceId;
    const [_, resourceType] = activeTaskType.value.split('-');
    switch (resourceType) {
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
        break;
    }
  }
</script>

<style lang="less" scoped>
  .task-wrapper {
    @apply flex h-full;
    .task-class {
      width: 268px;
      border-right: 1px solid var(--text-n8);
      @apply h-full;
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
      @apply h-full;
      :deep(.n-checkbox__label) {
        font-weight: 600;
      }
    }
  }
</style>
