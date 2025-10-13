<template>
  <CrmCard no-content-padding hide-footer :special-height="licenseStore.expiredDuring ? 64 : 0">
    <CrmSplitPanel class="h-full" :max="0.5" :min="0.25" :default-size="0.25">
      <template #1>
        <div class="org-tree-wrapper h-full">
          <OrgModuleTree
            ref="orgModuleTreeRef"
            @select-node="selectNode"
            @load-list="() => orgTableRef?.initOrgList()"
          />
        </div>
      </template>
      <template #2>
        <OrgTable
          ref="orgTableRef"
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

  import useLicenseStore from '@/store/modules/setting/license';

  const activeNodeId = ref<string | number>('');
  const offspringIds = ref<string[]>([]);

  const licenseStore = useLicenseStore();

  function selectNode(_selectedKeys: Array<string | number>, _offspringIds: string[]) {
    [activeNodeId.value] = _selectedKeys;
    offspringIds.value = _offspringIds;
  }

  const orgModuleTreeRef = ref<InstanceType<typeof OrgModuleTree>>();
  function addSuccess() {
    orgModuleTreeRef.value?.initTree();
  }

  const orgTableRef = ref<InstanceType<typeof OrgTable>>();
</script>

<style lang="less" scoped>
  .org-tree-wrapper {
    padding: 24px;
  }
</style>
