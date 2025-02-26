<template>
  <div
    ref="batchActionWrapperRef"
    :class="`mb-[16px] flex w-full flex-nowrap items-center gap-[12px] ${
      props.selectRowCount < 1 ? 'justify-between' : ''
    }`"
  >
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

      <n-button
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

  import { getNodeWidth } from '@lib/shared/method/dom';

  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import { ActionsItem } from '@/components/pure/crm-more-action/type';

  import { useI18n } from '@/hooks/useI18n';

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

  // 存储所有的action
  const allAction = ref<ActionsItem[]>([]);
  const refResizeObserver = ref<ResizeObserver>();

  const moreActionLength = ref<number>(0);
  const actionContainerRef = ref<HTMLElement>();

  // 控制是否重新计算
  const computedStatus = ref(true);

  const handleMoreActionLength = () => {
    moreActionLength.value = moreAction.value.length;
  };

  const moreActionClass = 'crm-batch-action-more-button';
  const clearBtnClass = 'crm-batch-clear-button';

  const batchActionWrapperRef = ref();
  const moreButtonRef = ref();
  const clearButtonRef = ref();
  const batchActionLeftSlotRef = ref();
  const batchActionRightSlotRef = ref();

  // 计算容器元素宽度
  function calculateWidths() {
    const wrapperWidth = batchActionWrapperRef.value?.clientWidth || 0;
    const moreButtonWidth = moreButtonRef.value?.clientWidth || 0;
    const clearButtonWidth = clearButtonRef.value?.clientWidth || 0;
    const leftContentWidth = batchActionLeftSlotRef.value?.clientWidth || 0;
    const rightContentWidth = batchActionRightSlotRef.value?.clientWidth || 0;
    return { wrapperWidth, moreButtonWidth, clearButtonWidth, leftContentWidth, rightContentWidth };
  }

  const computedLastVisibleIndex = () => {
    if (!actionContainerRef.value) {
      return;
    }
    if (!computedStatus.value) {
      computedStatus.value = true;
      return;
    }

    const { wrapperWidth, moreButtonWidth, clearButtonWidth, leftContentWidth, rightContentWidth } = calculateWidths();

    const childNodeList = [].slice.call(actionContainerRef.value.children) as HTMLElement[];
    // 初始总宽度为 左边容器 + 右边容器
    let totalWidth = leftContentWidth + rightContentWidth;
    let menuItemIndex = 0;
    // gap间距
    const gapDis = 12;

    for (let i = 0; i < childNodeList.length; i++) {
      const node = childNodeList[i];
      const classNames = node.className.split(' ');

      const isMoreButton = classNames.includes(moreActionClass);
      const isClearBtn = classNames.includes(clearBtnClass);
      // 更多按钮
      if (isMoreButton) {
        totalWidth += moreButtonWidth + gapDis;
        // 清空按钮
      } else if (isClearBtn) {
        totalWidth += clearButtonWidth + gapDis;
        // 基础操作按钮
      } else {
        totalWidth += getNodeWidth(node) + gapDis;
      }

      if (totalWidth > wrapperWidth) {
        const value = isClearBtn ? menuItemIndex - 1 : menuItemIndex - 2;
        baseAction.value = allAction.value.slice(0, value);
        moreAction.value = allAction.value.slice(value);
        handleMoreActionLength();
        computedStatus.value = false;
        return;
      }
      menuItemIndex++;
    }

    moreAction.value = props.actionConfig?.moreAction || [];
    baseAction.value = props.actionConfig?.baseAction || [];
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
        const newBaseAction = newVal.baseAction;
        const newMoreAction = newVal.moreAction ?? [];

        allAction.value = [...newBaseAction, ...newMoreAction];
        baseAction.value = [...newBaseAction];
        moreAction.value = [...newMoreAction];
      }
    },
    { immediate: true }
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
