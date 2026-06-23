<template>
  <n-scrollbar
    :class="`business ${activeTab === 'syncOrganization' && licenseStore.expiredDuring ? '!h-[calc(100%-64px)]' : ''}`"
    :content-class="`${
      ['pageSettings', 'syncOrganization'].includes(activeTab) ? 'overflow-auto' : 'h-full overflow-hidden'
    }`"
  >
    <div class="business-container">
      <CrmCard no-content-padding hide-footer auto-height class="mb-[16px]">
        <CrmTab
          v-model:active-tab="activeTab"
          no-content
          :tab-list="tabList"
          type="line"
          :before-leave="handleBeforeLeave"
        />
      </CrmCard>
      <PageSettings v-if="activeTab === 'pageSettings'" />
      <MailSettings v-if="activeTab === 'mailSettings'" />
      <!-- TODO license 先放开 <IntegrationList v-if="activeTab === 'syncOrganization' && xPack" /> -->
      <IntegrationList v-if="activeTab === 'syncOrganization'" />
    </div>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { NScrollbar } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import IntegrationList from './components/integrationList.vue';

  import useModal from '@/hooks/useModal.js';
  import useLicenseStore from '@/store/modules/setting/license';

  const PageSettings = defineAsyncComponent(() => import('./components/pageSettings.vue'));
  const MailSettings = defineAsyncComponent(() => import('./components/mailSettings.vue'));
  const { t } = useI18n();
  const { openModal } = useModal();

  const licenseStore = useLicenseStore();

  const activeTab = ref('syncOrganization');

  // TODO license 先放开
  // const tabList = ref([{ name: 'mailSettings', tab: t('system.business.tab.mailSettings') }]);

  const initTabList = [
    { name: 'pageSettings', tab: t('system.business.tab.interfaceSettings') },
    { name: 'syncOrganization', tab: t('system.business.tab.third') },
    { name: 'mailSettings', tab: t('system.business.tab.mailSettings') },
  ];

  const tabList = ref([...initTabList]);

  function handleBeforeLeave(newVal: string | number) {
    if (newVal === 'pageSettings' && !licenseStore.hasLicense() && licenseStore.isEnterpriseVersion()) {
      openModal(licenseStore.getNoLicenseModalConfig());
      return false;
    }
    return true;
  }
</script>

<style lang="less" scoped>
  :deep(.n-tabs-scroll-padding) {
    width: 16px !important;
  }
</style>
