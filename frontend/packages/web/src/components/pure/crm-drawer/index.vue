<template>
  <n-drawer
    v-bind="$attrs"
    v-model:show="show"
    :width="drawerWidth"
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
      <div v-if="!props.disabledWidthDrag && typeof drawerWidth === 'number'" class="handle" @mousedown="startResize">
        <CrmIcon type="iconicon_move" class="absolute left-[-3px] top-[50%] w-[14px]" :size="14" />
      </div>
      <n-spin :show="props.loading" class="h-full">
        <n-scrollbar
          x-scrollable
          content-class="h-full !w-full"
          :content-style="{ minWidth: props.minWidth ? `${props.minWidth}px` : 'auto' }"
        >
          <slot />
        </n-scrollbar>
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
  import { NButton, NDrawer, NDrawerContent, NIcon, NScrollbar, NSpin, NTooltip } from 'naive-ui';
  import { ChevronBackOutline } from '@vicons/ionicons5';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

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
      disabledWidthDrag?: boolean; // 禁止拖拽宽度
      minWidth?: number;
    }>(),
    {
      placement: 'right',
      showMask: true,
      footer: true,
      showBack: false,
      noPadding: false,
      closable: true,
      disabledWidthDrag: false,
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

  const resizing = ref(false); // 是否正在拖拽
  const drawerWidth = ref(props.width); // 抽屉初始宽度
  /**
   * 鼠标单击开始监听拖拽移动
   */
  const startResize = (event: MouseEvent) => {
    if (typeof drawerWidth.value === 'number') {
      resizing.value = true;
      const startX = event.clientX;
      const initialWidth = drawerWidth.value;

      // 计算鼠标移动距离
      const handleMouseMove = (_event: MouseEvent) => {
        if (resizing.value) {
          const newWidth = initialWidth + (startX - _event.clientX); // 新的宽度等于当前抽屉宽度+鼠标移动的距离
          if (typeof props.width === 'number' && newWidth >= 480 && newWidth <= window.innerWidth * 0.9) {
            // 最大最小宽度限制，最小宽度为480，最大宽度为视图窗口宽度的90%
            drawerWidth.value = newWidth;
          }
        }
      };

      // 松开鼠标按键，拖拽结束
      const handleMouseUp = () => {
        if (resizing.value) {
          // 如果当前是在拖拽，则重置拖拽状态，且移除鼠标监听事件
          resizing.value = false;
          window.removeEventListener('mousemove', handleMouseMove);
          window.removeEventListener('mouseup', handleMouseUp);
        }
      };

      window.addEventListener('mousemove', handleMouseMove);
      window.addEventListener('mouseup', handleMouseUp);
    }
  };

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
    .handle {
      @apply absolute left-0 top-0 flex h-full items-center;

      z-index: 200;
      width: 10px;
      background-color: var(--text-n8);
      cursor: col-resize;
    }
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
