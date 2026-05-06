<template>
  <CrmTag
    v-if="props.isTag && currentStatus?.label"
    :color="{
      textColor: currentStatus.tagColor,
      color: currentStatus.tagBgColor,
    }"
  >
    {{ currentStatus.label }}
  </CrmTag>
  <div v-else-if="currentStatus?.label" class="flex items-center gap-[8px]">
    <CrmIcon :type="currentStatus.icon" :size="16" :class="`text-[${currentStatus.color}]`" />
    {{ currentStatus.label }}
  </div>
  <div v-else>-</div>
</template>

<script setup lang="ts">
  import { ProcessStatusType } from '@lib/shared/models/system/process';

  import CrmTag from '@/components/pure/crm-tag/index.vue';

  import { processStatusMap } from '@/config/process';

  const props = defineProps<{
    status: ProcessStatusType;
    isTag?: boolean;
  }>();

  export type StatusInfo = { label: string; icon: string; color: string; tagBgColor: string; tagColor: string };

  const currentStatus = computed<StatusInfo | undefined>(() => {
    return processStatusMap[props.status as keyof typeof processStatusMap];
  });
</script>

<style scoped></style>
