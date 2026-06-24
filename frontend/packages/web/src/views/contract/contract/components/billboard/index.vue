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
    :before-move-to-stage="handleBeforeMoveToStage"
    :show-progress="false"
    @change="handleChange"
    @openDetail="(type, item) => emit('openDetail', type, item)"
    @init="(total) => emit('init', total)"
  >
    <template #card="{ item, fieldList: currentFieldList, openDetail }">
      <ContractBillboardCard
        :item="item"
        :field-list="currentFieldList as FormCreateField[]"
        @open-detail="openDetail"
      />
    </template>
  </CrmStageBoard>

  <VoidReasonModal
    v-model:visible="showVoidReasonModal"
    :name="activeSourceName"
    :sourceId="activeSourceId"
    @refresh="handleVoidRefresh"
  />
</template>

<script setup lang="ts">
  import { ContractStatusEnum } from '@lib/shared/enums/contractEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import type { ContractItem } from '@lib/shared/models/contract';
  import type { OpportunityStageConfig, StageBoardPageQueryParams } from '@lib/shared/models/opportunity';

  import type { FilterResult } from '@/components/pure/crm-advance-filter/type';
  import type { FormCreateField } from '@/components/business/crm-form-create/types';
  import CrmStageBoard from '@/components/business/crm-stage-board/index.vue';
  import type {
    OpenDetailType,
    StageBoardBlockedMovePayload,
    StageBoardLoadParams,
    StageBoardMoveCheckPayload,
    StageBoardMovePayload,
    StageBoardStatistic,
  } from '@/components/business/crm-stage-board/types';
  import VoidReasonModal from '../voidReasonModal.vue';
  import ContractBillboardCard from './card.vue';

  import { getContractList, getContractStatistic, getContractStatusConfig, sortContract } from '@/api/modules';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    advanceFilter?: FilterResult;
    viewId?: string;
    keyword?: string;
    enableApproval?: boolean;
    hasStagePermission?: (row: ContractItem) => boolean;
  }>();

  const emit = defineEmits<{
    (e: 'change'): void;
    (e: 'init', total: number): void;
    (e: 'openDetail', type: OpenDetailType, item: any): void;
  }>();

  const { initFormConfig, fieldList } = useFormCreateApi({
    formKey: computed(() => FormDesignKeyEnum.CONTRACT),
  });

  const stageConfig = ref<OpportunityStageConfig>();
  async function initStageConfig() {
    try {
      stageConfig.value = await getContractStatusConfig();
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
    return getContractList({
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
    return getContractStatistic({
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

  async function moveStageItem(payload: StageBoardMovePayload<ContractItem>) {
    await sortContract({
      dropNodeId: payload.nextItem?.id || payload.previousItem?.id || '',
      dragNodeId: payload.item.id,
      dropPosition: payload.nextItem ? -1 : 1,
      stage: payload.toStageId,
    });
  }

  function canMoveItem(payload: StageBoardMoveCheckPayload<ContractItem>) {
    if (!hasAnyPermission(['CONTRACT:STAGE'])) {
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

  const showVoidReasonModal = ref(false);
  const activeSourceId = ref('');
  const activeSourceName = ref('');

  function handleBeforeMoveToStage(payload: StageBoardBlockedMovePayload<ContractItem>) {
    if (payload.toStageId !== ContractStatusEnum.VOID) {
      return true;
    }

    activeSourceId.value = payload.item.id;
    activeSourceName.value = payload.item.name;
    showVoidReasonModal.value = true;
    return false;
  }

  function handleVoidRefresh() {
    refresh();
    emit('change');
  }

  defineExpose({
    refresh,
  });

  await Promise.all([initStageConfig(), initFormConfig()]);
</script>
