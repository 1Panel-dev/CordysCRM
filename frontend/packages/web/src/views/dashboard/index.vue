<template>
  <CrmCard hide-footer no-content-padding>
    <CrmSplitPanel :default-size="0.3" :min="0.2" :max="0.5">
      <template #1>
        <tree
          ref="folderTreeRef"
          v-model:value="selectedKeys"
          @init="handleFolderTreeInit"
          @add-dashboard="handleAddDashboard"
          @select-node="handleNodeSelect"
          @edit-dashboard="handleEditDashboard"
          @collect="refreshTable"
          @delete="refreshTable"
          @move="refreshTable"
        />
      </template>
      <template #2>
        <dashboardTable
          v-if="activeNode?.type !== 'DASHBOARD'"
          ref="dashboardTableRef"
          :active-folder-id="selectedKeys[0]"
          :is-favorite="activeNode?.id === 'favorite'"
          :offspring-ids="offspringIds"
          @create="handleAddDashboard"
          @edit="handleEditDashboard"
          @collect="handleCollectDashboard"
        />
        <dashboard
          v-else-if="activeNode"
          :title="activeNode.name"
          :dashboard-id="activeNode.resourceId"
          :is-favorite="!!activeNode.myCollect"
          @toggle-favorite="favoriteToggle(activeNode)"
        />
      </template>
    </CrmSplitPanel>
  </CrmCard>
  <addDashboardModal
    v-model:show="show"
    :folder-tree="folderTree"
    :dashboard-id="activeDashboardId"
    @finish="handleFinish"
  />
</template>

<script setup lang="ts">
  import { TreeSelectOption, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { mapTree } from '@lib/shared/method';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import addDashboardModal from './components/addDashboardModal.vue';
  import dashboard from './components/dashboard.vue';
  import dashboardTable from './components/table.vue';
  import tree from './components/tree.vue';

  import { dashboardCollect, dashboardUnCollect } from '@/api/modules';

  const { t } = useI18n();
  const Message = useMessage();

  const show = ref(false);
  const folderTree = ref<TreeSelectOption[]>([]);
  const folderTreeRef = ref<InstanceType<typeof tree>>();
  const selectedKeys = ref<Array<string>>([]);
  const activeNode = ref<CrmTreeNodeData>();
  const offspringIds = ref<Array<string>>([]);
  const activeDashboardId = ref<string | undefined>('');

  function handleNodeSelect(
    node: CrmTreeNodeData,
    _selectedKeys: Array<string | number>,
    _offspringIds: Array<string | number>
  ) {
    activeNode.value = node;
    offspringIds.value = _offspringIds as Array<string>;
  }

  function handleFolderTreeInit(_tree: CrmTreeNodeData[]) {
    folderTree.value = mapTree(_tree, (item) => {
      if (item.type === 'MODULE') {
        if (item.children?.length === 0) {
          item.children = undefined;
        }
        return item;
      }
      return null;
    });
  }

  function handleAddDashboard() {
    activeDashboardId.value = '';
    show.value = true;
  }

  function handleEditDashboard(id: string) {
    show.value = true;
    activeDashboardId.value = id;
  }

  async function favoriteToggle(option: CrmTreeNodeData) {
    try {
      if (option.myCollect) {
        await dashboardUnCollect(option.id);
      } else {
        await dashboardCollect(option.id);
      }
      option.myCollect = !option.myCollect;
      Message.success(option.myCollect ? t('dashboard.favoriteSuccess') : t('dashboard.unFavoriteSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function handleCollectDashboard(id: string, collect: boolean) {
    folderTreeRef.value?.toggleDashboardCollect(id, collect);
  }

  const dashboardTableRef = ref<InstanceType<typeof dashboardTable>>();
  function refreshTable() {
    dashboardTableRef.value?.loadList();
  }

  function handleFinish() {
    folderTreeRef.value?.initTree();
    refreshTable();
  }
</script>

<style lang="less" scoped></style>
