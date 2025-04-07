<template>
  <n-scrollbar
    class="business"
    :content-class="`${activeTab === 'pageSettings' ? 'overflow-auto' : 'h-full overflow-hidden'}`"
  >
    <CrmCard no-content-padding hide-footer auto-height class="mb-[16px]">
      <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line" />
    </CrmCard>
    <PageSettings v-if="activeTab === 'pageSettings'" />
    <AuthenticationSettings v-if="activeTab === 'authenticationSettings'" />
    <MailSettings v-if="activeTab === 'mailSettings'" />
    <IntegrationList v-if="activeTab === 'syncOrganization'" />
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { NScrollbar } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import IntegrationList from './components/integrationList.vue';

  const AuthenticationSettings = defineAsyncComponent(() => import('./components/authenticationSettings.vue'));
  const PageSettings = defineAsyncComponent(() => import('./components/pageSettings.vue'));
  const MailSettings = defineAsyncComponent(() => import('./components/mailSettings.vue'));
  const { t } = useI18n();

  const activeTab = ref('syncOrganization');
  const tabList = ref([
    // { name: 'pageSettings', tab: t('system.business.tab.interfaceSettings') }, // 第一版先不上
    { name: 'syncOrganization', tab: t('system.business.tab.third') },
    { name: 'mailSettings', tab: t('system.business.tab.mailSettings') },
    { name: 'authenticationSettings', tab: t('system.business.tab.authenticationSettings') },
  ]);
</script>

<style lang="less" scoped>
  :deep(.n-tabs-scroll-padding) {
    width: 16px !important;
  }
</style>
