<template>
  <CrmOverviewDrawer
    ref="crmOverviewDrawerRef"
    v-model:show="showOptOverviewDrawer"
    v-model:active-tab="activeTab"
    :tab-list="tabList"
    show-tab-setting
    :button-list="buttonList"
    :title="titleName"
    :subtitle="subTitleName"
    :form-key="FormDesignKeyEnum.BUSINESS"
    :source-id="sourceId"
    :formViewSize="formViewSize"
    @button-select="handleSelect"
    @saved="refreshList"
  >
    <template #left>
      <div class="h-full overflow-hidden">
        <CrmFormDescription
          :form-key="FormDesignKeyEnum.BUSINESS"
          :source-id="sourceId"
          :refresh-key="refreshKey"
          class="p-[16px_24px]"
          :column="layout === 'vertical' ? 3 : undefined"
          :label-width="layout === 'vertical' ? 'auto' : undefined"
          :value-align="layout === 'vertical' ? 'start' : undefined"
          @init="handleDescriptionInit"
          @open-customer-detail="emit('openCustomerDrawer', $event)"
        />
      </div>
    </template>
    <template #rightTop>
      <CrmWorkflowCard
        v-model:stage="currentStatus"
        :show-confirm-status="true"
        class="mb-[16px]"
        :stage-config-list="stageConfig?.stageConfigList || []"
        is-limit-back
        :failure-reason="lastFailureReason"
        :back-stage-permission="['OPPORTUNITY_MANAGEMENT:UPDATE', 'OPPORTUNITY_MANAGEMENT:RESIGN']"
        :source-id="sourceId"
        :operation-permission="['OPPORTUNITY_MANAGEMENT:UPDATE']"
        :update-api="updateOptStage"
        :afoot-roll-back="stageConfig?.afootRollBack"
        :end-roll-back="stageConfig?.endRollBack"
        @load-detail="refreshList"
      />
    </template>
    <template #right>
      <FollowDetail
        v-if="['followRecord', 'followPlan'].includes(activeTab)"
        class="mt-[16px]"
        :refresh-key="refreshKey"
        :active-type="(activeTab as 'followRecord'| 'followPlan')"
        wrapper-class="h-[calc(100vh-290px)]"
        virtual-scroll-height="calc(100vh - 382px)"
        :follow-api-key="FormDesignKeyEnum.BUSINESS"
        :source-id="sourceId"
        :initial-source-name="initialSourceName"
        :show-add="hasAnyPermission(['OPPORTUNITY_MANAGEMENT:UPDATE'])"
        :show-action="hasAnyPermission(['OPPORTUNITY_MANAGEMENT:UPDATE'])"
        :parentFormKey="FormDesignKeyEnum.BUSINESS"
      />

      <ContactTable
        v-if="activeTab === 'contact'"
        :form-key="FormDesignKeyEnum.BUSINESS_CONTACT"
        :refresh-key="refreshKey"
        readonly
        :source-id="sourceId"
      />
      <CrmCard v-else-if="activeTab === 'quotation'" no-content-bottom-padding hide-footer>
        <quotationTable
          :form-key="FormDesignKeyEnum.OPPORTUNITY_QUOTATION"
          :source-id="sourceId"
          :refresh-key="refreshKey"
          :source-name="titleName"
        />
      </CrmCard>
      <CrmCard v-else-if="activeTab === 'stageHistory'" no-content-bottom-padding hide-footer>
        <div class="p-[16px] h-full overflow-auto">
          <n-timeline v-if="stageHistoryList.length" :horizontal="false">
            <n-timeline-item
              v-for="item in stageHistoryList"
              :key="item.id"
              :type="getTimelineType(item)"
            >
              <template #dot>
                <div
                  :class="[
                    'w-[16px] h-[16px] rounded-full flex items-center justify-center text-white text-xs',
                    getTimelineBgClass(item),
                  ]"
                >
                  <n-icon size="12">
                    <component :is="getTimelineIcon(item)" />
                  </n-icon>
                </div>
              </template>
              <div class="mb-[8px]">
                <div class="flex items-center gap-[8px] mb-[4px]">
                  <span class="text-[14px] font-medium text-[var(--text-n1)]">
                    {{ item.fromStageName || '-' }}
                  </span>
                  <n-icon size="14" color="var(--text-n4)">
                    <ArrowRightOutlined />
                  </n-icon>
                  <span
                    :class="[
                      'text-[14px] font-medium',
                      getStageTextClass(item),
                    ]"
                  >
                    {{ item.toStageName || '-' }}
                  </span>
                </div>
                <div class="flex items-center gap-[8px] text-[12px] text-[var(--text-n4)]">
                  <span>{{ item.operatorName || '-' }}</span>
                  <span>{{ dayjs(item.changeTime).format('YYYY-MM-DD HH:mm:ss') }}</span>
                </div>
                <div
                  v-if="item.failureReasonName"
                  class="mt-[8px] p-[8px] bg-[var(--text-n9)] rounded text-[12px] text-[var(--text-n2)]"
                >
                  <span class="text-[var(--text-n4)]">{{ t('common.fail') }}：</span>
                  {{ item.failureReasonName }}
                </div>
              </div>
            </n-timeline-item>
          </n-timeline>
          <n-empty v-else :description="t('common.noData')" />
        </div>
      </CrmCard>
    </template>

    <template #transferPopContent>
      <TransferForm ref="transferFormRef" v-model:form="transferForm" class="mt-[16px] w-[320px]" />
    </template>
  </CrmOverviewDrawer>
