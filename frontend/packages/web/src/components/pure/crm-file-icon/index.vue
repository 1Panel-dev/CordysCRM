<template>
  <CrmIon :type="iconType" :size="props.size || 32" />
</template>

<script setup lang="ts">
  import { UploadStatus } from '@lib/shared/enums/uploadEnum';
  import { getFileIcon } from '@lib/shared/method';

  import CrmIon from '@/components/pure/crm-icon-font/index.vue';

  const props = defineProps<{
    name: string;
    size: number;
  }>();

  const iconType = computed(() => {
    return getFileIcon(props.name, UploadStatus.done);
  });
</script>

<style lang="less" scoped></style>
