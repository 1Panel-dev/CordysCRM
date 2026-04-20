/** 图转换：将布局结果转换为 X6 可渲染的数据 */
import { useI18n } from '@lib/shared/hooks/useI18n';

import type { FlowSchema } from '../types';
import type { FlowLayoutOptions } from './layout';
import buildFlowLayout from './layout';

const CUSTOM_COMPONENT_NODE_SHAPES = new Set([
  'flow-start-node',
  'flow-action-node',
  'flow-condition-branch-node',
  'flow-end-node',
  'flow-add-node',
]);

export default function transformDslToCells(
  flow: FlowSchema,
  layoutOptions: Partial<FlowLayoutOptions> = {}
): unknown[] {
  const { t } = useI18n();

  // 先做 DSL -> 布局计算，再做布局 -> X6 cells 转换
  const { nodes, edges } = buildFlowLayout(flow, layoutOptions);
  const nodeMap = new Map(
    nodes.map((node) => [
      node.id,
      {
        centerX: node.x + node.width / 2,
      },
    ])
  );

  const nodeCells = nodes.map((item) => {
    // add-node 置顶，避免被连线或普通节点遮挡
    const zIndex = (() => {
      if (item.data.kind === 'add-node') {
        return 40;
      }
      if (item.data.kind === 'add-condition') {
        return 30;
      }
      return 20;
    })();

    const baseNode = {
      id: item.id,
      shape: item.shape,
      x: item.x,
      y: item.y,
      width: item.width,
      height: item.height,
      data: item.data,
      zIndex,
    };

    if (CUSTOM_COMPONENT_NODE_SHAPES.has(item.shape)) {
      return baseNode;
    }

    if (item.shape === 'flow-add-condition-node') {
      return {
        ...baseNode,
        // “添加条件”的样式
        attrs: {
          label: {
            text: `+ ${t('advanceFilter.addCondition')}`,
            fill: 'var(--primary-8)',
            fontSize: 14,
            cursor: 'pointer',
          },
          body: {
            fill: '#ffffff',
            strokeWidth: 0,
            rx: 4,
            ry: 4,
            cursor: 'pointer',
            filter: {
              name: 'dropShadow',
              args: {
                dx: 0,
                dy: 4,
                blur: 10,
                color: 'rgba(100, 103, 103, 0.15)',
              },
            },
          },
        },
      };
    }

    return {
      ...baseNode,
    };
  });

  const edgeCells = edges.map((item, index) => {
    // 起点是否为“绝对坐标点”（而非节点 id）。
    const sourceIsPoint = typeof item.source !== 'string';
    // 终点是否为“绝对坐标点”（而非节点 id）。
    const targetIsPoint = typeof item.target !== 'string';
    // 起终点都为坐标点时，通常不需要再走正交路由。
    const bothEndpointArePoints = sourceIsPoint && targetIsPoint;
    // 统一计算起点中心 x：节点来源取节点中心，坐标来源取自身 x。
    const sourceCenterX = typeof item.source === 'string' ? nodeMap.get(item.source)?.centerX ?? 0 : item.source.x;
    // 统一计算终点中心 x：节点来源取节点中心，坐标来源取自身 x。
    const targetCenterX = typeof item.target === 'string' ? nodeMap.get(item.target)?.centerX ?? 0 : item.target.x;
    // 判断是否近似垂直对齐（允许 1px 误差）。
    const isVerticalAligned = Math.abs(sourceCenterX - targetCenterX) <= 1;
    // 根据连线场景动态选择路由器：normal（直连）或 orth（正交）。
    const routerConfig = (() => {
      // 已有 vertices、点到点、任一端是点、或基本垂直对齐时，使用 normal 避免多余折线。
      if (item.vertices || isVerticalAligned || bothEndpointArePoints || sourceIsPoint || targetIsPoint) {
        return { name: 'normal' as const };
      }
      // 其它常规节点到节点场景使用正交路由。
      return { name: 'orth' as const };
    })();

    return {
      id: item.id ?? `edge_${index}`,
      shape: 'edge', // 形状
      zIndex: 10, // 层级：放在节点下方，避免遮挡 add-node 等交互点
      // source：字符串时按“节点 + 底部锚点”连接；坐标时按点连接
      source:
        typeof item.source === 'string'
          ? {
              cell: item.source,
              anchor: 'bottom',
            }
          : {
              x: item.source.x,
              y: item.source.y,
            },
      // target：字符串时按“节点 + 顶部锚点”连接；坐标时按点连接
      target:
        typeof item.target === 'string'
          ? {
              cell: item.target,
              anchor: 'top',
            }
          : {
              x: item.target.x,
              y: item.target.y,
            },
      vertices: item.vertices, // 可选中间拐点（由布局层提供）
      router: routerConfig, // 路由策略（normal / orth）
      connector: {
        name: 'normal', // 连接器：normal 保持折线路径稳定，不做额外平滑变形
      },
      // 线条视觉样式
      attrs: {
        line: {
          // 连线颜色。
          stroke: '#D9D9D9',
          // 连线粗细。
          strokeWidth: 2,
          // 终点箭头配置
          targetMarker: targetIsPoint
            ? null
            : {
                name: 'block', // 箭头样式：实心块状
                width: 8, // 箭头宽度
                height: 14, // 箭头高度
                fill: 'var(--text-n4)',
                stroke: 'var(--text-n4)',
                strokeLinejoin: 'round',
              },
        },
      },
    };
  });

  return [...nodeCells, ...edgeCells];
}
