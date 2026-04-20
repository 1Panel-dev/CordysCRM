<template>
  <div v-if="currentStatus?.label" class="flex items-center gap-[8px]">
    <CrmIcon :type="currentStatus.icon" :size="16" :class="`text-[${currentStatus.color}]`" />
    {{ currentStatus.label }}
  </div>
  <div v-else>-</div>
</template>

<script setup lang="ts">
  import { ProcessStatusType } from '@lib/shared/models/system/process';

  import { processStatusMap } from '@/config/process';

  const props = defineProps<{
    status: ProcessStatusType;
  }>();

  export type StatusInfo = { label: string; icon: string; color: string };

  const currentStatus = computed<StatusInfo | undefined>(() => {
    return processStatusMap[props.status as keyof typeof processStatusMap];
  });
</script>

<style scoped></style>
