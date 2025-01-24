<template>
  <n-virtual-list
    v-bind="props"
    ref="listRef"
    :class="['crm-list', containerStatusClass, props.class]"
    :item-size="props.itemHeight"
    :items="listData"
    :style="{
      height: props.virtualScrollHeight,
    }"
    @scroll="handleScroll"
    @wheel="handleWheel"
  >
    <template #default="{ item }">
      <div :key="item.key">
        <slot :item="item">
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
          >
            <div class="flex-1 overflow-x-hidden" @click="emit('itemClick', item)">
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
                  :options="props.itemMoreActions"
                  trigger="click"
                  @select="handleMoreActionSelect($event, item)"
                  @close="handleMoreActionClose"
                  @click="handleClickMore(item)"
                >
                </CrmMoreAction>
              </div>
              <slot name="itemRight" :item="item"></slot>
            </div>
          </div>
        </slot>
      </div>
    </template>
  </n-virtual-list>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NVirtualList } from 'naive-ui';

  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';

  import useContainerShadow from '@/hooks/useContainerShadow';

  import { useDraggable } from 'vue-draggable-plus';

  const props = withDefaults(
    defineProps<{
      mode?: 'static' | 'remote'; // 静态数据或者远程数据
      bordered?: boolean; // 是否显示边框
      activeItemKey?: string | number; // 当前选中的项的 key
      focusItemKey?: string | number; // 聚焦的项的 key
      itemMoreActions?: ActionsItem[]; // 节点展示在省略号按钮内的更多操作
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
    innerFocusItemKey.value = '';
    emit('moreActionsClose');
  }

  function handleClickMore(item: Record<string, any>) {
    innerFocusItemKey.value = item[props.keyField];
    emit('clickMore', item);
  }

  const listRef: Ref = ref(null);
  // TODO 暂时还未做拖拽
  watch(
    listData.value,
    () => {
      if (listData.value.length > 0 && !isInitListener.value) {
        // 如果数据不为空，且没有初始化容器的滚动监听器
        if (props.draggable) {
          // 如果开启拖拽
          if (props.virtualScrollHeight) {
            // 如果开启虚拟滚动，需要将拖拽的容器设置为虚拟滚动的容器
            useDraggable('.crm-list .v-vl', listData, {
              ghostClass: 'crm-list-ghost',
            });
          } else {
            // 否则直接设置为列表容器
            useDraggable('.v-vl', listData, {
              ghostClass: 'crm-list-ghost',
            });
          }
        }
        nextTick(() => {
          const listContent = listRef.value?.$el.querySelector('.v-vl');

          setContainer(listContent);
          initScrollListener();
        });
      }
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
    if (scrollTop + clientHeight >= scrollHeight) {
      handleReachBottom();
    }
  }

  function handleWheel(event: Event) {
    emit('wheel', event);
  }
</script>

<style lang="less">
  .crm-list {
    .crm-container--shadow-y();
    .v-vl-visible-items {
      padding-right: 10px;
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
  }
  .crm-list-ghost {
    opacity: 0.5;
  }
</style>
