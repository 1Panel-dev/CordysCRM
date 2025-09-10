<template>
  <n-popover
    :show-arrow="false"
    trigger="hover"
    placement="bottom-start"
    :disabled="typeof props.value !== 'number' || props.value < 1"
  >
    <template #trigger>
      <slot></slot>
    </template>
    <div class="text-[16px] font-medium tracking-wide text-[var(--text-n2)]">
      {{ abbreviateNumber(props.value, props.unit).full }}
    </div>
  </n-popover>
</template>

<script setup lang="ts">
  import { NPopover } from 'naive-ui';

  import { abbreviateNumber } from '@lib/shared/method';

  const props = defineProps<{
    value: number;
    unit: string;
  }>();
</script>

<style scoped></style>
