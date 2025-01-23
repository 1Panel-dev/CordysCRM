<template>
  <n-modal
    v-bind="$attrs"
    v-model:show="showModal"
    :show-icon="props.showIcon"
    :preset="props.preset"
    :positive-text="props.positiveText"
    :negative-text="props.negativeText"
    :class="`crm-modal-${props.size || 'medium'}`"
    @positive-click="positiveClick"
    @negative-click="negativeClick"
    @close="close"
    @after-leave="handleAfterLeave"
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
      </slot>
    </template>
    <div>
      <slot></slot>
    </div>
    <template #action>
      <slot name="footer">
        <div class="flex items-center justify-between">
          <slot name="footerLeft"> </slot>
          <slot name="footerRight">
            <div class="flex items-center gap-[8px]">
              <n-button
                :disabled="props.okLoading"
                v-bind="{ secondary: true, ...props.cancelButtonProps }"
                @click="negativeClick"
              >
                {{ t(props.negativeText) }}
              </n-button>
              <n-button v-if="showContinue" :loading="props.okLoading" type="tertiary" @click="handleContinue">
                {{ t(props.continueText || '') }}
              </n-button>
              <n-button
                :loading="props.okLoading"
                v-bind="{ type: 'primary', ...props.okButtonProps }"
                @click="positiveClick"
              >
                {{ t(props.positiveText) }}
              </n-button>
            </div>
          </slot>
        </div>
      </slot>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
  import { ButtonProps, NButton, NModal, NTooltip } from 'naive-ui';

  import { useI18n } from '@/hooks/useI18n';

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
    }>(),
    {
      preset: 'dialog',
      positiveText: 'common.confirm',
      negativeText: 'common.cancel',
      showIcon: false,
      showContinue: false,
    }
  );

  const emit = defineEmits<{
    (e: 'confirm'): void;
    (e: 'cancel'): void;
    (e: 'continue'): void;
    (e: 'close'): void;
  }>();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  function close() {
    showModal.value = false;
    emit('close');
  }

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

  const handleAfterLeave = () => {
    return props.afterLeave ? props.afterLeave() : true;
  };
</script>

<style></style>
