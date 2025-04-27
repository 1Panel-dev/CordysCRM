<template>
  <CrmCard no-content-padding hide-footer>
    <CrmSplitPanel :max="0.5" :min="0.2" :default-size="0.2">
      <template #1>
        <div class="org-tree-wrapper">
          <OrgModuleTree
            ref="orgModuleTreeRef"
            :is-sync-from-third-checked="isSyncFromThirdChecked"
            @select-node="selectNode"
            @load-list="() => orgTableRef?.initOrgList()"
          />
        </div>
      </template>
      <template #2>
        <OrgTable
          ref="orgTableRef"
          :is-sync-from-third-checked="isSyncFromThirdChecked"
          :active-node="activeNodeId"
          :offspring-ids="offspringIds"
          @add-success="addSuccess"
        />
      </template>
    </CrmSplitPanel>
  </CrmCard>
</template>

<script setup lang="ts">
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import OrgModuleTree from './components/moduleTree.vue';
  import OrgTable from '@/views/system/org/components/orgTable.vue';

  import { checkSyncUserFromThird } from '@/api/modules';

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

  const isSyncFromThirdChecked = ref(false);
  async function initCheckSyncType() {
    try {
      isSyncFromThirdChecked.value = await checkSyncUserFromThird();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const orgTableRef = ref<InstanceType<typeof OrgTable>>();

  onBeforeMount(() => {
    initCheckSyncType();
  });
</script>

<style lang="less" scoped>
  .org-tree-wrapper {
    padding: 24px;
  }
</style>
