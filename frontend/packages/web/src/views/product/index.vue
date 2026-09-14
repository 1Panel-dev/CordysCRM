<template>
  <CrmCard hide-footer no-content-bottom-padding>
    <ProductTable />
  </CrmCard>
</template>

<script lang="ts" setup>
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import ProductTable from './components/productTable.vue';
</script>
