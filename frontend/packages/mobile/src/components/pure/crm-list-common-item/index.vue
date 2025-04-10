<template>
  <div class="crm-list-common-item" @click="emit('click', item)">
    <div class="crm-list-common-item-header">
      <div class="text-[14px] font-semibold text-[var(--text-n1)]">{{ props.item.name }}</div>
      <van-tag
        v-if="item.tag"
        :color="item.tagBgColor"
        :text-color="item.tagColor"
        class="rounded-[var(--border-radius-small)] !p-[2px_6px]"
      >
        {{ item.tag }}
      </van-tag>
    </div>
    <div class="crm-list-common-item-content">
      <div
        v-for="desc of item.description"
        :key="desc.label"
        class="flex items-start gap-[8px]"
        :class="desc.fullLine ? 'basis-full' : ''"
      >
        <div class="whitespace-nowrap text-[12px] text-[var(--text-n4)]">{{ desc.label }}</div>
        <div class="text-[12px] text-[var(--text-n1)]">{{ desc.value }}</div>
      </div>
    </div>
    <van-divider v-if="props.actions?.length" class="!m-0" />
    <div v-if="props.actions?.length" class="crm-list-common-item-actions">
      <CrmTextButton
        v-for="btn of props.actions"
        :key="btn.label"
        :text="btn.label"
        :icon="btn.icon"
        @click="btn.action(item)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';

  export interface CrmListCommonItemActionsItem {
    label: string;
    icon: string;
    permission: string[];
    action: (data: any) => void;
  }
  const props = defineProps<{
    item: Record<string, any>;
    actions?: CrmListCommonItemActionsItem[];
  }>();
  const emit = defineEmits<{
    (e: 'click', item: Record<string, any>): void;
  }>();
</script>

<style lang="less" scoped>
  .crm-list-common-item {
    @apply flex flex-col;

    padding: 16px 20px;
    border-radius: var(--border-radius-small);
    background-color: var(--text-n10);
    gap: 8px;
    .crm-list-common-item-header,
    .crm-list-common-item-content {
      @apply flex items-center justify-between;
    }
    .crm-list-common-item-content {
      @apply flex-wrap;

      gap: 8px;
    }
    .crm-list-common-item-actions {
      @apply flex w-full items-center justify-between;

      gap: 8px;
    }
  }
</style>
