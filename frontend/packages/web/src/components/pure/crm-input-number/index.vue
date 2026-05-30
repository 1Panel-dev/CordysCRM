<template>
  <n-input-number v-model:value="value" :placeholder="props.placeholder ?? t('common.pleaseInput')" v-bind="$attrs">
    <template #suffix>
      <slot name="suffix"> </slot>
    </template>
    <template #add-icon>
      <CrmIcon type="iconicon_chevron_up" :size="14" color="var(--text-n2)" />
    </template>
    <template #minus-icon>
      <CrmIcon type="iconicon_chevron_down" :size="14" color="var(--text-n2)" />
    </template>
  </n-input-number>
</template>

<script setup lang="ts">
  import { NInputNumber } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  const { t } = useI18n();
  const props = defineProps<{
    placeholder?: string;
  }>();

  const value = defineModel<number | null | undefined>('value');
</script>

<style scoped lang="less">
  :deep(.n-input) {
    .n-input-wrapper {
      padding-right: 28px;
      .n-input__suffix {
        gap: 2px;
        .n-input-number-suffix,
        .n-base-clear {
          margin-right: 4px;
        }
        .n-button {
          position: absolute;
          right: 0;
          width: 28px;
          height: 14px !important;
          visibility: hidden;
          &:first-of-type {
            bottom: 1px;
          }
          &:last-of-type {
            top: 1px;
          }
          &:hover {
            background-color: var(--text-n9);
          }
          &:active {
            background-color: var(--text-n8);
          }
          &--disabled {
            i {
              color: var(--text-n6) !important;
            }
            &:hover {
              background-color: var(--text-n9);
            }
            &:active {
              background-color: transparent;
            }
          }
        }
      }
    }
    &:hover,
    &:active {
      .n-input__suffix .n-button {
        visibility: visible;
      }
    }
  }
</style>
