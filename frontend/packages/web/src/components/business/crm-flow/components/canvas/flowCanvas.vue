<template>
  <div ref="flowCanvasRef" class="flow-canvas" :class="{ 'is-pan-mode': isPanMode }">
    <div ref="canvasRef" class="flow-canvas__graph" />

    <!-- 添加节点浮窗 -->
    <AddNodePopover
      :show="addPopoverVisible"
      :position="addPopoverPosition"
      @mouseenter="cancelHidePopoverClose"
      @mouseleave="scheduleClosePopover"
    >
      <template v-if="hasInsertNodeContentSlot" #content>
        <slot name="insertNodeContent" :anchorNodeId="addAnchorNodeId" :anchorBranch="addAnchorBranch" />
      </template>
    </AddNodePopover>

    <!-- 工具栏 -->
    <div class="flow-canvas__toolbar-wrap">
      <FlowCanvasToolbar
        :graph-controller="graphController"
        :is-pan-mode="isPanMode"
        :view-mode="viewMode"
        @toggle-pan-mode="togglePanMode"
        @update-view-mode="handleViewModeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, onBeforeUnmount, onMounted, ref, useSlots, watch } from 'vue';

  import AddNodePopover from './addNodePopover.vue';
  import FlowCanvasToolbar from './flowCanvasToolbar.vue';

  import useX6Graph from '../../composables/useX6Graph';
  import bindFlowGraphEvents from '../../graph/events';
  import transformDslToCells from '../../graph/transform';
  import type {
    AddNodeClickPayload,
    BranchClickPayload,
    FlowGraphController,
    FlowGraphNodeClickPayload,
    FlowGraphNodeData,
    NodeClickPayload,
  } from '../../graph/types';
  import type { FlowSchema, NodeSelectionState } from '../../types';

  defineOptions({
    name: 'FlowCanvas',
  });

  const props = withDefaults(
    defineProps<{
      flow: FlowSchema;
      selection: NodeSelectionState;
    }>(),
    {}
  );
  const slots = useSlots();
  const hasInsertNodeContentSlot = computed(() => Boolean(slots.insertNodeContent));

  const emit = defineEmits<{
    (event: 'nodeClick', payload: NodeClickPayload): void;
    (event: 'branchClick', payload: BranchClickPayload): void;
    (event: 'nodeDelete', payload: { nodeId: string }): void;
    (event: 'branchDelete', payload: { groupId: string; branchId: string }): void;
    (event: 'addConditionBranch', groupId: string): void;
    (event: 'blankClick'): void;
  }>();

  const isPanMode = ref(false);
  const viewMode = ref<'compact' | 'detail'>('detail');

  const addPopoverVisible = ref(false);
  const addPopoverPosition = ref<{ x: number; y: number } | null>(null);
  const addAnchorNodeId = ref<string | null>(null);
  const addAnchorBranch = ref<{ groupId: string; branchId: string } | null>(null);

  let closePopoverTimer: ReturnType<typeof setTimeout> | null = null;

  const flowCanvasRef = ref<HTMLElement | null>(null);

  // 将浏览器坐标转换为画布容器内坐标
  function syncCanvasPosition(position: { x: number; y: number }) {
    if (!flowCanvasRef.value) {
      return null;
    }

    const rect = flowCanvasRef.value.getBoundingClientRect();
    return {
      x: position.x - rect.left,
      y: position.y - rect.top,
    };
  }

  function cancelHidePopoverClose() {
    if (closePopoverTimer) {
      clearTimeout(closePopoverTimer);
      closePopoverTimer = null;
    }
  }

  function openAddPopover(payload: AddNodeClickPayload) {
    if (isPanMode.value || !hasInsertNodeContentSlot.value) {
      return;
    }

    const localPosition = syncCanvasPosition(payload.position);
    if (!localPosition) {
      return;
    }

    if (payload.groupId && payload.branchId) {
      // 条件分支下方的 + 入口
      addAnchorNodeId.value = null;
      addAnchorBranch.value = {
        groupId: payload.groupId,
        branchId: payload.branchId,
      };
    } else {
      // 主链节点下方的 + 入口
      addAnchorNodeId.value = payload.nodeId ?? null;
      addAnchorBranch.value = null;
    }

    addPopoverPosition.value = localPosition;
    addPopoverVisible.value = true;
    cancelHidePopoverClose();
  }

  function closeAddPopover() {
    addPopoverVisible.value = false;
    addPopoverPosition.value = null;
    addAnchorNodeId.value = null;
    addAnchorBranch.value = null;
  }

  function scheduleClosePopover() {
    cancelHidePopoverClose();
    closePopoverTimer = setTimeout(() => {
      closeAddPopover();
    }, 120);
  }

  const graphController = ref<FlowGraphController | null>(null);
  function togglePanMode() {
    isPanMode.value = !isPanMode.value;
    graphController.value?.setPanMode(isPanMode.value);
    if (isPanMode.value) {
      closeAddPopover();
    }
  }

  function resolveSelectedState(data: FlowGraphNodeData): boolean {
    if (data.kind === 'condition-branch') {
      return props.selection.type === 'branch' && data.branchId === props.selection.id;
    }

    if (data.kind === 'start' || data.kind === 'action' || data.kind === 'end') {
      return props.selection.type === 'node' && data.nodeId === props.selection.id;
    }

    return false;
  }

  function renderFlow() {
    if (!graphController.value) {
      return;
    }

    const cells = transformDslToCells(props.flow, {
      cardHeight: viewMode.value === 'compact' ? 58 : 104,
      showNodeDescription: viewMode.value === 'detail',
    });

    const cellsWithSelection = (cells as any[]).map((cell) => {
      if (!cell?.data) {
        return cell;
      }

      const data = cell.data as FlowGraphNodeData;
      return {
        ...cell,
        data: {
          ...data,
          selected: resolveSelectedState(data),
        },
      };
    });

    graphController.value.render(cellsWithSelection);
  }

  function updateRenderedSelectionState() {
    const graph = graphController.value?.getGraph();
    if (!graph) {
      return;
    }

    graph.getNodes().forEach((node: any) => {
      const data = (node.getData?.() ?? null) as FlowGraphNodeData | null;
      if (!data) {
        return;
      }

      const nextSelected = resolveSelectedState(data);
      if (data.selected === nextSelected) {
        return;
      }

      node.setData({
        ...data,
        selected: nextSelected,
      });
    });
  }

  watch(
    () => props.selection,
    () => {
      updateRenderedSelectionState();
    },
    {
      deep: true,
    }
  );

  function handleViewModeChange(mode: 'compact' | 'detail') {
    viewMode.value = mode;
    renderFlow();
  }

  let hasAutoFitted = false;
  const canvasRef = ref<HTMLElement | null>(null);

  function fitAfterInit() {
    if (!graphController.value || !canvasRef.value || hasAutoFitted) {
      return;
    }

    const { clientWidth, clientHeight } = canvasRef.value;
    if (!clientWidth || !clientHeight) {
      return;
    }

    graphController.value.fitToContent();
    hasAutoFitted = true;
  }

  const { createGraph } = useX6Graph();
  let unbindEvents: (() => void) | null = null;

  async function initGraph() {
    if (!canvasRef.value) {
      return;
    }

    await nextTick();

    graphController.value = createGraph({
      container: canvasRef.value,
      resizeTarget: flowCanvasRef.value ?? canvasRef.value,
    });

    const graph = graphController.value.getGraph();
    if (!graph) {
      return;
    }

    unbindEvents = bindFlowGraphEvents(graph, {
      onNodeClick(payload: FlowGraphNodeClickPayload) {
        if (isPanMode.value) {
          return;
        }

        const { data, position } = payload;

        if (data.kind === 'add-node' && position) {
          openAddPopover({
            position,
            nodeId: data.nodeId,
            nodeType: data.nodeType,
            groupId: data.groupId,
            branchId: data.branchId,
          });
          return;
        }

        closeAddPopover();

        if (data.kind === 'add-condition' && data.groupId) {
          emit('addConditionBranch', data.groupId);
          return;
        }

        if (data.kind === 'condition-branch' && data.groupId && data.branchId) {
          emit('branchClick', {
            groupId: data.groupId,
            branchId: data.branchId,
          });
          return;
        }

        if (data.nodeId && data.nodeType) {
          emit('nodeClick', {
            nodeId: data.nodeId,
            nodeType: data.nodeType,
          });
        }
      },
      onNodeDelete({ data }) {
        if (isPanMode.value) {
          return;
        }
        if (data.kind === 'condition-branch' && data.groupId && data.branchId) {
          emit('branchDelete', {
            groupId: data.groupId,
            branchId: data.branchId,
          });
          return;
        }
        if (data.nodeId) {
          emit('nodeDelete', {
            nodeId: data.nodeId,
          });
        }
      },
      onBlankClick() {
        if (isPanMode.value) {
          return;
        }
        closeAddPopover();
        emit('blankClick');
      },
    });

    renderFlow();
    fitAfterInit();
  }

  watch(
    () => props.flow,
    () => {
      renderFlow();
      if (addPopoverVisible.value) {
        closeAddPopover();
      }
    },
    {
      deep: true,
    }
  );

  onMounted(() => {
    initGraph();
  });

  onBeforeUnmount(() => {
    cancelHidePopoverClose();
    unbindEvents?.();
    unbindEvents = null;
    graphController.value?.dispose();
    hasAutoFitted = false;
    graphController.value = null;
    closeAddPopover();
  });
