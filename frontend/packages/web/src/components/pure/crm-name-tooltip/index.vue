<template>
  <NTooltip v-if="text" :delay="300" flip :disabled="!text">
    <template #trigger>
      <div class="flex items-center gap-[8px] overflow-hidden">
        <slot name="prefix"></slot>
        <div class="one-line-text h-full min-w-0 max-w-full items-center">
          {{ text }}
        </div>
      </div>
    </template>
    {{ text }}
  </NTooltip>
  <span v-else>-</span>
</template>

<script setup lang="ts">
  import { NTooltip } from 'naive-ui';

  defineProps<{ text?: string }>();
</script>
