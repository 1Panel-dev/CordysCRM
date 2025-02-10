<template>
  <CrmCard no-content-padding hide-footer>
    <CrmSplitPanel :max="0.5" :min="0.2" :default-size="0.2">
      <template #1>
        <div class="org-tree-wrapper">
          <OrgModuleTree @select-node="selectNode" />
        </div>
      </template>
      <template #2>
        <OrgTable :active-node="activeNodeId" @add-success="addSuccess" />
      </template>
    </CrmSplitPanel>
  </CrmCard>
</template>

<script setup lang="ts">
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import OrgModuleTree from './components/moduleTree.vue';
  import OrgTable from '@/views/system/org/components/orgTable.vue';

  const activeNodeId = ref<string | number>('');

  function selectNode(_selectedKey: string | number) {
    activeNodeId.value = _selectedKey;
  }

  function addSuccess() {}
</script>

<style lang="less" scoped>
  .org-tree-wrapper {
    padding: 24px;
  }
</style>
