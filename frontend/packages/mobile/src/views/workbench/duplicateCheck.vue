<template>
  <CrmPageWrapper :title="t('workbench.duplicateCheck')">
    <van-search v-model="keyword" shape="round" :placeholder="t('workbench.searchPlaceholder')" />
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';

  const { t } = useI18n();

  const keyword = ref('');
</script>

<style lang="less" scoped></style>
