<template>
  <div class="crm-segment-tabs" :style="{ height: props.height }">
    <button
      v-for="item in props.options"
      :key="item.value"
      class="crm-segment-tabs-item"
      :class="{
        'crm-segment-tabs-item--active': modelValue === item.value,
        'crm-segment-tabs-item--disabled': item.disabled,
      }"
      type="button"
      :disabled="item.disabled"
      @click="handleChange(item)"
    >
      <span class="one-line-text">{{ item.label }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
  export interface CrmSegmentTabsOption {
    label: string;
    value: string | number;
    disabled?: boolean;
  }

  const props = withDefaults(
    defineProps<{
      options: CrmSegmentTabsOption[];
      height?: string;
    }>(),
    {
      height: '48px',
    }
  );

  const modelValue = defineModel<string | number>();

  const emit = defineEmits<{
    (e: 'change', value: string | number, option: CrmSegmentTabsOption): void;
  }>();

  function handleChange(option: CrmSegmentTabsOption) {
    if (option.disabled || modelValue.value === option.value) {
      return;
    }
    modelValue.value = option.value;
    emit('change', option.value, option);
  }
</script>

<style lang="less" scoped>
  .crm-segment-tabs {
    --crm-segment-tabs-slant: 28px;

    display: flex;
    overflow: hidden;
    padding: 0;
    width: 100%;
    height: 48px !important;
    min-height: 48px;
    border: 0;
    border-radius: 0;
    background: var(--text-n9);
  }
  .crm-segment-tabs-item {
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
    overflow: hidden;
    padding: 0 12px;
    min-width: 0;
    height: 48px;
    min-height: 48px;
    font-size: 14px;
    border: 0;
    border-radius: 0;
    color: var(--text-n1);
    background: transparent;
    flex: 1;
    box-sizing: border-box;
    line-height: 20px;
    appearance: none;
    .one-line-text {
      position: relative;
      z-index: 1;
    }
  }
  .crm-segment-tabs-item--active {
    font-weight: 600;
    border-radius: 0;
    color: var(--primary-8);
    background: transparent;
    &::before {
      position: absolute;
      top: 2px;
      right: 0;
      bottom: 2px;
      left: 0;
      background: var(--text-n10);
      content: '';
      clip-path: polygon(
        var(--crm-segment-tabs-slant) 0,
        calc(100% - var(--crm-segment-tabs-slant)) 0,
        100% 100%,
        0 100%
      );
      pointer-events: none;
    }
  }
  .crm-segment-tabs-item--active:first-child::before {
    clip-path: polygon(0 0, 100% 0, calc(100% - var(--crm-segment-tabs-slant)) 100%, 0 100%);
  }
  .crm-segment-tabs-item--active:last-child::before {
    clip-path: polygon(var(--crm-segment-tabs-slant) 0, 100% 0, 100% 100%, 0 100%);
  }
  .crm-segment-tabs-item--disabled {
    color: var(--text-n6);
    opacity: 0.6;
  }
</style>
