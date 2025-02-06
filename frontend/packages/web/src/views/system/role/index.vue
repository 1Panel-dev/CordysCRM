<template>
  <CrmCard :loading="loading" hide-footer no-content-padding>
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
            <n-tooltip trigger="hover" :delay="300">
              <template #trigger>
                <n-button type="tertiary" class="px-[6px]" @click="addRole">
                  <template #icon>
                    <n-icon><Add /></n-icon>
                  </template>
                </n-button>
              </template>
              {{ t('role.addRole') }}
            </n-tooltip>
          </div>
          <CrmTree
            v-model:selected-keys="selectedKeys"
            v-model:data="roles"
            :render-prefix="renderPrefix"
            :node-more-actions="nodeMoreActions"
            title-tooltip-position="top-start"
            :filter-more-action-func="filterMoreActionFunc"
            :field-names="{ keyField: 'id', labelField: 'name', childrenField: 'children' }"
            @more-action-select="handleMoreActionSelect"
          />
        </div>
      </template>
      <template #2>
        <div class="h-full pt-[13px]">
          <CrmTab v-model:active-tab="activeTab" :tab-list="tabList" type="line">
            <template #permission>
              <permissionTab />
            </template>
            <template #member>
              <memberTab />
            </template>
          </CrmTab>
        </div>
      </template>
    </CrmSplitPanel>
  </CrmCard>
</template>

<script lang="ts" setup>
  import { NButton, NIcon, NInput, NTooltip, TabPaneProps, useMessage } from 'naive-ui';
  import { Add, Search } from '@vicons/ionicons5';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmTree from '@/components/pure/crm-tree/index.vue';
  import { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import memberTab from './components/memberTab.vue';
  import permissionTab from './components/permissionTab.vue';
  import roleTreeNodePrefix from './components/roleTreeNodePrefix.vue';

  import { getRoles } from '@/api/modules/system/role';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import { getGenerateId } from '@/utils';

  import type { RoleItem } from '@lib/shared/models/system/role';

  const { t } = useI18n();
  const { openModal } = useModal();
  const message = useMessage();

  const keyword = ref('');
  const roles = ref<RoleItem[]>([]);
  const selectedKeys = ref<string[]>([]);

  function renderPrefix(node: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
    if (node.option.internal) {
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
    if (node.internal || node.isNew) {
      return [];
    }
    return items;
  }

  function handleMoreActionSelect(item: ActionsItem, node: CrmTreeNodeData) {
    switch (item.key) {
      case 'rename':
        break;
      case 'copy':
        const id = getGenerateId();
        roles.value.push({
          ...roles.value[roles.value.length - 1],
          name: `${node.name}Copy`,
          internal: false,
          id,
        });
        selectedKeys.value = [id];
        break;
      case 'delete':
        openModal({
          type: 'error',
          title: t('common.deleteConfirmTitle', { name: node.label }),
          content: t('role.deleteConfirmContent'),
          positiveText: t('common.confirmDelete'),
          negativeText: t('common.cancel'),
          onPositiveClick: async () => {
            roles.value = roles.value.filter((role) => role.id !== node.id);
            if (selectedKeys.value.includes(node.id)) {
              selectedKeys.value = [roles.value[0].id];
            }
            message.success(t('common.deleteSuccess'));
          },
        });
        break;
      default:
        break;
    }
  }

  function addRole() {
    const id = getGenerateId();
    roles.value.push({
      id,
      name: '新角色',
      internal: false,
      dataScope: 'all',
      description: '',
      isNew: true,
    });
    selectedKeys.value = [id];
  }

  const activeTab = ref('permission');
  const tabList: TabPaneProps[] = [
    {
      name: 'permission',
      tab: t('role.permission'),
    },
    {
      name: 'member',
      tab: t('role.member'),
    },
  ];

  const loading = ref(false);

  async function init() {
    loading.value = true;
    try {
      roles.value = await getRoles();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  onBeforeMount(() => {
    init();
  });
</script>

<style lang="less" scoped>
  :deep(.n-tree-node-switcher) {
    @apply hidden;
  }
  :deep(.n-tree-node-wrapper) {
    padding: 0;
  }
  :deep(.n-tabs-nav--line-type) {
    padding: 0 24px;
  }
  :deep(.n-tabs),
  :deep(.n-tab-pane) {
    @apply h-full overflow-hidden;
  }
</style>
