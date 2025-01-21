<template>
  <CrmCard no-content-padding hide-footer auto-height class="mb-[16px]">
    <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line" />
  </CrmCard>
  <AuthenticationSettings v-if="activeTab === 'authenticationSettings'" />
  <MailSettings v-if="activeTab === 'mailSettings'" />
</template>

<script setup lang="ts">
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  const AuthenticationSettings = defineAsyncComponent(() => import('./components/authenticationSettings.vue'));
  const MailSettings = defineAsyncComponent(() => import('./components/mailSettings.vue'));
  const { t } = useI18n();

  const activeTab = ref('mailSettings');
  const tabList = ref([
    { name: 'interfaceSettings', tab: t('system.business.tab.interfaceSettings') },
    { name: 'scanLogin', tab: t('system.business.tab.scanLogin') },
    { name: 'mailSettings', tab: t('system.business.tab.mailSettings') },
    { name: 'authenticationSettings', tab: t('system.business.tab.authenticationSettings') },
    { name: 'syncOrganization', tab: t('system.business.tab.syncOrganization') },
  ]);
</script>

<style lang="less" scoped>
  :deep(.n-tabs-scroll-padding) {
    width: 16px !important;
  }
</style>
