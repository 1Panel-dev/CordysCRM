<template>
  <CrmCard hide-footer no-content-padding>
    <div class="h-full px-[16px] pt-[16px]">
      <quotationTable :formKey="FormDesignKeyEnum.BUSINESS" />
    </div>
  </CrmCard>
</template>

<script setup lang="ts">
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import quotationTable from './components/quotation/quotationTable.vue';
</script>

<style scoped></style>
