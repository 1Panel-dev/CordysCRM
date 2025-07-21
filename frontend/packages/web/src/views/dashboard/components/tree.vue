<template>
  <div class="flex h-full flex-col p-[24px]">
    <div class="mb-[8px] flex flex-col items-center justify-between gap-[8px]">
      <n-input v-model:value="keyword" :placeholder="t('common.searchByName')">
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
          <div>{{ t('dashboard.myFavorite') }}</div>
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
        <div>{{ t('dashboard.all') }}</div>
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
          <span>{{ expandAll ? t('dashboard.collapseChild') : t('dashboard.expandChild') }}</span>
        </n-tooltip>
        <n-tooltip trigger="hover" :style="{ padding: '4px' }">
          <template #trigger>
            <n-button
              v-permission="['DASHBOARD:ADD']"
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
          <span>{{ t('dashboard.newChild') }}</span>
        </n-tooltip>
      </div>
    </div>
    <CrmTree
      ref="folderTreeRef"
      v-model:data="folderTree"
      v-model:selected-keys="selectedKeys"
      v-model:expanded-keys="expandedKeys"
      v-model:default-expand-all="expandAll"
      :draggable="hasAnyPermission(['DASHBOARD:EDIT'])"
      :keyword="keyword"
      :render-prefix="renderPrefixDom"
      :node-more-actions="nodeMoreOptions"
      :filter-more-action-func="filterMoreActionFunc"
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
  import { characterLimit, getGenerateId, mapTree, traverseTree } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTree from '@/components/pure/crm-tree/index.vue';
  import { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import favoriteIcon from './favoriteIcon.vue';

  import {
    dashboardCollect,
    dashboardDelete,
    dashboardDrag,
    dashboardModuleAdd,
    dashboardModuleCount,
    dashboardModuleDelete,
    dashboardModuleDrag,
    dashboardModuleRename,
    dashboardModuleTree,
    dashboardRename,
    dashboardUnCollect,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const { openModal } = useModal();

  const Message = useMessage();

  const { t } = useI18n();

  const emit = defineEmits<{
    (e: 'selectNode', node: CrmTreeNodeData, _selectedKeys: Array<string | number>, offspringIds: string[]): void;
    (e: 'init', folderTree: CrmTreeNodeData[]): void;
    (e: 'addDashboard', option: CrmTreeNodeData): void;
    (e: 'editDashboard', id: string): void;
    (e: 'collect'): void;
  }>();

  const selectedKeys = defineModel<Array<string | number>>('value', {
    required: true,
  });

  const folderTree = ref<CrmTreeNodeData[]>([]);
  const myFavoriteCount = ref<number>(0);
  const expandedKeys = ref<Array<string | number>>([]);
  const keyword = ref<string>('');
  const expandAll = ref<boolean>(true);

  async function favoriteToggle(option: CrmTreeNodeData) {
    try {
      if (option.myCollect) {
        await dashboardUnCollect(option.id);
      } else {
        await dashboardCollect(option.id);
      }
      option.myCollect = !option.myCollect;
      Message.success(option.myCollect ? t('dashboard.favoriteSuccess') : t('dashboard.unFavoriteSuccess'));
      emit('collect');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function renderPrefixDom(infoProps: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
    const { option } = infoProps;
    if (option.type === 'DASHBOARD') {
      return h(favoriteIcon, {
        value: option.myCollect,
        class: 'mr-[8px]',
        onclick: (e: MouseEvent) => {
          e.stopPropagation();
          favoriteToggle(option);
        },
      });
    }
    if (option.type === 'MODULE') {
      return h(CrmIcon, {
        type: 'iconicon_wallet',
        class: 'mr-[8px]',
        size: 16,
      });
    }
    return null;
  }

  const nodeMoreOptions = ref<ActionsItem[]>([
    {
      label: t('dashboard.addDashboard'),
      key: 'addDashboard',
      permission: [],
    },
    {
      label: t('common.edit'),
      key: 'edit',
      permission: [],
    },
    {
      label: t('common.rename'),
      key: 'rename',
      permission: [],
    },
    {
      type: 'divider',
    },
    {
      label: t('common.delete'),
      key: 'delete',
      danger: true,
      permission: [],
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

  function filterMoreActionFunc(items: ActionsItem[], node: CrmTreeNodeData) {
    if (!hasAnyPermission(['DASHBOARD:EDIT'])) {
      items = items.filter((e) => {
        return e.key !== 'edit' && e.key !== 'rename';
      });
    }
    if (!hasAnyPermission(['DASHBOARD:ADD'])) {
      items = items.filter((e) => {
        return e.key !== 'addDashboard';
      });
    }
    if (!hasAnyPermission(['DASHBOARD:DELETE'])) {
      items = items.filter((e) => {
        return e.key !== 'delete';
      });
    }
    return items.filter((e) => {
      if (node.type === 'MODULE') {
        return e.key !== 'edit';
      }
      if (node.type === 'DASHBOARD') {
        return e.key !== 'addDashboard';
      }
      return true;
    });
  }

  // 获取模块树
  async function initTree(isInit = false) {
    try {
      folderTree.value = await dashboardModuleTree();
      emit('init', folderTree.value);
      if (isInit) {
        selectedKeys.value = folderTree.value[0] ? [folderTree.value[0].id] : [];
        const offspringIds = getSpringIds(folderTree.value);

        emit('selectNode', folderTree.value[0], selectedKeys.value, offspringIds);
        nextTick(() => {
          expandedKeys.value = [folderTree.value[0].id];
        });
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const folderTreeCount = ref<Record<string, any>>({});
  async function initModuleCount() {
    try {
      folderTreeCount.value = await dashboardModuleCount();
      traverseTree(folderTree.value, (node) => {
        if (node.type === 'MODULE') {
          node.count = folderTreeCount.value[node.id] || 0;
        } else if (node.type === 'DASHBOARD') {
          node.count = folderTreeCount.value[node.parentId] || 0;
        }
      });
      myFavoriteCount.value = folderTreeCount.value.myCollect || 0;
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
      if (option.type === 'MODULE') {
        await dashboardModuleRename(params);
      } else {
        await dashboardRename({
          ...params,
          dashboardModuleId: option.parentId || '',
        });
      }
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
      await dashboardModuleAdd({
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
        type: 'MODULE',
      };

      if (parent) {
        parent.children = parent.children ?? [];
        parent.children.push(newNode);
      } else {
        folderTree.value.push(newNode);
      }
      expandedKeys.value.push(currentParentId.value);
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
    if (hasAnyPermission(['SYS_ORGANIZATION:ADD'])) {
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
          default: () => h('span', {}, t('dashboard.newChild')),
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
    if (dragNode.type === 'MODULE' && dropNode.type === 'DASHBOARD' && dropPosition === 'inside') {
      // 模块不能拖拽到仪表板上
      return;
    }
    try {
      if (dragNode.type === 'MODULE') {
        const positionMap: Record<'before' | 'inside' | 'after', 0 | -1 | 1> = {
          before: -1,
          inside: 0,
          after: 1,
        };
        await dashboardModuleDrag({
          dragNodeId: dragNode.id as string,
          dropNodeId: dropNode.type === 'DASHBOARD' ? dropNode.parentId : dropNode.id,
          dropPosition: positionMap[dropPosition],
        });
      } else {
        const positionMap: Record<'before' | 'inside' | 'after', 'AFTER' | 'BEFORE' | 'APPEND'> = {
          before: 'BEFORE',
          inside: 'APPEND',
          after: 'AFTER',
        };
        await dashboardDrag({
          moveId: dragNode.id,
          moveMode: positionMap[dropPosition],
          targetId: dropNode.type === 'MODULE' ? dragNode.id : dropNode.id, // 释放节点是模块，则传入当前拖动的 仪表板 的id；释放节点是 仪表板 节点的话就传入释放节点的 id
          dashboardModuleId: dropNode.type === 'DASHBOARD' ? dropNode.parentId : dropNode.id, // 释放节点是 仪表板，则传入它所属模块id；模块的话直接是模块id
        });
      }
      Message.success(t('common.moveSuccess'));
      initTree();
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  /**
   * 删除
   */
  async function handleDelete(option: CrmTreeNodeData) {
    if (option.type === 'MODULE') {
      try {
        const offspringIds = getSpringIds(option.children || []);
        await dashboardModuleDelete([option.id, ...offspringIds]);
        Message.success(t('common.deleteSuccess'));
        initTree(true);
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    } else {
      openModal({
        type: 'error',
        title: t('common.deleteConfirmTitle', { name: characterLimit(option.name) }),
        content: t('dashboard.deleteDashboardTip'),
        positiveText: t('common.confirm'),
        negativeText: t('common.cancel'),
        positiveButtonProps: {
          type: 'error',
          size: 'medium',
        },
        onPositiveClick: async () => {
          try {
            await dashboardDelete(option.id);
            Message.success(t('common.deleteSuccess'));
            initTree(true);
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
          }
        },
      });
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
      case 'edit':
        emit('editDashboard', option.id);
        break;
      case 'addDashboard':
        emit('addDashboard', option);
        break;
      default:
        break;
    }
  }

  function handleFolderClick(folderId: string | number) {
    selectedKeys.value = [folderId];
    emit('selectNode', { id: folderId }, selectedKeys.value, []);
  }

  function toggleDashboardCollect(nodeId: string, collect: boolean) {
    traverseTree(folderTree.value, (node) => {
      if (node.id === nodeId) {
        node.myCollect = collect;
        return true; // 停止遍历
      }
      return false; // 继续遍历
    });
    initModuleCount();
  }

  onBeforeMount(async () => {
    await initTree(true);
    initModuleCount();
  });

  defineExpose({
    initTree,
    toggleDashboardCollect,
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
