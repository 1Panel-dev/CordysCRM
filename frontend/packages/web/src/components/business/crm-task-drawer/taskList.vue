<template>
  <n-spin :show="loading" class="h-[calc(100%-48px)] min-h-[300px]">
    <n-checkbox-group v-model:value="selectedKeys" :disabled="!approvalConfigDetail?.allowBatchProcess" class="h-full">
      <CrmList
        v-if="list.length"
        v-model:data="list"
        :virtual-scroll-height="props.virtualScrollHeight"
        :key-field="props.keyField"
        :item-height="114"
        mode="remote"
        @reach-bottom="handleReachBottom"
      >
        <template #item="{ item }">
          <div
            class="task-item"
            :class="selectedKeys.includes(item.approvalTaskId) ? '!border-[var(--primary-8)]' : ''"
            @click.stop="
              () => {
                if (!props.activeTaskType.includes('copied') || getResourcePermission(item)) {
                  emit('openDetail', item.resourceId, item.approvalFlowId, item.approvalTaskId);
                }
              }
            "
          >
            <n-checkbox
              v-if="props.activeTaskType?.includes('pending') && approvalConfigDetail?.allowBatchProcess"
              :value="item.approvalTaskId"
              class="mt-[4px]"
              @click.stop
            />
            <div class="task-item-content">
              <div class="flex w-full items-center gap-[16px]">
                <div class="flex items-center gap-[8px]">
                  <CrmTag
                    v-if="props.activeTaskType?.includes('approved')"
                    :color="getApprovedTagColor(item.approvalOperation)"
                    bordered
                  >
                    {{ t(`taskDrawer.operation.${item.approvalOperation}`) }}
                  </CrmTag>
                  <CrmApprovalStatus :status="item.dataResult" isTag scene="approvalRecord" />
                </div>
                <CrmTableButton
                  v-if="!props.activeTaskType.includes('copied')"
                  type="primary"
                  text
                  size="small"
                  class="text-[14px]"
                  @click="emit('openDetail', item.resourceId, item.approvalFlowId, item.approvalTaskId)"
                >
                  {{ item.resourceName }}
                  <template #trigger> {{ item.resourceName }} </template>
                </CrmTableButton>
                <CrmTableButton
                  v-else-if="getResourcePermission(item)"
                  type="primary"
                  text
                  size="small"
                  class="text-[14px]"
                  @click="emit('openDetail', item.resourceId, item.approvalFlowId, item.approvalTaskId)"
                >
                  {{ item.resourceName }}
                  <template #trigger> {{ item.resourceName }} </template>
                </CrmTableButton>
                <n-tooltip v-else trigger="hover">
                  <template #trigger>
                    {{ item.resourceName }}
                  </template>
                  {{ item.resourceName }}
                </n-tooltip>
              </div>
              <div class="flex w-full items-center justify-between">
                <div class="flex gap-[24px]">
                  <div class="flex items-center gap-[8px]">
                    <div class="text-[var(--text-n2)]">{{ t('taskDrawer.applicant') }}</div>
                    <div>{{ item.applicant }}</div>
                  </div>
                  <div class="flex items-center gap-[8px]">
                    <div class="text-[var(--text-n2)]">{{ t('taskDrawer.applyTime') }}</div>
                    <div>{{ dayjs(item.submitTime).format('YYYY-MM-DD HH:mm:ss') }}</div>
                  </div>
                </div>
                <div v-if="props.activeTaskType?.includes('pending')" class="flex gap-[12px]">
                  <n-button type="error" ghost size="small" @click.stop="handleReject(item)">
                    {{ t('common.reject') }}
                  </n-button>
                  <n-button type="primary" size="small" @click.stop="handleApprove(item)">
                    {{ t('common.approve') }}
                  </n-button>
                </div>
              </div>
            </div>
          </div>
        </template>
      </CrmList>
      <div v-else-if="!loading && finished" class="w-full p-[16px] text-center text-[var(--text-n4)]">
        {{ props.emptyText || t('common.noData') }}
      </div>
    </n-checkbox-group>
  </n-spin>
  <approvalModal
    v-model:show="approvalVisible"
    :approval-type="approvalType"
    :approval-item="approvalItem"
    :approval-item-keys="selectedKeys"
    :approval-flow-id="approvalItem?.approvalFlowId || ''"
    module="WORKBENCH"
    @approval-cancel="handleApproveCancel"
    @approval-success="handleApproveSuccess"
  />
</template>

