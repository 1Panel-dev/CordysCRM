<template>
  <div :id="`crm-chart-${id}`" class="h-full"> </div>
</template>

<script setup lang="ts">
  import { LineChart } from 'echarts/charts';
  import { GridComponent, TitleComponent, TooltipComponent } from 'echarts/components';
  import { CanvasRenderer } from 'echarts/renderers';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getGenerateId } from '@lib/shared/method';

  import { ChartTypeEnum } from '../type';
  import useChart from '../useChart';

  const props = defineProps<{
    groupName: string;
    dataIndicatorName: string;
    aggregationMethodName: string;
    xData: string[];
    data: any[];
    containerRef?: Element;
    isFullScreen: boolean;
  }>();

  const { t } = useI18n();

  const id = getGenerateId();
  const { containerRef, groupName, dataIndicatorName, aggregationMethodName, xData, data } = toRefs(props);
  const { initChart, refreshChart, downloadChartImage } = useChart({
    type: ChartTypeEnum.LINE,
    components: [TooltipComponent, TitleComponent, GridComponent, LineChart, CanvasRenderer],
    id,
    groupName,
    dataIndicatorName,
    aggregationMethodName,
    xData,
    data,
    series: [
      {
        name:
          props.aggregationMethodName === t('crmViewSelect.count')
            ? t('crmViewSelect.counts')
            : props.dataIndicatorName,
        type: ChartTypeEnum.LINE,
        data: props.data,
      },
    ],
    containerRef,
  });

  onMounted(() => {
    const chartDom = document.getElementById(`crm-chart-${id}`);
    initChart(chartDom);
  });

  defineExpose({ refreshChart, downloadChartImage });
</script>

<style lang="less" scoped></style>
