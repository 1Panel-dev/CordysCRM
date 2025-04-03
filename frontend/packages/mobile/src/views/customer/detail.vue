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
          <CrmDescription :description="description" />
        </div>
        <CrmContactList v-else-if="tab.name === 'contact'" />
        <CrmFollowRecordList v-else-if="tab.name === 'record'" :type="FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER" />
        <CrmFollowPlanList v-else-if="tab.name === 'plan'" :type="FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER" />
      </van-tab>
    </van-tabs>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

  import CrmDescription, { CrmDescriptionItem } from '@/components/pure/crm-description/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmContactList from '@/components/business/crm-contact-list/index.vue';
  import CrmFollowPlanList from '@/components/business/crm-follow-list/followPlan.vue';
  import CrmFollowRecordList from '@/components/business/crm-follow-list/followRecord.vue';

  import { useI18n } from '@cordys/web/src/hooks/useI18n';

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
      title: t('common.followRecord'),
    },
    {
      name: 'plan',
      title: t('common.followPlan'),
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

  const description: CrmDescriptionItem[] = [
    {
      label: '基本信息',
      isTitle: true,
    },
    {
      label: t('customer.customerName'),
      value: '张三',
    },
    {
      label: t('customer.customerType'),
      value: 'VIP客户',
    },
    {
      label: t('customer.customerLevel'),
      value: 'VIP客户',
    },
    {
      label: t('customer.customerSource'),
      value: '市场活动',
    },
    {
      label: t('customer.customerStatus'),
      value: '潜在客户',
    },
  ];
</script>

<style lang="less" scoped>
  :deep(.crm-page-content) {
    @apply !overflow-hidden;
  }
  .detail-tabs {
    @apply flex-1 overflow-hidden;
    :deep(.van-tabs__content) {
      @apply h-full;
      .van-tab__panel {
        @apply h-full;
      }
    }
  }
</style>
