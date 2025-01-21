<template>
  <CrmCard no-content-padding>
    <CrmSplitPanel class="h-full" :max="0.5" :min="0.25" :default-size="0.25">
      <template #1>
        <div class="p-[24px]">
          <div class="mb-[8px] flex items-center justify-between gap-[8px]">
            <n-input v-model:value="keyword" :placeholder="t('common.searchByName')" clearable>
              <template #suffix>
                <n-icon>
                  <Search />
                </n-icon>
              </template>
            </n-input>
            <n-button type="tertiary" class="px-[6px]">
              <template #icon>
                <n-icon><Add /></n-icon>
              </template>
            </n-button>
          </div>
          <CrmTree
            :data="roles"
            :render-prefix="renderPrefix"
            :node-more-actions="nodeMoreActions"
            title-tooltip-position="top-start"
            :filter-more-action-func="filterMoreActionFunc"
          />
        </div>
      </template>
    </CrmSplitPanel>
  </CrmCard>
</template>

<script lang="ts" setup>
  import { NButton, NIcon, NInput } from 'naive-ui';
  import { Add, Search } from '@vicons/ionicons5';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import CrmTree from '@/components/pure/crm-tree/index.vue';
  import { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import roleTreeNodePrefix from './components/roleTreeNodePrefix.vue';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const keyword = ref('');
  const roles = ref<CrmTreeNodeData[]>([
    {
      id: 1,
      name: '超级管理员',
      system: true,
    },
    {
      id: 2,
      name: '管理员',
    },
    {
      id: 3,
      name: '普通用户',
    },
  ]);

  function renderPrefix(node: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
    if (node.option.system) {
      return h(roleTreeNodePrefix);
    }
  }

  const nodeMoreActions: ActionsItem[] = [
    {
      key: 'rename',
      label: t('common.rename'),
    },
    {
      key: 'copy',
      label: t('common.copy'),
    },
    {
      type: 'divider',
    },
    {
      key: 'delete',
      label: t('common.delete'),
      danger: true,
    },
  ];

  function filterMoreActionFunc(items: ActionsItem[], node: CrmTreeNodeData) {
    if (node.system) {
      return items.slice(0, 2);
    }
    return items;
  }
</script>

<style lang="less" scoped>
  :deep(.n-tree-node-switcher) {
    @apply hidden;
  }
  :deep(.n-tree-node-wrapper) {
    padding: 0;
  }
</style>
