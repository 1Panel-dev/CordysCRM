<template>
  <CrmDrawer v-model:show="visible" width="100%" :footer="false"> </CrmDrawer>
</template>

<script setup lang="ts">
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';

  const visible = defineModel<boolean>('visible', {
    required: true,
  });
</script>

<style lang="less" scoped></style>
