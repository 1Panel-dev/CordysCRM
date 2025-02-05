<template>
  <div ref="container" class="crm-overflow-tag-list w-full">
    <!-- 测量所有标签宽度 -->
    <div v-if="tags.length > 0" ref="measureRef" class="crm-measure-tag-item">
      <template v-for="(item, index) in tags" :key="`crm-measure-tag-item-${index}`">
        <CrmTag :size="props.size" :type="props.type" :theme="props.theme">
          {{ props.isStringTags ? item : item[props.labelKey] }}
        </CrmTag>
      </template>
    </div>
    <!-- 显示可见标签 -->
    <CrmTag
      v-for="(item, index) in visibleItems"
      :key="index"
      :size="props.size"
      :type="props.type"
      :theme="props.theme"
    >
      {{ props.isStringTags ? item : item[props.labelKey] }}
    </CrmTag>
    <!-- 显示 "+n" -->
    <n-tooltip trigger="hover" :disabled="hiddenItemsCount < 1" flip :delay="300" :placement="props.placement">
      <template #trigger>
        <div ref="moreButtonRef" class="more-items">
          <CrmTag
            v-if="hiddenItemsCount > 0"
            :tooltip-disabled="true"
            :size="props.size"
            :type="props.type"
            :theme="props.theme"
            class="tag"
          >
            +{{ hiddenItemsCount }}
          </CrmTag>
        </div>
      </template>
      {{ tagsTooltip }}
    </n-tooltip>
  </div>
</template>

<script setup lang="ts">
  import { NTooltip } from 'naive-ui';

  import CrmTag from '@/components/pure/crm-tag/index.vue';

  const props = withDefaults(
    defineProps<{
      tags: Record<string, any>[]; // 标签组
      size?: 'small' | 'medium' | 'large';
      type?: 'default' | 'primary' | 'info' | 'success' | 'warning' | 'error';
      theme?: 'dark' | 'light' | 'outline' | 'lightOutLine' | 'default';
      isStringTags?: boolean; // 是否是字符串标签 注: table使用时候不要打开table的省略属性，否则容器宽度会被限制导致无法自适应table单元格宽度
      labelKey?: string;
      placement?:
        | 'top-start'
        | 'top'
        | 'top-end'
        | 'right-start'
        | 'right'
        | 'right-end'
        | 'bottom-start'
        | 'bottom'
        | 'bottom-end'
        | 'left-start'
        | 'left'
        | 'left-end';
    }>(),
    {
      size: 'medium',
      type: 'default',
      theme: 'default',
      labelKey: 'label',
      isStringTags: false,
      placement: 'top',
    }
  );

  const container = ref<HTMLElement | null>(null);
  const measureRef = ref<HTMLElement | null>(null);
  const visibleItems = ref<Record<string, any>[]>([]);
  const hiddenItemsCount = ref<number>(0); // 隐藏数量
  const tagsTotalWidth = ref<number>(0); // 标签总宽度
  const defaultTagGap = 4; // 默认标签间距

  const moreButtonRef = ref<HTMLElement | null>(null);

  const updateVisibleItems = () => {
    if (!container.value || tagsTotalWidth.value === 0) return;

    const containerWidth = container.value.offsetWidth;

    const moreButtonWidth = moreButtonRef.value?.offsetWidth || 40;

    let currentWidth = 0;
    let visibleCount = 0;

    for (let i = 0; i < props.tags.length; i++) {
      const tagWidth = (measureRef.value?.children[i] as HTMLElement).offsetWidth;
      currentWidth += tagWidth + defaultTagGap;

      if (currentWidth + moreButtonWidth > containerWidth) break; // 预留 "+n" 按钮空间

      visibleCount++;
    }

    visibleItems.value = props.tags.slice(0, visibleCount);
    hiddenItemsCount.value = props.tags.length - visibleCount;
  };

  const updateVisibleItemsWithDelay = async () => {
    // 等待 DOM 更新，确保测量容器渲染完成
    await nextTick();

    if (measureRef.value) {
      tagsTotalWidth.value = measureRef.value.offsetWidth || 100;
    }

    updateVisibleItems(); // 计算可见项
  };

  const filterTagList = computed(() => {
    return (props.tags || []).filter((item: any) => item) || [];
  });

  const tagsTooltip = computed(() => {
    return filterTagList.value.map((e: any) => (props.isStringTags ? e : e[props.labelKey])).join('，');
  });

  const onResize = () => {
    updateVisibleItemsWithDelay();
  };

  let resizeObserver: ResizeObserver | null = null;

  onMounted(() => {
    updateVisibleItemsWithDelay();
    if (container.value) {
      resizeObserver = new ResizeObserver(updateVisibleItemsWithDelay);
      resizeObserver.observe(container.value);
    }
    window.addEventListener('resize', onResize);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('resize', onResize);
    if (resizeObserver && container.value) {
      resizeObserver.unobserve(container.value);
      resizeObserver = null;
    }
  });

  watch(
    () => props.tags,
    () => {
      nextTick(updateVisibleItemsWithDelay);
    }
  );
</script>

<style scoped lang="less">
  .crm-overflow-tag-list {
    display: flex;
    flex-wrap: nowrap;
    gap: 4px;
    align-items: center;
  }
  .crm-measure-tag-item {
    position: absolute;
    display: flex;
    align-items: center;
    visibility: hidden;
    flex-wrap: nowrap;
    pointer-events: none;
    gap: 4px;
  }
</style>
