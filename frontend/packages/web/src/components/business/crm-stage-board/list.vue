<template>
  <div
    class="flex h-full flex-col gap-[8px] overflow-hidden rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]"
  >
    <div class="flex items-center justify-between">
      <CrmTag :type="stageColor" theme="dark">{{ props.stage.name }}</CrmTag>
      <div class="font-semibold">
        {{
          `${abbreviateNumber(statisticInfo?.amount ?? 0, '').value} ${
            abbreviateNumber(statisticInfo?.amount ?? 0, '').unit
          } / ${pageNation.total}`
        }}
      </div>
    </div>
    <n-progress
      v-if="props.showProgress !== false"
      type="line"
      color="var(--primary-8)"
      rail-color="var(--text-n8)"
      :percentage="Number(props.stage.rate || 0)"
    />
    <n-spin :show="loading" content-class="h-full">
      <VueDraggable
        v-model="list"
        :animation="150"
        ghost-class="crm-stage-board-item-ghost"
        :group="{ name: 'crm-stage-board', pull: 'clone', put: true }"
        target=".n-scrollbar-content"
        class="h-full"
        @add="handleAddItem"
        @move="handleMove"
        @update="handleUpdate"
        @remove="handleRemove"
      >
        <n-scrollbar :content-class="`${props.stage.id} h-full`" @scroll="handleReachBottom">
          <div v-for="item in list" :key="item.id" class="crm-stage-board-item">
            <slot
              name="card"
              :item="item"
              :field-list="props.fieldList || []"
              :option-map="optionMap"
              :stage="props.stage"
              :refresh="refreshList"
              :open-detail="openDetail"
            />
          </div>
          <div v-if="list.length === 0 && !loading" class="flex h-[300px] flex-1 items-center justify-center">
            <n-empty :description="t('common.noData')" />
          </div>
        </n-scrollbar>
      </VueDraggable>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
  import { NEmpty, NProgress, NScrollbar, NSpin } from 'naive-ui';
  import { VueDraggable } from 'vue-draggable-plus';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { abbreviateNumber } from '@lib/shared/method';
  import { StageConfigItem } from '@lib/shared/models/opportunity';

  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import type { FormCreateField } from '@/components/business/crm-form-create/types';

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
      index: number;
      stage: StageConfigItem;
      stageIds: string[];
      keyword?: string;
      viewId?: string;
      advanceFilter?: any;
      refreshTimeStamp?: number;
      fieldList?: FormCreateField[];
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
    (e: 'init', total: number): void;
    (e: 'change', stageId: string): void;
    (e: 'openDetail', type: OpenDetailType, item: any): void;
  }>();

  const { t } = useI18n();

  const stageColor = computed(() => {
    const isSuccess =
      (props.stage?.type === 'END' && String(props.stage?.rate) === '100') ||
      (props.stage?.type === 'END' && ['COMPLETED', 'ARCHIVED'].includes(props.stage.id));
    const isFail =
      (props.stage?.type === 'END' && String(props.stage?.rate) === '0') ||
      (props.stage?.type === 'END' && ['VOIDED', 'VOID'].includes(props.stage.id));

    if (isSuccess) {
      return 'success';
    }
    if (isFail) {
      return 'error';
    }
    if (props.index === 0) {
      return 'info';
    }
    return 'primary';
  });

  const list = ref<any[]>([]);
  const optionMap = ref<Record<string, any>>({});
  const loading = ref(false);
  const pageNation = ref({
    total: 0,
    pageSize: 10,
    current: 1,
  });
  const statisticInfo = ref<StageBoardStatistic>({ amount: 0, averageAmount: 0 });

  async function loadStageData(refresh = true) {
    try {
      loading.value = true;
      if (refresh) {
        pageNation.value.current = 1;
      }

      const res = await props.loadStageList({
        current: pageNation.value.current || 1,
        pageSize: pageNation.value.pageSize,
        keyword: props.keyword,
        advanceFilter: props.advanceFilter,
        stageId: props.stage.id,
        viewId: props.viewId,
      });

      if (refresh) {
        list.value = [];
      }

      list.value = list.value.concat(res.list || []);
      pageNation.value.total = res.total || 0;
      optionMap.value = res.optionMap || {};
      emit('init', res.total || 0);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function loadStageStatistic() {
    if (!props.loadStageStatistic) {
      statisticInfo.value = { amount: 0, averageAmount: 0 };
      return;
    }

    try {
      statisticInfo.value = await props.loadStageStatistic({
        keyword: props.keyword,
        advanceFilter: props.advanceFilter,
        stageId: props.stage.id,
        viewId: props.viewId,
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function refreshList() {
    loadStageData();
    loadStageStatistic();
  }

  function openDetail(type: OpenDetailType, item: any) {
    emit('openDetail', type, item);
  }

  function handleReachBottom(e: Event) {
    const el = e.target as HTMLElement;
    if (el.scrollHeight - el.scrollTop <= el.clientHeight) {
      pageNation.value.current += 1;
      if (pageNation.value.current > Math.ceil(pageNation.value.total / pageNation.value.pageSize)) {
        return;
      }
      loadStageData(false);
    }
  }

  async function sortItem(item: any) {
    try {
      loading.value = true;
      const previousItem = list.value[item.newIndex - 1];
      const nextItem = list.value[item.newIndex + 1];
      await props.moveStageItem({
        item: item.data,
        fromStageId: item.from?.className?.split(' ')[1],
        toStageId: props.stage.id,
        previousItem,
        nextItem,
        rawItem: item,
      });
      refreshList();
      emit('change', item.from?.className?.split(' ')[1] || props.stage.id);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function handleUpdate(item: any) {
    try {
      loading.value = true;
      const previousItem = list.value[item.newIndex - 1];
      const nextItem = list.value[item.newIndex + 1];
      await props.moveStageItem({
        item: item.data,
        fromStageId: item.from?.className?.split(' ')[1],
        toStageId: props.stage.id,
        previousItem,
        nextItem,
        rawItem: item,
      });
      refreshList();
      if (item.to.className !== item.from.className) {
        emit('change', item.from.className.split(' ')[1]);
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function handleRemove(item: any) {
    list.value = list.value.filter((current) => current.id !== item.data.id);
    emit('change', item.from.className.split(' ')[1]);
  }

  async function handleAddItem(item: any) {
    const canContinue =
      (await props.beforeMoveToStage?.({
        item: item.data,
        fromStageId: item.from?.className?.split(' ')[1],
        toStageId: props.stage.id,
        rawItem: item,
      })) ?? true;

    if (!canContinue) {
      list.value = list.value.filter((current) => current.id !== item.data.id);
      return;
    }

    try {
      loading.value = true;
      const previousItem = list.value[item.newIndex - 1];
      const nextItem = list.value[item.newIndex + 1];
      await props.moveStageItem({
        item: item.data,
        fromStageId: item.from?.className?.split(' ')[1],
        toStageId: props.stage.id,
        previousItem,
        nextItem,
        rawItem: item,
      });
      refreshList();
      if (item.to.className !== item.from.className) {
        emit('change', item.from.className.split(' ')[1]);
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function resolveDraggedItem(evt: any) {
    return (
      evt?.draggedContext?.element ||
      evt?.draggedContext?.item?._underlying_vm_ ||
      evt?.item?._underlying_vm_ ||
      evt?.data ||
      undefined
    );
  }

  function handleMove(evt: any) {
    return (
      props.canMoveItem?.({
        fromStageId: evt.from?.className?.split(' ')[1],
        toStageId: evt.to?.className?.split(' ')[1],
        item: resolveDraggedItem(evt),
        rawEvent: evt,
      }) ?? true
    );
  }

  watch([() => props.refreshTimeStamp, () => props.viewId], () => {
    refreshList();
  });

  onBeforeMount(() => {
    loadStageStatistic();
    loadStageData();
  });

  defineExpose<StageBoardListExpose>({
    refreshList,
    sortItem,
  });
</script>

<style lang="less" scoped>
  .crm-stage-board-item {
    @apply flex cursor-move flex-col;

    margin-bottom: 8px;
    padding: 16px;
    border-radius: var(--border-radius-small);
    background-color: var(--text-n10);
    gap: 8px;
  }
  .crm-stage-board-item-ghost {
    @apply flex flex-col;

    padding: 16px;
    border: 1px solid var(--primary-8);
    border-radius: var(--border-radius-small);
    background-color: var(--primary-7);
    gap: 8px;
  }
  :deep(.crm-stage-board-item-desc) {
    @apply flex items-center;

    gap: 16px;
    .crm-stage-board-item-desc-label {
      @apply overflow-hidden overflow-ellipsis whitespace-nowrap;

      width: 60px;
      color: var(--text-n4);
      line-height: 24px;
      word-break: keep-all;
    }
    .crm-stage-board-item-desc-value {
      @apply flex-1 overflow-hidden;

      color: var(--text-n1);
    }
  }
  :deep(.crm-stage-board-item-desc--wide) {
    .crm-stage-board-item-desc-label {
      width: 72px;
    }
  }
  :deep(.n-progress-icon--as-text) {
    word-break: keep-all;
  }
  :deep(.n-spin-container) {
    height: calc(100% - 48px);
  }
</style>
