<template>
  <n-avatar
    size="small"
    round
    class="mr-[4px] box-content h-[16px] w-[16px] border border-solid border-[var(--text-n8)] bg-[var(--text-n9)] text-[12px] leading-[20px] text-[var(--text-n4)]"
  >
    <n-tooltip trigger="hover" :delay="300" :disabled="!props.tooltip">
      <template #trigger>
        {{ props.text }}
      </template>
      {{ props.tooltip }}
    </n-tooltip>
  </n-avatar>
</template>

<script setup lang="ts">
  import { NAvatar, NTooltip } from 'naive-ui';

  const props = defineProps<{
    text: string;
    tooltip?: string;
  }>();
</script>

<style lang="less" scoped></style>
