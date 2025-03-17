<template>
  <n-tooltip trigger="hover" :delay="300">
    <template #trigger>
      <n-button type="primary" text class="w-full" @click="emit('click')">
        <div class="one-line-text">
          <slot name="trigger"></slot>
        </div>
      </n-button>
    </template>
    <slot></slot>
  </n-tooltip>
</template>

<script setup lang="ts">
  import { NButton, NTooltip } from 'naive-ui';

  const emit = defineEmits<{
    (e: 'click'): void;
  }>();
</script>

<style lang="less" scoped></style>
