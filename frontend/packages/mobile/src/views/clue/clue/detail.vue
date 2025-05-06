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
              :form-stage-key="FormDesignKeyEnum.CLUE"
              :title="t('clue.clueProgress')"
              show-error-btn
              :base-steps="workflowList"
              :source-id="sourceId"
              :readonly="isConverted"
              :operation-permission="['CLUE_MANAGEMENT:UPDATE']"
              @load-detail="() => initStage(true)"
            />
          </div>
          <CrmDescription :description="descriptions" />
        </div>
        <CrmFollowRecordList
          v-else-if="tab.name === 'record'"
          ref="recordListRef"
          :source-id="sourceId"
          :type="FormDesignKeyEnum.FOLLOW_RECORD_CLUE"
          :readonly="readonly"
          :initial-source-name="sourceName"
        />
        <CrmFollowPlanList
          v-else-if="tab.name === 'plan'"
          ref="planListRef"
          :source-id="sourceId"
          :type="FormDesignKeyEnum.FOLLOW_PLAN_CLUE"
          :readonly="readonly"
          :initial-source-name="sourceName"
        />
        <CrmHeaderList v-else-if="tab.name === 'header'" :load-list-api="getClueHeaderList" :source-id="sourceId" />
      </van-tab>
    </van-tabs>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';

  import { ClueStatusEnum } from '@lib/shared/enums/clueEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { StageResultEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDescription from '@/components/pure/crm-description/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmFollowPlanList from '@/components/business/crm-follow-list/followPlan.vue';
  import CrmFollowRecordList from '@/components/business/crm-follow-list/followRecord.vue';
  import CrmHeaderList from '@/components/business/crm-header-list/index.vue';
  import CrmWorkflowCard from '@/components/business/crm-workflow-card/index.vue';

  import { getClueHeaderList } from '@/api/modules';
  import { clueBaseSteps } from '@/config/clue';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import { hasAnyPermission } from '@/utils/permission';

  import { ClueRouteEnum } from '@/enums/routeEnum';

  defineOptions({
    name: ClueRouteEnum.CLUE_DETAIL,
  });
  const route = useRoute();
  const { t } = useI18n();

  const activeTab = ref('info');
  const tabList = [
    {
      name: 'info',
      title: t('customer.info'),
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
  ];

  const sourceId = computed(() => route.query.id?.toString() ?? '');

  const { sourceName, descriptions, initFormConfig, initFormDescription, detail } = useFormCreateApi({
    formKey: FormDesignKeyEnum.CLUE,
    sourceId: sourceId.value,
    needInitDetail: true,
  });
  onBeforeMount(async () => {
    await initFormConfig();
    initFormDescription();
  });

  const currentStatus = ref<string>(ClueStatusEnum.NEW);
  const lastOptStage = ref<string>(ClueStatusEnum.NEW);
  const readonly = computed(
    () =>
      ([StageResultEnum.FAIL, StageResultEnum.SUCCESS] as string[]).includes(currentStatus.value) ||
      !hasAnyPermission(['CLUE_MANAGEMENT:UPDATE'])
  );

  const workflowList = [
    ...clueBaseSteps,
    {
      value: StageResultEnum.SUCCESS,
      label: t('common.success'),
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

  watch([() => detail.value.stage, () => detail.value.lastStage], () => {
    initStage(false);
  });

  const isConverted = computed<boolean>(
    () =>
      !!route.query?.transitionType?.toString().length &&
      ['CUSTOMER', 'OPPORTUNITY'].includes(route.query?.transitionType?.toString())
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
