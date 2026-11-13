<template>
  <div class="crm-radio-card-group" role="radiogroup" :aria-label="props.ariaLabel">
    <div
      v-for="option in props.options"
      :key="option.value"
      class="crm-radio-card"
      :class="{
        'crm-radio-card--selected': value === option.value,
        'crm-radio-card--disabled': option.disabled,
      }"
      role="radio"
      :aria-checked="value === option.value"
      :aria-disabled="option.disabled"
      :tabindex="option.disabled ? -1 : 0"
      @click="handleSelect(option)"
      @keydown="handleOptionKeydown($event, option)"
    >
      <span class="crm-radio-card__radio" aria-hidden="true"></span>
      <span class="crm-radio-card__copy">
        <span class="crm-radio-card__title">{{ option.label }}</span>
        <span v-if="option.description" class="crm-radio-card__description">{{ option.description }}</span>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
  export interface CrmRadioCardOption {
    value: string | number;
    label: string;
    description?: string;
    disabled?: boolean;
  }

  const props = defineProps<{
    options: CrmRadioCardOption[];
    ariaLabel?: string;
  }>();

  const value = defineModel<string | number>('value', { required: true });

  function handleSelect(option: CrmRadioCardOption) {
    if (!option.disabled) value.value = option.value;
  }

  function handleOptionKeydown(event: KeyboardEvent, option: CrmRadioCardOption) {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      handleSelect(option);
    }
  }
</script>

<style scoped lang="less">
  .crm-radio-card-group {
    display: flex;
    flex-direction: column;
    gap: 8px;
    width: 100%;
  }
  .crm-radio-card {
    display: flex;
    align-items: center;
    padding: 8px;
    cursor: pointer;
    border: 1px solid var(--text-n8);
    border-radius: 4px;
    background: var(--text-n10);
    transition: border-color 120ms ease, background-color 120ms ease;
    gap: 12px;
    &:focus-visible {
      outline: 2px solid var(--primary-8);
      outline-offset: 2px;
    }
    &--selected {
      border-color: var(--primary-8);
      background: var(--primary-10, #f0fbfb);
      .crm-radio-card__radio {
        border-color: var(--primary-8);
        &::after {
          transform: scale(1);
        }
      }
    }
    &--disabled {
      cursor: not-allowed;
      opacity: 0.6;
    }
    &__radio {
      display: flex;
      justify-content: center;
      align-items: center;
      width: 20px;
      height: 20px;
      border: 1px solid var(--text-n6);
      border-radius: 50%;
      flex: 0 0 20px;
      &::after {
        width: 10px;
        height: 10px;
        content: '';
        border-radius: 50%;
        background: var(--primary-8);
        transform: scale(0);
        transition: transform 120ms ease;
      }
    }
    &__copy {
      display: flex;
      min-width: 0;
      flex-direction: column;
      gap: 4px;
    }
    &__title {
      line-height: 22px;
      color: var(--text-n1);
    }
    &__description {
      line-height: 20px;
      color: var(--text-n4);
    }
  }
</style>
