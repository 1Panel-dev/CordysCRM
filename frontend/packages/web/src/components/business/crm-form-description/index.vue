<template>
  <CrmDescription :descriptions="descriptions"> </CrmDescription>
</template>

<script setup lang="ts">
  import CrmDescription, { Description } from '@/components/pure/crm-description/index.vue';

  const descriptions = ref<Description[]>([]);
</script>

<style lang="less" scoped></style>
