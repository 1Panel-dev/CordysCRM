<template>
  <CrmPageWrapper :title="t('workbench.task')">
    <div class="flex h-full flex-col overflow-hidden">
      <van-tabs v-model:active="activeName" border class="detail-tabs" @change="changeResourceTab">
        <van-tab v-for="tab of tabList" :key="tab.name" :name="tab.name">
          <template #title>
            <div class="text-[16px]" :class="activeName === tab.name ? 'text-[var(--primary-8)]' : ''">
              {{ tab.title }}
            </div>
          </template>
          <div class="filter-buttons flex gap-2">
            <template v-for="item of resourceTypes" :key="item.name">
              <van-button
                round
                size="small"
                class="!border-none !px-[16px] !py-[4px] !text-[14px]"
                :class="
                  resourceType === item.name
                    ? '!bg-[var(--primary-7)] !text-[var(--primary-8)]'
                    : '!bg-[var(--text-n9)] !text-[var(--text-n1)]'
                "
                @click="
                  () => {
                    resourceType = item.name;
                    selectedKeys = [];
                  }
                "
              >
                <div class="flex items-center gap-[4px]">
                  {{ item.tab }}
                  <div v-if="activeName === ApprovalListTypeEnum.PENDING">
                    {{ item.count > 99 ? '99+' : item.count }}
                  </div>
                </div>
              </van-button>
            </template>
          </div>
        </van-tab>
      </van-tabs>
      <CrmList
        ref="crmListRef"
        :list-params="listParams"
        class="bg-[var(--text-n9)] p-[16px]"
        :class="selectedKeys.length > 0 ? 'mb-[36px]' : ''"
        :item-gap="16"
        :keyword="keyword"
        :load-list-api="lisApiMap[activeName as ApprovalListTypeEnum]"
      >
        <template #item="{ item }">
          <div
            class="approval-item-card"
            :class="selectedKeys.includes(item.approvalTaskId) ? 'approval-item-card--active' : ''"
            @click="() => handleItemClick(item)"
          >
            <div
              v-if="activeName === ApprovalListTypeEnum.PENDING"
              class="icon-split-bg"
              :class="selectedKeys.includes(item.approvalTaskId) ? 'icon-split-bg--active' : ''"
            >
              <CrmIcon name="iconicon_check" width="16px" height="16px" color="var(--text-n10)" />
            </div>
            <div class="relative z-10">
              <div class="flex items-center gap-[8px]">
                <CrmTag
                  plain
                  :tag="processStatusMap[item.dataResult as ProcessStatusEnum].label"
                  :text-color="processStatusMap[item.dataResult as ProcessStatusEnum].color"
                  :bgColor="processStatusMap[item.dataResult as ProcessStatusEnum].bgColor"
                />
                <div class="one-line-text font-semibold">{{ item.resourceName }}</div>
              </div>
              <div class="mt-[8px] flex items-center gap-[8px]">
                <div class="flex w-[32px] items-center">
                  <CrmAvatar :text="item.applicant" :size="32" />
                </div>
                <div class="flex w-full flex-col gap-[2px]">
                  <div class="flex items-center justify-between">
                    <div class="one-line-text flex-1">{{ item.applicant }}</div>
                    <div class="flex items-center gap-[8px]">
                      <van-button type="primary" size="mini" class="h-[20px]" @click.stop="goDetail(item)">
                        {{ t('common.detail') }}
                      </van-button>
                      <template v-if="activeName === ApprovalListTypeEnum.PENDING">
                        <van-button
                          color="var(--error-5)"
                          size="mini"
                          class="h-[20px]"
                          @click.stop="handleReject(item)"
                        >
                          <div class="text-[var(--error-red)]">{{ t('workbench.operation.REJECT') }}</div>
                        </van-button>
                        <van-button type="success" size="mini" class="h-[20px]" @click.stop="handleApprove(item)">
                          {{ t('workbench.operation.APPROVE') }}
                        </van-button>
                      </template>
                    </div>
                  </div>
                  <div class="flex items-center justify-between text-[12px] text-[var(--text-n4)]">
                    <div class="flex items-center gap-[8px]">
                      <div class="text-[var(--text-n2)]">{{ t('workbench.approvalType') }}</div>
                      <div>{{ getExecuteType(item.executeTime) }}</div>
                    </div>
                    <div>{{ dayjs(item.submitTime).format('YYYY-MM-DD HH:mm:ss') }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </CrmList>
      <div
        v-if="selectedKeys.length > 0"
        class="fixed bottom-0 left-0 right-0 z-10 flex items-center justify-between gap-[16px] bg-[var(--text-n10)] p-[16px]"
      >
        <van-button color="var(--error-5)" block @click.stop="handleBatchReject">
          <div class="text-[var(--error-red)]">{{ t('workbench.operation.REJECT') }}</div>
        </van-button>
        <van-button type="success" block @click.stop="handleBatchApprove">
          {{ t('workbench.operation.APPROVE') }}
        </van-button>
      </div>
    </div>
  </CrmPageWrapper>
  <ApprovalPopup
    v-model:show="showApprovalPopup"
    :approving-item="approvingItem"
    :is-rejecting="isRejecting"
    :resource-type="resourceType"
    :selected-keys="selectedKeys"
    @refresh="refreshTaskList(true)"
  />
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import ApprovalPopup from '../approval/approvalPopup.vue';

  import {
    getCcApprovalList,
    getInitiatedApprovalList,
    getPendingApprovalList,
    getProcessedApprovalList,
    getTodoStatistic,
  } from '@/api/modules';
  import type { ApprovalTodoItem, TodoStatistic } from '@lib/shared/models/system/process';
  import {
    ApprovalListTypeEnum,
    ApprovalResourceTypeEnum,
    ApprovalTaskExecuteTimeEnum,
    ProcessStatusEnum,
  } from '@lib/shared/enums/process';
  import { useRoute, useRouter } from 'vue-router';
  import dayjs from 'dayjs';
  import { WorkbenchRouteEnum } from '@/enums/routeEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum.js';

  const { t } = useI18n();
  const route = useRoute();
  const router = useRouter();

  const activeName = ref();

  const tabList = [
    {
      name: ApprovalListTypeEnum.PENDING,
      title: t('workbench.myApproval'),
      count: 0,
    },
    {
      name: ApprovalListTypeEnum.APPROVAL,
      title: t('workbench.myProcess'),
      count: 0,
    },
    {
      name: ApprovalListTypeEnum.INITIATED,
      title: t('workbench.myApply'),
      count: 0,
    },
    {
      name: ApprovalListTypeEnum.COPIED,
      title: t('workbench.copyToMe'),
      count: 0,
    },
  ];

  const keyword = ref('');
  const resourceType = ref(ApprovalResourceTypeEnum.QUOTATION);
  const statistic = ref<TodoStatistic>();
  const resourceTypes = computed(() => {
    return [
      {
        name: ApprovalResourceTypeEnum.QUOTATION,
        tab: t('formCreate.quotation'),
        count: statistic.value?.quotation || 0,
      },
      {
        name: ApprovalResourceTypeEnum.CONTRACT,
        tab: t('formCreate.contract'),
        count: statistic.value?.contract || 0,
      },
      {
        name: ApprovalResourceTypeEnum.ORDER,
        tab: t('formCreate.order'),
        count: statistic.value?.order || 0,
      },
      {
        name: ApprovalResourceTypeEnum.INVOICE,
        tab: t('formCreate.invoice'),
        count: statistic.value?.invoice || 0,
      },
    ];
  });
  const selectedKeys = ref<string[]>([]);

  const lisApiMap = {
    [ApprovalListTypeEnum.PENDING]: getPendingApprovalList,
    [ApprovalListTypeEnum.APPROVAL]: getProcessedApprovalList,
    [ApprovalListTypeEnum.INITIATED]: getInitiatedApprovalList,
    [ApprovalListTypeEnum.COPIED]: getCcApprovalList,
  };
  const listParams = computed(() => {
    return {
      resourceType: resourceType.value,
    };
  });

  async function initStatistic() {
    try {
      statistic.value = await getTodoStatistic();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const processStatusMap: Record<ProcessStatusEnum, any> = {
    [ProcessStatusEnum.APPROVED]: {
      label: t('workbench.result.APPROVED'),
      icon: 'iconicon_succeed_filled',
      color: 'var(--success-green)',
      bgColor: 'var(--success-green)',
    },
    [ProcessStatusEnum.AUTO_APPROVED]: {
      label: t('workbench.result.AUTO_APPROVED'),
      icon: 'iconicon_succeed_filled',
      color: 'var(--success-green)',
      bgColor: 'var(--success-green)',
    },
    [ProcessStatusEnum.APPROVING]: {
      label: t('workbench.result.APPROVING'),
      icon: 'iconicon_wait',
      color: 'var(--warning-yellow)',
      bgColor: 'var(--warning-yellow)',
    },
    [ProcessStatusEnum.AUTO_UNAPPROVED]: {
      label: t('workbench.result.AUTO_UNAPPROVED'),
      icon: 'iconicon_close_circle_filled',
      color: 'var(--error-red)',
      bgColor: 'var(--error-red)',
    },
    [ProcessStatusEnum.UNAPPROVED]: {
      label: t('workbench.result.UNAPPROVED'),
      icon: 'iconicon_close_circle_filled',
      color: 'var(--error-red)',
      bgColor: 'var(--error-red)',
    },
    [ProcessStatusEnum.REVOKED]: {
      label: t('workbench.result.REVOKED'),
      icon: 'iconicon_skip_planarity',
      color: 'var(--text-n4)',
      bgColor: '',
    },
    [ProcessStatusEnum.PENDING]: {
      label: t('workbench.result.PENDING'),
      icon: 'iconicon_minus_circle_filled1',
      color: 'var(--text-n4)',
      bgColor: '',
    },

    [ProcessStatusEnum.NONE]: {
      label: '-',
      icon: '',
      color: '',
      bgColor: '',
    },
  };

  function getExecuteType(executeTime: ApprovalTaskExecuteTimeEnum) {
    switch (executeTime) {
      case ApprovalTaskExecuteTimeEnum.CREATE:
        return t('common.create');
      case ApprovalTaskExecuteTimeEnum.UPDATE:
        return t('common.edit');
      case ApprovalTaskExecuteTimeEnum.DELETE:
        return t('common.delete');
      default:
        return '-';
    }
  }

  function handleItemClick(item: ApprovalTodoItem) {
    if (activeName.value !== ApprovalListTypeEnum.PENDING) {
      return;
    }
    const index = selectedKeys.value.indexOf(item.approvalTaskId);
    if (index > -1) {
      selectedKeys.value.splice(index, 1);
    } else {
      selectedKeys.value.push(item.approvalTaskId);
    }
  }

  const crmListRef = ref<InstanceType<typeof CrmList>>();

  function refreshTaskList(refreshCount = false) {
    nextTick(() => {
      crmListRef.value?.loadList(true);
    });
    if (refreshCount) {
      initStatistic();
    }
    selectedKeys.value = [];
  }

  function changeResourceTab() {
    refreshTaskList();
    selectedKeys.value = [];
  }
  const formKeyMap = {
    [ApprovalResourceTypeEnum.ALL]: '',
    [ApprovalResourceTypeEnum.QUOTATION]: FormDesignKeyEnum.OPPORTUNITY_QUOTATION_SNAPSHOT,
    [ApprovalResourceTypeEnum.CONTRACT]: FormDesignKeyEnum.CONTRACT_SNAPSHOT,
    [ApprovalResourceTypeEnum.ORDER]: FormDesignKeyEnum.ORDER_SNAPSHOT,
    [ApprovalResourceTypeEnum.INVOICE]: FormDesignKeyEnum.INVOICE_SNAPSHOT,
  };
  function goDetail(item: ApprovalTodoItem) {
    router.push({
      name: WorkbenchRouteEnum.WORKBENCH_APPROVAL,
      query: {
        id: item.resourceId,
        formKey: formKeyMap[item.resourceType],
        taskId: item.approvalTaskId,
        approvalStatus: item.approvalOperation,
      },
    });
  }

  const showApprovalPopup = ref(false);
  const isRejecting = ref(false);
  const approvingItem = ref<ApprovalTodoItem>();

  function handleApprove(item: ApprovalTodoItem) {
    isRejecting.value = false;
    approvingItem.value = item;
    selectedKeys.value = [];
    showApprovalPopup.value = true;
  }

  function handleReject(item: ApprovalTodoItem) {
    isRejecting.value = true;
    approvingItem.value = item;
    selectedKeys.value = [];
    showApprovalPopup.value = true;
  }

  function handleBatchReject() {
    if (selectedKeys.value.length === 0) {
      return;
    }
    isRejecting.value = true;
    approvingItem.value = undefined;
    showApprovalPopup.value = true;
  }

  function handleBatchApprove() {
    if (selectedKeys.value.length === 0) {
      return;
    }
    isRejecting.value = false;
    approvingItem.value = undefined;
    showApprovalPopup.value = true;
  }

  watch(
    () => resourceType.value,
    () => {
      refreshTaskList();
    }
  );

  onBeforeMount(() => {
    initStatistic();
    if (route.query.type) {
      activeName.value = route.query.type as ApprovalListTypeEnum;
    } else {
      activeName.value = ApprovalListTypeEnum.PENDING;
    }
  });
</script>

<style lang="less" scoped>
  .approval-item-card {
    @apply relative overflow-hidden;

    padding: 16px;
    border: 1px solid transparent;
    border-radius: var(--border-radius-small);
    background-color: var(--text-n10);
    &--active {
      border: 1px solid var(--primary-8);
    }
  }
  .icon-split-bg {
    @apply absolute left-0 top-0 z-10 flex;

    width: 28px;
    height: 28px;
    color: var(--text-n10);
    background: linear-gradient(to bottom right, var(--text-n6) 50%, transparent 50%);
    &--active {
      background: linear-gradient(to bottom right, var(--primary-8) 50%, transparent 50%);
    }
  }
  .detail-tabs {
    :deep(.van-hairline--top-bottom) {
      margin-top: -0.5px;
    }
  }
  .filter-buttons {
    @apply flex;

    gap: 8px;
    padding: 8px 4px;
    background-color: var(--text-n10);
    .half-px-border-bottom();
  }
</style>
