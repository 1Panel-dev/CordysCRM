<template>
  <NTooltip v-if="text" :delay="300" flip :disabled="!text">
    <template #trigger>
      <div class="overflow-hidden">
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
