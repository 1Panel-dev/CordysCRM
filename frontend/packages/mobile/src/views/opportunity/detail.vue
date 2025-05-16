<template>
  <CrmPageWrapper :title="route.query.name?.toString() || ''">
    <van-tabs v-model:active="activeTab" border class="detail-tabs">
      <van-tab v-for="tab of tabList" :key="tab.name" :name="tab.name">
        <template #title>
          <div class="text-[16px]" :class="activeTab === tab.name ? 'text-[var(--primary-8)]' : ''">
            {{ tab.title }}
          </div>
        </template>
        <div v-if="tab.name === 'info'" class="relative h-full overflow-auto bg-[var(--text-n9)]">
          <div class="bg-[var(--text-n9)] p-[16px]">
            <CrmWorkflowCard
              v-model:stage="currentStatus"
              v-model:last-stage="lastOptStage"
              :form-stage-key="FormDesignKeyEnum.BUSINESS"
              :show-confirm-status="true"
              :title="t('opportunity.progress')"
              :base-steps="baseStepList"
              :source-id="sourceId"
              :operation-permission="['OPPORTUNITY_MANAGEMENT:UPDATE']"
              @load-detail="() => initStage(true)"
            />
          </div>
          <CrmDescription :description="descriptions" />
        </div>
        <CrmFollowRecordList
          v-else-if="tab.name === 'record'"
          ref="recordListRef"
          :source-id="sourceId"
          :type="FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS"
          :readonly="readonly"
          :initial-source-name="sourceName"
        />
        <CrmFollowPlanList
          v-else-if="tab.name === 'plan'"
          ref="planListRef"
          :source-id="sourceId"
          :type="FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS"
          :readonly="readonly"
          :initial-source-name="sourceName"
        />
      </van-tab>
    </van-tabs>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { useRoute } from 'vue-router';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { OpportunityStatusEnum, StageResultEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDescription from '@/components/pure/crm-description/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmFollowPlanList from '@/components/business/crm-follow-list/followPlan.vue';
  import CrmFollowRecordList from '@/components/business/crm-follow-list/followRecord.vue';
  import CrmWorkflowCard from '@/components/business/crm-workflow-card/index.vue';

  import { baseStepList } from '@/config/opportunity';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import { hasAllPermission } from '@/utils/permission';

  import { OpportunityRouteEnum } from '@/enums/routeEnum';

  defineOptions({
    name: OpportunityRouteEnum.OPPORTUNITY_DETAIL,
  });
  const route = useRoute();
  const { t } = useI18n();

  const activeTab = ref('info');
  const tabList = [
    {
      name: 'info',
      title: t('opportunity.info'),
    },
    {
      name: 'record',
      title: t('common.record'),
    },
    {
      name: 'plan',
      title: t('common.plan'),
    },
  ];

  const sourceId = computed(() => route.query.id?.toString() ?? '');

  const { sourceName, descriptions, initFormConfig, initFormDescription, detail } = useFormCreateApi({
    formKey: FormDesignKeyEnum.BUSINESS,
    sourceId: sourceId.value,
    needInitDetail: true,
  });

  const currentStatus = ref<string>(OpportunityStatusEnum.CREATE);

  const lastOptStage = ref<string>(OpportunityStatusEnum.CREATE);

  async function initStage(isInit = false) {
    if (isInit) {
      initFormDescription();
    }

    const { stage, lastStage } = detail.value;
    currentStatus.value = stage;
    lastOptStage.value = lastStage;
  }

  const readonly = computed(
    () => currentStatus.value === StageResultEnum.SUCCESS || !hasAllPermission(['OPPORTUNITY_MANAGEMENT:UPDATE'])
  );

  const recordListRef = ref<InstanceType<typeof CrmFollowRecordList>[]>();
  const planListRef = ref<InstanceType<typeof CrmFollowPlanList>[]>();

  onActivated(() => {
    if (activeTab.value === 'record') {
      recordListRef.value?.[0].loadList();
    } else if (activeTab.value === 'plan') {
      planListRef.value?.[0].loadList();
    }
  });

  onBeforeMount(async () => {
    await initFormConfig();
    initFormDescription();
  });

  watch([() => detail.value.stage, () => detail.value.lastStage], () => {
    initStage(false);
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
