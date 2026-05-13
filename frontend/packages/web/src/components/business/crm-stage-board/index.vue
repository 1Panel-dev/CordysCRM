<template>
  <n-scrollbar
    :content-style="{ gridTemplateColumns: `repeat(${props.stages.length || 7}, 300px)` }"
    content-class="grid gap-[16px] h-full"
    class="mb-[16px] flex-1"
    x-scrollable
  >
    <StageBoardList
      v-for="(item, index) in props.stages"
      ref="listRef"
      :key="item.id"
      :index="index"
      :stage="item"
      :stage-ids="props.stages.map((stage) => stage.id)"
      :keyword="props.keyword"
      :view-id="props.viewId"
      :field-list="props.fieldList"
      :refresh-time-stamp="refreshTimeStamp"
      :advance-filter="props.advanceFilter"
      :load-stage-list="props.loadStageList"
      :load-stage-statistic="props.loadStageStatistic"
      :move-stage-item="props.moveStageItem"
      :can-move-item="props.canMoveItem"
      :before-move-to-stage="props.beforeMoveToStage"
      :show-progress="props.showProgress"
      @change="refreshList"
      @openDetail="(type, currentItem) => emit('openDetail', type, currentItem)"
      @init="(total) => handleListInit(item.id, total)"
    >
      <template #card="slotProps">
        <slot name="card" v-bind="slotProps" />
      </template>
    </StageBoardList>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { NScrollbar } from 'naive-ui';

  import { StageConfigItem } from '@lib/shared/models/opportunity';

  import StageBoardList from './list.vue';

  import type {
    OpenDetailType,
    StageBoardBlockedMovePayload,
    StageBoardListExpose,
    StageBoardLoadParams,
    StageBoardLoadResult,
    StageBoardMoveCheckPayload,
    StageBoardMovePayload,
    StageBoardStatistic,
  } from './types';

  const props = withDefaults(
    defineProps<{
      stages: StageConfigItem[];
      keyword?: string;
      viewId?: string;
      advanceFilter?: any;
      fieldList?: any[];
      showProgress?: boolean;
      loadStageList: (params: StageBoardLoadParams) => Promise<StageBoardLoadResult>;
      loadStageStatistic?: (params: Omit<StageBoardLoadParams, 'current' | 'pageSize'>) => Promise<StageBoardStatistic>;
      moveStageItem: (payload: StageBoardMovePayload) => Promise<void>;
      canMoveItem?: (payload: StageBoardMoveCheckPayload) => boolean;
      beforeMoveToStage?: (payload: StageBoardBlockedMovePayload) => boolean | Promise<boolean>;
    }>(),
    {
      showProgress: true,
    }
  );

  const emit = defineEmits<{
    (e: 'change'): void;
    (e: 'init', total: number): void;
    (e: 'openDetail', type: OpenDetailType, item: any): void;
  }>();

  const refreshTimeStamp = ref(0);
  const listRef = ref<StageBoardListExpose[]>();
  const totalMap = ref<Record<string, number>>({});
  const sumTotal = computed(() => Object.values(totalMap.value).reduce((acc, curr) => acc + curr, 0));

  function refresh() {
    nextTick(() => {
      refreshTimeStamp.value += 1;
    });
  }

  async function sortStageItem(index: number, item: any) {
    await listRef.value?.[index]?.sortItem(item);
  }

  function refreshList(stageId: string) {
    const index = props.stages.findIndex((item) => item.id === stageId);
    if (index >= 0) {
      listRef.value?.[index]?.refreshList();
    }
    emit('change');
  }

  function handleListInit(id: string, total: number) {
    totalMap.value[id] = total;
    nextTick(() => {
      emit('init', sumTotal.value);
    });
  }

  defineExpose({
    refresh,
    sortStageItem,
  });
</script>

<style lang="less" scoped>
  :deep(.n-scrollbar-rail--horizontal--bottom) {
    bottom: 0 !important;
  }
</style>
