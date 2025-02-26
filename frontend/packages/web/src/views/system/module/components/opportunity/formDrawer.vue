<template>
  <CrmFormDrawer v-model:visible="visible" :title="title"> </CrmFormDrawer>
</template>

<script setup lang="ts">
  import CrmFormDrawer from '@/components/business/crm-form-drawer/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const title = ref(t('opportunity.newOpportunity'));
</script>

<style lang="less" scoped></style>
