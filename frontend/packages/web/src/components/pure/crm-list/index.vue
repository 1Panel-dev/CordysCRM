<template>
  <n-spin :show="props.loading" class="h-full" content-class="h-full">
    <VueDraggable
      v-if="props.draggable"
      ref="draggableListRef"
      v-model="listData"
      tag="div"
      :class="[
        'crm-list',
        'crm-list-draggable',
        suppressDragHover ? 'crm-list--suppress-hover' : '',
        containerStatusClass,
        props.class,
      ]"
      :style="{ height: props.virtualScrollHeight }"
      handle=".crm-list-drag-icon"
      ghost-class="crm-list-ghost"
      :animation="150"
      @scroll="handleScroll"
      @wheel="handleWheel"
      @start="handleDragStart"
      @end="handleDragEnd"
    >
      <template v-for="item of listData" :key="item[props.keyField]">
        <slot name="item" :item="item">
          <div
            :class="[
              'crm-list-item',
              props.noHover ? 'crm-list-item--no-hover' : '',
              props.itemBorder ? 'crm-list-item--bordered' : '',
              props.itemClass,
              innerFocusItemKey === item[keyField] ? 'crm-list-item--focus' : '',
              innerActiveItemKey === item[keyField] ? props.activeItemClass : '',
            ]"
            :style="{ height: `${props.itemHeight}px` }"
            @pointerdown="handleItemPointerDown"
            @click="handleItemClick(item)"
          >
            <span class="crm-list-drag-icon cursor-move">
              <CrmIcon type="iconicon_move" class="text-[var(--text-n4)]" />
            </span>
            <slot name="titleLeft" :item="item"></slot>
            <div class="flex-1 overflow-x-hidden">
              <slot name="title" :item="item"></slot>
            </div>
            <div class="flex items-center gap-[4px]">
              <div
                v-if="
                  $slots['itemAction'] || (props.itemMoreActions && props.itemMoreActions.length > 0 && !props.disabled)
                "
                class="crm-list-item-actions"
                :style="{ visibility: suppressDragHover ? 'hidden' : undefined }"
              >
                <slot name="itemAction" :item="item"></slot>
                <CrmMoreAction
                  v-if="props.itemMoreActions && props.itemMoreActions.length > 0"
                  :options="
                    typeof props.itemMoreActions === 'function' ? props.itemMoreActions(item) : props.itemMoreActions
                  "
                  trigger="click"
                  @select="handleMoreActionSelect($event, item)"
                  @close="handleMoreActionClose"
                  @update-show="handleMoreActionUpdateShow"
                  @click.stop="handleClickMore(item)"
                >
                </CrmMoreAction>
              </div>
              <slot name="itemRight" :item="item"></slot>
            </div>
          </div>
        </slot>
      </template>
    </VueDraggable>
    <n-virtual-list
      v-else
      v-bind="props"
      ref="listRef"
      :class="['crm-list', containerStatusClass, props.class]"
      :item-size="props.itemHeight"
      :items="listData"
      :style="{
        height: props.virtualScrollHeight,
      }"
      item-resizable
      @scroll="handleScroll"
      @wheel="handleWheel"
    >
      <template #default="{ item }">
        <slot name="item" :item="item">
          <div
            :key="item[props.keyField]"
            :class="[
              'crm-list-item',
              props.noHover ? 'crm-list-item--no-hover' : '',
              props.itemBorder ? 'crm-list-item--bordered' : '',
              props.itemClass,
              innerFocusItemKey === item[keyField] ? 'crm-list-item--focus' : '',
              innerActiveItemKey === item[keyField] ? props.activeItemClass : '',
            ]"
            :style="{
              height: `${props.itemHeight}px`,
            }"
            @pointerdown="handleItemPointerDown"
            @click="handleItemClick(item)"
          >
            <slot name="titleLeft" :item="item"></slot>
            <div class="flex-1 overflow-x-hidden">
              <slot name="title" :item="item"></slot>
            </div>
            <div class="flex items-center gap-[4px]">
              <div
                v-if="
                  $slots['itemAction'] || (props.itemMoreActions && props.itemMoreActions.length > 0 && !props.disabled)
                "
                class="crm-list-item-actions"
              >
                <slot name="itemAction" :item="item"></slot>
                <CrmMoreAction
                  v-if="props.itemMoreActions && props.itemMoreActions.length > 0"
                  :options="
                    typeof props.itemMoreActions === 'function' ? props.itemMoreActions(item) : props.itemMoreActions
                  "
                  trigger="click"
                  @select="handleMoreActionSelect($event, item)"
                  @close="handleMoreActionClose"
                  @update-show="handleMoreActionUpdateShow"
                  @click.stop="handleClickMore(item)"
                >
                </CrmMoreAction>
              </div>
              <slot name="itemRight" :item="item"></slot>
            </div>
          </div>
        </slot>
      </template>
    </n-virtual-list>
  </n-spin>
</template>

