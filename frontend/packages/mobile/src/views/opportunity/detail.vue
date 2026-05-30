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
              :stage-config-list="stageConfig?.stageConfigList || []"
              :form-stage-key="FormDesignKeyEnum.BUSINESS"
              :show-confirm-status="true"
              :title="t('opportunity.progress')"
              is-limit-back
              :back-stage-permission="['OPPORTUNITY_MANAGEMENT:UPDATE', 'OPPORTUNITY_MANAGEMENT:RESIGN']"
              :source-id="sourceId"
              :operation-permission="['OPPORTUNITY_MANAGEMENT:UPDATE']"
              :failure-reason="lastFailureReason"
              :afoot-roll-back="stageConfig?.afootRollBack"
              :end-roll-back="stageConfig?.endRollBack"
              @load-detail="() => initStage(true)"
            />
          </div>
          <CrmDescription :description="descriptions" />
        </div>
        <CrmContactList
          v-else-if="tab.name === 'contact'"
          :source-id="route.query.id?.toString()"
          :form-key="FormDesignKeyEnum.BUSINESS_CONTACT"
          readonly
        />
        <CrmFollowRecordList
          v-else-if="tab.name === 'record'"
          ref="recordListRef"
          :source-id="sourceId"
          :type="FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS"
          :readonly="readonly"
          :initial-source-name="initialSourceName"
        />
        <CrmFollowPlanList
          v-else-if="tab.name === 'plan'"
          ref="planListRef"
          :source-id="sourceId"
          :type="FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS"
          :readonly="readonly"
          :initial-source-name="initialSourceName"
        />
      </van-tab>
    </van-tabs>
    <template
      v-if="activeTab === 'info' && (showEditButton || hasAnyPermission(['OPPORTUNITY_MANAGEMENT:DELETE']))"
      #footer
    >
      <CrmActionButtons :show-edit-button="showEditButton" :actions="actions" @select="handleMoreSelect" />
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { showConfirmDialog, showSuccessToast } from 'vant';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { OpportunityStageConfig } from '@lib/shared/models/opportunity';

  import CrmDescription from '@/components/pure/crm-description/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmActionButtons, { CrmActionButtonsItem } from '@/components/business/crm-action-buttons/index.vue';
  import CrmContactList from '@/components/business/crm-contact-list/index.vue';
  import CrmFollowPlanList from '@/components/business/crm-follow-list/followPlan.vue';
  import CrmFollowRecordList from '@/components/business/crm-follow-list/followRecord.vue';
  import CrmWorkflowCard from '@/components/business/crm-workflow-card/index.vue';

  import { deleteOpt, getOpportunityStageConfig } from '@/api/modules';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import { hasAllPermission, hasAnyPermission } from '@/utils/permission';

  import { CommonRouteEnum, CustomerRouteEnum, OpportunityRouteEnum } from '@/enums/routeEnum';

  defineOptions({
    name: OpportunityRouteEnum.OPPORTUNITY_DETAIL,
  });
  const route = useRoute();
  const { t } = useI18n();
  const router = useRouter();

  const activeTab = ref('info');
  const tabList = [
    {
      name: 'info',
      title: t('opportunity.info'),
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
  ];

  const sourceId = computed(() => route.query.id?.toString() ?? '');
  const stageConfig = ref<OpportunityStageConfig>();
  async function initStageConfig() {
    try {
      stageConfig.value = await getOpportunityStageConfig();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
  const { descriptions, initFormConfig, initFormDescription, detail } = useFormCreateApi({
    formKey: FormDesignKeyEnum.BUSINESS,
    sourceId,
    needInitDetail: true,
  });

  const initialSourceName = computed(() => {
    const { customerName, customerId, name } = detail.value;

    return JSON.stringify({
      name,
      customerName,
      customerId,
    });
  });

  const currentStatus = ref<string>(stageConfig.value?.stageConfigList?.[0].id || '');

  const lastFailureReason = ref('');
  async function initStage(isInit = false) {
    if (isInit) {
      initFormDescription();
    }

    const { stage, failureReason } = detail.value;
    currentStatus.value = stage;
    lastFailureReason.value = failureReason;
  }

  const isSuccess = computed(
    () =>
      currentStatus.value === stageConfig.value?.stageConfigList.find((e) => e.type === 'END' && e.rate === '100')?.id
  );
  const isFail = computed(
    () => currentStatus.value === stageConfig.value?.stageConfigList.find((e) => e.type === 'END' && e.rate === '0')?.id
  );

  const readonly = computed(() => !hasAllPermission(['OPPORTUNITY_MANAGEMENT:UPDATE']));

  const showEditButton = computed(() => {
    if (isFail.value) return false;
    if (isSuccess.value) return hasAllPermission(['OPPORTUNITY_MANAGEMENT:UPDATE', 'OPPORTUNITY_MANAGEMENT:RESIGN']);
    return hasAllPermission(['OPPORTUNITY_MANAGEMENT:UPDATE']);
  });
  const actions = computed<CrmActionButtonsItem[]>(() => {
    const transferAction: CrmActionButtonsItem[] = [
      {
        key: 'transfer',
        text: t('common.transfer'),
        permission: ['OPPORTUNITY_MANAGEMENT:TRANSFER'],
      },
    ];

    const deleteAction: CrmActionButtonsItem[] = [
      {
        key: 'delete',
        text: t('common.delete'),
        color: 'var(--error-red)',
        permission: ['OPPORTUNITY_MANAGEMENT:DELETE'],
      },
    ];

    return isSuccess.value ? [...deleteAction] : [...transferAction, ...deleteAction];
  });

  function handleTransfer(id: string) {
    router.push({
      name: CustomerRouteEnum.CUSTOMER_TRANSFER,
      query: {
        id,
        apiKey: FormDesignKeyEnum.BUSINESS,
      },
    });
  }

  function handleMoreSelect(key: string) {
    switch (key) {
      case 'edit':
        router.push({
          name: CommonRouteEnum.FORM_CREATE,
          query: {
            id: sourceId.value,
            formKey: FormDesignKeyEnum.BUSINESS,
            needInitDetail: 'Y',
          },
        });
        break;
      case 'transfer':
        handleTransfer(sourceId.value);
        break;
      case 'delete':
        showConfirmDialog({
          title: t('opportunity.deleteTitle'),
          message: t('opportunity.deleteContentTip'),
          confirmButtonText: t('common.confirmDelete'),
          confirmButtonColor: 'var(--error-red)',
          beforeClose: async (action) => {
            if (action === 'confirm') {
              try {
                await deleteOpt(sourceId.value);
                showSuccessToast(t('common.deleteSuccess'));
                router.back();
                return Promise.resolve(true);
              } catch (error) {
                // eslint-disable-next-line no-console
                console.log(error);
                return Promise.resolve(false);
              }
            } else {
              return Promise.resolve(true);
            }
          },
        });
        break;
      default:
        break;
    }
  }

  const recordListRef = ref<InstanceType<typeof CrmFollowRecordList>[]>();
  const planListRef = ref<InstanceType<typeof CrmFollowPlanList>[]>();

  onActivated(() => {
    if (activeTab.value === 'record') {
      recordListRef.value?.[0].loadList();
    } else if (activeTab.value === 'plan') {
      planListRef.value?.[0].loadList();
    } else if (activeTab.value === 'info') {
      initFormDescription();
    }
  });

  onBeforeMount(async () => {
    initStageConfig();
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
