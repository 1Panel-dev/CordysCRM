<template>
  <CrmCard hide-footer no-content-padding>
    <CrmSplitPanel :default-size="0.3" :min="0.2" :max="0.5">
      <template #1>
        <tree
          v-model:value="selectedKeys"
          @init="folderTree = $event"
          @add-dashboard="handleAddDashboard"
          @select-node="handleNodeSelect"
        />
      </template>
      <template #2>
        <!-- <dashboardTable :active-folder-id="selectedKeys[0]" @create="handleAddDashboard" /> -->
        <dashboard
          :title="activeDashboard.name"
          :dashboard-id="activeDashboard.id"
          :is-favorite="activeDashboard.isFavorite"
        />
      </template>
    </CrmSplitPanel>
  </CrmCard>
  <addDashboardModal v-model:show="show" :folder-tree="folderTree" />
</template>

<script setup lang="ts">
  import { TreeSelectOption } from 'naive-ui';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import addDashboardModal from './components/addDashboardModal.vue';
  import dashboard from './components/dashboard.vue';
  import dashboardTable from './components/table.vue';
  import tree from './components/tree.vue';

  const show = ref(false);
  const folderTree = ref<TreeSelectOption[]>([]);
  const selectedKeys = ref<Array<string | number>>([]);
  const activeDashboard = ref<any>({});

  function handleNodeSelect(node: CrmTreeNodeData) {
    activeDashboard.value = node;
  }

  function handleAddDashboard() {
    show.value = true;
  }
</script>

<style lang="less" scoped></style>
