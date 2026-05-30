import { onBeforeUnmount, type Ref, ref, watch } from 'vue';

import type { Node } from '@antv/x6';

// 将 X6 Node 的 data 转成 Vue 响应式状态，并自动处理事件绑定/解绑
export default function useX6NodeData<T extends Record<string, unknown>>(nodeRef: Ref<Node | undefined>) {
  const nodeData = ref<T>({} as T);
  let unbindDataChange: (() => void) | null = null;

  // 从当前 node 拉取最新 data（首次渲染和 change:data 后都会调用）
  function syncNodeData() {
    nodeData.value = (nodeRef.value?.getData?.() ?? {}) as T;
  }

  watch(
    nodeRef,
    (node) => {
      // node 实例变化时先解绑旧监听，避免内存泄漏和重复触发
      unbindDataChange?.();
      unbindDataChange = null;

      if (node) {
        const handleDataChange = () => syncNodeData();
        // 监听 X6 数据变更，让组件能响应 selected 等增量更新
        node.on('change:data', handleDataChange);
        unbindDataChange = () => {
          node.off('change:data', handleDataChange);
        };
      }

      syncNodeData();
    },
    { immediate: true }
  );

  // 组件卸载时兜底清理监听
  onBeforeUnmount(() => {
    unbindDataChange?.();
    unbindDataChange = null;
  });

  return {
    nodeData,
  };
}
