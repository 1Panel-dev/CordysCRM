<template>
  <div ref="leadCardRef" class="h-full">
    <CrmCard no-content-padding hide-footer>
      <div class="h-full px-[16px] pt-[16px]">
        <ClueTable :table-form-key="FormDesignKeyEnum.CLUE" />
      </div>
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import ClueTable from './components/clueTable.vue';
</script>

<style lang="less" scoped>
  :deep(.n-tabs-scroll-padding) {
    width: 16px !important;
  }
</style>
