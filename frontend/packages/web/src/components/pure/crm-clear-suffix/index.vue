<template>
  <n-tooltip flip :delay="300" placement="top" trigger="hover" v-bind="$attrs" :disabled="status === 'default'">
    <template #trigger>
      <div
        :class="`crm-clear-icon cursor-pointer crm-clear-icon--${props.size} crm-clear-icon--${props.size}--${props.status}`"
        @click.stop="() => emit('clear')"
      >
        <CrmIcon type="iconicon_close" :size="12" class="text-[var(--text-n10)]" />
      </div>
    </template>
    <span> {{ props.tooltipContent || t('common.value.notNull') }} </span>
  </n-tooltip>
</template>

<script setup lang="ts">
  import { NTooltip } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  const { t } = useI18n();

  export type ClearStatusType = 'error' | 'warning' | 'default';

  const props = withDefaults(
    defineProps<{
      status: ClearStatusType; // 提醒状态
      tooltipContent?: string; // 提示内容
      size?: 'small' | 'default' | 'large';
    }>(),
    {
      status: 'default',
      size: 'default',
    }
  );

  const emit = defineEmits<{
    (e: 'clear'): void;
  }>();
</script>

<style scoped lang="less">
  .crm-clear-icon {
    border-radius: 100%;
    background: var(--text-n7);
    @apply flex items-center justify-center;
    &--small {
      width: 12px;
      height: 12px;
      &--error {
        background: var(--error-red);
      }
      &--warning {
        background: var(--warning-yellow);
      }
    }
    &--default {
      width: 14px;
      height: 14px;
      &--error {
        background: var(--error-red);
      }
      &--warning {
        background: var(--warning-yellow);
      }
    }
    &--large {
      width: 16px;
      height: 16px;
      &--error {
        background: var(--error-red);
      }
      &--warning {
        background: var(--warning-yellow);
      }
    }
  }
</style>
