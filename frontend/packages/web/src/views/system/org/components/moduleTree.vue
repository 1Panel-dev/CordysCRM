<template>
  <div class="mb-[8px] flex items-center justify-between gap-[8px]">
    <n-input v-model:value="keyword" :placeholder="t('common.searchByName')">
      <template #suffix>
        <CrmIcon type="iconicon_search" :size="16" class="text-[var(--text-n4)]" />
      </template>
    </n-input>
    <n-button type="primary" ghost class="!px-[7px]" @click="changeExpand">
      <template #icon>
        <CrmIcon type="iconicon_add" class="" :size="16" />
      </template>
    </n-button>
  </div>
  <CrmTree
    v-model:data="orgModuleTree"
    v-model:selected-keys="selectedKeys"
    v-model:checked-keys="checkedKeys"
    v-model:expanded-keys="expandedKeys"
    v-model:default-expand-all="expandAll"
    draggable
    :keyword="keyword"
    :render-prefix="renderPrefixDom"
    :node-more-actions="nodeMoreOptions"
    :render-extra="renderExtraDom"
    :virtual-scroll-props="{ virtualScroll: false, virtualScrollHeight: 'calc(100vh - 176px)' }"
    :field-names="{
      keyField: 'id',
      labelField: 'name',
      childrenField: 'children',
      disabledField: 'disabled',
      isLeaf: 'isLeaf',
    }"
    @drop="handleDrag"
    @more-action-select="handleFolderMoreSelect"
  />
  <SetDepHeadModal v-model:show="showSetHeadModal" />
</template>

