<template>
  <CrmPageWrapper :title="route.query.name?.toString() || ''">
    <van-tabs v-model:active="activeTab" border class="detail-tabs">
      <van-tab v-for="tab of tabList" :key="tab.name" :name="tab.name">
        <template #title>
          <div class="text-[16px]" :class="activeTab === tab.name ? 'text-[var(--primary-8)]' : ''">
            {{ tab.title }}
          </div>
        </template>
        <div v-if="tab.name === 'info'" class="relative h-full bg-[var(--text-n9)] pt-[16px]">
          <CrmDescription :description="descriptions" />
        </div>
        <CrmContactList v-else-if="tab.name === 'contact'" />
        <CrmFollowRecordList v-else-if="tab.name === 'record'" :type="FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER" />
        <CrmFollowPlanList v-else-if="tab.name === 'plan'" :type="FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER" />
        <relation v-else-if="tab.name === 'relation'" :source-id="route.query.id?.toString() || ''" />
        <collaborator v-else-if="tab.name === 'collaborator'" :source-id="route.query.id?.toString() || ''" />
      </van-tab>
    </van-tabs>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDescription from '@/components/pure/crm-description/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmContactList from '@/components/business/crm-contact-list/index.vue';
  import CrmFollowPlanList from '@/components/business/crm-follow-list/followPlan.vue';
  import CrmFollowRecordList from '@/components/business/crm-follow-list/followRecord.vue';
  import collaborator from './components/collaborator.vue';
  import relation from './components/relation.vue';

  import useFormCreateApi from '@/hooks/useFormCreateApi';

  const route = useRoute();
  const { t } = useI18n();

  const activeTab = ref('info');
  const tabList = [
    {
      name: 'info',
      title: t('customer.info'),
    },
    {
      name: 'contact',
      title: t('menu.contact'),
    },
    {
      name: 'record',
      title: t('common.record'),
    },
    {
      name: 'plan',
      title: t('common.plan'),
    },
    {
      name: 'header',
      title: t('customer.headerRecord'),
    },
    {
      name: 'relation',
      title: t('customer.relation'),
    },
    {
      name: 'collaborator',
      title: t('customer.collaborator'),
    },
  ];

  const { descriptions, initFormConfig, initFormDescription } = useFormCreateApi({
    formKey: FormDesignKeyEnum.CUSTOMER,
    sourceId: route.query.id?.toString(),
    needInitDetail: true,
  });

  onBeforeMount(async () => {
    await initFormConfig();
    initFormDescription();
  });
</script>

<style lang="less" scoped>
  :deep(.crm-page-content) {
    @apply !overflow-hidden;
  }
  .detail-tabs {
    @apply flex-1 overflow-hidden;
    :deep(.van-tabs__content) {
      height: calc(100% - var(--van-tabs-line-height));
      .van-tab__panel {
        @apply h-full;
      }
    }
  }
</style>
