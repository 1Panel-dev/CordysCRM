<template>
  <div class="mb-[8px] flex items-center justify-between gap-[8px]">
    <n-input v-model:value="keyword" :placeholder="t('common.searchByName')">
      <template #suffix>
        <n-icon>
          <Search />
        </n-icon>
      </template>
    </n-input>
    <n-tooltip trigger="hover" :delay="300">
      <template #trigger>
        <n-button type="primary" ghost class="n-btn-outline-primary px-[7px]" @click="addDepart">
          <template #icon>
            <n-icon><Add /></n-icon>
          </template>
        </n-button>
      </template>
      {{ t('org.addDepartment') }}
    </n-tooltip>
  </div>
  <CrmTree
    ref="deptTreeRef"
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
    :rename-api="renameHandler"
    @drop="handleDrag"
    @more-action-select="handleFolderMoreSelect"
  />
  <SetDepHeadModal v-model:show="showSetHeadModal" :department-id="departmentId" @close="closeSetCommanderId" />
</template>

<script setup lang="ts">
  import { NButton, NIcon, NInput, NTooltip, useMessage } from 'naive-ui';
  import { Add, Search } from '@vicons/ionicons5';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTree from '@/components/pure/crm-tree/index.vue';
  import type { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import SetDepHeadModal from './setDepHead.vue';

  import {
    addDepartment,
    checkDeleteDepartment,
    deleteDepartment,
    getDepartmentTree,
    renameDepartment,
  } from '@/api/modules/system/org';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import { characterLimit } from '@/utils';

  const { openModal } = useModal();

  const Message = useMessage();

  const { t } = useI18n();

  const emit = defineEmits<{
    (e: 'selectNode', nodeId: string | number): void;
  }>();

  const orgModuleTree = ref<CrmTreeNodeData[]>([]);

  const selectedKeys = ref<Array<string | number>>([]);
  const expandedKeys = ref<Array<string | number>>([]);
  const checkedKeys = ref<Array<string | number>>([]);

  const keyword = ref<string>('');

  const expandAll = ref<boolean>(true);

  const activeNodeId = computed(() => selectedKeys.value[0]);

  function renderPrefixDom(infoProps: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
    const { option } = infoProps;
    if (option.parentId === 'NONE') {
      return h(CrmIcon, {
        type: 'iconicon_control_platform1',
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

  // 获取模块树
  async function initTree(isInit = false) {
    try {
      orgModuleTree.value = await getDepartmentTree();

      if (isInit) {
        selectedKeys.value = orgModuleTree.value[0] ? [orgModuleTree.value[0].id] : [];
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  //  重命名 TODO 接口有问题
  async function renameHandler(option: CrmTreeNodeData) {
    try {
      await renameDepartment({
        id: option.id,
        name: option.name,
      });
      Message.success(t('common.updateSuccess'));
      initTree();
      return Promise.resolve(true);
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
      return Promise.resolve(false);
    }
  }

  const deptTreeRef = ref();
  const currentParentId = ref<string>('');

  // 添加节点
  async function addNode(parent: CrmTreeNodeData | null) {
    currentParentId.value = parent ? parent.id : orgModuleTree.value[0].id;

    try {
      const data = await addDepartment({
        name: t('common.unNamed'),
        parentId: currentParentId.value,
      });

      const newNode: CrmTreeNodeData = {
        id: data.id,
        name: data.name,
        children: undefined,
        hideMoreAction: true,
      };

      if (parent) {
        parent.children = parent.children ?? [];
        parent.children.push(newNode);
      } else {
        orgModuleTree.value[0].children = orgModuleTree.value[0].children ?? [];
        orgModuleTree.value[0].children.push(newNode);
      }

      expandedKeys.value.push(currentParentId.value);

      nextTick(() => {
        deptTreeRef.value?.toggleEdit(data.id);
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
  function addDepart() {
    addNode(null);
  }

  function renderExtraDom(infoProps: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
    const { option } = infoProps;
    // 额外的节点
    return h(
      NButton,
      {
        type: 'primary',
        size: 'small',
        bordered: false,
        class: `crm-suffix-btn !p-[4px] ml-[4px] h-[24px] h-[24px]  mr-[4px] rounded`,
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
   * 设置部门负责人
   */
  const showSetHeadModal = ref<boolean>(false);
  const departmentId = ref<string>('');
  function handleSetHead(option: CrmTreeNodeData) {
    departmentId.value = option.id;
    showSetHeadModal.value = true;
  }

  function closeSetCommanderId() {
    showSetHeadModal.value = false;
    departmentId.value = '';
  }

  /**
   * 删除  TODO 接口有问题
   */
  async function handleDelete(option: CrmTreeNodeData) {
    const isNotAllow = await checkDeleteDepartment(option.id);
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(option.name) }),
      content: !isNotAllow ? t('org.deleteExistUserDepartment') : t('org.deleteDepartmentContent'),
      positiveText: !isNotAllow ? t('org.ok') : t('common.confirm'),
      negativeText: !isNotAllow ? '' : t('common.cancel'),
      positiveButtonProps: {
        type: !isNotAllow ? 'primary' : 'error',
        size: 'medium',
      },
      onPositiveClick: async () => {
        try {
          if (isNotAllow) {
            await deleteDepartment(option.id);
            Message.success(t('common.deleteSuccess'));
            initTree(true);
          }
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  function handleFolderMoreSelect(item: ActionsItem, option: CrmTreeNodeData) {
    switch (item.key) {
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

  watch(
    () => activeNodeId.value,
    (val) => {
      emit('selectNode', val);
    }
  );

  onBeforeMount(() => {
    initTree(true);
  });

  defineExpose({
    initTree,
  });
</script>

<style scoped></style>
