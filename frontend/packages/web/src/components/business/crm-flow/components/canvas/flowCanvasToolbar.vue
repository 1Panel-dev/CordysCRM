<template>
  <div class="flow-canvas-toolbar flex gap-[8px]">
    <n-tabs
      :value="viewMode"
      type="segment"
      size="large"
      class="flow-canvas-toolbar__view-tabs"
      @update:value="setViewMode"
    >
      <n-tab-pane name="compact" class="hidden">
        <template #tab>
          <n-tooltip trigger="hover">
            <template #trigger>
              <CrmIcon type="iconicon_simplified" :size="16" />
            </template>
            <span>{{ t('crmFlow.viewModeCompactTip') }}</span>
          </n-tooltip>
        </template>
      </n-tab-pane>
      <n-tab-pane name="detail" class="hidden">
        <template #tab>
          <n-tooltip trigger="hover">
            <template #trigger>
              <CrmIcon type="iconicon_numerous" :size="16" />
            </template>
            <span>{{ t('crmFlow.viewModeDetailTip') }}</span>
          </n-tooltip>
        </template>
      </n-tab-pane>
    </n-tabs>

    <div class="flow-canvas-toolbar_navigator large-box-shadow">
      <n-tooltip trigger="hover">
        <template #trigger>
          <CrmIcon
            class="flow-canvas-toolbar__icon"
            :class="{ 'is-active': isPanMode }"
            type="iconicon_move2"
            :size="16"
            @click="togglePanMode"
          />
        </template>
        <span>{{ t('crmFlow.pan') }}</span>
      </n-tooltip>

      <n-divider vertical class="!m-0" />

      <n-tooltip trigger="hover">
        <template #trigger>
          <CrmIcon
            class="flow-canvas-toolbar__icon"
            :class="{ 'is-disabled': !canZoomOut }"
            type="iconicon_zoom_out1"
            :size="16"
            @click="zoomOut"
          />
        </template>
        <span>{{ t('crmFlow.zoomOut') }}</span>
      </n-tooltip>

      <n-dropdown
        trigger="click"
        placement="top"
        class="zoom-dropdown"
        :options="zoomDropdownOptions"
        :value="selectedZoomOptionKey"
        @select="handleZoomSelect"
      >
        <span class="flow-canvas-toolbar__zoom">{{ zoomPercent }}%</span>
      </n-dropdown>

      <n-tooltip trigger="hover">
        <template #trigger>
          <CrmIcon
            class="flow-canvas-toolbar__icon"
            :class="{ 'is-disabled': !canZoomIn }"
            type="iconicon_zoom_in1"
            :size="18"
            @click="zoomIn"
          />
        </template>
        <span>{{ t('crmFlow.zoomIn') }}</span>
      </n-tooltip>

      <n-divider vertical class="!m-0" />

      <n-tooltip trigger="hover">
        <template #trigger>
          <CrmIcon class="flow-canvas-toolbar__icon" type="iconicon_aiming" :size="16" @click="centerContent" />
        </template>
        <span>{{ t('crmFlow.centerNode') }}</span>
      </n-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, onBeforeUnmount, ref, watch } from 'vue';
  import { type DropdownOption, NDivider, NDropdown, NTabPane, NTabs, NTooltip } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import type { FlowGraphController } from '../../graph/types';

  defineOptions({
    name: 'FlowCanvasToolbar',
  });

  const props = defineProps<{
    graphController: FlowGraphController | null;
    isPanMode: boolean;
    viewMode: 'compact' | 'detail';
  }>();
  const emit = defineEmits<{
    (event: 'togglePanMode'): void;
    (event: 'updateViewMode', value: 'compact' | 'detail'): void;
  }>();

  const { t } = useI18n();

  const isPanMode = computed(() => props.isPanMode);
  const viewMode = computed(() => props.viewMode);

  function togglePanMode() {
    emit('togglePanMode');
  }

  function setViewMode(mode: 'compact' | 'detail') {
    emit('updateViewMode', mode);
  }

  // 自适应
  function centerContent() {
    props.graphController?.centerContent();
  }

  const zoomPercent = ref(100);
  const zoomLevels = [50, 75, 100, 125, 150, 200] as const;
  const canZoomOut = computed(() => zoomPercent.value > 50);
  const canZoomIn = computed(() => zoomPercent.value < 200);

  const selectedZoomOptionKey = computed(() => {
    const key = `zoom-${zoomPercent.value}`;
    return zoomLevels.includes(zoomPercent.value as (typeof zoomLevels)[number]) ? key : undefined;
  });
  const zoomDropdownOptions = computed<DropdownOption[]>(() => [
    ...zoomLevels.map((item) => ({
      label: `${item}%`,
      key: `zoom-${item}`,
    })),
    {
      type: 'divider',
      key: 'zoom-divider',
    },
    {
      label: t('crmFlow.fitToContent'),
      key: 'fit',
    },
  ]);

  function syncZoomPercent() {
    const zoom = props.graphController?.getZoom?.() ?? 1;
    const nextPercent = Math.round(zoom * 100);
    zoomPercent.value = Number.isFinite(nextPercent) ? nextPercent : 100;
  }

  function zoomIn() {
    if (!canZoomIn.value) {
      return;
    }
    props.graphController?.zoomIn();
    syncZoomPercent();
  }

  function zoomOut() {
    if (!canZoomOut.value) {
      return;
    }
    props.graphController?.zoomOut();
    syncZoomPercent();
  }

  function handleZoomSelect(key: string) {
    if (key === 'fit') {
      props.graphController?.fitToContent();
      syncZoomPercent();
      return;
    }

    const percent = Number(key.replace('zoom-', ''));
    if (!Number.isFinite(percent)) {
      return;
    }
    props.graphController?.setZoom(percent / 100);
    syncZoomPercent();
  }

  function bindScaleListener() {
    const graph = props.graphController?.getGraph();
    if (!graph) {
      return undefined;
    }
    graph.on('scale', syncZoomPercent);
    return () => {
      graph.off('scale', syncZoomPercent);
    };
  }

  // 让工具栏里的 100% 和画布真实缩放同步
  watch(
    () => props.graphController,
    (_controller, _old, onCleanup) => {
      syncZoomPercent();
      const unbind = bindScaleListener();
      onCleanup(() => {
        unbind?.();
      });
    },
    {
      immediate: true,
    }
  );

  onBeforeUnmount(() => {
    const graph = props.graphController?.getGraph();
    graph?.off('scale', syncZoomPercent);
  });
