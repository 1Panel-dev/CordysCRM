<template>
  <n-popconfirm
    v-model:show="show"
    class="crm-pop-confirm-wrapper"
    :show-icon="false"
    flip
    arrow-point-to-center
    :show-arrow="props.showArrow"
    :negative-text="props.negativeText"
    :positive-text="props.positiveText"
    :placement="props.placement"
    :trigger="props.trigger"
    :disabled="props.disabled"
    @positive-click="handlePositiveClick"
    @negative-click="handleNegativeClick"
  >
    <template #trigger>
      <template v-if="hasAnyPermission(props.permission)">
        <slot></slot>
      </template>
      <template v-else>
        <span class="hidden"></span>
      </template>
    </template>
    <template #default>
      <div v-permission="props.permission" class="crm-pop-confirm-content-wrapper">
        <slot name="title">
          <div class="crm-pop-confirm-title">
            <div class="one-line-text flex items-center">
              <CrmIcon
                v-if="showIcon"
                :class="`crm-pop-confirm-icon-${props.iconType} mr-[8px]`"
                type="iconicon_info_circle_filled"
                :size="20"
              />
              <div class="one-line-text">{{ props.title }}</div>
            </div>
            <CrmIcon
              v-if="props.showClose"
              class="cursor-pointer text-[var(--text-n2)]"
              type="iconicon_close"
              :size="16"
              @click="() => (show = false)"
            />
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

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();

  export type CrmPopConfirmIconType = 'error' | 'warning' | 'primary';

  export type CrmPopConfirmProps = {
    loading?: boolean;
    title: string; // 标题
    content?: string; // 内容
    iconType?: CrmPopConfirmIconType; // 图标类型
    negativeButtonProps?: ButtonProps; // 取消按钮文字
    positiveButtonProps?: ButtonProps; // 确定按钮的属性
    positiveText?: string | null; // 确定按钮文本
    negativeText?: string | null; // 取消按钮文本
    showIcon?: boolean; // 显示icon
    showArrow?: boolean;
    showClose?: boolean; // 是否显示关闭
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
    permission?: string[]; // 权限
    trigger?: 'hover' | 'click' | 'focus'; // 触发方式
    disabled?: boolean;
  };

  const props = withDefaults(defineProps<CrmPopConfirmProps>(), {
    iconType: 'error',
    positiveText: 'common.remove',
    negativeText: 'common.cancel',
    showIcon: true,
    showArrow: true,
    permission: () => [],
    trigger: 'click',
  });

  const emit = defineEmits<{
    (e: 'confirm'): void;
    (e: 'cancel'): void;
  }>();

  const show = defineModel<boolean>('show', {
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
        @apply flex items-center justify-between font-semibold;
      }
      .crm-pop-confirm-content {
        padding: 8px 16px 16px 28px;
        font-size: 12px;
        color: var(--text-n2);
      }
      .n-form-item:last-of-type {
        .n-form-item-feedback-wrapper {
          height: 16px !important;
          min-height: 16px !important;
        }
      }
    }
  }
</style>
