<template>
  <CrmIon :name="iconType" :width="props.width" :height="props.height" />
</template>

<script setup lang="ts">
  import { getFileIconType } from '@lib/shared/method';

  import CrmIon from '@/components/pure/crm-icon-font/index.vue';

  const props = defineProps<{
    type: string;
    width: string;
    height: string;
  }>();

  const iconType = computed(() => {
    return getFileIconType(props.type);
  });
</script>

<style lang="less" scoped></style>
