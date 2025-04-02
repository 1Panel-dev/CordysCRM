<template>
  <CrmPageWrapper :title="t('workbench.duplicateCheck')">
    <van-search v-model="keyword" shape="round" :placeholder="t('workbench.searchPlaceholder')" />
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';

  import { useI18n } from '@cordys/web/src/hooks/useI18n';

  const { t } = useI18n();

  const keyword = ref('');
</script>

<style lang="less" scoped></style>
