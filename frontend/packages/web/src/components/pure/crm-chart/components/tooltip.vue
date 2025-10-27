<template>
  <div class="w-fit min-w-[120px]">
    <div class="text-[14px] font-medium text-[var(--text-n1)]">{{ props.seriesName }}</div>
    <div class="mt-[8px] flex items-center justify-between gap-[12px]">
      <div class="flex items-center gap-[8px] text-[12px] text-[var(--text-n2)]">
        <div class="h-[8px] w-[8px]" :style="{ backgroundColor: props.color }"></div>
        {{ props.name }}
      </div>
      <div class="text-[12px] font-semibold text-[var(--text-n1)]">{{ props.value }}</div>
      <div v-if="props.percent !== undefined" class="text-[12px] font-semibold text-[var(--text-n1)]">
        {{ props.percent }}%
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  const props = defineProps<{
    name: string;
    value: number;
    seriesName: string;
    percent?: number;
    color?: string;
  }>();
</script>

<style lang="less" scoped></style>
