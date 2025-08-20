<template>
  <div class="flex h-full flex-col">
    <div class="flex flex-1 items-center justify-center">
      <van-loading vertical text-color="var(--primary-8)" color="var(--primary-8)">
        {{ t('login.form.loadingTip') }}
      </van-loading>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  const { t } = useI18n();
</script>

<style scoped></style>
