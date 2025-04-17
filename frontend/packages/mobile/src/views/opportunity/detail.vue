<template>
  <CrmPageWrapper :title="route.query.name?.toString() || ''">
    <van-tabs v-model:active="activeTab" border class="detail-tabs">
      <van-tab v-for="tab of tabList" :key="tab.name" :name="tab.name">
        <template #title>
          <div class="text-[16px]" :class="activeTab === tab.name ? 'text-[var(--primary-8)]' : ''">
            {{ tab.title }}
          </div>
        </template>
        <div class="bg-[var(--text-n9)] p-[16px]">
          <CrmWorkflowCard
            v-if="tab.name === 'info'"
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
        <div v-if="tab.name === 'info'" class="relative h-full bg-[var(--text-n9)]">
          <CrmDescription :description="descriptions" />
        </div>
        <CrmFollowRecordList
          v-else-if="tab.name === 'record'"
          :source-id="sourceId"
          :type="FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS"
        />
        <CrmFollowPlanList
          v-else-if="tab.name === 'plan'"
          :source-id="sourceId"
          :type="FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS"
        />
      </van-tab>
    </van-tabs>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { useRoute } from 'vue-router';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { OpportunityStatusEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDescription from '@/components/pure/crm-description/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmFollowPlanList from '@/components/business/crm-follow-list/followPlan.vue';
  import CrmFollowRecordList from '@/components/business/crm-follow-list/followRecord.vue';
  import type { Options } from '@/components/business/crm-workflow-card/index.vue';
  import CrmWorkflowCard from '@/components/business/crm-workflow-card/index.vue';

  import useFormCreateApi from '@/hooks/useFormCreateApi';

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

  const { descriptions, initFormConfig, initFormDescription, detail } = useFormCreateApi({
    formKey: FormDesignKeyEnum.BUSINESS,
    sourceId: sourceId.value,
    needInitDetail: true,
  });

  const currentStatus = ref(route.query.stage?.toString());

  const lastOptStage = ref(route.query.lastStage?.toString() ?? OpportunityStatusEnum.CREATE);

  const baseStepList: Options[] = [
    {
      value: OpportunityStatusEnum.CREATE,
      label: t('opportunity.newCreate'),
    },
    {
      value: OpportunityStatusEnum.CLEAR_REQUIREMENTS,
      label: t('opportunity.clearRequirements'),
    },
    {
      value: OpportunityStatusEnum.SCHEME_VALIDATION,
      label: t('opportunity.schemeValidation'),
    },
    {
      value: OpportunityStatusEnum.PROJECT_PROPOSAL_REPORT,
      label: t('opportunity.projectProposalReport'),
    },
    {
      value: OpportunityStatusEnum.BUSINESS_PROCUREMENT,
      label: t('opportunity.businessProcurement'),
    },
    {
      value: OpportunityStatusEnum.END,
      label: t('opportunity.end'),
    },
  ];

  async function initStage(isInit = false) {
    if (isInit) {
      initFormDescription();
    }

    const { stage, lastStage } = detail.value;
    currentStatus.value = stage;
    lastOptStage.value = lastStage;
  }

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
