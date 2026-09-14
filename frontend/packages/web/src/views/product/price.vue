<template>
  <CrmCard hide-footer no-content-padding class="h-full">
    <PriceTable />
  </CrmCard>
</template>

<script setup lang="ts">
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import PriceTable from './components/priceTable.vue';
</script>
