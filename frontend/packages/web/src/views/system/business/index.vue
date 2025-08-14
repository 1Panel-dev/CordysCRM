<template>
  <n-scrollbar
    class="business"
    :content-class="`${activeTab === 'pageSettings' ? 'overflow-auto' : 'h-full overflow-hidden'}`"
  >
    <CrmCard no-content-padding hide-footer auto-height class="mb-[16px]">
      <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line" />
    </CrmCard>
    <PageSettings v-if="activeTab === 'pageSettings'" />
    <!-- TODO license 先放开 <AuthenticationSettings v-if="activeTab === 'authenticationSettings' && xPack" /> -->
    <AuthenticationSettings v-if="activeTab === 'authenticationSettings'" />
    <MailSettings v-if="activeTab === 'mailSettings'" />
    <!-- TODO license 先放开 <IntegrationList v-if="activeTab === 'syncOrganization' && xPack" /> -->
    <IntegrationList v-if="activeTab === 'syncOrganization'" />
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { NScrollbar } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import IntegrationList from './components/integrationList.vue';

  import useLicenseStore from '@/store/modules/setting/license';

  const AuthenticationSettings = defineAsyncComponent(() => import('./components/authenticationSettings.vue'));
  const PageSettings = defineAsyncComponent(() => import('./components/pageSettings.vue'));
  const MailSettings = defineAsyncComponent(() => import('./components/mailSettings.vue'));
  const { t } = useI18n();

  const licenseStore = useLicenseStore();
  const xPack = computed(() => licenseStore.hasLicense());

  const activeTab = ref('syncOrganization');

  // TODO license 先放开
  // const tabList = ref([{ name: 'mailSettings', tab: t('system.business.tab.mailSettings') }]);
  const tabList = ref([
    { name: 'syncOrganization', tab: t('system.business.tab.third') },
    { name: 'mailSettings', tab: t('system.business.tab.mailSettings') },
    { name: 'authenticationSettings', tab: t('system.business.tab.authenticationSettings') },
  ]);
  // TODO license 先放开
  // watch(
  //   () => xPack.value,
  //   async (val) => {
  //     if (val) {
  //       tabList.value = [
  //         { name: 'syncOrganization', tab: t('system.business.tab.third') },
  //         { name: 'mailSettings', tab: t('system.business.tab.mailSettings') },
  //         { name: 'authenticationSettings', tab: t('system.business.tab.authenticationSettings') },
  //       ];
  //       activeTab.value = 'syncOrganization';
  //     } else {
  //       tabList.value = [{ name: 'mailSettings', tab: t('system.business.tab.mailSettings') }];
  //       activeTab.value = 'mailSettings';
  //     }
  //   },
  //   {
  //     immediate: true,
  //   }
  // );
</script>

<style lang="less" scoped>
  :deep(.n-tabs-scroll-padding) {
    width: 16px !important;
  }
</style>
