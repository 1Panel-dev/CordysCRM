<template>
  <div class="flex h-full flex-col p-[24px]">
    <div class="mb-[8px] flex flex-col items-center justify-between gap-[8px]">
      <n-input v-model:value="keyword" :placeholder="t('common.searchByName')" clearable>
        <template #suffix>
          <n-icon>
            <Search />
          </n-icon>
        </template>
      </n-input>
      <div
        class="folder-button"
        :class="{ 'folder-button--active': selectedKeys.includes('favorite') }"
        @click="handleFolderClick('favorite')"
      >
        <div class="folder-button-left">
          <favoriteIcon />
          <div>{{ t('agent.myFavorite') }}</div>
        </div>
        <div class="folder-button-right">{{ myFavoriteCount }}</div>
      </div>
    </div>
    <NDivider class="!m-0" />
    <div
      class="folder-button mt-[4px]"
      :class="{ 'folder-button--active': selectedKeys.includes('all') }"
      @click="handleFolderClick('all')"
    >
      <div class="folder-button-left">
        <CrmIcon type="iconicon_wallet" :size="16" />
        <div>{{ t('agent.all') }}</div>
      </div>
      <div class="flex items-center gap-[8px]">
        <n-tooltip trigger="hover" :style="{ padding: '4px' }">
          <template #trigger>
            <n-button
              type="default"
              size="tiny"
              ghost
              class="n-button--default-type h-[20px] px-[3px]"
              @click.stop="expandAll = !expandAll"
            >
              <template #icon>
                <CrmIcon
                  :type="expandAll ? 'iconicon_folder_collapse' : 'iconicon_folder_expansion'"
                  class="text-[var(--text-n4)]"
                  :size="16"
                />
              </template>
            </n-button>
          </template>
          <span>{{ expandAll ? t('agent.collapseChild') : t('agent.expandChild') }}</span>
        </n-tooltip>
        <n-tooltip trigger="hover" :style="{ padding: '4px' }">
          <template #trigger>
            <n-button
              v-permission="['AGENT:ADD']"
              type="primary"
              size="tiny"
              ghost
              class="n-btn-outline-primary h-[20px] px-[3px]"
              @click.stop="addFolder"
            >
              <template #icon>
                <n-icon><Add /></n-icon>
              </template>
            </n-button>
          </template>
          <span>{{ t('agent.newChild') }}</span>
        </n-tooltip>
      </div>
    </div>
    <CrmTree
      ref="folderTreeRef"
      v-model:data="folderTree"
      v-model:selected-keys="selectedKeys"
      v-model:expanded-keys="expandedKeys"
      v-model:default-expand-all="expandAll"
      :draggable="hasAnyPermission(['AGENT:UPDATE'])"
      :keyword="keyword"
      :render-prefix="renderPrefixDom"
      :node-more-actions="nodeMoreOptions"
      :render-extra="renderExtraDom"
      :virtual-scroll-props="{ virtualScroll: true, virtualScrollHeight: '100%' }"
      :field-names="{
        keyField: 'id',
        labelField: 'name',
        childrenField: 'children',
        disabledField: 'disabled',
        isLeaf: 'isLeaf',
      }"
      :rename-api="renameHandler"
      :create-api="handleCreateNode"
      @drop="handleDrag"
      @select="handleNodeSelect"
      @more-action-select="handleFolderMoreSelect"
    />
  </div>
</template>

