<template>
  <CrmStageBoard
    ref="boardRef"
    :stages="stageConfig?.stageConfigList || []"
    :keyword="props.keyword"
    :view-id="props.viewId"
    :field-list="fieldList"
    :advance-filter="props.advanceFilter"
    :load-stage-list="loadStageList"
    :load-stage-statistic="loadStageStatistic"
    :move-stage-item="moveStageItem"
    :can-move-item="canMoveItem"
    :show-progress="false"
    @change="handleChange"
    @openDetail="(type, item) => emit('openDetail', type, item)"
    @init="(total) => emit('init', total)"
  >
    <template #card="{ item, fieldList: currentFieldList, openDetail }">
      <OrderBillboardCard :item="item" :field-list="currentFieldList" @open-detail="openDetail" />
    </template>
  </CrmStageBoard>
</template>

<script setup lang="ts">
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import type { OpportunityStageConfig, StageBoardPageQueryParams } from '@lib/shared/models/opportunity';
  import type { OrderItem } from '@lib/shared/models/order';

  import type { FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmStageBoard from '@/components/business/crm-stage-board/index.vue';
  import type {
    OpenDetailType,
    StageBoardLoadParams,
    StageBoardMoveCheckPayload,
    StageBoardMovePayload,
    StageBoardStatistic,
  } from '@/components/business/crm-stage-board/types';
  import OrderBillboardCard from './card.vue';

  import { getOrderList, getOrderStatistic, getOrderStatusConfig, sortOrder } from '@/api/modules';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    advanceFilter?: FilterResult;
    viewId?: string;
    keyword?: string;
    readonly?: boolean;
    enableApproval?: boolean;
    hasStagePermission?: (row: OrderItem) => boolean;
  }>();

  const emit = defineEmits<{
    (e: 'change'): void;
    (e: 'init', total: number): void;
    (e: 'openDetail', type: OpenDetailType, item: any): void;
  }>();

  const { initFormConfig, fieldList } = useFormCreateApi({
    formKey: computed(() => FormDesignKeyEnum.ORDER),
  });

  const stageConfig = ref<OpportunityStageConfig>();
  async function initStageConfig() {
    try {
      stageConfig.value = await getOrderStatusConfig();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const boardRef = ref<InstanceType<typeof CrmStageBoard>>();
  function refresh() {
    boardRef.value?.refresh();
  }

  function handleChange() {
    emit('change');
  }

  async function loadStageList(params: StageBoardPageQueryParams) {
    return getOrderList({
      current: params.current,
      pageSize: params.pageSize,
      keyword: params.keyword,
      combineSearch: params.advanceFilter,
      filters: [
        {
          name: 'stage',
          value: params.stageId,
          multipleValue: false,
          operator: 'EQUALS',
        },
      ],
      board: true,
      viewId: params.viewId,
    });
  }

  async function loadStageStatistic(params: Omit<StageBoardLoadParams, 'current' | 'pageSize'>) {
    return getOrderStatistic({
      keyword: params.keyword,
      combineSearch: params.advanceFilter,
      filters: [
        {
          name: 'stage',
          value: params.stageId,
          multipleValue: false,
          operator: 'EQUALS',
        },
      ],
      viewId: params.viewId,
    }) as Promise<StageBoardStatistic>;
  }

  async function moveStageItem(payload: StageBoardMovePayload<OrderItem>) {
    await sortOrder({
      dropNodeId: payload.nextItem?.id || payload.previousItem?.id || '',
      dragNodeId: payload.item.id,
      dropPosition: payload.nextItem ? -1 : 1,
      stage: payload.toStageId,
    });
  }

  function canMoveItem(payload: StageBoardMoveCheckPayload<OrderItem>) {
    if (props.readonly || !hasAnyPermission(['ORDER:UPDATE'])) {
      return false;
    }

    if (props.enableApproval && (!payload.item || !props.hasStagePermission?.(payload.item))) {
      return false;
    }

    if (
      payload.rawEvent.to.className.includes(payload.toStageId) &&
      payload.rawEvent.from.className.includes(payload.toStageId)
    ) {
      return true;
    }

    const targetIndex = stageConfig.value?.stageConfigList.findIndex((item) => item.id === payload.toStageId) ?? -1;
    const fromIndex = stageConfig.value?.stageConfigList.findIndex((item) => item.id === payload.fromStageId) ?? -1;
    const isCurrentEndStage =
      stageConfig.value?.stageConfigList.find((item) => item.id === payload.fromStageId)?.type === 'END';

    if (isCurrentEndStage) {
      return !!stageConfig.value?.endRollBack;
    }

    if (stageConfig.value?.afootRollBack) {
      return true;
    }

    return targetIndex >= fromIndex;
  }

  await Promise.all([initStageConfig(), initFormConfig()]);

  defineExpose({
    refresh,
  });
</script>