</script>

<style scoped lang="less">
  .flow-canvas {
    position: relative;
    width: 100%;
    height: 100%;
    .flow-canvas__graph {
      width: 100%;
      height: 100%;
      :deep(.x6-graph-scroller),
      :deep(.x6-graph),
      :deep(.x6-graph-svg) {
        width: 100%;
        height: 100%;
      }
      :deep(.x6-graph-scroller) {
        overflow: hidden !important;
        scrollbar-width: none;
        &::-webkit-scrollbar {
          display: none;
        }
      }
      :deep(.x6-graph-scroller::-webkit-scrollbar) {
        width: 0;
        height: 0;
      }
    }
    &.is-pan-mode {
      cursor: grab;
      :deep(.x6-node),
      :deep(.x6-node *),
      :deep(.x6-edge),
      :deep(.x6-edge *),
      :deep(.x6-graph-svg) {
        cursor: grab !important;
      }
      // 节点 hover 不再高亮边框
      :deep(.base-flow-node:not(.is-selected):hover) {
        border-color: transparent !important;
      }
      // 拖拽模式禁用删除图标 hover 与 tooltip 触发
      :deep(.base-flow-node__header-extra) {
        pointer-events: none !important;
        color: var(--text-n4) !important;
      }
      :deep(.add-node__icon:hover) {
        color: var(--primary-4) !important;
      }
      &:active {
        cursor: grabbing;
        :deep(.x6-node),
        :deep(.x6-node *),
        :deep(.x6-edge),
        :deep(.x6-edge *),
        :deep(.x6-graph-svg) {
          cursor: grabbing !important;
        }
      }
    }
    .flow-canvas__toolbar-wrap {
      position: absolute;
      bottom: 16px;
      left: 50%;
      z-index: 15;
      transform: translateX(-50%);
    }
  }
</style>
