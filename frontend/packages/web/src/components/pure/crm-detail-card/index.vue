<template>
  <div class="crm-detail-card">
    <slot name="prefix"></slot>
    <div v-for="item of props.description" :key="item.key" class="crm-detail-desc-item" :style="{ width: item.width }">
      <div class="whitespace-nowrap text-[var(--text-n4)]">
        {{ item.label }}
      </div>
      <slot :name="item.key" :value="item.value">
        <n-tooltip trigger="hover" :delay="300" :disabled="isEmpty(item.value)" :placement="item.placement">
          <template #trigger>
            <div class="text-ov max-w-[300px] overflow-hidden overflow-ellipsis whitespace-nowrap">
              {{ item.value || '-' }}
            </div>
          </template>
          {{ item.value }}
        </n-tooltip>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NTooltip } from 'naive-ui';
  import { isEmpty } from 'lodash-es';

  export interface Description {
    key: string;
    label: string;
    value: string | string[];
    width?: string;
    placement?:
      | 'top-start'
      | 'top'
      | 'top-end'
      | 'right-start'
      | 'right'
      | 'right-end'
      | 'bottom-start'
      | 'bottom'
      | 'bottom-end'
      | 'left-start'
      | 'left'
      | 'left-end';
  }

  const props = defineProps<{
    description: Description[];
  }>();
</script>

<style lang="less" scoped>
  .crm-detail-card {
    gap: 24px;
    row-gap: 16px;
    @apply flex  flex-wrap items-center;
    .crm-detail-desc-item {
      gap: 8px;
      @apply flex flex-nowrap items-center;
    }
  }
</style>
