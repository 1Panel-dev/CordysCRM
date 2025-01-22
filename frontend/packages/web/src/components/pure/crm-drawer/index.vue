<template>
  <n-drawer
    v-bind="$attrs"
    v-model:show="show"
    :width="props.width"
    :show-mask="props.showMask"
    :placement="props.placement"
  >
    <n-drawer-content :title="props.title" closable>
      <slot />
      <template #footer>
        <slot name="footer">
          <div class="flex items-center justify-between">
            <slot name="footerLeft"></slot>

            <div class="flex gap-[8px]">
              <n-button secondary @click="handleCancel">
                {{ t(props.cancelText || 'common.cancel') }}
              </n-button>
              <n-button v-if="props.showContinue" type="primary" ghost :disabled="okDisabled" @click="handleContinue">
                {{ t(props.saveContinueText || 'common.saveAndContinue') }}</n-button
              >
              <n-button type="primary" :disabled="okDisabled" @click="handleOk">
                {{ t(props.okText || 'common.add') }}
              </n-button>
            </div>
          </div>
        </slot>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup lang="ts">
  import { NButton, NDrawer, NDrawerContent } from 'naive-ui';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();
  const props = withDefaults(
    defineProps<{
      width?: string | number;
      placement?: 'left' | 'right' | 'top' | 'bottom';
      showMask?: boolean | 'transparent';
      title: string;
      cancelText?: string;
      saveContinueText?: string;
      okText?: string;
      showContinue?: boolean; // 是否显示保存并继续添加按钮
      okDisabled?: boolean;
    }>(),
    {
      placement: 'right',
      showMask: true,
    }
  );

  const emit = defineEmits<{
    (e: 'continue'): void;
    (e: 'confirm'): void;
    (e: 'cancel'): void;
  }>();

  const show = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  function handleContinue() {
    emit('continue');
  }

  function handleOk() {
    emit('confirm');
  }

  function handleCancel() {
    show.value = false;
    emit('cancel');
  }
</script>
