<template>
  <div class="p-[24px]">
    <CrmCard title="按钮类型demo" sub-title="subTitle" no-content-padding>
      <div class="flex">
        <div class="flex flex-1 flex-col gap-4 p-4">
          <!-- TODO 按钮类型demo 暂时提供参考 you can delete it  ^_^  -->
          <div class="flex gap-[16px]">
            <NButton type="primary">主要按钮</NButton>
            <NButton type="tertiary">次要按钮</NButton>
            <NButton type="default" class="outline--secondary">次要外框按钮</NButton>
            <NButton strong secondary>次要背景按钮</NButton>
            <NButton type="success"> 成功背景按钮 </NButton>
            <NButton type="warning"> 警告背景按钮 </NButton>
            <NButton type="error"> 错误背景按钮 </NButton>
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
            <n-button type="info" ghost class="n-btn-outline-info" @click="info"> Info </n-button>
            <n-button type="success" ghost class="n-btn-outline-success" @click="success"> Success </n-button>
            <n-button type="warning" ghost class="n-btn-outline-warning" @click="warning"> Warning </n-button>
            <n-button type="error" ghost class="n-btn-outline-error" @click="error"> Error </n-button>
          </div>
          <div class="mt-[24px] flex gap-[16px]">
            <n-button type="primary" ghost @click="loading"> Primary </n-button>
            <n-button type="info" ghost @click="notify"> Info </n-button>
            <n-button type="success" ghost @click="dialogHandler"> Success </n-button>
            <n-button type="warning" ghost> Warning </n-button>
            <n-button type="error" ghost> Error </n-button>
          </div>
          <div class="flex flex-1 items-start">
            <CrmMoreAction :options="moreOptions" />
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
              :virtual-scroll-props="{ virtualScroll: false, virtualScrollHeight: '400px' }"
              @rename="renameHandler"
              @create="createHandler"
              @drop="handleDrag"
            />
          </div>
        </div>
      </div>
    </CrmCard>

    <TableDemo class="my-[16px]" />
  </div>
</template>

<script setup lang="ts">
  import { NButton, NInput, NTag } from 'naive-ui';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTree from '@/components/pure/crm-tree/index.vue';
  import type { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import TableDemo from './TableDemo.vue';

  import useDiscreteApi from '@/hooks/useDiscreteApi';
  // 暂时提供参考 you can delete it  ^_^
  const { message, notification, dialog } = useDiscreteApi();

  function info() {
    message.info("I don't know why nobody told you how to unfold your love", {
      keepAliveOnHover: true,
    });
  }
  function error() {
    message.error('Once upon a time you dressed so fine', {
      duration: 100000,
    });
  }
  function warning() {
    message.warning('How many roads must a man walk down');
  }
  function success() {
    message.success("'Cause you walked hand in hand With another man in my place");
  }
  function loading() {
    message.loading('If I were you, I will realize that I love you more than any other guy');
  }

  function notify() {
    notification.info({
      title: 'hahahahha',
      content: `（一般是）从浏览器顶部降下来的神谕。`,
      duration: 103000,
    });
  }

  function dialogHandler() {
    const d = dialog.success({
      title: '想说点啥',
      content: '但是好像也没有说的,怎么样都是要离开的',
      positiveText: '离开',
      negativeText: '算了',
      onPositiveClick: async () => {
        d.loading = true;
        try {
          await new Promise((resolve) => {
            setTimeout(resolve, 2000);
          });
          message.success('删除成功');
          d.loading = false;
        } catch (e) {
          message.error('删除失败，请重试');
        }
      },
      onNegativeClick: () => {
        message.error('不确定');
      },
    });
  }

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
                  name: '模块88',
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
