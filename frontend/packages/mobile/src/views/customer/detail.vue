<template>
  <CrmPageWrapper :title="route.query.name?.toString() || ''">
    <van-tabs v-model:active="activeTab" border class="detail-tabs">
      <van-tab v-for="tab of tabList" :key="tab.name" :name="tab.name">
        <template #title>
          <div class="text-[16px]" :class="activeTab === tab.name ? 'text-[var(--primary-8)]' : ''">
            {{ tab.title }}
          </div>
        </template>
        <div v-if="tab.name === 'info'" class="relative h-full overflow-auto bg-[var(--text-n9)] pt-[16px]">
          <CrmDescription :description="descriptions" />
        </div>
        <CrmContactList
          v-else-if="tab.name === 'contact'"
          :source-id="route.query.id?.toString()"
          :customer-name="route.query.name?.toString()"
          :form-key="FormDesignKeyEnum.CUSTOMER_CONTACT"
          :readonly="!hasAnyPermission(['CUSTOMER_MANAGEMENT:UPDATE']) || collaborationType === 'READ_ONLY'"
        />
        <CrmFollowRecordList
          v-else-if="tab.name === 'record'"
          ref="recordListRef"
          :source-id="sourceId"
          :type="FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER"
          :initial-source-name="sourceName"
          :readonly="!hasAnyPermission(['CUSTOMER_MANAGEMENT:UPDATE']) || collaborationType === 'READ_ONLY'"
        />
        <CrmFollowPlanList
          v-else-if="tab.name === 'plan'"
          ref="planListRef"
          :source-id="sourceId"
          :type="FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER"
          :initial-source-name="sourceName"
          :readonly="!hasAnyPermission(['CUSTOMER_MANAGEMENT:UPDATE']) || collaborationType === 'READ_ONLY'"
        />
        <relation
          v-else-if="tab.name === 'relation'"
          ref="relationListRef"
          :source-id="sourceId"
          :readonly="!hasAnyPermission(['CUSTOMER_MANAGEMENT:UPDATE']) || collaborationType === 'READ_ONLY'"
        />
        <collaborator v-else-if="tab.name === 'collaborator'" ref="collaboratorListRef" :source-id="sourceId" />
        <CrmHeaderList v-else :source-id="sourceId" :load-list-api="getCustomerHeaderList" />
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
  import CrmHeaderList from '@/components/business/crm-header-list/index.vue';
  import collaborator from './components/collaborator.vue';
  import relation from './components/relation.vue';

  import { getCustomerHeaderList } from '@/api/modules';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import { hasAnyPermission } from '@/utils/permission';

  import { CustomerRouteEnum } from '@/enums/routeEnum';

  defineOptions({
    name: CustomerRouteEnum.CUSTOMER_DETAIL,
  });
  const route = useRoute();
  const { t } = useI18n();

  const sourceId = computed(() => route.query.id?.toString() ?? '');
  const { sourceName, descriptions, collaborationType, initFormConfig, initFormDescription } = useFormCreateApi({
    formKey: FormDesignKeyEnum.CUSTOMER,
    sourceId,
    needInitDetail: true,
  });

  const activeTab = ref('info');
  const tabList = computed(() => {
    const fullTabList = [
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
    if (collaborationType.value) {
      return fullTabList.filter((item) => item.name !== 'collaborator');
    }
    return fullTabList;
  });
  const recordListRef = ref<InstanceType<typeof CrmFollowRecordList>[]>();
  const planListRef = ref<InstanceType<typeof CrmFollowPlanList>[]>();
  const relationListRef = ref<InstanceType<typeof relation>[]>();
  const collaboratorListRef = ref<InstanceType<typeof collaborator>[]>();

  onBeforeMount(async () => {
    await initFormConfig();
    initFormDescription();
  });

  onActivated(() => {
    if (activeTab.value === 'record') {
      recordListRef.value?.[0].loadList();
    } else if (activeTab.value === 'plan') {
      planListRef.value?.[0].loadList();
    } else if (activeTab.value === 'relation') {
      relationListRef.value?.[0].initList();
    } else if (activeTab.value === 'collaborator') {
      collaboratorListRef.value?.[0].initList();
    }
  });
</script>

<style lang="less" scoped>
  :deep(.crm-page-content) {
    @apply !overflow-hidden;
  }
  .detail-tabs {
    @apply flex-1 overflow-hidden;
    :deep(.van-hairline--top-bottom) {
      margin-top: -0.5px;
    }
    :deep(.van-tabs__content) {
      height: calc(100% - var(--van-tabs-line-height));
      .van-tab__panel {
        @apply h-full;
      }
    }
  }
</style>