<script lang="ts" setup>
  import { NButton, NCheckbox, NCheckboxGroup, NSpin, NTooltip } from 'naive-ui';
  import dayjs from 'dayjs';

  import { ApprovalListTypeEnum, ApprovalOperationEnum, ApprovalResourceTypeEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ApprovalProcessDetail, ApprovalTodoItem } from '@lib/shared/models/system/process';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmApprovalStatus from '@/components/business/crm-approval/components/crm-approval-status.vue';
  import approvalModal from './approvalModal.vue';

  import {
    getCcApprovalList,
    getInitiatedApprovalList,
    getPendingApprovalList,
    getProcessedApprovalList,
  } from '@/api/modules/index';
  import { hasAnyPermission } from '@/utils/permission.js';

  const { t } = useI18n();

  const props = defineProps<{
    keyField: string;
    virtualScrollHeight: string;
    emptyText?: string;
    loadParams?: Record<string, any>;
    activeTaskType: string;
    approvalConfigDetail?: ApprovalProcessDetail;
  }>();

  const emit = defineEmits<{
    (e: 'openDetail', id: string, approvalFlowId: string, approvalTaskId: string): void;
    (e: 'listInit', total: number, keys: string[]): void;
    (e: 'approvalSuccess'): void;
  }>();

  const selectedKeys = defineModel<any[]>('selectedKeys', {
    required: false,
    default: () => [],
  });

  const list = ref<ApprovalTodoItem[]>([]);
  const loading = ref(false);

  const pageNation = ref({
    total: 0,
    pageSize: 10,
    current: 1,
  });

  const finished = ref(false);
  const lisApiMap = {
    [ApprovalListTypeEnum.PENDING]: getPendingApprovalList,
    [ApprovalListTypeEnum.APPROVAL]: getProcessedApprovalList,
    [ApprovalListTypeEnum.INITIATED]: getInitiatedApprovalList,
    [ApprovalListTypeEnum.COPIED]: getCcApprovalList,
  };
  async function loadTaskList(refresh = true, keyword?: string) {
    try {
      loading.value = true;
      if (refresh) {
        finished.value = false;
        pageNation.value.current = 1;
        list.value = [];
      }
      const [listType, resourceType] = props.activeTaskType.split('-');
      const res = await lisApiMap[listType as ApprovalListTypeEnum]({
        current: pageNation.value.current,
        pageSize: 20,
        resourceType: resourceType as ApprovalResourceTypeEnum,
        ...props.loadParams,
        keyword: keyword !== undefined ? keyword : props.loadParams?.keyword || '',
      });
      if (res) {
        list.value = list.value.concat(res.list);
        pageNation.value.total = res.total;
        emit(
          'listInit',
          res.total,
          list.value.map((e) => e.approvalTaskId)
        );
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
      finished.value = true;
    }
  }

  function handleReachBottom() {
    pageNation.value.current += 1;
    if (pageNation.value.current > Math.ceil(pageNation.value.total / pageNation.value.pageSize)) {
      return;
    }
    loadTaskList(false);
  }

  watch(
    () => props.activeTaskType,
    (val) => {
      if (val) {
        loadTaskList();
      }
    },
    {
      immediate: true,
    }
  );

  const approvalVisible = ref(false);
  const approvalType = ref<'approve' | 'reject'>('approve');
  const approvalItem = ref<ApprovalTodoItem>();

  function handleReject(item: ApprovalTodoItem) {
    approvalItem.value = item;
    approvalType.value = 'reject';
    approvalVisible.value = true;
  }

  function handleApprove(item: ApprovalTodoItem) {
    approvalItem.value = item;
    approvalType.value = 'approve';
    approvalVisible.value = true;
  }

  function handleApproveSuccess() {
    loadTaskList(true);
    selectedKeys.value = [];
    emit('approvalSuccess');
  }

  function handleApproveCancel() {
    approvalVisible.value = false;
    approvalItem.value = undefined;
    selectedKeys.value = [];
  }

  function getApprovedTagColor(result: ApprovalOperationEnum) {
    switch (result) {
      case ApprovalOperationEnum.APPROVE:
        return {
          color: 'transparent',
          textColor: 'var(--success-green)',
          borderColor: 'var(--success-green)',
        };
      case ApprovalOperationEnum.REJECT:
        return {
          color: 'transparent',
          textColor: 'var(--error-red)',
          borderColor: 'var(--error-red)',
        };
      case ApprovalOperationEnum.SIGN:
        return {
          color: 'transparent',
          textColor: 'var(--info-blue)',
          borderColor: 'var(--info-blue)',
        };
      case ApprovalOperationEnum.BACK:
      default:
        return {
          color: 'transparent',
          textColor: 'var(--text-n1)',
          borderColor: 'var(--text-n7)',
        };
    }
  }

  function getResourcePermission(item: ApprovalTodoItem) {
    switch (item.resourceType) {
      case ApprovalResourceTypeEnum.CONTRACT:
        return hasAnyPermission(['CONTRACT:READ']);
      case ApprovalResourceTypeEnum.INVOICE:
        return hasAnyPermission(['CONTRACT_INVOICE:READ']);
      case ApprovalResourceTypeEnum.ORDER:
        return hasAnyPermission(['ORDER:READ']);
      case ApprovalResourceTypeEnum.QUOTATION:
        return hasAnyPermission(['OPPORTUNITY_QUOTATION:READ']);
      default:
        return false;
    }
  }

  defineExpose({
    loadTaskList,
  });
</script>

<style lang="less" scoped>
  .task-item {
    @apply flex cursor-pointer justify-between;

    margin-bottom: 16px;
    padding: 16px;
    border: 1px solid var(--text-n8);
    border-radius: var(--border-radius-small);
    gap: 16px;
    &:hover {
      background-color: var(--text-n9);
    }
    .task-item-content {
      @apply flex flex-1 flex-wrap items-center;

      gap: 8px;
    }
  }
  :deep(.crm-list-item) {
    &:hover {
      background: var(--text-n9);
    }
  }
</style>