<script setup lang="ts">
  import { NButton, NInput, useMessage } from 'naive-ui';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTree from '@/components/pure/crm-tree/index.vue';
  import type { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import SetDepHeadModal from './setDepHead.vue';

  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import { characterLimit, getGenerateId } from '@/utils';

  const { openModal } = useModal();

  const Message = useMessage();

  const { t } = useI18n();

  const orgModuleTree = ref<CrmTreeNodeData[]>([
    {
      id: 'root',
      name: '未规划用例',
      type: 'MODULE',
      parentId: 'NONE',
      projectId: null,
      attachInfo: {},
      count: 0,
      corporation: true,
      path: '/未规划用例',
      children: [
        {
          id: '849389914030080',
          name: '测试默默',
          type: 'module',
          parentId: 'NONE',
          projectId: null,
          count: 10,
          children: [
            {
              id: 'd3qgf11a02w0',
              name: '1',
              type: 'module',
              parentId: '849389914030080',
              projectId: null,
              children: [
                {
                  id: '172473746390800000',
                  name: '测试1',
                  type: 'module',
                  parentId: 'd3qgf11a02w0',
                  projectId: null,
                  children: [
                    {
                      id: 'd4q2hgqzlc80',
                      name: '模块88模块88模块88模块88模块88模块88模块88模块88模块88模块88模块88模块88模块88模块88模块88模块88模块88模块88模块88模块88模块88模块88模块88模块88模块88模块88模块88模块88',
                      type: 'module',
                      parentId: '172473746390800000',
                      projectId: null,
                      children: undefined,
                      attachInfo: {},
                      count: 0,
                      path: '/测试默默/1/测试1/模块88',
                    },
                  ],
                  attachInfo: {},
                  count: 0,
                  path: '/测试默默/1/测试1',
                },
              ],
              attachInfo: {},
              count: 0,
              path: '/测试默默/1',
            },
          ],
          attachInfo: {},

          path: '/测试默默',
        },
        {
          id: '905344769294336',
          name: '22222',
          type: 'module',
          parentId: 'NONE',
          projectId: null,
          children: [
            {
              id: '905722726416384',
              name: 'ceceafsfa',
              type: 'module',
              parentId: '905344769294336',
              projectId: null,
              children: [],
              attachInfo: {},
              count: 0,
              path: '/22222/ceceafsfa',
            },
          ],
          attachInfo: {},
          count: 0,
          path: '/22222',
        },
        {
          id: '859989918220288',
          name: '鑫鑫测试',
          type: 'module',
          parentId: 'NONE',
          projectId: null,
          children: [],
          attachInfo: {},
          count: 0,
          path: '/鑫鑫测试',
        },
        {
          id: '1096900316143616',
          name: 'yuan',
          type: 'module',
          parentId: 'NONE',
          projectId: null,
          children: [
            {
              id: '705714697707520',
              name: '4444',
              type: 'module',
              parentId: '1096900316143616',
              projectId: null,
              children: [],
              attachInfo: {},
              count: 0,
              path: '/yuan/4444',
            },
            {
              id: '706590871035904',
              name: '555',
              type: 'module',
              parentId: '1096900316143616',
              projectId: null,
              children: [],
              attachInfo: {},
              count: 0,
              path: '/yuan/555',
            },
          ],
          attachInfo: {},
          count: 0,
          path: '/yuan',
        },
        {
          id: '674876834054144',
          name: '郭雨琦测试',
          type: 'module',
          parentId: 'NONE',
          projectId: null,
          children: [],
          attachInfo: {},
          count: 0,
          path: '/郭雨琦测试',
        },
      ],
    },
  ]);

  const selectedKeys = ref<Array<string | number>>([]);
  const expandedKeys = ref<Array<string | number>>([]);
  const checkedKeys = ref<Array<string | number>>([]);

  const keyword = ref<string>('');

  const expandAll = ref<boolean>(true);
  function changeExpand() {
    expandAll.value = !expandAll.value;
  }

  function renderPrefixDom(infoProps: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
    const { option } = infoProps;
    if (option.corporation) {
      return h(CrmIcon, {
        type: 'iconicon_product',
        size: 16,
      });
    }
    return null;
  }

  const nodeMoreOptions = ref<ActionsItem[]>([
    {
      label: t('common.rename'),
      key: 'rename',
    },
    {
      label: t('org.addSubDepartment'),
      key: 'addSub',
    },
    {
      label: t('org.setDepartmentHead'),
      key: 'setHead',
    },
    {
      type: 'divider',
    },
    {
      label: t('common.delete'),
      key: 'delete',
      danger: true,
    },
  ]);

  async function handleAdd(option: CrmTreeNodeData) {
    const nodeKey: string = getGenerateId();
    const newNode: CrmTreeNodeData = {
      id: nodeKey,
      name: '未命名',
      children: undefined,
    };
    if (option.children && option.children.length) {
      option.children.unshift(newNode);
    } else {
      option.children = [newNode];
    }
    expandedKeys.value.push(nodeKey);
    try {
      Message.success(t('common.addSuccess'));
    } catch (error) {
      console.log(error);
    }
  }

  const loading = ref<boolean>(false);
  const allNamesList = ref([]);
  function renderExtraDom(infoProps: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
    const { option } = infoProps;
    // 额外的节点
    return h(
      NButton,
      {
        size: 'small',
        class: `crm-suffix-btn h-[24px] h-[24px] !p-[4px] mr-[4px] rounded`,
        onClick: () => handleAdd(option),
      },
      () => {
        return h(CrmIcon, {
          size: 18,
          type: 'iconicon_add',
          class: `text-[var(--primary-8)] hover:text-[var(--primary-8)]`,
        });
      }
    );
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
      console.log(dragNode, dropNode, dropPosition);
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  /**
   * 重命名
   */
  function handleRename(option: CrmTreeNodeData) {}

  /**
   * 添加子部门
   */
  function handleAddSubDepartment() {}

  /**
   * 设置部门负责人
   */
  const showSetHeadModal = ref<boolean>(false);
  function handleSetHead(option: CrmTreeNodeData) {
    showSetHeadModal.value = true;
  }

  /**
   * 删除
   */
  function handleDelete(option: CrmTreeNodeData) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(option.name) }),
      content: option.count ? t('org.deleteExistUserDepartment') : t('org.deleteDepartmentContent'),
      positiveText: option.count ? t('org.ok') : t('common.confirm'),
      negativeText: option.count ? '' : t('common.cancel'),
      positiveButtonProps: {
        type: option.count ? 'primary' : 'error',
        size: 'medium',
      },
      onPositiveClick: async () => {
        try {
          if (!option.count) {
            Message.success(t('common.deleteSuccess'));
          }
        } catch (error) {
          console.log(error);
        }
      },
    });
  }

  function handleFolderMoreSelect(item: ActionsItem, option: CrmTreeNodeData) {
    switch (item.key) {
      case 'rename':
        handleRename(option);
        break;
      case 'addSub':
        handleAddSubDepartment();
        break;
      case 'setHead':
        handleSetHead(option);
        break;
      case 'delete':
        handleDelete(option);
        break;
      default:
        break;
    }
  }
</script>

<style scoped></style>
