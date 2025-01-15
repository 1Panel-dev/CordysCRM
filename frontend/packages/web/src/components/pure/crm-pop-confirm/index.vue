<template>
  <n-popconfirm
    v-model:show="show"
    class="crm-pop-confirm-wrapper"
    :show-icon="false"
    flip
    arrow-point-to-center
    :placement="props.placement"
    @positive-click="handlePositiveClick"
    @negative-click="handleNegativeClick"
  >
    <template #trigger>
      <slot></slot>
    </template>
    <template #default>
      <div class="crm-pop-confirm-content-wrapper">
        <div class="crm-pop-confirm-title">
          <CrmIcon
            :class="`crm-pop-confirm-icon-${props.iconType} mr-[8px]`"
            type="iconicon_info_circle_filled"
            :size="20"
          />
          <div class="one-line-text">{{ props.title }}</div>
        </div>
        <div class="crm-pop-confirm-content">{{ props.content }}</div>
      </div>
    </template>
    <template #action>
      <div class="flex items-center justify-end">
        <NButton :disabled="props.loading" v-bind="cancelButtonProps" @click="handleNegativeClick">
          {{ t(props.negativeText) }}
        </NButton>
        <NButton :loading="props.loading" v-bind="okButtonProps" @click="handlePositiveClick">
          {{ t(props.positiveText) }}
        </NButton>
      </div>
    </template>
  </n-popconfirm>
</template>

<script setup lang="ts">
  import { ButtonProps, NButton, NPopconfirm } from 'naive-ui';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const props = withDefaults(
    defineProps<{
      loading: boolean;
      title: string; // 标题
      content: string; // 内容
      iconType?: 'error' | 'warning' | 'primary'; // 图标类型
      negativeButtonProps?: ButtonProps; // 取消按钮文字
      positiveButtonProps?: ButtonProps; // 确定按钮的属性
      positiveText?: string; // 确定按钮文本
      negativeText?: string; // 取消按钮文本
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
    }>(),
    {
      iconType: 'error',
      positiveText: 'common.remove',
      negativeText: 'common.cancel',
    }
  );

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
