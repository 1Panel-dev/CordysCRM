<template>
  <div
    class="crm-approver-avatar-list__avatar-wrap"
    :class="{ 'crm-approver-avatar-list__avatar-wrap--active': props.approver?.id === activeApproverId }"
  >
    <n-avatar v-if="isShowAutoBot" round :size="props.size" class="cursor-pointer" @click="emit('toggleActive')">
      <CrmIcon type="iconicon_bot" :size="18" class="text-[var(--text-n10)]" />
    </n-avatar>
    <CrmAvatar
      v-else
      :avatar="props.approver?.avatar"
      :word="props.approver?.name"
      :is-user="false"
      :size="props.size"
      class="cursor-pointer"
      @click="emit('toggleActive')"
    />
    <div
      v-if="props.approver?.approveResult"
      :class="getStatusClass(props.approver.approveResult)"
      class="crm-approver-avatar-list__status"
    >
      <CrmIcon :type="getStatusIcon(props.approver.approveResult)" :size="14" />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NAvatar } from 'naive-ui';

  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import type { ApproverItem } from '@lib/shared/models/system/process';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';

  const props = withDefaults(
    defineProps<{
      approver?: ApproverItem;
      size?: number;
      activeApproverId?: string | number;
      signNode?: boolean;
    }>(),
    {
      approvers: () => [],
      size: 24,
    }
  );
  const emit = defineEmits<{
    (e: 'toggleActive'): void;
  }>();

  const isShowAutoBot = computed(() => props.approver?.id === 'Cbot');

  function getStatusIcon(status: ProcessStatusEnum) {
    if (props.signNode) {
      return 'iconicon_add_one';
    }
    if ([ProcessStatusEnum.UNAPPROVED, ProcessStatusEnum.AUTO_UNAPPROVED].includes(status)) {
      return 'iconicon_close_circle_filled';
    }
    return 'iconicon_succeed_filled';
  }

  function getStatusClass(status: ProcessStatusEnum) {
    if (props.signNode) {
      return 'text-[var(--info-blue)]';
    }
    switch (status) {
      case ProcessStatusEnum.UNAPPROVED:
      case ProcessStatusEnum.AUTO_UNAPPROVED:
        return 'text-[var(--error-red)]';
      case ProcessStatusEnum.APPROVED:
      case ProcessStatusEnum.AUTO_APPROVED:
        return 'text-[var(--success-green)]';
      default:
        return 'text-[var(--text-n4)]';
    }
  }
</script>

<style lang="less" scoped>
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
</style>