<script setup lang="ts">
  import { NButton, NDivider, NIcon, NInput, NTooltip, useMessage } from 'naive-ui';
  import { Add, Search } from '@vicons/ionicons5';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getGenerateId, mapTree, traverseTree } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTree from '@/components/pure/crm-tree/index.vue';
  import { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import favoriteIcon from './favoriteIcon.vue';

  import {
    agentModuleAdd,
    agentModuleDelete,
    agentModuleMove,
    agentModuleRename,
    getAgentModuleTree,
    getAgentModuleTreeCount,
  } from '@/api/modules';
  import { hasAnyPermission } from '@/utils/permission';

  const Message = useMessage();

  const { t } = useI18n();

  const emit = defineEmits<{
    (e: 'selectNode', node: CrmTreeNodeData, _selectedKeys: Array<string | number>, offspringIds: string[]): void;
    (e: 'init', folderTree: CrmTreeNodeData[]): void;
    (e: 'addAgent', option: CrmTreeNodeData): void;
    (e: 'collect'): void;
    (e: 'delete'): void;
    (e: 'move'): void;
  }>();

  const selectedKeys = defineModel<Array<string | number>>('value', {
    required: true,
  });

  const folderTree = ref<CrmTreeNodeData[]>([]);
  const myFavoriteCount = ref<number>(0);
  const expandedKeys = ref<Array<string | number>>([]);
  const keyword = ref<string>('');
  const expandAll = ref<boolean>(true);

  const folderTreeCount = ref<Record<string, any>>({});
  async function initModuleCount() {
    try {
      folderTreeCount.value = await getAgentModuleTreeCount();
      traverseTree(folderTree.value, (node) => {
        node.count = folderTreeCount.value[node.id] || 0;
      });
      myFavoriteCount.value = folderTreeCount.value.myCollect || 0;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function renderPrefixDom() {
    return h(CrmIcon, {
      type: 'iconicon_wallet',
      class: 'mr-[8px]',
      size: 16,
    });
  }

  const nodeMoreOptions = ref<ActionsItem[]>([
    {
      label: t('common.rename'),
      key: 'rename',
      permission: ['AGENT:UPDATE'],
    },
    {
      label: t('common.delete'),
      key: 'delete',
      permission: ['AGENT:DELETE'],
    },
  ]);

  function getSpringIds(children: CrmTreeNodeData[] | undefined): string[] {
    const offspringIds: string[] = [];
    mapTree(children || [], (e) => {
      offspringIds.push(e.id);
      return e;
    });
    return offspringIds;
  }

  function handleNodeSelect(
    _selectedKeys: Array<string | number>,
    option: Array<CrmTreeNodeData | null> | CrmTreeNodeData
  ) {
    const offspringIds = getSpringIds((option as CrmTreeNodeData).children);
    emit('selectNode', option as CrmTreeNodeData, _selectedKeys, offspringIds);
  }

  // 获取模块树
  async function initTree(isInit = false) {
    try {
      const res = await getAgentModuleTree();
      folderTree.value = res;
      emit('init', folderTree.value);
      if (isInit) {
        selectedKeys.value = ['all'];
        const offspringIds = getSpringIds(folderTree.value);
        emit('selectNode', folderTree.value[0], selectedKeys.value, offspringIds);
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  //  重命名
  async function renameHandler(option: CrmTreeNodeData) {
    try {
      const params = {
        id: option.id,
        name: option.name,
      };
      await agentModuleRename(params);
      initTree();
      return Promise.resolve(true);
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
      return Promise.resolve(false);
    }
  }

  // 添加节点
  async function handleCreateNode(option: CrmTreeNodeData) {
    try {
      await agentModuleAdd({
        parentId: option.parentId || '',
        name: option.name,
      });
      initTree();
      return Promise.resolve(true);
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
      return Promise.resolve(false);
    }
  }

  const folderTreeRef = ref();
  const currentParentId = ref<string>('');

  // 添加节点
  async function addNode(parent: CrmTreeNodeData | null) {
    currentParentId.value = parent ? parent.id : 'NONE';
    try {
      const id = getGenerateId();
      const newNode: CrmTreeNodeData = {
        id,
        isNew: true,
        parentId: currentParentId.value,
        name: '',
        children: undefined,
      };
      if (parent) {
        parent.children = parent.children ?? [];
        parent.children.push(newNode);
        expandedKeys.value.push(parent.id);
      } else {
        folderTree.value.push(newNode);
      }
      nextTick(() => {
        folderTreeRef.value?.toggleEdit(id);
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  // 添加子节点
  function handleAdd(option: CrmTreeNodeData) {
    addNode(option);
  }

  // 添加到根节点
  function addFolder() {
    addNode(null);
  }

  function renderExtraDom(infoProps: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
    if (hasAnyPermission(['AGENT:ADD'])) {
      const { option } = infoProps;
      // 额外的节点
      return h(
        NTooltip,
        {},
        {
          trigger: () =>
            h(
              NButton,
              {
                type: 'primary',
                size: 'tiny',
                ghost: true,
                class: 'n-btn-outline-primary h-[20px] px-[3px]',
                onClick: (e: MouseEvent) => {
                  e.stopPropagation();
                  handleAdd(option);
                },
              },
              {
                icon: () => h(NIcon, {}, { default: () => h(Add) }),
              }
            ),
          default: () => h('span', {}, t('agent.newChild')),
        }
      );
    }
    return null;
  }

  /**
   * 处理文件夹树节点拖拽事件
   * @param tree 树数据
   * @param dragNode 拖拽节点
   * @param dropNode 释放节点
   * @param dropPosition 释放位置
   */
  async function handleDrag(
    tree: CrmTreeNodeData[],
    dragNode: CrmTreeNodeData,
    dropNode: CrmTreeNodeData,
    dropPosition: 'before' | 'inside' | 'after'
  ) {
    try {
      const positionMap: Record<'before' | 'inside' | 'after', 0 | -1 | 1> = {
        before: -1,
        inside: 0,
        after: 1,
      };
      await agentModuleMove({
        dragNodeId: dragNode.id as string,
        dropNodeId: dropNode.id,
        dropPosition: positionMap[dropPosition],
      });
      Message.success(t('common.moveSuccess'));
      initTree();
      emit('move');
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  /**
   * 删除
   */
  async function handleDelete(option: CrmTreeNodeData) {
    try {
      const offspringIds = getSpringIds(option.children || []);
      await agentModuleDelete([option.id, ...offspringIds]);
      if (selectedKeys.value.includes(option.id)) {
        selectedKeys.value = ['all'];
      }
      Message.success(t('common.deleteSuccess'));
      initTree(true);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function handleFolderMoreSelect(item: ActionsItem, option: CrmTreeNodeData) {
    switch (item.key) {
      case 'delete':
        handleDelete(option);
        break;
      case 'rename':
        folderTreeRef.value?.toggleEdit(option.id);
        break;
      default:
        break;
    }
  }

  function handleFolderClick(folderId: string | number) {
    selectedKeys.value = [folderId];
    emit('selectNode', { id: folderId }, selectedKeys.value, []);
  }

  onMounted(async () => {
    await initTree(true);
    initModuleCount();
  });

  defineExpose({
    initTree,
    initModuleCount,
  });
</script>

<style lang="less" scoped>
  .folder-button {
    @apply flex w-full cursor-pointer items-center justify-between;

    padding: 6px;
    border-radius: 4px;
    .folder-button-left {
      @apply flex items-center;

      gap: 4px;
    }
    .folder-button-right {
      color: var(--text-n4);
    }
    &:hover {
      background-color: var(--primary-7);
    }
    &--active {
      background-color: var(--primary-7);
      .folder-button-left {
        color: var(--primary-8);
      }
    }
  }
  :deep(.n-tree-node-content__prefix) {
    height: 35px;
  }
</style>
