<template>
  <n-card :class="props.noContentPadding ? 'n-card--no-padding' : ''">
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
    <n-scrollbar :style="{ 'max-height': props.contentMaxHeight || 'auto' }">
      <slot></slot>
    </n-scrollbar>
    <template #footer>
      <slot name="footer"></slot>
    </template>
    <template #action>
      <slot name="action"></slot>
    </template>
  </n-card>
</template>

<script setup lang="ts">
  import { NCard, NScrollbar } from 'naive-ui';

  const props = defineProps<{
    title?: string;
    subTitle?: string;
    description?: string;
    contentMaxHeight?: string;
    noContentPadding?: boolean;
  }>();
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
      padding: 0;
    }
  }
</style>
