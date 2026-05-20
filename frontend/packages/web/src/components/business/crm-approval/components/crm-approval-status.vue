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
  import { ProcessStatusEnum } from '@lib/shared/enums/process';

  import CrmTag from '@/components/pure/crm-tag/index.vue';

  import { approvalRecordStatusMap, processStatusMap } from '@/config/process';

  const props = defineProps<{
    status: ProcessStatusEnum;
    isTag?: boolean;
    scene?: 'process' | 'approvalRecord';
  }>();

  export type StatusInfo = { label: string; icon: string; color: string; tagBgColor: string; tagColor: string };

  const currentStatus = computed<StatusInfo | undefined>(() => {
    const currentMap = props.scene === 'approvalRecord' ? approvalRecordStatusMap : processStatusMap;
    return currentMap[props.status as keyof typeof currentMap];
  });
</script>

<style scoped></style>
