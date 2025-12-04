<template>
  <CrmOverviewDrawer
    ref="crmOverviewDrawerRef"
    v-model:show="showOptOverviewDrawer"
    v-model:active-tab="activeTab"
    :tab-list="tabList"
    :button-list="buttonList"
    :title="titleName"
    :subtitle="subTitleName"
    :form-key="FormDesignKeyEnum.BUSINESS"
    :show-tab-setting="false"
    :source-id="sourceId"
    @button-select="handleSelect"
    @saved="() => (refreshKey += 1)"
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
        :show-action="isNotFail && hasAnyPermission(['OPPORTUNITY_MANAGEMENT:UPDATE'])"
        :parentFormKey="FormDesignKeyEnum.BUSINESS"
      />

      <ContactTable
        v-if="activeTab === 'contact'"
        :form-key="FormDesignKeyEnum.BUSINESS_CONTACT"
        :refresh-key="refreshKey"
        readonly
        :source-id="sourceId"
      />
    </template>

    <template #transferPopContent>
      <TransferForm ref="transferFormRef" v-model:form="transferForm" class="mt-[16px] w-[320px]" />
    </template>
  </CrmOverviewDrawer>
</template>

<script setup lang="ts">
  import { useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { CollaborationType, TransferParams } from '@lib/shared/models/customer';
  import type { OpportunityItem, OpportunityStageConfig } from '@lib/shared/models/opportunity';

  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import FollowDetail from '@/components/business/crm-follow-detail/index.vue';
  import ContactTable from '@/components/business/crm-form-create-table/contactTable.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmOverviewDrawer from '@/components/business/crm-overview-drawer/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import CrmWorkflowCard from '@/components/business/crm-workflow-card/index.vue';

  import { deleteOpt, getOpportunityStageConfig, transferOpt, updateOptStage } from '@/api/modules';
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
  async function initStageConfig() {
    try {
      stageConfig.value = await getOpportunityStageConfig();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
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
    },
  ];

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
      title: t('common.deleteConfirmTitle', { name: characterLimit(props.detail?.opportunityName) }),
      content: t('opportunity.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteOpt(sourceId.value);
          Message.success(t('common.deleteSuccess'));
          showOptOverviewDrawer.value = false;
          emit('refresh');
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
  }

  const titleName = ref('');
  const subTitleName = ref('');
  const initialSourceName = ref('');

  function handleDescriptionInit(
    _collaborationType?: CollaborationType,
    _sourceName?: string,
    detail?: Record<string, any>
  ) {
    if (detail) {
      const { customerName, customerId, name, stage, failureReason } = detail;
      // 商机阶段
      currentStatus.value = stage;
      // 用于回显跟进类型、商机、商机对应客户
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
  }

  watch(
    () => showOptOverviewDrawer.value,
    (val) => {
      if (val) {
        initStageConfig();
      }
    }
  );
</script>
