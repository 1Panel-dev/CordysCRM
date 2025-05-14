<template>
  <CrmContactList :customer-id="props.customerId" />
</template>

<script setup lang="ts">
  import CrmContactList from '@/components/business/crm-contact-list/index.vue';

  const props = defineProps<{
    customerId?: string;
  }>();
</script>

<style lang="less" scoped></style>
