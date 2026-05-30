<template>
  <div class="crm-approval-approver-content">
    <div v-if="approvers.length" class="crm-approver-avatar-list">
      <div
        v-for="approver in approvers"
        :key="approver.id"
        class="crm-approver-avatar-list__item"
        :class="{ 'crm-approver-avatar-list__item--active': approver.id === activeApproverId }"
      >
        <CrmApprovalAvatar
          :approver="approver"
          :size="props.size"
          :activeApproverId="activeApproverId"
          @toggleActive="toggleActive(approver)"
        />
        <n-tooltip :delay="300" trigger="hover">
          <template #trigger>
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
          </template>
          {{ approver.name }}
        </n-tooltip>
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
  import { NTooltip } from 'naive-ui';

  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { ApproverItem } from '@lib/shared/models/system/process';

  import CrmApprovalAvatar from './crm-approval-avatar.vue';

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

  const { t } = useI18n();

  const approvers = computed(() => props.approvers || []);

  const activeApproverId = defineModel<string | number>('activeId', {
    default: '',
  });

  const currentApproverMap = computed(() => new Map(approvers.value.map((item) => [String(item.id), item])));
  const currentApproverReason = computed(() => {
    const currentApprover = currentApproverMap.value.get(String(activeApproverId.value));

    if (!currentApprover) {
      return '';
    }

    if (currentApprover.approveResult === ProcessStatusEnum.AUTO_UNAPPROVED) {
      return t('crm.approval.autoReject');
    }

    return currentApprover.approveReason ?? '';
  });

  function isActiveApproval(approver: ApproverItem) {
    return approver.approveResult === ProcessStatusEnum.APPROVED && approver.id === activeApproverId.value;
  }

  function isActiveRejected(approver: ApproverItem) {
    return approver.approveResult === ProcessStatusEnum.UNAPPROVED && approver.id === activeApproverId.value;
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
