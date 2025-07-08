<template>
  <div class="p-[24px]">
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
        @click="selectedKeys = ['favorite']"
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
      @click="selectedKeys = ['all']"
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
      :draggable="true"
      :keyword="keyword"
      :render-prefix="renderPrefixDom"
      :node-more-actions="nodeMoreOptions"
      :filter-more-action-func="filterMoreActionFunc"
      :render-extra="renderExtraDom"
      :virtual-scroll-props="{ virtualScroll: true, virtualScrollHeight: 'calc(100vh - 176px)' }"
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
  import { characterLimit, getGenerateId, mapTree } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTree from '@/components/pure/crm-tree/index.vue';
  import type { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import favoriteIcon from './favoriteIcon.vue';

  import { getDepartmentTree, sortDepartment } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const { openModal } = useModal();

  const Message = useMessage();

  const { t } = useI18n();

  // const props = defineProps<{
  // }>();

  const emit = defineEmits<{
    (e: 'selectNode', node: CrmTreeNodeData, _selectedKeys: Array<string | number>, offspringIds: string[]): void;
    (e: 'init', folderTree: CrmTreeNodeData[]): void;
    (e: 'addDashboard', option: CrmTreeNodeData): void;
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
      option.isFavorite = !option.isFavorite;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function renderPrefixDom(infoProps: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
    const { option } = infoProps;
    if (option.parentId === 'NONE') {
      return h(favoriteIcon, {
        value: option.isFavorite,
        class: 'mr-[8px]',
        onclick: () => favoriteToggle(option),
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
    return items.filter((e) => {
      if (node.parentId === 'NONE') {
        return e.key !== 'delete';
      }
      return true;
    });
  }

  // 获取模块树
  async function initTree(isInit = false) {
    try {
      folderTree.value = await getDepartmentTree();
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

  //  重命名
  async function renameHandler(option: CrmTreeNodeData) {
    try {
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
    currentParentId.value = parent ? parent.id : folderTree.value[0].id;
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
      } else {
        folderTree.value[0].children = folderTree.value[0].children ?? [];
        folderTree.value[0].children.push(newNode);
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
    const positionMap: Record<'before' | 'inside' | 'after', 0 | -1 | 1> = {
      before: -1,
      inside: 0,
      after: 1,
    };
    try {
      await sortDepartment({
        dragNodeId: dragNode.id,
        dropNodeId: dropNode.id,
        dropPosition: positionMap[dropPosition],
      });
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
          Message.success(t('common.deleteSuccess'));
          initTree(true);
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  function handleFolderMoreSelect(item: ActionsItem, option: CrmTreeNodeData) {
    switch (item.key) {
      case 'delete':
        handleDelete(option);
        break;
      case 'rename':
        folderTreeRef.value?.toggleEdit(option.id);
        break;
      case 'addDashboard':
        emit('addDashboard', option);
        break;
      default:
        break;
    }
  }

  onBeforeMount(() => {
    initTree(true);
  });

  defineExpose({
    initTree,
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
</style>
