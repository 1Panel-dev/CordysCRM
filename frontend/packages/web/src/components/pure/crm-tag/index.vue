<template>
  <n-tag
    v-bind="$attrs"
    :size="props.size"
    :class="`${props.color ? '' : `crm-tag-${props.theme} crm-tag-${props.theme}--${props.type}`} customClass`"
    :bordered="hasBorder"
    :color="props.color"
    :type="props.type"
    @close="emit('close')"
  >
    <template #avatar>
      <slot name="avatar"></slot>
    </template>
    <template #icon>
      <slot name="icon"></slot>
    </template>
    <n-tooltip
      flip
      :delay="300"
      :placement="props.tooltipPlacement || 'top'"
      trigger="hover"
      :disabled="props.tooltipDisabled"
    >
      <template #trigger>
        <div class="one-line-text !leading-none">
          <slot></slot>
        </div>
      </template>
      <template v-if="$slots.tooltipContent" #default>
        <slot name="tooltipContent"></slot>
      </template>
      <template v-else #default>
        <slot></slot>
      </template>
    </n-tooltip>
  </n-tag>
</template>

<script setup lang="ts">
  import { NTag, NTooltip } from 'naive-ui';

  const props = withDefaults(
    defineProps<{
      size?: 'small' | 'medium' | 'large';
      type?: 'default' | 'primary' | 'info' | 'success' | 'warning' | 'error';
      theme?: 'dark' | 'light' | 'outline' | 'lightOutLine' | 'default'; // '深色带背景'| '明亮背景'| '边框形' | '明亮边框形'
      bordered?: boolean;
      color?: { color?: string; borderColor?: string; textColor?: string }; // 标签颜色，设置该项后 type 无效
      customClass?: string; // 自定义类
      tooltipDisabled?: boolean; // 是否禁用tooltip
      tooltipPlacement?:
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
        | 'left-end'; // tooltip方向
    }>(),
    {
      size: 'medium',
      type: 'default',
      theme: 'dark',
    }
  );

  const emit = defineEmits<{
    (e: 'close'): void;
  }>();

  const hasBorder = computed(() => {
    if (props.bordered) {
      return props.bordered;
    }
    return !['dark', 'light'].includes(props.theme);
  });
</script>

<style lang="less">
  .crm-tag {
    // 背景暗黑类
    &-dark {
      color: var(--text-n10);
      .n-base-icon {
        color: var(--text-n10);
      }
      &--default {
        color: var(--text-n1);
        background: var(--text-n8);
        .n-base-icon {
          color: var(--text-n4);
        }
      }
      &--primary {
        background: var(--primary-8);
      }
      &--success {
        background: var(--success-green);
      }
      &--warning {
        background: var(--warning-yellow);
      }
      &--error {
        background: var(--error-red);
      }
      &--info {
        background: var(--info-blue);
      }
    }
    // 明亮类
    &-light {
      color: var(--text-n10);
      &--default {
        color: var(--text-n1);
        background: var(--text-n8);
      }
      &--primary {
        color: var(--primary-8);
        background: var(--primary-7);
      }
      &--success {
        color: var(--success-green);
        background: var(--success-5);
      }
      &--warning {
        color: var(--warning-yellow);
        background: var(--warning-5);
      }
      &--error {
        color: var(--error-red);
        background: var(--error-5);
      }
      &--info {
        color: var(--info-blue);
        background: var(--info-5);
      }
    }
    // 边框类
    &-outline {
      color: var(--text-n10);
      background: var(--text-n10);
      &--default {
        color: var(--text-n1);
      }
      &--primary {
        color: var(--primary-8);
      }
      &--success {
        color: var(--success-green);
      }
      &--warning {
        color: var(--warning-yellow);
      }
      &--error {
        color: var(--error-red);
      }
      &--info {
        color: var(--info-blue);
      }
    }
    // lightOutLine类
    &-lightOutLine {
      color: var(--text-n1);
      &--default {
        border-color: var(--text-n8);
        color: var(--text-n1);
        background: var(--text-n9);
      }
      &--primary {
        color: var(--primary-8);
        background: var(--primary-7);
      }
      &--success {
        color: var(--success-green);
        background: var(--success-5);
      }
      &--warning {
        color: var(--warning-yellow);
        background: var(--warning-5);
      }
      &--error {
        color: var(--error-red);
        background: var(--error-5);
      }
      &--info {
        color: var(--info-blue);
        background: var(--info-5);
      }
    }
    .n-tag__close {
      &:hover {
        background: transparent;
      }
    }
  }
</style>
