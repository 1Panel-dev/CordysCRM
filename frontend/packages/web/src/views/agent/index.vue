<template>
  <CrmCard hide-footer no-content-padding>
    <CrmSplitPanel :default-size="0.3" :min="0.2" :max="0.5">
      <template #1>
        <tree
          ref="folderTreeRef"
          v-model:value="selectedKeys"
          @init="handleFolderTreeInit"
          @select-node="handleNodeSelect"
          @collect="refreshTable"
          @delete="refreshTable"
          @move="refreshTable"
        />
      </template>
      <template #2>
        <agentTable
          ref="agentTableRef"
          :active-folder-id="selectedKeys[0]"
          :is-favorite="activeNode?.id === 'favorite'"
          :offspring-ids="offspringIds"
          @create="handleAddAgent"
          @edit="handleEditAgent"
          @detail="handleAgentDetail"
          @collect="handleCollectAgent"
          @delete="refreshTree"
        />
      </template>
    </CrmSplitPanel>
  </CrmCard>
  <addAgentModal
    v-model:show="show"
    :folder-tree="folderTree"
    :agent-id="activeAgentId"
    :is-detail="isDetail"
    :active-folder="activeNode?.id"
    @finish="handleFinish"
  />
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import { TreeSelectOption } from 'naive-ui';

  import { mapTree } from '@lib/shared/method';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import addAgentModal from './components/addAgentModal.vue';
  import agentTable from './components/table.vue';
  import tree from './components/tree.vue';

  const route = useRoute();

  const show = ref(false);
  const folderTree = ref<TreeSelectOption[]>([]);
  const folderTreeRef = ref<InstanceType<typeof tree>>();
  const selectedKeys = ref<Array<string>>([]);
  const activeNode = ref<CrmTreeNodeData>();
  const offspringIds = ref<Array<string>>([]);
  const activeAgentId = ref<string | undefined>('');

  function handleNodeSelect(
    node: CrmTreeNodeData,
    _selectedKeys: Array<string | number>,
    _offspringIds: Array<string | number>
  ) {
    activeNode.value = node;
    offspringIds.value = _offspringIds as Array<string>;
  }

  const isDetail = ref(false);
  function handleAddAgent() {
    activeAgentId.value = '';
    show.value = true;
    isDetail.value = false;
  }

  function handleFolderTreeInit(_tree: CrmTreeNodeData[]) {
    folderTree.value = mapTree(_tree, (item) => {
      if (item.children?.length === 0) {
        item.children = undefined;
      }
      return item;
    });
  }

  function handleEditAgent(id: string) {
    activeAgentId.value = id;
    show.value = true;
    isDetail.value = false;
  }

  function handleAgentDetail(id: string) {
    activeAgentId.value = id;
    show.value = true;
    isDetail.value = true;
  }

  function handleCollectAgent() {
    folderTreeRef.value?.initModuleCount();
  }

  const agentTableRef = ref<InstanceType<typeof agentTable>>();
  function refreshTable() {
    agentTableRef.value?.loadList();
  }

  function handleFinish() {
    folderTreeRef.value?.initTree();
    refreshTable();
  }

  function refreshTree() {
    folderTreeRef.value?.initTree();
    folderTreeRef.value?.initModuleCount();
  }

  watch(
    () => route.query.t,
    () => {
      if (route.query.showAdd === 'Y' && folderTree.value.length > 0) {
        handleAddAgent();
      }
    }
  );

  onBeforeMount(() => {
    if (route.query.showAdd === 'Y') {
      handleAddAgent();
    }
  });
</script>

<style lang="less" scoped></style>
