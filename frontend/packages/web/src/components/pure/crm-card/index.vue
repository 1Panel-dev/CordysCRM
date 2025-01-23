<template>
  <div :class="`relative ${props.autoHeight ? '' : 'h-full'}`">
    <!-- 有卡片footer 时，高度为100%-64px，64px 为：footer 高度80px 减去底部内边距 16px -->
    <n-card
      class="h-full"
      :class="[props.noContentPadding ? 'n-card--no-padding' : '', props.hideFooter ? '' : 'h-[calc(100%-64px)]']"
      :bordered="props.bordered"
    >
      <template #header>
        <slot name="header">
          <div v-if="props.title" class="flex items-center gap-[16px]">
            <div>
              {{ props.title }}
            </div>
            <div v-if="props.subTitle" class="n-card-header-subtitle">{{ props.subTitle }}</div>
          </div>
          <div v-if="props.description" class="n-card-header-description">
            {{ props.description }}
          </div>
        </slot>
      </template>
      <template #header-extra>
        <slot name="header-extra"></slot>
      </template>
      <n-scrollbar :content-style="{ 'max-height': props.contentMaxHeight || '100%' }">
        <slot></slot>
      </n-scrollbar>
      <template #footer>
        <slot name="footer"></slot>
      </template>
      <template #action>
        <slot name="action"></slot>
      </template>
    </n-card>
    <div v-if="!props.hideFooter" class="n-card-footer" :style="{ width: `calc(100% - ${menuWidth + 16}px)` }">
      <div class="ml-0 mr-auto">
        <slot name="footerLeft"></slot>
      </div>
      <slot name="footerRight">
        <div class="flex justify-end gap-[16px]">
          <n-button :disabled="props.loading" secondary @click="emit('cancel')">
            {{ t('common.cancel') }}
          </n-button>
          <n-button
            v-if="!props.hideContinue && !props.isEdit"
            :loading="props.loading"
            secondary
            @click="emit('saveAndContinue')"
          >
            {{ props.saveAndContinueText || t('common.saveAndContinue') }}
          </n-button>
          <n-button :loading="props.loading" type="primary" @click="emit('save')">
            {{ props.saveText || t(props.isEdit ? 'common.update' : 'common.save') }}
          </n-button>
        </div>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NButton, NCard, NScrollbar } from 'naive-ui';

  import { useI18n } from '@/hooks/useI18n';
  import useAppStore from '@/store/modules/app';

  const props = withDefaults(
    defineProps<{
      title?: string;
      subTitle?: string;
      description?: string;
      contentMaxHeight?: string;
      noContentPadding?: boolean;
      loading?: boolean;
      saveText?: string;
      isEdit?: boolean;
      saveAndContinueText?: string;
      hideFooter?: boolean;
      hideContinue?: boolean;
      bordered?: boolean;
      autoHeight?: boolean;
    }>(),
    {
      noContentPadding: false,
      hideFooter: false,
      hideContinue: false,
    }
  );
  const emit = defineEmits(['cancel', 'save', 'saveAndContinue']);

  const appStore = useAppStore();
  const { t } = useI18n();

  const menuWidth = computed(() => {
    return appStore.menuCollapsed ? appStore.collapsedWidth : 180;
  });
</script>

<style lang="less">
  .n-card {
    border-radius: var(--border-radius-medium);
    .n-card-header {
      padding: 16px 24px;
      gap: 8px;
      .n-card-header__main {
        font-size: 16px;
        line-height: 24px;
        font-weight: 400;
        .n-card-header-subtitle,
        .n-card-header-description {
          font-size: 14px;
          line-height: 22px;
          color: var(--text-n2);
        }
      }
    }
    .n-card__content {
      padding: 0 24px 16px;
    }
    &:hover {
      box-shadow: @box-middle-shadow;
    }
  }
  .n-card--no-padding {
    .n-card__content {
      padding: 0 !important;
    }
  }
  .n-card-footer {
    @apply fixed flex justify-between;

    right: 0;
    bottom: 0;
    z-index: 100;
    padding: 24px;
    border-bottom: 0;
    background-color: var(--text-n10);
    box-shadow: var(--tw-ring-offset-shadow, 0 0 #00000000), var(--tw-ring-shadow, 0 0 #00000000), var(--tw-shadow);

    --tw-shadow: 0 -1px 4px rgb(2 2 2 / 10%);
    --tw-shadow-colored: 0 -1px 4px var(--tw-shadow-color);
  }
</style>
