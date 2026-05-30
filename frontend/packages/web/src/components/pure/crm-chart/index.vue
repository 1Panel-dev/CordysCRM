<template>
  <div class="relative h-full">
    <suspense>
      <component
        :is="getComponent()"
        ref="chartRef"
        :group-name="props.groupName"
        :data-indicator-name="props.dataIndicatorName"
        :aggregation-method-name="props.aggregationMethodName"
        :x-data="props.xData"
        :data="props.data"
        :container-ref="props.containerRef"
        :is-full-screen="isFullScreen"
        @chart-click="emit('chartClick', $event)"
      />
    </suspense>
    <toolbox
      v-if="props.containerRef"
      :container-ref="props.containerRef"
      @refresh="handleRefresh"
      @download="handleDownload"
      @toggle-full-screen="(val) => (isFullScreen = val)"
    />
  </div>
</template>

<script setup lang="ts">
  import toolbox from './components/toolbox.vue';

  import { ChartTypeEnum } from './type';

  const CrmBarChart = defineAsyncComponent(() => import('./charts/bar.vue'));
  const CrmLineChart = defineAsyncComponent(() => import('./charts/line.vue'));
  const CrmPieChart = defineAsyncComponent(() => import('./charts/pie.vue'));
  const CrmDoughnutChart = defineAsyncComponent(() => import('./charts/doughnut.vue'));
  const CrmFunnelChart = defineAsyncComponent(() => import('./charts/funnel.vue'));

  const props = defineProps<{
    type: ChartTypeEnum;
    groupName: string;
    dataIndicatorName: string;
    aggregationMethodName: string;
    xData?: string[];
    data: any[];
    containerRef?: Element;
  }>();
  const emit = defineEmits<{
    (e: 'refresh'): void;
    (e: 'chartClick', params: any): void;
  }>();

  const isFullScreen = ref(false);

  function getComponent() {
    switch (props.type) {
      case ChartTypeEnum.BAR:
        return CrmBarChart;
      case ChartTypeEnum.LINE:
        return CrmLineChart;
      case ChartTypeEnum.PIE:
        return CrmPieChart;
      case ChartTypeEnum.DONUT:
        return CrmDoughnutChart;
      case ChartTypeEnum.FUNNEL:
        return CrmFunnelChart;
      default:
    }
  }

  const chartRef = ref<InstanceType<
    typeof CrmBarChart | typeof CrmLineChart | typeof CrmPieChart | typeof CrmDoughnutChart | typeof CrmFunnelChart
  > | null>(null);

  function handleRefresh() {
    emit('refresh');
  }

  function handleDownload() {
    chartRef.value?.downloadChartImage();
  }
</script>

<style lang="less" scoped></style>
