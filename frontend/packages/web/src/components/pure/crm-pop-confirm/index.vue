<template>
  <n-popconfirm
    v-model:show="show"
    class="crm-pop-confirm-wrapper"
    :show-icon="false"
    flip
    arrow-point-to-center
    :negative-text="props.negativeText"
    :positive-text="props.positiveText"
    :placement="props.placement"
    @positive-click="handlePositiveClick"
    @negative-click="handleNegativeClick"
  >
    <template #trigger>
      <slot></slot>
    </template>
    <template #default>
      <div class="crm-pop-confirm-content-wrapper">
        <slot name="title">
          <div class="crm-pop-confirm-title">
            <CrmIcon
              v-if="showIcon"
              :class="`crm-pop-confirm-icon-${props.iconType} mr-[8px]`"
              type="iconicon_info_circle_filled"
              :size="20"
            />
            <div class="one-line-text">{{ props.title }}</div>
          </div>
        </slot>
        <slot name="content">
          <div class="crm-pop-confirm-content">{{ props.content }}</div>
        </slot>
      </div>
    </template>
    <template #action>
      <div class="flex items-center justify-end">
        <n-button
          v-if="props.negativeText"
          :disabled="props.loading"
          v-bind="cancelButtonProps"
          @click="handleNegativeClick"
        >
          {{ t(props.negativeText) }}
        </n-button>
        <n-button
          v-if="props.positiveText"
          :loading="props.loading"
          v-bind="okButtonProps"
          @click="handlePositiveClick"
        >
          {{ t(props.positiveText) }}
        </n-button>
      </div>
    </template>
  </n-popconfirm>
</template>

<script setup lang="ts">
  import { ButtonProps, NButton, NPopconfirm } from 'naive-ui';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  export type CrmPopConfirmProps = {
    loading?: boolean;
    title: string; // 标题
    content?: string; // 内容
    iconType?: 'error' | 'warning' | 'primary'; // 图标类型
    negativeButtonProps?: ButtonProps; // 取消按钮文字
    positiveButtonProps?: ButtonProps; // 确定按钮的属性
    positiveText?: string | null; // 确定按钮文本
    negativeText?: string | null; // 取消按钮文本
    showIcon?: boolean; // 显示icon
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
  };

  const props = withDefaults(defineProps<CrmPopConfirmProps>(), {
    iconType: 'error',
    positiveText: 'common.remove',
    negativeText: 'common.cancel',
    showIcon: true,
  });

  const emit = defineEmits<{
    (e: 'confirm'): void;
    (e: 'cancel'): void;
  }>();

  const show = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  // 默认确定按钮属性
  const defaultPositiveProps: ButtonProps = {
    type: 'primary',
    size: 'small',
  };

  // 默认取消按钮属性
  const defaultNegativeProps: ButtonProps = {
    secondary: true,
    size: 'small',
  };

  function handleNegativeClick() {
    show.value = false;
    emit('cancel');
  }

  function handlePositiveClick() {
    emit('confirm');
  }

  const okButtonProps = computed(() => {
    return {
      ...defaultPositiveProps,
      ...props.positiveButtonProps,
    };
  });

  const cancelButtonProps = computed(() => {
    return {
      ...defaultNegativeProps,
      ...props.negativeButtonProps,
    };
  });
</script>

<style scoped lang="less">
  .crm-pop-confirm-icon {
    &-primary {
      color: var(--primary-8);
    }
    &-error {
      color: var(--error-red);
    }
    &-warning {
      color: var(--warning-yellow);
    }
  }
</style>

<style lang="less">
  .crm-pop-confirm-wrapper {
    padding: 16px !important;
    .crm-pop-confirm-content-wrapper {
      min-width: 128px;
      max-width: 368px;
      @apply w-full;
      .crm-pop-confirm-title {
        font-size: 14px;
        color: var(--text-n1);
        @apply flex items-center font-semibold;
      }
      .crm-pop-confirm-content {
        padding: 8px 16px 16px 28px;
        font-size: 12px;
        color: var(--text-n2);
      }
    }
  }
</style>
