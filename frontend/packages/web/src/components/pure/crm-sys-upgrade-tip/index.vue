<template>
  <div class="flex items-center gap-[12px]">
    <div class="text-[var(--color-text-1)]">{{ t('crm.sys.upgrade.tip.content') }} </div>
    <n-button type="primary" text class="text-btn-primary text-[var(--primary-8)]" @click="refresh">
      {{ t('common.refresh') }}
    </n-button>
  </div>
</template>

<script setup lang="ts">
  import { NButton } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  const { t } = useI18n();

  function refresh() {
    window.location.reload();
  }
</script>

<style lang="less" scoped></style>
