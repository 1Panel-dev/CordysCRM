import type { BarSeriesOption, EChartsOption, LineSeriesOption } from 'echarts';
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components';
import * as echarts from 'echarts/core';
import { SVGRenderer } from 'echarts/renderers';

import { lightColors } from '@/components/pure/crm-chart/useChart';

// AI 助手目前会在 Markdown 中输出 Mermaid xychart-beta / pie 来表达简单图表。
// Mermaid 的图表样式可控性比较弱，中文报表里容易出现字号、留白、图例观感不佳的问题。
// 这里把有限 Mermaid 图表语法转成 ECharts，只用于 AI Chat Markdown 占位渲染，不影响普通 Mermaid 图。
interface AiChartDataItem {
  name: string;
  value: number;
}

interface AiXyChartSeries {
  name: string;
  type: 'bar' | 'line';
  data: number[];
}

interface ParsedMermaidXyChart {
  kind: 'xy';
  title: string;
  indicatorName: string;
  xData: string[];
  series: AiXyChartSeries[];
}

interface ParsedMermaidPieChart {
  kind: 'pie';
  title: string;
  data: AiChartDataItem[];
}

type ParsedAiChart = ParsedMermaidXyChart | ParsedMermaidPieChart;

const chartComponents = [
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
  SVGRenderer,
];
let isAiChartInitialized = false;
const chartResizeObservers = new WeakMap<HTMLElement, ResizeObserver>();

// ECharts 按需引入的组件需要先 use 注册。
// 聊天消息里可能有多个 Markdown block，也可能因流式输出多次触发渲染，所以这里做一次性注册保护。
export function initializeAiCharts(): void {
  if (isAiChartInitialized) {
    return;
  }

  echarts.use(chartComponents);
  isAiChartInitialized = true;
}

function parseJsonArray<T>(content: string | undefined): T[] {
  if (!content) {
    return [];
  }

  try {
    const parsed = JSON.parse(content);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function parseNumberArray(content: string | undefined): number[] {
  return parseJsonArray<number>(content)
    .map((item) => Number(item))
    .filter((item) => Number.isFinite(item));
}

// 这里只解析后端当前会返回的 xychart-beta 形态，并允许多条 bar / line：
// title "标题"
// x-axis ["分类A", "分类B"]
// y-axis "指标名" 0 --> 10
// bar [1, 2]
// line [2, 3]
// 如果后续要支持多 series、转置图、复杂 axis 配置，优先扩展这里
function parseMermaidXyChart(source: string, defaultIndicatorName: string): ParsedMermaidXyChart | undefined {
  if (!source.trimStart().startsWith('xychart-beta')) {
    return undefined;
  }

  const title = source.match(/^\s*title\s+"([^"]+)"/m)?.[1] ?? '';
  const indicatorName = source.match(/^\s*y-axis\s+"([^"]+)"/m)?.[1] ?? defaultIndicatorName;
  const xData = parseJsonArray<string>(source.match(/^\s*x-axis\s+(\[[^\n]+\])/m)?.[1]).map(String);
  const series = [...source.matchAll(/^\s*(bar|line)\s+(\[[^\n]+\])/gm)]
    .map((match, index) => {
      const type = match[1] as 'bar' | 'line';
      const data = parseNumberArray(match[2]);

      return {
        name: index === 0 ? indicatorName : `${indicatorName}${index + 1}`,
        type,
        data,
      };
    })
    .filter((item) => item.data.length > 0);

  if (xData.length === 0 || series.length === 0) {
    return undefined;
  }

  return {
    kind: 'xy',
    title,
    indicatorName,
    xData,
    series,
  };
}

// Mermaid pie 常见形态：
// pie title 阶段占比
//   "线索" : 10
//   "商机" : 2
// 这里只取标题和 name/value，showData 等 Mermaid 展示参数交给 ECharts tooltip/legend 统一处理。
function parseMermaidPieChart(source: string): ParsedMermaidPieChart | undefined {
  const normalizedSource = source.trimStart();

  if (!normalizedSource.startsWith('pie')) {
    return undefined;
  }

  const lines = normalizedSource.split('\n');
  const title =
    lines[0]
      .match(/^pie(?:\s+showData)?\s+title\s+(.+)$/)?.[1]
      ?.trim()
      .replace(/^"|"$/g, '') ??
    normalizedSource.match(/^\s*title\s+"?([^"\n]+)"?/m)?.[1]?.trim() ??
    '';
  const data = [...normalizedSource.matchAll(/^\s*"([^"]+)"\s*:\s*(-?\d+(?:\.\d+)?)\s*$/gm)].map((match) => ({
    name: match[1],
    value: Number(match[2]),
  }));

  if (data.length === 0) {
    return undefined;
  }

  return {
    kind: 'pie',
    title,
    data,
  };
}

function parseAiChart(source: string, defaultIndicatorName: string): ParsedAiChart | undefined {
  return parseMermaidXyChart(source, defaultIndicatorName) ?? parseMermaidPieChart(source);
}

function getThemeColor(name: string, fallback: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
}

