<template>
  <div class="crm-workflow-card">
    <div
      v-for="(item, index) of workflowData"
      :key="item.value"
      :class="`crm-workflow-item ${index === workflowData.length - 1 ? '' : 'flex-1'}`"
    >
      <div class="crm-workflow-item-status" :class="statusClass(index, item)">
        <CrmIcon
          v-if="index < currentStatusIndex || item.isError"
          :type="item.isError ? 'iconicon_close' : 'iconicon_check'"
          :size="16"
        />
        <div v-else class="flex items-center justify-center">{{ index + 1 }} </div>
      </div>
      <div class="crm-workflow-item-name" :class="statusClass(index, item)">
        {{ item.label }}
      </div>
      <div
        v-if="index !== workflowData.length - 1"
        class="crm-workflow-item-line"
        :class="{
          'in-progress': index < currentStatusIndex,
        }"
      >
      </div>
    </div>
    <slot v-if="currentStatusIndex !== workflowData.length - 1" name="action"> </slot>
  </div>
</template>

<script setup lang="ts">
  export interface workflowItem {
    value: string;
    label: string;
    isError: boolean;
  }
  const props = defineProps<{
    workflowList: workflowItem[];
  }>();

  const currentStatus = defineModel<string>('status', {
    required: true,
  });

  const workflowData = computed(() => props.workflowList || []);

  const currentStatusIndex = computed(() => workflowData.value.findIndex((e) => e.value === currentStatus.value));

  function statusClass(index: number, item: workflowItem) {
    return {
      done: index < currentStatusIndex.value && !item.isError,
      current: index === currentStatusIndex.value && !item.isError,
      error: item.isError,
    };
  }
</script>

<style scoped lang="less">
  .crm-workflow-card {
    padding: 24px;
    border-radius: var(--border-radius-medium);
    background: var(--text-n9);
    gap: 16px;
    @apply flex;
    .crm-workflow-item {
      gap: 16px;
      @apply flex flex-nowrap items-center;
      .crm-workflow-item-status {
        width: 24px;
        height: 24px;
        line-height: 24px;
        font-size: 16px;
        border: 1px solid var(--text-n4);
        border-radius: 50%;
        color: var(--text-n4);
        @apply flex flex-shrink-0 items-center justify-center font-semibold;
        &.current {
          border-color: var(--primary-8);
          color: var(--text-n10);
          background: var(--primary-8);
        }
        &.done {
          border-color: var(--primary-8);
          color: var(--primary-8);
        }
        &.error {
          border-color: var(--error-red);
          color: var(--error-red);
        }
      }
      .crm-workflow-item-name {
        font-size: 16px;
        color: var(--text-n4);
        @apply font-medium;
        &.current {
          color: var(--primary-8);
          @apply font-medium;
        }
        &.done {
          border-color: var(--primary-8);
          color: var(--text-n1);
          @apply font-normal;
        }
        &.error {
          color: var(--error-red);
          @apply font-medium;
        }
      }
      .crm-workflow-item-line {
        width: auto;
        height: 2px;
        background: var(--text-n7);

        @apply flex-1;
        &.in-progress {
          background: var(--primary-8);
        }
      }
    }
  }
</style>
