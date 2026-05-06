<template>
  <n-spin :show="loading" class="min-h-[300px]">
    <n-checkbox-group v-model:value="selectedKeys">
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
          <div class="task-item" :class="selectedKeys.includes(item.id) ? '!border-[var(--primary-8)]' : ''">
            <n-checkbox v-if="props.activeTaskType?.includes('pending')" :value="item.id" class="mt-[4px]" />
            <div class="task-item-content">
              <div class="flex w-full items-center justify-between">
                <div class="flex items-center gap-[8px]">
                  <CrmTag
                    v-if="props.activeTaskType?.includes('approved')"
                    :color="getApprovedTagColor(item.result)"
                    bordered
                  >
                    {{ t(`taskDrawer.result.${item.result}`) }}
                  </CrmTag>
                  <CrmApprovalStatus :status="item.status" isTag />
                </div>
                <CrmTableButton
                  type="primary"
                  text
                  size="small"
                  class="text-[14px]"
                  @click="emit('openDetail', item.id)"
                >
                  {{ item.name }}
                  <template #trigger> {{ item.name }} </template>
                </CrmTableButton>
              </div>
              <div class="flex w-full items-center justify-between">
                <div class="flex gap-[24px]">
                  <div class="flex items-center gap-[8px]">
                    <div class="text-[var(--text-n2)]">{{ t('taskDrawer.applicant') }}</div>
                    <div>{{ item.createUserName }}</div>
                  </div>
                  <div class="flex items-center gap-[8px]">
                    <div class="text-[var(--text-n2)]">{{ t('taskDrawer.applyTime') }}</div>
                    <div>{{ dayjs(item.applyTime).format('YYYY-MM-DD HH:mm:ss') }}</div>
                  </div>
                </div>
                <div v-if="props.activeTaskType?.includes('pending')" class="flex gap-[12px]">
                  <n-button type="error" ghost size="small" @click="handleReject(item)">
                    {{ t('common.reject') }}
                  </n-button>
                  <n-button type="primary" size="small" @click="handleApprove(item)">
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
    @approval-cancel="handleApproveCancel"
  />
</template>

<script lang="ts" setup>
  import { NButton, NCheckbox, NCheckboxGroup, NSpin } from 'naive-ui';
  import dayjs from 'dayjs';

  import { ProcessResultEnum, ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import crmApprovalDetail from '@/components/business/crm-approval/components/crm-approval-detail.vue';
  import CrmApprovalStatus from '@/components/business/crm-approval-status/index.vue';
  import approvalModal from './approvalModal.vue';

  const { t } = useI18n();

  const props = defineProps<{
    keyField: string;
    virtualScrollHeight: string;
    emptyText?: string;
    loadParams?: Record<string, any>;
    activeTaskType?: string;
  }>();

  const emit = defineEmits<{
    (e: 'openDetail', id: number): void;
  }>();

  const selectedKeys = defineModel<any[]>('selectedKeys', {
    required: false,
    default: () => [],
  });

  const list = ref<any[]>([
    {
      id: 1928391823791,
      name: 'xxxxxx',
      createUserName: 'adshasda',
      applyTime: 1729382938293,
      status: ProcessStatusEnum.APPROVING,
      result: ProcessResultEnum.AGREE,
    },
    {
      id: 1928391823791,
      name: 'xxxxxx',
      createUserName: 'adshasda',
      applyTime: 1729382938293,
      status: ProcessStatusEnum.APPROVING,
      result: ProcessResultEnum.REJECT,
    },
    {
      id: 1928391823791,
      name: 'xxxxxx',
      createUserName: 'adshasda',
      applyTime: 1729382938293,
      status: ProcessStatusEnum.APPROVING,
      result: ProcessResultEnum.ADD_SIGN,
    },
    {
      id: 1928391823791,
      name: 'xxxxxx',
      createUserName: 'adshasda',
      applyTime: 1729382938293,
      status: ProcessStatusEnum.APPROVING,
      result: ProcessResultEnum.FALLBACK,
    },
  ]);
  const loading = ref(false);

  const pageNation = ref({
    total: 0,
    pageSize: 10,
    current: 1,
  });

  const finished = ref(false);
  async function loadTaskList(refresh = true) {
    try {
      if (!props.loadParams) return;
      loading.value = true;

      if (refresh) {
        finished.value = false;
        pageNation.value.current = 1;
        list.value = [];
      }
      const res = [] as any; // TODO:
      if (res) {
        list.value = list.value.concat(res.list);
        pageNation.value.total = res.total;
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

  const approvalVisible = ref(false);
  const approvalType = ref<'approve' | 'reject'>('approve');
  const approvalItem = ref<any>({});

  function handleReject(item: any) {
    approvalItem.value = item;
    approvalType.value = 'reject';
    approvalVisible.value = true;
  }

  function handleApprove(item: any) {
    approvalItem.value = item;
    approvalType.value = 'approve';
    approvalVisible.value = true;
  }

  function handleApproveCancel() {
    approvalVisible.value = false;
    approvalItem.value = undefined;
  }

  function getApprovedTagColor(result: ProcessResultEnum) {
    switch (result) {
      case ProcessResultEnum.AGREE:
        return {
          color: 'transparent',
          textColor: 'var(--success-green)',
          borderColor: 'var(--success-green)',
        };
      case ProcessResultEnum.REJECT:
        return {
          color: 'transparent',
          textColor: 'var(--error-red)',
          borderColor: 'var(--error-red)',
        };
      case ProcessResultEnum.ADD_SIGN:
        return {
          color: 'transparent',
          textColor: 'var(--info-blue)',
          borderColor: 'var(--info-blue)',
        };
      case ProcessResultEnum.FALLBACK:
      default:
        return {
          color: 'transparent',
          textColor: 'var(--text-n1)',
          borderColor: 'var(--text-n7)',
        };
    }
  }

  defineExpose({
    loadTaskList,
  });
</script>

<style lang="less" scoped>
  .task-item {
    @apply flex justify-between;

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
