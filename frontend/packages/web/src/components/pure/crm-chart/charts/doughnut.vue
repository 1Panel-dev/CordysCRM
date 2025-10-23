<template>
  <div :id="`crm-chart-${id}`" class="h-full"> </div>
</template>

<script setup lang="ts">
  import { PieChart } from 'echarts/charts';
  import { GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components';
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

  const { t } = useI18n();

  const id = getGenerateId();
  const { containerRef, groupName, dataIndicatorName, aggregationMethodName, xData, data } = toRefs(props);
  const seriesData = {
    name:
      props.aggregationMethodName === t('crmViewSelect.count') ? t('crmViewSelect.counts') : props.dataIndicatorName,
    type: ChartTypeEnum.PIE as any,
    data: props.data,
    radius: ['30%', '50%'],
    center: ['13%', '50%'],
    label: {
      show: false,
    },
    labelLine: {
      show: false,
    },
  };
  const { initChart, refreshChart, downloadChartImage } = useChart({
    type: ChartTypeEnum.PIE,
    components: [TooltipComponent, TitleComponent, GridComponent, LegendComponent, PieChart, CanvasRenderer],
    id,
    groupName,
    dataIndicatorName,
    aggregationMethodName,
    xData,
    data,
    series: [seriesData],
    containerRef,
    customConfig: computed(() => ({
      legend: {
        left: '45%',
      },
    })),
  });

  onMounted(() => {
    const chartDom = document.getElementById(`crm-chart-${id}`);
    initChart(chartDom);
  });

  watch(
    () => props.isFullScreen,
    (val) => {
      nextTick(() => {
        if (val) {
          refreshChart({
            legend: {
              left: '45%',
            },
            series: [
              {
                ...seriesData,
                center: ['20%', '50%'],
              },
            ],
          });
        } else {
          refreshChart({
            legend: {
              left: '37%',
            },
            series: [
              {
                ...seriesData,
                center: ['13%', '50%'],
              },
            ],
          });
        }
      });
    }
  );
  defineExpose({ refreshChart, downloadChartImage });
</script>

<style lang="less" scoped></style>