<script setup lang="ts">
  import { NSpin, NVirtualList } from 'naive-ui';
  import { VueDraggable } from 'vue-draggable-plus';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';

  import useContainerShadow from '@/hooks/useContainerShadow';

  const props = withDefaults(
    defineProps<{
      mode?: 'static' | 'remote'; // 静态数据或者远程数据
      bordered?: boolean; // 是否显示边框
      activeItemKey?: string | number; // 当前选中的项的 key
      focusItemKey?: string | number; // 聚焦的项的 key
      itemMoreActions?: ActionsItem[] | ((item: Record<string, any>) => ActionsItem[]); // 节点展示在省略号按钮内的更多操作
      keyField?: string; // 唯一值 key 的字段名，默认为 key
      itemHeight?: number; // 每一项的高度
      emptyText?: string; // 空数据时的文案
      noMoreData?: boolean; // 远程模式下，是否没有更多数据
      noHover?: boolean; // 是否不显示列表项的 hover 效果
      itemBorder?: boolean; // 是否显示列表项的边框
      draggable?: boolean; // 是否允许拖拽
      disabled?: boolean;
      class?: string;
      itemClass?: string;
      activeItemClass?: string;
      virtualScrollHeight: string;
      loading?: boolean;
    }>(),
    {
      mode: 'static',
      keyField: 'key',
      itemHeight: 34,
      bordered: false,
      draggable: false,
      maxHeight: '300px',
      activeItemClass: 'crm-list-item--active',
    }
  );

  const emit = defineEmits<{
    (e: 'moreActionSelect', event: ActionsItem, item: Record<string, any>): void;
    (e: 'moreActionsClose'): void;
    (e: 'reachBottom'): void;
    (e: 'itemClick', item: Record<string, any>): void;
    (e: 'clickMore', item: Record<string, any>): void;
    (e: 'wheel', event: Event): void;
  }>();

  const listData = defineModel<Record<string, any>[]>('data', {
    default: [],
  });

  const innerFocusItemKey = defineModel<string>('focusItemKey', {
    default: '',
  });

  const innerActiveItemKey = defineModel<string>('activeItemKey', {
    default: '',
  });

  const { isInitListener, containerStatusClass, setContainer, initScrollListener } = useContainerShadow({
    overHeight: props.itemHeight,
    containerClassName: 'crm-list',
  });

  function handleMoreActionSelect(event: ActionsItem, item: Record<string, any>) {
    innerFocusItemKey.value = item[props.keyField];
    emit('moreActionSelect', event, item);
  }

  function handleMoreActionClose() {
    if (!innerFocusItemKey.value) return;
    innerFocusItemKey.value = '';
    emit('moreActionsClose');
  }

  function handleMoreActionUpdateShow(show: boolean) {
    if (!show) handleMoreActionClose();
  }

  function handleClickMore(item: Record<string, any>) {
    innerFocusItemKey.value = item[props.keyField];
    emit('clickMore', item);
  }

  const listRef: Ref = ref(null);
  const draggableListRef = ref();
  const isDragging = ref(false);
  const suppressDragClick = ref(false);
  const suppressDragHover = ref(false);

  function handleDragStart() {
    isDragging.value = true;
    suppressDragHover.value = true;
  }

  function handleDragEnd(event: { oldIndex?: number; newIndex?: number }) {
    if (event.oldIndex !== undefined && event.newIndex !== undefined && event.oldIndex !== event.newIndex) {
      suppressDragClick.value = true;
    }
    nextTick(() => {
      isDragging.value = false;
    });
  }

  function handleItemPointerDown() {
    suppressDragClick.value = false;
    if (!isDragging.value) suppressDragHover.value = false;
  }

  function handleItemClick(item: Record<string, any>) {
    if (suppressDragClick.value) {
      suppressDragClick.value = false;
      return;
    }
    emit('itemClick', item);
  }

  watch(
    () => [listData.value.length, props.draggable],
    () => {
      nextTick(() => {
        const listContent = props.draggable ? draggableListRef.value?.$el : listRef.value?.$el.querySelector('.v-vl');
        if (!listContent) return;
        setContainer(listContent);
        initScrollListener();
      });
    },
    {
      immediate: true,
    }
  );

  function handleReachBottom() {
    if (props.mode === 'remote' && !props.noMoreData && listData.value.length > 0) {
      emit('reachBottom');
    }
  }

  function handleScroll(event: Event) {
    const target = event.target as HTMLElement;
    const { scrollTop, scrollHeight, clientHeight } = target;
    // 判断是否触底
    if (scrollTop + clientHeight >= scrollHeight - 24) {
      handleReachBottom();
    }
  }

  function handleWheel(event: Event) {
    emit('wheel', event);
  }

  function scrollTo(options: { top?: number; left?: number }) {
    if (listRef.value?.scrollTo) {
      listRef.value.scrollTo(options);
      return;
    }

    const listContent = listRef.value?.$el.querySelector('.v-vl');

    if (!listContent) {
      return;
    }

    if (typeof options.top === 'number') {
      listContent.scrollTop = options.top;
    }
    if (typeof options.left === 'number') {
      listContent.scrollLeft = options.left;
    }
  }

  defineExpose({
    scrollTo,
  });
</script>

<style lang="less">
  .crm-list {
    .crm-container--shadow-y();
    .v-vl-visible-items {
      padding-right: 5px;
    }
    .crm-list-item {
      border-radius: var(--border-radius-small);
      @apply flex w-full cursor-pointer items-center justify-between;
      .crm-list-item-actions {
        @apply invisible flex items-center justify-end;
      }
      &:hover {
        background: var(--primary-7);
        // TODO 暂时没有拖拽
        .crm-list-drag-icon {
          @apply visible;
        }
        .crm-list-item-actions {
          @apply visible;
        }
      }
    }
    .crm-list-item--no-hover {
      @apply cursor-auto;
      &:hover {
        background-color: transparent;
      }
    }
    .crm-list-item--bordered {
      border: 1px solid var(--text-n8);
    }
    .crm-list-item--focus {
      background-color: var(--primary-7);
      .crm-list-item-actions {
        @apply visible;
      }
    }
    .crm-list-item--active {
      color: var(--primary-8);
    }
    &.crm-list--suppress-hover {
      .crm-list-item:hover {
        background: transparent;
      }
      .crm-list-item-actions {
        visibility: hidden;
      }
    }
  }
  .crm-list-draggable {
    overflow-y: auto;
  }
  .crm-list-ghost {
    opacity: 0.5;
  }
</style>
