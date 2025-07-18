<template>
  <NTooltip v-if="text" :delay="300" flip :disabled="!text">
    <template #trigger>
      <div class="h-full overflow-hidden overflow-ellipsis whitespace-nowrap">{{ text }}</div>
    </template>
    {{ text }}
  </NTooltip>
  <span v-else>-</span>
</template>

<script setup lang="ts">
  import { NTooltip } from 'naive-ui';

  defineProps<{ text?: string }>();
</script>
