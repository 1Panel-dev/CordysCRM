import { render } from 'vue';
import { debounce } from 'lodash-es';
import type { EChartsOption, LegendComponentOption, PieSeriesOption } from 'echarts';
import * as echarts from 'echarts/core';

import { useI18n } from '@lib/shared/hooks/useI18n';
import { abbreviateNumber, deepMerge } from '@lib/shared/method';

import tooltip from './components/tooltip.vue';

import { ChartTypeEnum } from './type';

export interface ChartProps {
  id: string;
  components: any[];
  groupName: Ref<string>;
  dataIndicatorName: Ref<string>;
  aggregationMethodName: Ref<string>;
  xData?: Ref<string[] | undefined>;
  series: Ref<EChartsOption['series']>;
  containerRef?: Ref<Element | undefined>;
  type: ChartTypeEnum;
  customConfig?: Ref<EChartsOption>;
  data: Ref<any[]>;
  onClick?: (params: any) => void;
}

export const lightColors = [
  '#3370FF',
  '#00C261',
  '#FFA200',
  '#FF8A51',
  '#E22E23',
  '#9175FF',
  '#CC71E2',
  '#FFBD4A',
  '#68DF92',
  '#3EBCF3',
  '#A4B5E0',
  '#00A6AB',
];

export const darkColors = [
  '#4282FF',
  '#37E0CC',
  '#FBF15A',
  '#FF9562',
  '#E94256',
  '#7F60F9',
  '#CC71E2',
  '#FFB958',
  '#76DE74',
  '#3BC2FC',
  '#798FC5',
  '#008D91',
];

