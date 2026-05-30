<template>
  <CrmCard hide-footer no-content-bottom-padding>
    <cluePoolTable :form-key="FormDesignKeyEnum.CLUE_POOL" />
  </CrmCard>
</template>

<script setup lang="ts">
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import cluePoolTable from './components/cluePoolTable.vue';
</script>

<style lang="less"></style>
