<template>
  <div class="flex h-full items-center justify-center text-[18px]"> {{ t('api.errMsg403') }} </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  const { t } = useI18n();
</script>

<style lang="less" scoped></style>
