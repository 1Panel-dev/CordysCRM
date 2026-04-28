<template>
  <div class="crm-approval-approver-content">
    <CrmApprovalApproverList v-model:active-id="activeApproverId" :approvers="approvers" />
    <div v-if="currentApproverReason" class="crm-approval-approver-content__reasons">
      <div class="crm-approval-approver-content__reason">
        {{ currentApproverReason }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import CrmApprovalApproverList, { type ApproverItem } from '@/components/business/crm-approver-avatar-list/index.vue';

  const props = withDefaults(
    defineProps<{
      approvers?: ApproverItem[];
    }>(),
    {
      approvers: () => [],
    }
  );

  const approvers = computed(() => props.approvers || []);

  const activeApproverId = defineModel<string | number>('activeId', {
    default: '',
  });

  const currentApproverMap = computed(() => new Map(approvers.value.map((item) => [String(item.id), item])));
  const currentApproverReason = computed(
    () => currentApproverMap.value.get(String(activeApproverId.value))?.approveReason ?? ''
  );

  watch(
    approvers,
    (list) => {
      if (!list.length) {
        activeApproverId.value = '';
        return;
      }

      const hasCurrentActive = list.some((item) => item.id === activeApproverId.value);
      if (!hasCurrentActive) {
        activeApproverId.value = list[0]?.id ?? '';
      }
    },
    { immediate: true }
  );
</script>

<style scoped lang="less">
  .crm-approval-approver-content {
    .crm-approval-approver-content__reasons {
      display: flex;
      border-radius: 4px;
      background: var(--text-n9);
      flex-direction: column;
      gap: 8px;
      .crm-approval-approver-content__reason {
        display: box;
        overflow: hidden;
        padding: 8px;
        font-size: 14px;
        border-radius: 4px;
        text-overflow: ellipsis;
        color: var(--text-n2);
        line-height: 22px;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
      }
    }
  }
</style>