</script>

<style scoped lang="less">
  .flow-canvas-toolbar__view-tabs {
    :deep(.n-tabs-tab) {
      padding: 6px;
    }
    :deep(.n-tabs-tab--active) {
      color: var(--primary-8) !important;
    }
  }
  .flow-canvas-toolbar_navigator {
    display: flex;
    align-items: center;
    padding: 0 12px;
    height: 32px;
    border-radius: 4px;
    background: var(--text-n10);
    gap: 12px;
  }
  .flow-canvas-toolbar__zoom {
    width: 40px;
    color: var(--text-n2);
    cursor: pointer;
    user-select: none;
    &:hover {
      color: var(--text-n1);
    }
  }
  .flow-canvas-toolbar__icon {
    display: inline-flex;
    justify-content: center;
    align-items: center;
    color: var(--text-n2);
    cursor: pointer;
    transition: all 0.2s ease;
    &:hover {
      color: var(--text-n1);
    }
    &.is-active {
      color: var(--primary-8);
    }
    &.is-disabled {
      color: var(--text-n6);
      cursor: not-allowed;
      &:hover {
        color: var(--text-n6);
      }
    }
  }
</style>

<style lang="less">
  .zoom-dropdown {
    .n-dropdown-option-body {
      height: 28px !important;
      line-height: 28px !important;
    }
    .n-dropdown-option-body::before {
      border-radius: 3px !important;
    }
  }
</style>
