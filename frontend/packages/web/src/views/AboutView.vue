<template>
  <div class="p-[24px]">
    <CrmCard title="按钮类型demo" sub-title="subTitle" no-content-padding>
      <div class="flex">
        <div class="flex flex-1 flex-col gap-4 p-4">
          <!-- TODO 按钮类型demo 暂时提供参考 you can delete it  ^_^  -->
          <div class="flex gap-[16px]">
            <n-button type="primary">主要按钮</n-button>
            <n-button type="tertiary">次要按钮</n-button>
            <n-button type="default" class="outline--secondary">次要外框按钮</n-button>
            <n-button strong secondary>次要背景按钮</n-button>
            <n-button type="success"> 成功背景按钮 </n-button>
            <n-button type="warning"> 警告背景按钮 </n-button>
            <n-button type="error"> 错误背景按钮 </n-button>
          </div>
          <div class="mt-[24px] flex gap-[16px]">
            <n-button quaternary class="text-btn-secondary"> 文本次要按钮 </n-button>
            <n-button quaternary type="primary" class="text-btn-primary"> 文本主题按钮 </n-button>
            <n-button quaternary type="info" class="text-btn-info"> 文本info按钮 </n-button>
            <n-button quaternary type="success" class="text-btn-success"> 文本success按钮 </n-button>
            <n-button quaternary type="warning" class="text-btn-warning"> 文本warning按钮 </n-button>
            <n-button quaternary type="error" class="text-btn-error"> 文本Error按钮 </n-button>
          </div>
          <div class="mt-[24px] flex gap-[16px]">
            <n-button type="primary" ghost> Primary </n-button>
            <n-button type="info" ghost class="n-btn-outline-info"> Info </n-button>
            <n-button type="success" ghost class="n-btn-outline-success"> Success </n-button>
            <n-button type="warning" ghost class="n-btn-outline-warning"> Warning </n-button>
            <n-button type="error" ghost class="n-btn-outline-error"> Error </n-button>
          </div>
          <div class="mt-[24px] flex gap-[16px]">
            <n-button type="primary" ghost> Primary </n-button>
            <n-button type="info" ghost> Info </n-button>
            <n-button type="success" ghost> Success </n-button>
            <n-button type="warning" ghost @click="openCrmModal"> Warning 打开弹窗 </n-button>
            <n-button type="error" ghost @click="handleDialog"> Error </n-button>
          </div>
          <div class="flex flex-1 flex-col gap-[8px]">
            <div class="flex items-center gap-2">
              <CrmTag size="small"> 标签 </CrmTag>
              <CrmTag size="small" type="primary"> 标签 </CrmTag>
              <CrmTag size="small" type="success"> 标签 </CrmTag>
              <CrmTag size="small" type="warning"> 标签 </CrmTag>
              <CrmTag size="small" type="error"> 标签 </CrmTag>
              <CrmTag size="small" type="info"> 标签 </CrmTag>
            </div>
            <div class="flex items-center gap-2">
              <CrmTag theme="light"> 标签 </CrmTag>
              <CrmTag theme="light" type="primary"> 标签 </CrmTag>
              <CrmTag theme="light" type="success"> 标签 </CrmTag>
              <CrmTag theme="light" type="warning"> 标签 </CrmTag>
              <CrmTag theme="light" type="error"> 标签 </CrmTag>
              <CrmTag theme="light" type="info"> 标签 </CrmTag>
            </div>
            <div class="flex items-center gap-2">
              <CrmTag theme="outline"> 标签 </CrmTag>
              <CrmTag theme="outline" type="primary"> 标签 </CrmTag>
              <CrmTag theme="outline" type="success"> 标签 </CrmTag>
              <CrmTag theme="outline" type="warning"> 标签 </CrmTag>
              <CrmTag theme="outline" type="error"> 标签 </CrmTag>
              <CrmTag theme="outline" type="info"> 标签 </CrmTag>
            </div>
            <div class="flex items-center gap-2">
              <CrmTag size="large" theme="lightOutLine"> 标签 </CrmTag>
              <CrmTag size="large" theme="lightOutLine" type="primary"> 标签 </CrmTag>
              <CrmTag size="large" theme="lightOutLine" type="success"> 标签 </CrmTag>
              <CrmTag size="large" theme="lightOutLine" type="warning"> 标签 </CrmTag>
              <CrmTag size="large" theme="lightOutLine" type="error"> 标签 </CrmTag>
              <CrmTag size="large" theme="lightOutLine" type="info">
                标签
                <template #tooltipContent>自定义</template>
              </CrmTag>
              <CrmRemoveButton
                :loading="deleteLoading"
                title="确定移除 小城与不确定性的墙 吗？"
                content="移除后，成员将不再拥有该角色权限"
                @confirm="confirm"
              />
            </div>
            <div>
              <CrmUpload
                v-model:file-list="fileList"
                :is-all-screen="true"
                :show-file-list="true"
                accept="excel"
                @change="changeHandler"
              />
            </div>
          </div>
        </div>
        <div class="flex-1">
          <div class="w-[800px] overflow-auto">
            <div class="flex items-center justify-between gap-2">
              <NInput v-model:value="keyword" />
              <n-button type="primary" ghost @click="changeExpand"> change</n-button>
            </div>
            <CrmTree
              v-model:data="moduleTree"
              v-model:selected-keys="selectedKeys"
              v-model:checked-keys="checkedKeys"
              v-model:expanded-keys="expandedKeys"
              v-model:default-expand-all="expandAll"
              checkable
              draggable
              :keyword="keyword"
              :render-prefix="renderPrefixDom"
              :node-more-actions="moreOptions"
              :render-extra="renderExtraDom"
              :render-label="renderLabel"
              :virtual-scroll-props="{ virtualScroll: false, virtualScrollHeight: '400px' }"
              @rename="renameHandler"
              @create="createHandler"
              @drop="handleDrag"
              @more-action-select="handleFolderMoreSelect"
            />
          </div>
        </div>
      </div>

      <CrmModal
        v-model:show="showModal"
        :title="'我是标题我是标题我是标题我是标题我是标题我是标题我是标题我是标题我是标题我是标题我是标题我是标题我是标题我是标题我是标题我是标题我是标题'"
        :ok-loading="loading"
      >
        <div>
          <n-form ref="formRef" :model="modelRef">
            <n-form-item path="age" label="年龄">
              <n-input v-model:value="modelRef.age" @keydown.enter.prevent />
            </n-form-item>
            <n-form-item path="password" label="密码">
              <n-input v-model:value="modelRef.password" />
            </n-form-item>
            <n-form-item ref="rPasswordFormItemRef" first path="reenteredPassword" label="重复密码">
              <n-input v-model:value="modelRef.reenteredPassword" type="password" @keydown.enter.prevent />
            </n-form-item>
          </n-form>
        </div>
      </CrmModal>
    </CrmCard>

    <TableDemo class="my-[16px]" />

    <CrmCard>
      <CrmTab v-model:active-tab="activeTab" :tab-list="tabList" type="line" />
      <CrmTab v-model:active-tab="activeTab" :tab-list="tabList" type="segment" size="small" />
      <CrmTab v-model:active-tab="activeTab" :tab-list="tabList" type="segment" />
      <CrmTab v-model:active-tab="activeTab" :tab-list="tabList" type="segment" size="large" />
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import { NButton, NForm, NFormItem, NInput, NTag, TabPaneProps, useMessage } from 'naive-ui';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmRemoveButton from '@/components/pure/crm-remove-button/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmTree from '@/components/pure/crm-tree/index.vue';
  import type { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import CrmUpload from '@/components/pure/crm-upload/index.vue';
  import type { CrmFileItem } from '@/components/pure/crm-upload/types';
  import TableDemo from './TableDemo.vue';

  import useModal from '@/hooks/useModal';

  const message = useMessage();

  const { openModal } = useModal();
  // 暂时提供参考 you can delete it  ^_^

  const activeTab = ref('1');
  const tabList = ref<TabPaneProps[]>([
    {
      name: '1',
      tab: '界面设置',
    },
    {
      name: '2',
      tab: '扫码登录',
    },
    {
      name: '3',
      tab: '邮件设置',
    },
  ]);

  const moduleTree = ref<CrmTreeNodeData[]>([
    {
      id: 'root',
      name: '未规划用例',
      type: 'MODULE',
      parentId: 'NONE',
      projectId: null,
      children: undefined,
      attachInfo: {},
      count: 0,
      hideMoreAction: true,
      path: '/未规划用例',
    },
    {
      id: '849389914030080',
      name: '测试默默',
      type: 'module',
      parentId: 'NONE',
      projectId: null,
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
      count: 0,
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
  ]);

  const moreOptions = ref<ActionsItem[]>([
    {
      label: '重命名',
      key: 'rename', // 事件标识
    },
    {
      label: '复制',
      key: 'copy', // 事件标识
    },
    {
      type: 'divider',
    },
    {
      label: '删除',
      key: 'delete', // 事件标识
      danger: true,
    },
  ]);

  const selectedKeys = ref([]);
  const expandedKeys = ref([]);

  const checkedKeys = ref([]);

  const expandAll = ref(false);

  function changeExpand() {
    expandAll.value = !expandAll.value;
  }

  function renameHandler(option: any, newName: string, done: (suc: boolean) => void) {
    try {
      setTimeout(() => {
        console.log('更新成功');
        done(true);
      }, 3000);
    } catch (e) {
      console.log(e);
    }
  }
  function createHandler(option: any, newName: string, done: (suc: boolean) => void) {
    try {
      setTimeout(() => {
        console.log('创建成功');
        done(true);
      }, 3000);
    } catch (e) {
      console.log(e);
    }
  }

  function renderPrefixDom(infoProps: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
    const { option } = infoProps;
    return h(
      NTag,
      {
        type: 'error',
      },
      {
        default: () => `${option.id === 'root' ? '体重并没有减轻' : '一月份的悲伤'}`,
      }
    );
  }

  function renderLabel(info: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
    return h(
      'div',
      {
        class: 'inline-flex w-full gap-[8px]',
      },
      {
        default: () => {
          return [
            h(
              'div',
              {
                class: 'one-line-text w-full',
              },
              info.option.name
            ),
            h(
              'div',
              {
                class: 'crm-tree-node-count ml-[4px]',
              },
              200
            ),
          ];
        },
      }
    );
  }

  function handleClick() {}

  function renderExtraDom(infoProps: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
    // 额外的节点
    return h(
      NButton,
      {
        size: 'small',
        class: `crm-suffix-btn h-[24px] h-[24px] !p-[4px] mr-[4px] rounded`,
        onClick: () => handleClick(),
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

  function handleFolderMoreSelect(item: ActionsItem, option: CrmTreeNodeData) {}

  const keyword = ref('');

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

  const modelRef = ref({
    age: null,
    password: null,
    reenteredPassword: null,
  });

  function handleDialog() {
    openModal({
      type: 'default',
      title: '想说点啥',
      content: '但是好像也没有说的,怎么样都是要离开的',
      positiveText: '离开',
      negativeText: '算了',
      onPositiveClick: async () => {
        try {
          await new Promise((resolve) => {
            setTimeout(resolve, 3000);
          });
          Promise.resolve(false);
          message.success('删除成功');
        } catch (e) {
          message.error('删除失败，请重试');
        }
      },
    });
  }

  const showModal = ref(false);
  const loading = ref(false);
  function openCrmModal() {
    showModal.value = true;
  }

  const deleteLoading = ref(false);
  async function confirm(cancel: () => void) {
    deleteLoading.value = true;
    try {
      await new Promise((resolve) => {
        setTimeout(resolve, 3000);
      });
      deleteLoading.value = false;
      cancel();
      message.success('删除成功');
    } catch (e) {
      message.error('删除失败，请重试');
    }
  }

  const fileList = ref<CrmFileItem[]>([]);

  function changeHandler(file: CrmFileItem[], _fileList: CrmFileItem) {}
</script>

<style>
  @media (min-width: 1024px) {
    .about {
      display: flex;
      align-items: center;
      min-height: 100vh;
    }
  }
</style>
