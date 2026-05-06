<template>
  <div class="crm-approval-approver-content">
    <div v-if="approvers.length" class="crm-approver-avatar-list">
      <div
        v-for="approver in approvers"
        :key="approver.id"
        class="crm-approver-avatar-list__item"
        :class="{ 'crm-approver-avatar-list__item--active': approver.id === activeApproverId }"
      >
        <div
          class="crm-approver-avatar-list__avatar-wrap"
          :class="{ 'crm-approver-avatar-list__avatar-wrap--active': approver.id === activeApproverId }"
        >
          <CrmAvatar
            :avatar="approver.avatar"
            :word="approver.name"
            :is-user="false"
            :size="props.size"
            class="cursor-pointer"
            @click="toggleActive(approver)"
          />
          <div
            v-if="approver.approveResult"
            :class="getStatusClass(approver.approveResult)"
            class="crm-approver-avatar-list__status"
          >
            <CrmIcon :type="getStatusIcon(approver.approveResult)" :size="14" />
          </div>
        </div>
        <div
          class="one-line-text crm-approver-avatar-list__name cursor-pointer"
          :class="{
            'crm-approver-avatar-list__name--active-approval': isActiveApproval(approver),
            'crm-approver-avatar-list__name--active-rejected': isActiveRejected(approver),
            'crm-approver-avatar-list__name--active': approver.id === activeApproverId,
          }"
          @click="toggleActive(approver)"
        >
          {{ approver.name }}
        </div>
      </div>
    </div>
    <div v-if="currentApproverReason" class="crm-approval-approver-content__reasons">
      <div class="crm-approval-approver-content__reason">
        {{ currentApproverReason }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ApprovalRecordStatusEnum } from '@lib/shared/enums/process';
  import { ApproverItem } from '@lib/shared/models/system/process';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';

  const props = withDefaults(
    defineProps<{
      approvers?: ApproverItem[];
      size?: number;
    }>(),
    {
      approvers: () => [],
      size: 24,
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

  function getStatusIcon(status: ApprovalRecordStatusEnum) {
    return status === ApprovalRecordStatusEnum.REJECT ? 'iconicon_close_circle_filled' : 'iconicon_succeed_filled';
  }

  function getStatusClass(status: ApprovalRecordStatusEnum) {
    switch (status) {
      case ApprovalRecordStatusEnum.REJECT:
        return 'text-[var(--error-red)]';
      case ApprovalRecordStatusEnum.PASS:
        return 'text-[var(--success-green)]';
      default:
        return 'text-[var(--text-n4)]';
    }
  }

  function isActiveApproval(approver: ApproverItem) {
    return approver.approveResult === ApprovalRecordStatusEnum.PASS && approver.id === activeApproverId.value;
  }

  function isActiveRejected(approver: ApproverItem) {
    return approver.approveResult === ApprovalRecordStatusEnum.REJECT && approver.id === activeApproverId.value;
  }

  function toggleActive(approver: ApproverItem) {
    activeApproverId.value = approver.id;
  }

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
    .crm-approver-avatar-list {
      display: flex;
      padding: 8px;
      flex-wrap: wrap;
      gap: 4px;
    }
    .crm-approver-avatar-list__item {
      display: flex;
      align-items: center;
      min-width: 0;
      max-width: 100%;
      column-gap: 8px;
      row-gap: 4px;
      cursor: pointer;
    }
    .crm-approver-avatar-list__avatar-wrap {
      position: relative;
      display: flex;
      justify-content: center;
      align-items: center;
      width: v-bind('`${props.size}px`');
      height: v-bind('`${props.size}px`');
      border-radius: 50%;
      transition: box-shadow 0.18s ease;
      @apply flex flex-shrink-0 items-center justify-between;
      &--active {
        box-shadow: 0 0 0 1px var(--primary-8);
      }
    }
    .crm-approver-avatar-list__item:hover {
      .crm-approver-avatar-list__name {
        color: var(--primary-8);
      }
    }
    .crm-approver-avatar-list__status {
      position: absolute;
      top: -3px;
      right: -3px;
      display: flex;
      justify-content: center;
      align-items: center;
      width: 12px;
      height: 12px;
      border-radius: 50%;
      background-color: var(--text-n10);
    }
    .crm-approver-avatar-list__name {
      color: var(--text-n1);
      &--active-approval {
        color: var(--primary-0);
      }
      &--active-rejected {
        color: var(--primary-1);
      }
      &--active {
        color: var(--primary-8);
      }
    }
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
