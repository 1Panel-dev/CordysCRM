<template>
  <CrmStageBoard
    ref="boardRef"
    :stages="stageConfig?.stageConfigList || []"
    :keyword="keyword"
    :view-id="props.viewId"
    :field-list="fieldList"
    :advance-filter="advanceFilter"
    :load-stage-list="loadStageList"
    :load-stage-statistic="loadStageStatistic"
    :move-stage-item="moveStageItem"
    :can-move-item="canMoveItem"
    :before-move-to-stage="beforeMoveToStage"
    @change="refreshList"
    @openDetail="(type, item) => emit('openDetail', type, item)"
    @init="handleListInit"
  >
    <template #card="{ item, fieldList: currentFieldList, optionMap, openDetail }">
      <OpportunityBillboardCard
        :item="item"
        :field-list="currentFieldList as FormCreateField[]"
        :option-map="optionMap"
        @open-detail="openDetail"
      />
    </template>
  </CrmStageBoard>
  <CrmModal
    v-model:show="updateStatusModal"
    :title="t('common.complete')"
    :ok-loading="updateStageLoading"
    size="small"
    @confirm="handleConfirm"
    @cancel="handleCancel"
  >
    <n-form ref="formRef" :model="form" label-placement="left" require-mark-placement="left">
      <n-form-item
        require-mark-placement="left"
        label-placement="left"
        path="failureReason"
        :label="t('opportunity.failureReason')"
        :rule="[{ required: true, message: t('common.notNull', { value: t('opportunity.failureReason') }) }]"
      >
        <n-select v-model:value="form.failureReason" :options="reasonList" :placeholder="t('common.pleaseSelect')" />
      </n-form-item>
    </n-form>
  </CrmModal>
</template>

<script setup lang="ts">
  import { NForm, NFormItem, NSelect } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { TableQueryParams } from '@lib/shared/models/common';
  import { OpportunityItem, OpportunityStageConfig } from '@lib/shared/models/opportunity';

  import { FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import { FormCreateField } from '@/components/business/crm-form-create/types';
  import type { Option } from '@/components/business/crm-select-user-drawer/type';
  import CrmStageBoard from '@/components/business/crm-stage-board/index.vue';
  import type {
    OpenDetailType,
    StageBoardBlockedMovePayload,
    StageBoardLoadParams,
    StageBoardMoveCheckPayload,
    StageBoardMovePayload,
    StageBoardStatistic,
  } from '@/components/business/crm-stage-board/types';
  import OpportunityBillboardCard from './card.vue';

  import {
    getOpportunityList,
    getOpportunityStageConfig,
    getOptStatistic,
    getReasonConfig,
    sortOpportunity,
    updateOptStage,
  } from '@/api/modules';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    advanceFilter?: FilterResult;
    viewId?: string;
    keyword?: string;
  }>();
  const emit = defineEmits<{
    (e: 'change'): void;
    (e: 'init', total: number): void;
    (e: 'openDetail', type: OpenDetailType, item: any): void;
  }>();

  const { t } = useI18n();

  const { initFormConfig: initOptFormConfig, fieldList } = useFormCreateApi({
    formKey: computed(() => FormDesignKeyEnum.BUSINESS),
  });

  const stageConfig = ref<OpportunityStageConfig>();
  async function initStageConfig() {
    try {
      stageConfig.value = await getOpportunityStageConfig();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const boardRef = ref<InstanceType<typeof CrmStageBoard>>();
  function refresh() {
    boardRef.value?.refresh();
  }
  function refreshList() {
    emit('change');
  }

  function handleListInit(total: number) {
    emit('init', total);
  }

  const form = ref({
    failureReason: null,
  });
  const updateStatusModal = ref<boolean>(false);
  const updateStageLoading = ref(false);
  const updateOptItem = ref<any>({});
  const enableReason = ref(false);
  const reasonList = ref<Option[]>([]);
  async function initReason() {
    try {
      const { dictList, enable } = await getReasonConfig(ReasonTypeEnum.OPPORTUNITY_FAIL_RS);
      enableReason.value = enable;
      reasonList.value = dictList.map((e) => ({ label: e.name, value: e.id }));
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  function handleFailItem(item: any) {
    updateOptItem.value = item;
    updateStatusModal.value = true;
    form.value.failureReason = null;
  }

  async function handleConfirm() {
    try {
      updateStageLoading.value = true;
      await boardRef.value?.sortStageItem((stageConfig.value?.stageConfigList.length || 1) - 1, updateOptItem.value);
      await updateOptStage({
        id: updateOptItem.value.data.id,
        stage: stageConfig.value?.stageConfigList.slice(-1)[0].id || '',
        failureReason: form.value.failureReason || '',
      });
      updateStatusModal.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      updateStageLoading.value = false;
    }
  }

  function handleCancel() {
    updateOptItem.value = {};
    updateStatusModal.value = false;
    form.value.failureReason = null;
  }

  async function loadStageList(params: TableQueryParams) {
    return getOpportunityList({
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
    return getOptStatistic({
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

  async function moveStageItem(payload: StageBoardMovePayload<OpportunityItem>) {
    await sortOpportunity({
      dropNodeId: payload.nextItem?.id || payload.previousItem?.id || '',
      dragNodeId: payload.item.id,
      dropPosition: payload.nextItem ? -1 : 1,
      stage: payload.toStageId,
    });
  }

  function beforeMoveToStage(payload: StageBoardBlockedMovePayload<OpportunityItem>) {
    const isFailStage =
      stageConfig.value?.stageConfigList.find((item) => item.id === payload.toStageId)?.type === 'END' &&
      stageConfig.value?.stageConfigList.find((item) => item.id === payload.toStageId)?.rate === '0';

    if (payload.item.stage !== payload.toStageId && isFailStage && enableReason.value) {
      handleFailItem(payload.rawItem);
      return false;
    }

    return true;
  }

  function canMoveItem(payload: StageBoardMoveCheckPayload) {
    const targetIndex = stageConfig.value?.stageConfigList.findIndex((item) => item.id === payload.toStageId) ?? -1;
    const fromIndex = stageConfig.value?.stageConfigList.findIndex((item) => item.id === payload.fromStageId) ?? -1;
    const currentStage = stageConfig.value?.stageConfigList.find((item) => item.id === payload.toStageId);
    const isSuccess = currentStage?.type === 'END' && currentStage?.rate === '100';
    const isFail = currentStage?.type === 'END' && currentStage?.rate === '0';

    if (
      payload.rawEvent.to.className.includes(payload.toStageId) &&
      payload.rawEvent.from.className.includes(payload.toStageId)
    ) {
      return true;
    }

    if (isSuccess) {
      return (
        (!!currentStage?.endRollBack &&
          !payload.rawEvent.to.className.includes(stageConfig.value?.stageConfigList.slice(-1)[0].id)) ||
        (hasAnyPermission(['OPPORTUNITY_MANAGEMENT:RESIGN']) &&
          payload.rawEvent.to.className.includes(stageConfig.value?.stageConfigList.slice(-1)[0].id))
      );
    }

    if (isFail) {
      return !!currentStage?.endRollBack;
    }

    if (targetIndex < fromIndex) {
      return !!currentStage?.afootRollBack;
    }

    return true;
  }

  onBeforeMount(async () => {
    await initStageConfig();
    initOptFormConfig();
    initReason();
  });

  defineExpose({
    refresh,
  });
</script>
