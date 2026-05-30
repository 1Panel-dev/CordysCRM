<template>
  <CrmIcon
    :type="isFavorite ? 'iconicon_star_filled' : 'iconicon_star'"
    :size="16"
    :class="isFavorite ? 'text-[var(--warning-yellow)]' : ''"
  />
</template>

<script setup lang="ts">
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  const isFavorite = defineModel<boolean>('value', {
    type: Boolean,
    default: false,
  });
</script>

<style lang="less" scoped></style>
