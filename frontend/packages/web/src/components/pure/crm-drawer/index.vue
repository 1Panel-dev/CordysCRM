<template>
  <n-drawer
    v-bind="$attrs"
    v-model:show="show"
    :width="props.width"
    :show-mask="props.showMask"
    :placement="props.placement"
    class="crm-drawer"
    @after-leave="emit('cancel')"
    @esc="emit('esc')"
  >
    <n-drawer-content
      :title="props.title"
      :closable="props.closable"
      :header-class="`${props.headerClass} crm-drawer-header-class ${
        !props.closable ? 'crm-drawer-header-class-no-close' : ''
      }`"
      :body-content-class="`${props.noPadding ? 'crm-no-padding-drawer' : ''} ${props.bodyContentClass || ''}`"
    >
      <template #header>
        <slot name="header">
          <div class="flex w-full items-center justify-between gap-[8px] overflow-hidden">
            <n-button v-if="props.showBack" text class="mr-[4px] w-[32px]" @click="handleCancel">
              <n-icon size="16">
                <ChevronBackOutline />
              </n-icon>
            </n-button>
            <div class="one-line-text flex flex-1 items-center gap-[8px]">
              <slot name="titleLeft"></slot>
              <slot name="title">
                <n-tooltip trigger="hover" :delay="300" :disabled="!props.title">
                  <template #trigger>
                    <span class="one-line-text">{{ props.title }}</span>
                  </template>
                  {{ props.title }}
                </n-tooltip>
              </slot>
            </div>
            <div class="flex flex-shrink-0 justify-end">
              <slot name="titleRight"></slot>
            </div>
          </div>
        </slot>
      </template>
      <n-spin :show="props.loading" class="h-full">
        <slot />
      </n-spin>
      <template v-if="footer" #footer>
        <slot name="footer">
          <div class="flex w-full items-center justify-between">
            <slot name="footerLeft"></slot>

            <div class="flex gap-[8px]">
              <n-button :disabled="props.loading" secondary @click="handleCancel">
                {{ t(props.cancelText || 'common.cancel') }}
              </n-button>
              <n-button
                v-if="props.showContinue"
                type="primary"
                ghost
                class="n-btn-outline-primary"
                :loading="props.loading"
                :disabled="okDisabled"
                @click="handleContinue"
              >
                {{ t(props.saveContinueText || 'common.saveAndContinue') }}</n-button
              >
              <n-button type="primary" :disabled="okDisabled" :loading="props.loading" @click="handleOk">
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
  import { NButton, NDrawer, NDrawerContent, NIcon, NSpin, NTooltip } from 'naive-ui';
  import { ChevronBackOutline } from '@vicons/ionicons5';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  const { t } = useI18n();
  const props = withDefaults(
    defineProps<{
      width?: string | number;
      placement?: 'left' | 'right' | 'top' | 'bottom';
      showMask?: boolean | 'transparent';
      title?: string;
      cancelText?: string;
      saveContinueText?: string;
      okText?: string;
      showContinue?: boolean; // 是否显示保存并继续添加按钮
      okDisabled?: boolean;
      headerClass?: string; // 头部的class
      bodyContentClass?: string; // 内容区的class
      footer?: boolean; // 是否展示footer
      loading?: boolean;
      closable?: boolean;
      showBack?: boolean; // 显示返回关闭按钮
      noPadding?: boolean; // 无内边距
    }>(),
    {
      placement: 'right',
      showMask: true,
      footer: true,
      showBack: false,
      noPadding: false,
      closable: true,
    }
  );

  const emit = defineEmits<{
    (e: 'continue'): void;
    (e: 'confirm'): void;
    (e: 'cancel'): void;
    (e: 'esc'): void;
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

<style lang="less">
  .crm-drawer {
    .n-spin-content {
      @apply h-full;
    }
  }
  .crm-drawer-header-class {
    .n-drawer-header__main {
      max-width: calc(100% - 28px);
    }
    &-no-close {
      .n-drawer-header__main {
        max-width: 100%;
      }
    }
  }
  .crm-no-padding-drawer {
    padding: 0 !important;
  }
</style>
