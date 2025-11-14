<template>
  <n-modal
    v-bind="$attrs"
    v-model:show="showModal"
    :show-icon="props.showIcon"
    :preset="props.preset"
    :class="`crm-modal crm-form-modal crm-modal-${props.size || 'medium'}`"
    @positive-click="positiveClick"
    @negative-click="negativeClick"
    @after-leave="emit('cancel')"
  >
    <template #header>
      <slot name="title">
        <n-tooltip flip :delay="300" trigger="hover">
          <template #trigger>
            <div :class="`crm-modal-title one-line-text ${props.titleClass}`" :style="{ ...props.titleStyle }">
              {{ props.title }}
            </div>
          </template>
          {{ props.title }}
        </n-tooltip>
        <slot name="titleRight"></slot>
      </slot>
    </template>
    <n-scrollbar style="padding-right: 8px; max-height: 70vh">
      <slot></slot>
    </n-scrollbar>
    <template v-if="props.footer" #action>
      <slot name="footer">
        <div class="flex w-full items-center justify-between">
          <slot name="footerLeft"></slot>
          <slot name="footerRight">
            <div class="flex flex-1 items-center justify-end gap-[12px]">
              <n-button
                :disabled="props.okLoading"
                v-bind="{ secondary: true, ...props.cancelButtonProps }"
                @click="negativeClick"
              >
                {{ props.negativeText || t('common.cancel') }}
              </n-button>
              <n-button
                v-if="showContinue"
                :loading="props.okLoading"
                class="n-btn-outline-primary"
                type="primary"
                ghost
                @click="handleContinue"
              >
                {{ t(props.continueText || t('common.saveAndContinue')) }}
              </n-button>
              <n-button
                :loading="props.okLoading"
                v-bind="{ type: 'primary', ...props.okButtonProps }"
                @click="positiveClick"
              >
                {{ props.positiveText || t('common.confirm') }}
              </n-button>
            </div>
          </slot>
        </div>
      </slot>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
  import { ButtonProps, NButton, NModal, NScrollbar, NTooltip } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  const { t } = useI18n();

  const props = withDefaults(
    defineProps<{
      okLoading?: boolean;
      showIcon?: boolean; // 是否显示icon
      positiveText?: string; // 确定显示文字
      negativeText?: string; // 取消显示文字
      continueText?: string; // 继续按钮显示文字
      showContinue?: boolean; // 显示继续按钮
      title?: string; // modal显示的标题
      preset?: 'dialog' | 'card'; // 预设类型
      size?: 'small' | 'medium' | 'large'; // 弹窗尺寸
      afterLeave?: () => boolean;
      cancelButtonProps?: ButtonProps; // 取消按钮属性
      okButtonProps?: ButtonProps; // 确定按钮属性
      titleClass?: string; // 标题类名
      titleStyle?: Record<string, any>; // 标题样式
      footer?: boolean; // 是否展示footer
    }>(),
    {
      preset: 'dialog',
      showIcon: false,
      showContinue: false,
      footer: true,
    }
  );

  const emit = defineEmits<{
    (e: 'confirm'): void;
    (e: 'cancel'): void;
    (e: 'continue'): void;
  }>();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  function negativeClick() {
    showModal.value = false;
    emit('cancel');
  }

  function positiveClick() {
    emit('confirm');
  }

  function handleContinue() {
    emit('continue');
  }
</script>

<style>
  .crm-modal {
    padding-right: 8px;
    .n-dialog__action {
      padding-right: 8px;
    }
  }
  :deep(.n-scrollbar-rail.n-scrollbar-rail--vertical--right) {
    right: 0;
  }
</style>
