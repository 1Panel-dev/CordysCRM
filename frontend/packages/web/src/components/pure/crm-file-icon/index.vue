<template>
  <CrmIon :type="iconType" :size="props.size || 32" />
</template>

<script setup lang="ts">
  import { getFileIconType } from '@lib/shared/method';

  import CrmIon from '@/components/pure/crm-icon-font/index.vue';

  const props = defineProps<{
    type: string;
    size: number;
  }>();

  const iconType = computed(() => {
    return getFileIconType(props.type);
  });
</script>

<style lang="less" scoped></style>
