<template>
  <CrmCard no-content-padding hide-footer auto-height class="mb-[16px]">
    <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line" />
  </CrmCard>
  <MessageList v-if="activeTab === 'notify'" />
  <AnnouncementList v-if="activeTab === 'announcement'" />
</template>

<script lang="ts" setup>
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import AnnouncementList from './components/announcementList.vue';
  import MessageList from './components/messageList.vue';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const activeTab = ref('notify');

  const tabList = ref([
    { name: 'notify', tab: t('system.message.notify') },
    { name: 'announcement', tab: t('system.message.announcement') },
  ]);
</script>

<style lang="less" scoped></style>
