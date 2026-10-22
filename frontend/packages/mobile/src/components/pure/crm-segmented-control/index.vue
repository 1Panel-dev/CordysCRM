<template>
  <div class="crm-segmented-control">
    <button
      v-for="option in props.options"
      :key="option.value"
      type="button"
      class="crm-segmented-control__item"
      :class="{ 'crm-segmented-control__item--active': modelValue === option.value }"
      :aria-pressed="modelValue === option.value"
      @click="handleChange(option.value)"
    >
      {{ option.label }}
    </button>
  </div>
</template>

<script setup lang="ts">
  export interface CrmSegmentedControlOption {
    label: string;
    value: string | number;
  }

  const props = defineProps<{
    options: CrmSegmentedControlOption[];
  }>();

  const modelValue = defineModel<string | number>();

  const emit = defineEmits<{
    (e: 'change', value: string | number): void;
    (e: 'select', value: string | number): void;
  }>();

  function handleChange(value: string | number) {
    if (modelValue.value !== value) {
      modelValue.value = value;
      emit('change', value);
    }
    emit('select', value);
  }
</script>

<style scoped lang="less">
  .crm-segmented-control {
    display: flex;
    padding: 2px;
    border-radius: 3px;
    background: var(--text-n8);
  }
  .crm-segmented-control__item {
    padding: 6px 8px;
    min-width: 0;
    font-size: 14px;
    border: 0;
    border-radius: 2px;
    color: var(--text-n2);
    background: transparent;
    flex: 1;
    line-height: 20px;
    cursor: pointer;
  }
  .crm-segmented-control__item--active {
    color: var(--text-n1);
    background: var(--text-n10);
  }
</style>
