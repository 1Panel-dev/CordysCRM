<template>
  <div class="page">
    <div class="content-wrapper">
      <div class="content">
        <div class="no-permission-svg-bright"></div>
        <div class="title">
          <span>{{ t('common.noResource') }}</span>
        </div>
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  const { t } = useI18n();
</script>

<style lang="less" scoped>
  .page {
    @apply h-full;

    background-color: var(--text-n10);
    .content-wrapper {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 100%;
      background-color: var(--text-n10);
      .content {
        .title {
          display: flex;
          justify-content: center;
          font-size: 16px;
          color: var(--text-n1);
          .user {
            margin-left: 16px;
          }
        }
      }
    }
  }
</style>
