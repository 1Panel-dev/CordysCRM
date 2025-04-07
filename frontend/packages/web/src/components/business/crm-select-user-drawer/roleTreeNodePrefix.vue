<template>
  <n-avatar
    size="small"
    round
    class="box-content h-[16px] w-[16px] border border-solid border-[var(--text-n8)] bg-[var(--text-n9)] text-[12px] leading-[20px] text-[var(--text-n4)]"
  >
    <n-tooltip trigger="hover" :delay="300">
      <template #trigger>
        {{ t('role.sys') }}
      </template>
      {{ t('role.systemInit') }}
    </n-tooltip>
  </n-avatar>
</template>

<script setup lang="ts">
  import { NAvatar, NTooltip } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  const { t } = useI18n();
</script>

<style lang="less" scoped></style>