function getXyChartSeries(series: AiXyChartSeries, textColor: string): BarSeriesOption | LineSeriesOption {
  if (series.type === 'bar') {
    return {
      name: series.name,
      type: 'bar',
      data: series.data,
      barMaxWidth: 28,
      label: {
        show: true,
        position: 'top',
        color: textColor,
        formatter: (params: any) => Number(params.value).toLocaleString('en-US'),
      },
      itemStyle: {
        borderRadius: [2, 2, 0, 0],
      },
    };
  }

  return {
    name: series.name,
    type: 'line',
    data: series.data,
    smooth: true,
  };
}

// 保持 Mermaid 原始图表语义：bar 固定竖向柱状图，line 固定折线图，pie 固定饼图。
// 这里只做视觉样式统一，不根据数据特征自动改变图表类型，避免和模型输出意图不一致。
function getAiChartOption(chart: ParsedAiChart): EChartsOption {
  const textColor = getThemeColor('--text-n1', '#1f2329');
  const secondaryTextColor = getThemeColor('--text-n4', '#8f959e');
  const splitLineColor = getThemeColor('--text-n8', '#eef0f3');
  const axisLabel = {
    color: secondaryTextColor,
    fontSize: 12,
  };

  if (chart.kind === 'pie') {
    return {
      color: lightColors,
      title: {
        text: chart.title,
        textAlign: 'left',
        left: '1%',
        top: '2%',
        textStyle: {
          color: textColor,
          fontSize: 14,
        },
      },
      tooltip: {
        trigger: 'item',
        borderWidth: 0,
        formatter: '{b}: {c} ({d}%)',
      },
      legend: {
        type: 'scroll',
        orient: 'vertical',
        left: '37%',
        top: 'middle',
        right: 0,
        itemGap: 16,
        itemWidth: 8,
        itemHeight: 8,
        itemStyle: {
          borderRadius: 2,
        },
        height: '80%',
        textStyle: {
          color: secondaryTextColor,
        },
      },
      series: [
        {
          type: 'pie',
          name: chart.title,
          radius: '40%',
          center: ['13%', '50%'],
          data: chart.data,
          avoidLabelOverlap: true,
          label: {
            show: false,
          },
          labelLine: {
            show: false,
          },
        },
      ],
    };
  }

  return {
    color: lightColors,
    title: {
      text: chart.title,
      textAlign: 'left',
      left: '1%',
      top: '2%',
      textStyle: {
        color: textColor,
        fontSize: 14,
      },
    },
    tooltip: {
      trigger: 'axis',
      borderWidth: 0,
      valueFormatter: (value) => Number(value).toLocaleString('en-US'),
    },
    grid: {
      top: '70px',
      right: '2%',
      bottom: chart.xData.length >= 30 ? '10%' : '3%',
      left: '2%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: chart.xData,
      axisLabel: {
        ...axisLabel,
        rotate: 45,
      },
      axisTick: {
        alignWithLabel: true,
      },
      axisLine: {
        lineStyle: {
          color: splitLineColor,
        },
      },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel,
      splitLine: {
        lineStyle: {
          type: 'dashed',
        },
      },
    },
    series: chart.series.map((series) => getXyChartSeries(series, textColor)),
  };
}

function disposeChart(renderTarget: HTMLElement): void {
  chartResizeObservers.get(renderTarget)?.disconnect();
  chartResizeObservers.delete(renderTarget);
  echarts.getInstanceByDom(renderTarget)?.dispose();
}

function observeChartResize(renderTarget: HTMLElement, chartInstance: echarts.ECharts): void {
  const resizeObserver = new ResizeObserver(() => {
    chartInstance.resize();
  });

  resizeObserver.observe(renderTarget);
  chartResizeObservers.set(renderTarget, resizeObserver);
}

export function renderAiChartBlock(block: HTMLElement, defaultIndicatorName: string): void {
  const source = block.querySelector<HTMLElement>('.ai-chart__source')?.textContent?.trim();
  const renderTarget = block.querySelector<HTMLElement>('.ai-chart__render');

  // data-rendered 记录已经渲染过的源码，避免流式 watch 重复 setOption。
  if (!source || !renderTarget || block.dataset.rendered === source) {
    return;
  }

  const chart = parseAiChart(source, defaultIndicatorName);

  if (!chart) {
    disposeChart(renderTarget);
    block.classList.add('ai-chart--error');
    return;
  }

  // 流式输出会反复替换 v-html 内容，旧 ECharts 实例如果继续绑定旧 DOM，容易出现空白或尺寸错误。
  // 所以同一个容器按最新源码先 dispose 再 init，保证实例和当前 DOM 一一对应。
  disposeChart(renderTarget);
  const nextChartInstance = echarts.init(renderTarget, undefined, { renderer: 'svg' });
  nextChartInstance.setOption(getAiChartOption(chart));
  observeChartResize(renderTarget, nextChartInstance);
  window.setTimeout(() => nextChartInstance.resize(), 0);
  block.dataset.rendered = source;
  block.classList.remove('ai-chart--error');
}

export function disposeAiChartBlock(renderTarget: HTMLElement): void {
  disposeChart(renderTarget);
}
