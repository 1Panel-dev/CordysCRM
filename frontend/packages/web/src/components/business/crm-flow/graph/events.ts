/** 图事件适配：把 X6 原生事件转换成业务事件回调 */
import type { FlowGraphEventHandlers, FlowGraphNodeData } from './types';
import type { Graph } from '@antv/x6';

export default function bindFlowGraphEvents(graph: Graph, handlers: FlowGraphEventHandlers): () => void {
  const onNodeClick = ({ node, e }: any) => {
    // X6 画布事件统一在 graph 层监听（node:click），事件先被 X6 捕获并上抛
    const data = (node.getData?.() ?? {}) as FlowGraphNodeData;
    // 是否命中删除图标
    const isDeleteIconClick = Boolean((e?.target as HTMLElement | null)?.closest?.('.base-flow-node__delete-icon'));
    if (isDeleteIconClick) {
      handlers.onNodeDelete?.({ data });
      return;
    }
    const bbox = node.getBBox?.();
    const clientPoint = bbox ? graph.localToClient(bbox.x + bbox.width / 2, bbox.y + bbox.height / 2) : null;

    handlers.onNodeClick?.({
      data,
      position: clientPoint
        ? {
            x: clientPoint.x,
            y: clientPoint.y,
          }
        : null,
    });
  };

  const onBlankClick = () => {
    // 点击空白区域通常用于清空选中态。
    handlers.onBlankClick?.();
  };

  graph.on('node:click', onNodeClick);
  graph.on('blank:click', onBlankClick);

  return () => {
    graph.off('node:click', onNodeClick);
    graph.off('blank:click', onBlankClick);
  };
}