export default function useChart(props: ChartProps) {
  const { t } = useI18n();

  echarts.use(props.components);
  echarts.registerTheme('light', {
    color: lightColors,
  });
  echarts.registerTheme('dark', {
    color: darkColors,
  });

  const isCount = props.aggregationMethodName.value === t('crmViewSelect.count');
  const title = computed(() =>
    isCount
      ? t('crmViewSelect.chartNameCount', { group: props.groupName.value })
      : t('crmViewSelect.chartName', {
          group: props.groupName.value,
          indicator: props.dataIndicatorName.value,
          aggregation: props.aggregationMethodName.value,
        })
  );
  const total = computed(() => props.data.value.reduce((sum, current) => sum + current.value, 0));
  const percentMap = computed(() => {
    const map: Record<string, any> = {};
    props.data.value.forEach((item: any) => {
      map[item.name] = total.value ? ((item.value / total.value) * 100).toFixed(2) : 0;
    });
    return map;
  });

  const xyAxis = computed<EChartsOption>(() => ({
    tooltip: {
      show: true,
      trigger: 'axis',
      borderWidth: 0,
      axisPointer: {
        type: 'shadow',
        shadowStyle: {
          color: getComputedStyle(document.documentElement).getPropertyValue('--text-n6').trim(),
          opacity: 0.1,
        },
        label: {
          show: true,
          backgroundColor: 'none',
          color: getComputedStyle(document.documentElement).getPropertyValue('--primary-8').trim(),
        },
      },
      formatter: (params: any) => {
        // 当trigger是'axis'时，params是数组
        const div = document.createElement('div');
        render(
          h(tooltip, {
            name: params[0].name,
            value: params[0].value,
            seriesName: params[0].seriesName,
            color: params[0].color,
          }),
          div
        );
        return div;
      },
    },
    grid: {
      left: '2%',
      right: '2%',
      bottom: props.xData?.value && props.xData?.value.length >= 100 ? '10%' : '3%',
      top: '70px',
      containLabel: true,
    },
    xAxis: [
      {
        type: 'category',
        data: props.xData?.value,
        axisLabel: {
          interval: 0,
          color: getComputedStyle(document.documentElement).getPropertyValue('--text-n4').trim(),
        },
        axisTick: {
          alignWithLabel: true,
        },
        axisLine: {
          lineStyle: {
            color: getComputedStyle(document.documentElement).getPropertyValue('--text-n8').trim(),
          },
        },
      },
    ],
    yAxis: [
      {
        type: 'value',
        axisLabel: {
          color: getComputedStyle(document.documentElement).getPropertyValue('--text-n4').trim(),
        },
        splitLine: {
          lineStyle: {
            type: 'dashed',
          },
        },
      },
    ],
    dataZoom:
      props.xData?.value && props.xData?.value.length >= 100
        ? [
            {
              type: 'inside',
              realtime: true,
              start: 30,
              end: 70,
            },
            {
              type: 'slider',
              realtime: true,
              start: 30,
              end: 70,
            },
          ]
        : [],
  }));
  const pieConfig = computed<EChartsOption>(() => ({
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const div = document.createElement('div');
        render(
          h(tooltip, {
            name: params.name,
            value: params.value,
            seriesName: params.seriesName,
            percent: params.percent,
            color: params.color,
          }),
          div
        );
        return div;
      },
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
      formatter(name) {
        const item = props.data.value.find((e: any) => e.name === name);
        return item
          ? `{name|${name}}  {value|${abbreviateNumber(item.value, '').value}${
              abbreviateNumber(item.value, '').unit
            }} {percent|${percentMap.value[item.name]}%}`
          : name;
      },
      textStyle: {
        width: 400,
        rich: {
          name: {
            width: '33%',
            fontSize: 12,
            color: getComputedStyle(document.documentElement).getPropertyValue('--text-n2').trim(),
          },
          value: {
            width: '33%',
            fontSize: 12,
            fontWeight: 500,
            color: getComputedStyle(document.documentElement).getPropertyValue('--text-n1').trim(),
          },
          percent: {
            width: '33%',
            fontSize: 12,
            fontWeight: 500,
            color: getComputedStyle(document.documentElement).getPropertyValue('--text-n1').trim(),
          },
        },
      },
    },
  }));
  const funnelConfig = computed<EChartsOption>(() => ({
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const div = document.createElement('div');
        render(
          h(tooltip, {
            name: params.name,
            value: params.value,
            seriesName: params.seriesName,
            color: params.color,
          }),
          div
        );
        return div;
      },
    },
  }));
  const partConfig = computed<EChartsOption>(() => {
    if (props.type === ChartTypeEnum.FUNNEL) {
      return funnelConfig.value;
    }
    if ([ChartTypeEnum.PIE, ChartTypeEnum.DONUT].includes(props.type)) {
      return pieConfig.value;
    }
    return xyAxis.value;
  });

  const chartOptions = ref<EChartsOption>(
    deepMerge(
      {
        title: {
          text: title.value,
          textAlign: 'left',
          left: '1%',
          top: '2%',
          textStyle: {
            fontSize: 14,
          },
        },
        ...partConfig.value,
        series: props.series.value,
      } as EChartsOption,
      props.customConfig?.value || {}
    )
  );

  const myChart = shallowRef<echarts.ECharts>();
  const chartDom = ref<HTMLElement | null>(null);
  /**
   * 处理图表容器尺寸变化
   */
  function handleChartDomResize() {
    const seriesData: PieSeriesOption = {};
    const legend: LegendComponentOption = {
      textStyle: {},
    };
    if (chartDom.value && [ChartTypeEnum.PIE, ChartTypeEnum.DONUT].includes(props.type)) {
      const pieLeft = chartDom.value.clientHeight / 4 + 16;
      seriesData.center = [pieLeft, '50%'];
      legend.left = pieLeft * 2 + 20;
      legend.textStyle!.width = Math.abs(chartDom.value.clientWidth - chartDom.value.clientHeight / 2 - 40);
      chartOptions.value = deepMerge(chartOptions.value, {
        legend,
        series: Array.isArray(props.series.value)
          ? props.series.value.map((e) => ({ ...e, ...seriesData }))
          : { ...props.series.value, ...seriesData },
      });
    }
  }

  function initChart(_chartDom: HTMLElement | null) {
    chartDom.value = _chartDom;
    handleChartDomResize();
    myChart.value = echarts.init(_chartDom, 'light');
    myChart.value.setOption(chartOptions.value);
    myChart.value.on('click', (params) => {
      // 触发点击事件
      props.onClick?.(params);
    });
  }

  function refreshChart(opt?: EChartsOption) {
    if (myChart.value) {
      myChart.value?.resize();
      handleChartDomResize();
      nextTick(() => {
        myChart.value?.setOption(deepMerge(chartOptions.value, opt || {}), true);
      });
    }
  }

  watch(
    () => props.data.value,
    () => {
      chartOptions.value = deepMerge(
        {
          title: {
            text: title.value,
            textAlign: 'left',
            left: '1%',
            top: '2%',
            textStyle: {
              fontSize: 14,
            },
          },
          ...partConfig.value,
          series: props.series.value,
        } as EChartsOption,
        props.customConfig?.value || {}
      );
      nextTick(() => {
        refreshChart();
      });
    }
  );

  function downloadChartImage() {
    if (myChart.value) {
      const url = myChart.value.getDataURL({
        type: 'png',
        pixelRatio: 2,
        backgroundColor: '#fff',
      });
      const link = document.createElement('a');
      link.href = url;
      link.download = `${title.value}.png`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }
  }

  onMounted(() => {
    let isFirstResize = true;
    if (!props.containerRef?.value) return;
    const handleResize = debounce(() => {
      if (isFirstResize) {
        isFirstResize = false; // 第一次触发，跳过
        return;
      }
      refreshChart();
    }, 300);
    const observer = new ResizeObserver((entries) => {
      entries.forEach(() => {
        handleResize();
      });
    });
    observer.observe(props.containerRef.value);
  });

  return {
    initChart,
    refreshChart,
    downloadChartImage,
  };
}
