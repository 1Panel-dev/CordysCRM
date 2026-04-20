/** X6 图实例组合：负责 Graph 生命周期、插件与控制方法。 */
import registerFlowNodes from '../graph/registry';
import type { FlowGraphController } from '../graph/types';
import { Graph, Selection } from '@antv/x6';

interface CreateGraphOptions {
  container: HTMLElement;
}

const MIN_SCALE = 0.5;
const MAX_SCALE = 2;

function clampScale(value: number): number {
  if (!Number.isFinite(value)) {
    return 1;
  }
  return Math.min(MAX_SCALE, Math.max(MIN_SCALE, value));
}

export default function useX6Graph() {
  let graph: Graph | null = null;
  let resizeObserver: ResizeObserver | null = null;
  let panMode = false;

  function createGraph(options: CreateGraphOptions): FlowGraphController {
    // 节点类型只需注册一次，重复调用会被内部短路
    registerFlowNodes();

    // 重建前先释放旧资源，避免事件和 observer 泄漏
    resizeObserver?.disconnect();
    resizeObserver = null;
    graph?.dispose();

    graph = new Graph({
      container: options.container,
      grid: {
        visible: true,
        type: 'dot',
        size: 12,
        args: {
          color: getComputedStyle(document.documentElement).getPropertyValue('--text-n8').trim(),
          thickness: 2,
        },
      },
      panning: {
        enabled: false, // 按住画布拖动平移
      },
      mousewheel: {
        enabled: true, // 滚轮缩放行为
        modifiers: ['ctrl', 'meta'], // 必须按住 Ctrl（Win）或 ⌘（Mac）再滚轮才缩放
        minScale: MIN_SCALE,
        maxScale: MAX_SCALE,
      },
      connecting: {
        // 连线规则
        router: 'orth',
        connector: 'rounded', // 拐角圆角
        connectionPoint: 'anchor',
        allowBlank: false, // 不能连到空白处
        allowLoop: false, // 不能自己连自己
        allowNode: false,
        allowEdge: false,
      },
      interacting: {
        nodeMovable: false, // 节点不可拖动
        edgeMovable: false, // 边不可拖动
        magnetConnectable: false, // 禁用从磁铁点拖拽新连线
      },
    });

    graph.use(new Selection({ enabled: false }));
    // graph.use(new Keyboard({ enabled: true })); // 键盘
    // graph.use(new Clipboard({ enabled: true })); // 剪贴板（复制粘贴）
    // graph.use(new History({ enabled: true })); // 历史记录（撤销重做）
    // graph.use(new Snapline({ enabled: false })); // 对齐线，拖动时显示辅助线

    graph.centerContent(); // 画布内容居中显示

    const resizeGraph = () => {
      if (!graph) {
        return;
      }
      const { clientWidth, clientHeight } = options.container;
      if (!clientWidth || !clientHeight) {
        return;
      }
      graph.resize(clientWidth, clientHeight);
    };

    resizeGraph();
    if (typeof ResizeObserver !== 'undefined') {
      resizeObserver = new ResizeObserver(() => {
        resizeGraph();
      });
      resizeObserver.observe(options.container);
    }

    const controller: FlowGraphController = {
      getGraph: () => graph,
      render(cells: unknown[]) {
        if (!graph) {
          return;
        }
        // 用 fromJSON 整体替换 cells，保证 DSL 结果和画布完全一致
        graph.fromJSON({
          cells: cells as any[],
        });
      },
      getZoom() {
        // 获取当前画布缩放
        if (!graph) {
          return 1;
        }
        const { sx } = graph.scale();
        return Number.isFinite(sx) ? sx : 1;
      },
      setZoom(value: number) {
        // 设置缩放
        if (!graph) {
          return;
        }
        const nextScale = clampScale(value);
        graph.zoomTo(nextScale);
      },
      zoomIn() {
        if (!graph) {
          return;
        }
        const nextScale = clampScale((graph.scale().sx ?? 1) + 0.1);
        graph.zoomTo(nextScale);
      },
      zoomOut() {
        if (!graph) {
          return;
        }
        const nextScale = clampScale((graph.scale().sx ?? 1) - 0.1);
        graph.zoomTo(nextScale);
      },
      fitToContent() {
        graph?.zoomToFit({
          padding: 24,
          minScale: MIN_SCALE,
          maxScale: 1,
        });
      },
      centerContent() {
        graph?.centerContent();
      },
      setPanMode(enabled) {
        panMode = enabled;
        if (!graph) {
          return;
        }
        // 仅切换平移能力，不改其它交互配置，避免影响点击选中链路。
        if (enabled) {
          graph.enablePanning();
        } else {
          graph.disablePanning();
        }
      },
      isPanMode() {
        return panMode;
      },
      dispose() {
        resizeObserver?.disconnect();
        resizeObserver = null;
        graph?.dispose();
        graph = null;
        panMode = false;
      },
    };

    return controller;
  }

  return {
    createGraph,
  };
}
