<template>
  <CrmIcon :name="iconType" :width="props.width" :height="props.height" />
</template>

<script setup lang="ts">
  import { getFileIcon } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import { UploadStatus } from '@lib/shared/enums/uploadEnum';

  const props = defineProps<{
    name: string;
    width: string;
    height: string;
  }>();

  const iconType = computed(() => {
    return getFileIcon(props.name, UploadStatus.done);
  });
</script>

<style lang="less" scoped></style>
