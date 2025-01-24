<template>
  <CrmCard no-content-padding hide-footer>
    <CrmSplitPanel :max="0.5" :min="0.2" :default-size="0.2">
      <template #1>
        <div class="org-tree-wrapper">
          <OrgModuleTree />
        </div>
      </template>
      <template #2>
        <OrgTable @add-success="addSuccess" />
      </template>
    </CrmSplitPanel>
  </CrmCard>
</template>

<script setup lang="ts">
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import OrgModuleTree from './components/moduleTree.vue';
  import OrgTable from '@/views/system/org/components/orgTable.vue';

  function addSuccess() {}
</script>

<style lang="less" scoped>
  .org-tree-wrapper {
    padding: 24px;
  }
</style>