</template>

<script setup lang="ts">
  import { NEmpty, NIcon, NTimeline, NTimelineItem, useMessage } from 'naive-ui';
  import { ArrowRightOutlined, CheckCircleOutlined, CloseCircleOutlined, SyncOutlined } from '@vicons/antd';
  import dayjs from 'dayjs';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { CollaborationType, TransferParams } from '@lib/shared/models/customer';
  import type { OpportunityItem, OpportunityStageConfig, OpportunityStageHistoryItem } from '@lib/shared/models/opportunity';
  import type { FormConfig, FormViewSize } from '@lib/shared/models/system/module';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import FollowDetail from '@/components/business/crm-follow-detail/index.vue';
  import ContactTable from '@/components/business/crm-form-create-table/contactTable.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmOverviewDrawer from '@/components/business/crm-overview-drawer/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import CrmWorkflowCard from '@/components/business/crm-workflow-card/index.vue';
  import quotationTable from './quotation/quotationTable.vue';

  import { deleteOpt, getOpportunityStageConfig, getOptStageHistoryList, transferOpt, updateOptStage } from '@/api/modules';
  import { defaultTransferForm } from '@/config/opportunity';
  import useModal from '@/hooks/useModal';
  import { hasAllPermission, hasAnyPermission } from '@/utils/permission';

  const { openModal } = useModal();

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    detail?: Partial<OpportunityItem>;
  }>();

  const emit = defineEmits<{
    (e: 'refresh'): void;
    (e: 'remove'): void;
    (e: 'openCustomerDrawer', params: { customerId: string; inCustomerPool: boolean; poolId: string }): void;
  }>();

  const showOptOverviewDrawer = defineModel<boolean>('show', {
    required: true,
  });

  const crmOverviewDrawerRef = ref<InstanceType<typeof CrmOverviewDrawer>>();
  const layout = computed(() => crmOverviewDrawerRef.value?.layout);

  const transferForm = ref<TransferParams>({
    owner: null,
    ids: [],
  });

  const stageConfig = ref<OpportunityStageConfig>();
  const stageHistoryList = ref<OpportunityStageHistoryItem[]>([]);

  async function initStageConfig() {
    try {
      stageConfig.value = await getOpportunityStageConfig();
    } catch (error) {
      console.log(error);
    }
  }

  async function loadStageHistory() {
    if (!sourceId.value) {
      stageHistoryList.value = [];
      return;
    }
    try {
      const res = await getOptStageHistoryList(sourceId.value);
      stageHistoryList.value = res || [];
    } catch (error) {
      console.log(error);
      stageHistoryList.value = [];
    }
  }

  const sourceId = computed(() => props.detail?.id ?? '');
  const refreshKey = ref(0);
  const lastFailureReason = ref('');
  const currentStatus = ref<string>(stageConfig.value?.stageConfigList[0]?.id || '');
  const isNotFail = computed(() => currentStatus.value !== stageConfig.value?.stageConfigList.slice(-1)[0]?.id);
  const isSuccess = computed(
    () =>
      currentStatus.value === stageConfig.value?.stageConfigList.find((e) => e.type === 'END' && e.rate === '100')?.id
  );
  const isFail = computed(
    () => currentStatus.value === stageConfig.value?.stageConfigList.find((e) => e.type === 'END' && e.rate === '0')?.id
  );

  function getTimelineType(item: OpportunityStageHistoryItem): 'success' | 'error' | 'default' {
    const endStage = stageConfig.value?.stageConfigList.find(
      (s) => s.type === 'END' && s.id === item.toStage
    );
    if (endStage) {
      if (endStage.rate === '100') {
        return 'success';
      }
      if (endStage.rate === '0') {
        return 'error';
      }
    }
    return 'default';
  }

  function getTimelineBgClass(item: OpportunityStageHistoryItem): string {
    const endStage = stageConfig.value?.stageConfigList.find(
      (s) => s.type === 'END' && s.id === item.toStage
    );
    if (endStage) {
      if (endStage.rate === '100') {
        return 'bg-[var(--success-6)]';
      }
      if (endStage.rate === '0') {
        return 'bg-[var(--error-6)]';
      }
    }
    return 'bg-[var(--primary-6)]';
  }

  function getStageTextClass(item: OpportunityStageHistoryItem): string {
    const endStage = stageConfig.value?.stageConfigList.find(
      (s) => s.type === 'END' && s.id === item.toStage
    );
    if (endStage) {
      if (endStage.rate === '100') {
        return 'text-[var(--success-6)]';
      }
      if (endStage.rate === '0') {
        return 'text-[var(--error-6)]';
      }
    }
    return 'text-[var(--primary-6)]';
  }

  function getTimelineIcon(item: OpportunityStageHistoryItem) {
    const endStage = stageConfig.value?.stageConfigList.find(
      (s) => s.type === 'END' && s.id === item.toStage
    );
    if (endStage) {
      if (endStage.rate === '100') {
        return CheckCircleOutlined;
      }
      if (endStage.rate === '0') {
        return CloseCircleOutlined;
      }
    }
    return SyncOutlined;
  }

  const transferLoading = ref(false);

  const buttonList = computed<ActionsItem[]>(() => {
    const transferAction: ActionsItem[] = [
      {
        label: t('common.transfer'),
        key: 'transfer',
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
        popConfirmProps: {
          loading: transferLoading.value,
          title: t('common.transfer'),
          positiveText: t('common.confirm'),
          iconType: 'primary',
        },
        popSlotName: 'transferPopTitle',
        popSlotContent: 'transferPopContent',
        permission: ['OPPORTUNITY_MANAGEMENT:TRANSFER'],
      },
    ];

    const editAction: ActionsItem[] = [
      {
        label: t('common.edit'),
        key: 'edit',
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
        permission: ['OPPORTUNITY_MANAGEMENT:UPDATE'],
      },
    ];

    const deleteAction: ActionsItem[] = [
      {
        label: t('common.delete'),
        key: 'delete',
        text: false,
        ghost: true,
        danger: true,
        class: 'n-btn-outline-primary',
        permission: ['OPPORTUNITY_MANAGEMENT:DELETE'],
      },
    ];

    if (isFail.value) {
      return [...transferAction, ...deleteAction];
    }

    if (isSuccess.value) {
      return hasAllPermission(['OPPORTUNITY_MANAGEMENT:UPDATE', 'OPPORTUNITY_MANAGEMENT:RESIGN'])
        ? [...editAction, ...deleteAction]
        : [...deleteAction];
    }

    return [...editAction, ...transferAction, ...deleteAction];
  });

  const activeTab = ref('followRecord');
  const tabList: TabContentItem[] = [
    {
      name: 'followRecord',
      tab: t('crmFollowRecord.followRecord'),
      enable: true,
    },
    {
      name: 'followPlan',
      tab: t('common.plan'),
      enable: true,
    },
    {
      name: 'contact',
      tab: t('opportunity.contactInfo'),
      enable: true,
      permission: ['CUSTOMER_MANAGEMENT_CONTACT:READ'],
    },
    {
      name: 'quotation',
      tab: t('opportunity.quotation'),
      enable: true,
      permission: ['OPPORTUNITY_QUOTATION:READ'],
    },
    {
      name: 'stageHistory',
      tab: '阶段变更历史',
      enable: true,
    },
  ];

  const titleName = ref('');
  const subTitleName = ref('');
  const initialSourceName = ref('');

  // 转移
  const transferFormRef = ref<InstanceType<typeof TransferForm>>();
  function handleTransfer(done?: () => void) {
    transferFormRef.value?.formRef?.validate(async (error) => {
      if (!error) {
        try {
          transferLoading.value = true;
          await transferOpt({
            ...transferForm.value,
            ids: [sourceId.value],
          });
          Message.success(t('common.transferSuccess'));
          transferForm.value = { ...defaultTransferForm };
          showOptOverviewDrawer.value = false;
          done?.();
          emit('refresh');
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          transferLoading.value = false;
        }
      }
    });
  }

  // 删除
  function handleDelete() {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(titleName.value) }),
      content: t('opportunity.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteOpt(sourceId.value);
          Message.success(t('common.deleteSuccess'));
          showOptOverviewDrawer.value = false;
          emit('remove');
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  function handleSelect(key: string, done?: () => void) {
    switch (key) {
      case 'pop-transfer':
        handleTransfer(done);
        break;
      case 'delete':
        handleDelete();
        break;
      default:
        break;
    }
  }

  function refreshList() {
    refreshKey.value += 1;
    emit('refresh');
    loadStageHistory();
  }

  const formViewSize = ref<FormViewSize>('large');
  function handleDescriptionInit(
    _collaborationType?: CollaborationType,
    _sourceName?: string,
    detail?: Record<string, any>,
    config?: FormConfig
  ) {
    if (detail) {
      const { customerName, customerId, name, stage, failureReason } = detail;
      currentStatus.value = stage;
      titleName.value = _sourceName || '';
      subTitleName.value = customerName;
      lastFailureReason.value = failureReason;
      initialSourceName.value =
        JSON.stringify({
          name,
          customerName,
          customerId,
        }) || '';
    }
    formViewSize.value = config?.viewSize || 'large';
  }

  watch(
    () => showOptOverviewDrawer.value,
    (val) => {
      if (val) {
        initStageConfig();
        loadStageHistory();
      }
    }
  );
</script>
