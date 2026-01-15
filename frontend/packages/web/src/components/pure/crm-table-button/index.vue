<template>
  <n-tooltip trigger="hover" :delay="300">
    <template #trigger>
      <n-button type="primary" text class="max-w-full" :size="props.size" :class="props.class" @click="emit('click')">
        <div class="one-line-text flex w-[fit-content] items-center">
          <slot name="trigger"></slot>
        </div>
      </n-button>
    </template>
    <slot></slot>
  </n-tooltip>
</template>

<script setup lang="ts">
  import { NButton, NTooltip } from 'naive-ui';

  const props = defineProps<{
    class?: string;
    size?: 'tiny' | 'small' | 'medium' | 'large';
  }>();
  const emit = defineEmits<{
    (e: 'click'): void;
  }>();
</script>

<style lang="less" scoped></style>
