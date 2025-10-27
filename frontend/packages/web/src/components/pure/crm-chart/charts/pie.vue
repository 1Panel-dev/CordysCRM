<template>
  <div :id="`crm-chart-${id}`" class="h-full"> </div>
</template>

<script setup lang="ts">
  import { PieSeriesOption } from 'echarts';
  import { PieChart } from 'echarts/charts';
  import { LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components';
  import { CanvasRenderer } from 'echarts/renderers';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getGenerateId } from '@lib/shared/method';

  import { ChartTypeEnum } from '../type';
  import useChart from '../useChart';

  const props = defineProps<{
    groupName: string;
    dataIndicatorName: string;
    aggregationMethodName: string;
    xData?: string[];
    data: any[];
    containerRef?: Element;
    isFullScreen: boolean;
  }>();
  const emit = defineEmits<{
    (e: 'chartClick', params: any): void;
  }>();

  const { t } = useI18n();

  const id = getGenerateId();

  const { containerRef, groupName, dataIndicatorName, aggregationMethodName, xData, data } = toRefs(props);
  const series = computed<PieSeriesOption>(() => {
    return {
      name:
        props.aggregationMethodName === t('crmViewSelect.count') ? t('crmViewSelect.counts') : props.dataIndicatorName,
      type: ChartTypeEnum.PIE as any,
      data: data.value,
      radius: '50%',
      center: ['13%', '50%'],
      label: {
        show: false,
      },
      labelLine: {
        show: false,
      },
    };
  });
  const { initChart, refreshChart, downloadChartImage } = useChart({
    type: ChartTypeEnum.PIE,
    id,
    components: [TooltipComponent, TitleComponent, LegendComponent, PieChart, CanvasRenderer],
    groupName,
    dataIndicatorName,
    aggregationMethodName,
    xData,
    data,
    series,
    containerRef,
    onClick(params) {
      emit('chartClick', params);
    },
  });

  onMounted(() => {
    const chartDom = document.getElementById(`crm-chart-${id}`);
    initChart(chartDom);
  });

  defineExpose({ refreshChart, downloadChartImage });
</script>

<style lang="less" scoped></style>
