<template>
  <n-split v-model:size="size" direction="horizontal" :max="props.max" :min="props.min">
    <template #1><slot name="1"></slot></template>
    <template #resize-trigger>
      <div class="n-split__resize-trigger">
        <div class="n-split__resize-trigger-icon" @click.stop="changeSplit">
          <CrmIconFont :type="collapsed ? 'iconicon_page_last' : 'iconicon_page_first'" />
        </div>
      </div>
    </template>
    <template #2><slot name="2"></slot></template>
  </n-split>
</template>

<script setup lang="ts">
  import { NSplit } from 'naive-ui';

  import CrmIconFont from '@/components/pure/crm-icon-font/index.vue';

  const props = defineProps<{
    defaultSize: number | string;
    max: number | string;
    min: number | string;
  }>();

  const size = defineModel<number | string>('size', {
    default: 0.25,
  });
  const collapsed = ref(false);

  watch(
    () => size.value,
    (val) => {
      if (val === 0) {
        collapsed.value = true;
      } else {
        collapsed.value = false;
      }
    }
  );

  function changeSplit() {
    collapsed.value = !collapsed.value;
    if (collapsed.value) {
      size.value = 0;
    } else {
      size.value = props.defaultSize || 0.25;
    }
  }
</script>

<style lang="less">
  .n-split__resize-trigger-wrapper {
    .n-split__resize-trigger {
      width: 1px !important;
      height: 100%;
      background-color: var(--text-n8);
      .n-split__resize-trigger-icon {
        @apply relative z-10 flex cursor-pointer items-center justify-center;

        top: 28px;
        width: 14px;
        height: 24px;
        border: 1px solid var(--text-n8);
        border-left: 0;
        border-radius: 0 var(--border-radius-mini) var(--border-radius-mini) 0;
        background-color: var(--text-n10);
      }
    }
    &:hover {
      .n-split__resize-trigger {
        width: 3px !important;
      }
    }
  }
  .n-split-pane-1,
  .n-split-pane-2 {
    transition: flex 0.3s ease;
  }
</style>
