<template>
  <div ref="batchActionWrapperRef" :class="`mb-[16px] flex w-full flex-nowrap items-center justify-between gap-[12px]`">
    <div ref="batchActionLeftSlotRef">
      <slot name="actionLeft"></slot>
    </div>

    <div
      v-if="props.selectRowCount > 0"
      ref="actionContainerRef"
      class="flex flex-grow flex-nowrap items-center gap-[12px]"
    >
      <n-button
        v-for="item in baseAction"
        :key="item.key"
        :type="item.danger ? 'error' : 'primary'"
        :size="props.size"
        ghost
        :class="`n-btn-outline-${item.danger ? 'error' : 'primary'}`"
        @click="handleSelect(item)"
      >
        {{ item.label }}
      </n-button>
      <div v-if="moreAction.length" ref="moreButtonRef" :class="`${moreActionClass}`">
        <CrmMoreAction :options="moreAction" :size="props.size" @select="handleMoreSelect" />
      </div>
      <div ref="selectCountRef" class="flex flex-nowrap items-center gap-[4px]" :class="selectCountClass">
        <div class="whitespace-nowrap">{{ t('crmPagination.checked') }}</div>
        <div class="whitespace-nowrap text-[var(--primary-8)]">{{ props.selectRowCount }}</div>
        <div>{{ t('crmPagination.item') }}</div>
      </div>
      <n-button
        v-if="moreAction?.length || baseAction.length"
        ref="clearButtonRef"
        :size="props.size"
        quaternary
        type="primary"
        :class="`text-btn-primary ${clearBtnClass}`"
        @click="emit('clear')"
      >
        {{ t('common.clear') }}
      </n-button>
    </div>

    <div ref="batchActionRightSlotRef">
      <slot name="actionRight"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import { ActionsItem } from '@/components/pure/crm-more-action/type';

  import { hasAllPermission, hasAnyPermission } from '@/utils/permission';

  import { BatchActionConfig } from '../type';

  const { t } = useI18n();

  const props = defineProps<{
    selectRowCount: number;
    actionConfig?: BatchActionConfig;
    size?: 'small' | 'medium' | 'large';
  }>();

  const emit = defineEmits<{
    (e: 'batchAction', value: ActionsItem): void;
    (e: 'clear'): void;
  }>();

  const baseAction = ref<ActionsItem[]>([]);
  const moreAction = ref<ActionsItem[]>([]);
  const originalBaseAction = ref<ActionsItem[]>([]);
  const originalMoreAction = ref<ActionsItem[]>([]);

  // 存储所有的action
  const allAction = ref<ActionsItem[]>([]);
  const refResizeObserver = ref<ResizeObserver>();

  const moreActionLength = ref<number>(0);
  const actionContainerRef = ref<HTMLElement>();

  const handleMoreActionLength = () => {
    moreActionLength.value = moreAction.value.length;
  };

  const moreActionClass = 'crm-batch-action-more-button';
  const clearBtnClass = 'crm-batch-clear-button';
  const selectCountClass = 'crm-select-count';

  const batchActionWrapperRef = ref();
  const moreButtonRef = ref();
  const clearButtonRef = ref();
  const batchActionLeftSlotRef = ref();
  const batchActionRightSlotRef = ref();
  const selectCountRef = ref();

  // 计算容器元素宽度
  function calculateWidths() {
    const wrapperWidth = batchActionWrapperRef.value?.clientWidth || 0;
    const moreButtonWidth = moreButtonRef.value?.clientWidth || 0;
    const clearButtonWidth = clearButtonRef.value?.clientWidth || 0;
    const leftContentWidth = batchActionLeftSlotRef.value?.clientWidth || 0;
    const rightContentWidth = batchActionRightSlotRef.value?.clientWidth || 0;
    const selectCountWidth = selectCountRef.value?.clientWidth || 0;
    return { wrapperWidth, moreButtonWidth, clearButtonWidth, leftContentWidth, rightContentWidth, selectCountWidth };
  }

  const filterActionList = (list: ActionsItem[] = []) =>
    list.filter((e) => hasAllPermission(e.permission) && hasAnyPermission(e?.anyPermission));

  function estimateButtonWidth(action: ActionsItem): number {
    const baseWidth = 60;
    const charWidth = 8;
    return baseWidth + (action.label?.length ?? 0) * charWidth;
  }

  const computedLastVisibleIndex = () => {
    if (!actionContainerRef.value) return;

    const gapDis = 12;

    const { wrapperWidth, moreButtonWidth, clearButtonWidth, leftContentWidth, rightContentWidth, selectCountWidth } =
      calculateWidths();

    // 固定内容宽度（slot、清除按钮、已选择数量、more按钮预留）
    const fixedWidth =
      leftContentWidth + rightContentWidth + clearButtonWidth + selectCountWidth + moreButtonWidth + gapDis * 5;

    const availableWidth = wrapperWidth - fixedWidth;

    let totalWidth = 0;
    let lastVisibleIndex = -1;

    for (let i = 0; i < originalBaseAction.value.length; i++) {
      const currentAction = originalBaseAction.value[i] as ActionsItem;
      const buttonWidth = estimateButtonWidth(currentAction) + gapDis;
      if (totalWidth + buttonWidth <= availableWidth - 24) {
        totalWidth += buttonWidth;
        lastVisibleIndex = i;
      } else {
        break;
      }
    }

    baseAction.value = originalBaseAction.value.slice(0, lastVisibleIndex + 1);
    moreAction.value = [...originalBaseAction.value.slice(lastVisibleIndex + 1), ...originalMoreAction.value];

    handleMoreActionLength();
  };

  function handleSelect(item: ActionsItem) {
    emit('batchAction', item);
  }

  function handleMoreSelect(item: ActionsItem) {
    handleSelect(item);
  }

  watch(
    () => props.actionConfig,
    (newVal) => {
      if (newVal) {
        const newBaseAction = filterActionList(newVal.baseAction);
        const newMoreAction = filterActionList(newVal.moreAction ?? []);

        allAction.value = [...newBaseAction, ...newMoreAction];
        baseAction.value = [...newBaseAction];
        moreAction.value = [...newMoreAction];
        originalBaseAction.value = [...newBaseAction];
        originalMoreAction.value = [...newMoreAction];
        nextTick(() => {
          computedLastVisibleIndex();
        });
      }
    },
    { immediate: true }
  );

  watch(
    () => props.selectRowCount,
    (val) => {
      if (val) {
        nextTick(() => {
          computedLastVisibleIndex();
        });
      }
    }
  );

  onMounted(() => {
    refResizeObserver.value = new ResizeObserver((entries: ResizeObserverEntry[]) => {
      entries.forEach(computedLastVisibleIndex);
    });

    if (batchActionWrapperRef.value) {
      refResizeObserver.value.observe(batchActionWrapperRef.value);
    }
  });

  onUnmounted(() => {
    refResizeObserver.value?.disconnect();
  });
</script>

<style scoped></style>
