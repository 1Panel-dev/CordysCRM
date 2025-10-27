<template>
  <div :id="`crm-chart-${id}`" class="h-full"> </div>
</template>

<script setup lang="ts">
  import { FunnelSeriesOption } from 'echarts';
  import { FunnelChart } from 'echarts/charts';
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
    data: any[];
    containerRef?: Element;
    isFullScreen: boolean;
  }>();
  const emit = defineEmits<{
    (e: 'chartClick', params: any): void;
  }>();

  const { t } = useI18n();

  const id = getGenerateId();
  const { containerRef, groupName, dataIndicatorName, aggregationMethodName, data } = toRefs(props);
  const series = computed<FunnelSeriesOption>(() => ({
    name:
      props.aggregationMethodName === t('crmViewSelect.count') ? t('crmViewSelect.counts') : props.dataIndicatorName,
    type: ChartTypeEnum.FUNNEL,
    data: props.data,
    min: 0,
    max: 100,
    minSize: '0%',
    maxSize: '100%',
    sort: 'descending',
    gap: 1,
    label: {
      show: true,
      position: 'inside',
    },
  }));
  const { initChart, refreshChart, downloadChartImage } = useChart({
    type: ChartTypeEnum.FUNNEL,
    id,
    components: [TooltipComponent, TitleComponent, LegendComponent, GridComponent, FunnelChart, CanvasRenderer],
    groupName,
    dataIndicatorName,
    aggregationMethodName,
    data,
    series,
    containerRef,
    customConfig: computed(() => ({
      legend: {
        top: 'bottom',
        orient: 'horizontal',
        left: 'center',
        formatter(name) {
          return name;
        },
      },
    })),
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
