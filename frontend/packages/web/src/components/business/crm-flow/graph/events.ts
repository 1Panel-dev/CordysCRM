/** 图事件适配：把 X6 原生事件转换成业务事件回调 */
import type { FlowGraphEventHandlers, FlowGraphNodeData } from './types';
import type { Graph } from '@antv/x6';

export default function bindFlowGraphEvents(graph: Graph, handlers: FlowGraphEventHandlers): () => void {
  const onNodeClick = ({ node }: any) => {
    const data = (node.getData?.() ?? {}) as FlowGraphNodeData;
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
