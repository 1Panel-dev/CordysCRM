<template>
  <div ref="businessCardRef" class="h-full">
    <CrmCard no-content-padding hide-footer>
      <div class="h-full px-[16px] pt-[16px]">
        <businessTitleTable />
      </div>
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import businessTitleTable from './components/businessTitleTable.vue';
</script>

<style scoped></style>
