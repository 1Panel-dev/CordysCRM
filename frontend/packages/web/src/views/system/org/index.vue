<template>
  <CrmCard no-content-padding hide-footer>
    <CrmSplitPanel :max="0.5" :min="0.2" :default-size="0.2">
      <template #1>
        <div class="org-tree-wrapper">
          <OrgModuleTree ref="orgModuleTreeRef" @select-node="selectNode" />
        </div>
      </template>
      <template #2>
        <OrgTable :active-node="activeNodeId" :offspring-ids="offspringIds" @add-success="addSuccess" />
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
  const offspringIds = ref<string[]>([]);

  function selectNode(_selectedKeys: Array<string | number>, _offspringIds: string[]) {
    [activeNodeId.value] = _selectedKeys;
    offspringIds.value = _offspringIds;
  }

  const orgModuleTreeRef = ref<InstanceType<typeof OrgModuleTree>>();
  function addSuccess() {
    orgModuleTreeRef.value?.initTree();
  }
</script>

<style lang="less" scoped>
  .org-tree-wrapper {
    padding: 24px;
  }
</style>
