<template>
  <div class="rounded-[4px] bg-[var(--text-n9)] px-[12px] py-[5px]">
    <n-tooltip v-if="descriptionItems.length" trigger="hover" :delay="300">
      <template #trigger>
        <div ref="descriptionWrapRef" class="relative min-w-0 overflow-hidden">
          <div class="min-w-0 overflow-hidden text-ellipsis whitespace-nowrap">
            <span
              v-for="item in visibleDescriptionItems"
              :key="item.id"
              class="mr-[8px] inline-flex align-middle text-[var(--text-n2)] last:mr-0"
            >
              {{ item.name }}
            </span>
          </div>
          <span
            v-if="hiddenDescriptionCount"
            class="absolute bottom-0 right-0 bg-[var(--text-n9)] pl-[4px] text-[var(--text-n2)]"
          >
            +{{ hiddenDescriptionCount }}
          </span>
          <div
            ref="descriptionMeasureRef"
            class="pointer-events-none invisible absolute left-0 top-0 inline-block whitespace-nowrap"
            aria-hidden="true"
          >
            <span
              v-for="item in descriptionItems"
              :key="item.id"
              class="mr-[8px] inline-flex align-middle text-[var(--text-n2)] last:mr-0"
            >
              {{ item.name }}
            </span>
          </div>
        </div>
      </template>
      {{ descriptionTooltip }}
    </n-tooltip>
    <n-tooltip v-else trigger="hover" :delay="300" :disabled="!description">
      <template #trigger>
        <span class="block w-full overflow-hidden text-ellipsis whitespace-nowrap text-[var(--text-n2)]">
          {{ description }}
        </span>
      </template>
      {{ description }}
    </n-tooltip>
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
  import { NTooltip } from 'naive-ui';

  import type { FlowNodeDescriptionItem } from '../../types';

  defineOptions({
    name: 'FlowNodeDescription',
  });

  const props = withDefaults(
    defineProps<{
      description?: string;
      items?: FlowNodeDescriptionItem[];
    }>(),
    {
      description: '',
      items: () => [],
    }
  );

  const visibleDescriptionCount = ref(0);
  const descriptionItems = computed(() => props.items.filter((item) => item.name));
  const visibleDescriptionItems = computed(() => descriptionItems.value.slice(0, visibleDescriptionCount.value));
  const hiddenDescriptionCount = computed(() =>
    Math.max(descriptionItems.value.length - visibleDescriptionCount.value, 0)
  );
  const descriptionTooltip = computed(() => descriptionItems.value.map((item) => item.name).join('、'));
  let descriptionResizeObserver: ResizeObserver | null = null;

  function getItemWidth(element: Element) {
    const item = element as HTMLElement;
    return item.offsetWidth + Number.parseFloat(window.getComputedStyle(item).marginRight || '0');
  }

  // 逐个累加完整名称宽度，并给右侧 “+N” 预留空间。
  function getVisibleCount(itemElements: Element[], availableWidth: number) {
    let usedWidth = 0;
    for (let index = 0; index < itemElements.length; index += 1) {
      const hiddenCount = itemElements.length - index - 1;
      const reservedWidth = hiddenCount > 0 ? `+${hiddenCount}`.length * 8 + 4 : 0;
      const itemWidth = getItemWidth(itemElements[index]);
      if (usedWidth + itemWidth + reservedWidth > availableWidth) {
        return index;
      }

      usedWidth += itemWidth;
    }

    return itemElements.length;
  }

  const descriptionWrapRef = ref<HTMLElement | null>(null);
  const descriptionMeasureRef = ref<HTMLElement | null>(null);

  // 只监听可见容器宽度，隐藏测量层随内容变化由 watch 主动重算。
  function observeDescriptionWrap() {
    if (descriptionResizeObserver && descriptionWrapRef.value) {
      descriptionResizeObserver.observe(descriptionWrapRef.value);
    }
  }

  function updateDescriptionOverflow() {
    const wrap = descriptionWrapRef.value;
    const measure = descriptionMeasureRef.value;
    if (!wrap || !measure) {
      visibleDescriptionCount.value = descriptionItems.value.length;
      return;
    }

    const itemElements = Array.from(measure.children);
    const totalWidth = itemElements.reduce((sum, item) => sum + getItemWidth(item), 0);
    visibleDescriptionCount.value =
      totalWidth <= wrap.clientWidth ? itemElements.length : getVisibleCount(itemElements, wrap.clientWidth);
  }

  onMounted(() => {
    if (typeof ResizeObserver !== 'undefined') {
      descriptionResizeObserver = new ResizeObserver(updateDescriptionOverflow);
    }
    observeDescriptionWrap();
    updateDescriptionOverflow();
  });

  onBeforeUnmount(() => {
    descriptionResizeObserver?.disconnect();
  });

  watch(descriptionItems, async () => {
    await nextTick();
    updateDescriptionOverflow();
  });
</script>
