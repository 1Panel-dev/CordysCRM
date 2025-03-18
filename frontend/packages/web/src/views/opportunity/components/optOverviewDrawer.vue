<template>
  <CrmOverviewDrawer
    v-model:show="showOptOverviewDrawer"
    v-model:active-tab="activeTab"
    v-model:cached-list="cachedList"
    :tab-list="tabList"
    :button-list="buttonList"
    :title="props.detail?.opportunityName"
    :subtitle="props.detail?.customerName"
    :form-key="FormDesignKeyEnum.BUSINESS"
    :show-tab-setting="true"
    @button-select="handleSelect"
  >
    <template #left>
      <CrmFormDescription :form-key="FormDesignKeyEnum.BUSINESS" :source-id="sourceId" />
    </template>
    <template #rightTop>
      <CrmWorkflowCard
        v-model:status="currentStatus"
        :show-confirm-status="true"
        class="mb-[16px]"
        :workflow-list="workflowList"
        :source-id="sourceId"
        :update-api="updateOptStage"
        @load-detail="loadStageDetail"
      />
    </template>
    <template #right>
      <FollowDetail
        v-if="['followRecord', 'followPlan'].includes(activeTab)"
        class="mt-[16px]"
        :show-title="activeTab === 'followRecord'"
        :active-type="(activeTab as 'followRecord'| 'followPlan')"
        wrapper-class="h-[calc(100vh-290px)]"
        virtual-scroll-height="calc(100vh - 322px)"
        :follow-api-key="FormDesignKeyEnum.BUSINESS"
        :source-id="sourceId"
      />

      <HeaderTable
        v-if="activeTab === 'headRecord'"
        class="mt-[16px] h-[calc(100vh-290px)]"
        :source-id="sourceId"
        :load-list-api="getUserList"
      />
    </template>

    <template #transferPopContent>
      <TransferForm ref="transferFormRef" v-model:form="transferForm" class="mt-[16px] w-[320px]" />
    </template>
  </CrmOverviewDrawer>
</template>

<script setup lang="ts">
  import { SelectOption, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { OpportunityStatusEnum, StageResultEnum } from '@lib/shared/enums/opportunityEnum';
  import type { TransferParams } from '@lib/shared/models/customer';
  import type { OpportunityItem } from '@lib/shared/models/opportunity';

  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import FollowDetail from '@/components/business/crm-follow-detail/index.vue';
  import HeaderTable from '@/components/business/crm-form-create-table/headerTable.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmOverviewDrawer from '@/components/business/crm-overview-drawer/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import CrmWorkflowCard from '@/components/business/crm-workflow-card/index.vue';

  import { deleteOpt, getOptStageDetail, transferOpt, updateOptStage } from '@/api/modules/opportunity';
  import { getUserList } from '@/api/modules/system/org';
  import { defaultTransferForm } from '@/config/opportunity';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import { characterLimit } from '@/utils';

  const { openModal } = useModal();

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    baseSteps: SelectOption[];
    detail?: OpportunityItem;
  }>();

  const emit = defineEmits<{
    (e: 'refresh'): void;
  }>();

  const showOptOverviewDrawer = defineModel<boolean>('show', {
    required: true,
  });

  const transferForm = ref<TransferParams>({
    owner: null,
    ids: [],
  });

  const sourceId = computed(() => props.detail?.id ?? '');

  const transferLoading = ref(false);

  const buttonList: ActionsItem[] = [
    {
      label: t('common.edit'),
      key: 'edit',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
    {
      label: t('common.followPlan'),
      key: 'followPlan',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
    {
      label: t('crmFollowRecord.followRecord'),
      key: 'followRecord',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
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
    },
    {
      label: t('common.delete'),
      key: 'delete',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
  ];

  const activeTab = ref('followRecord');
  const cachedList = ref([]);
  const tabList: TabContentItem[] = [
    {
      name: 'followRecord',
      tab: t('crmFollowRecord.followRecord'),
      enable: true,
      allowClose: false,
    },
    {
      name: 'followPlan',
      tab: t('common.followPlan'),
      enable: true,
      allowClose: true,
    },
    {
      name: 'headRecord',
      tab: t('common.headRecord'),
      enable: true,
      allowClose: true,
    },
  ];

  const currentStatus = ref<string>(OpportunityStatusEnum.CREATE);

  const endStage = computed<SelectOption>(() => {
    const status = currentStatus.value;

    if (status === StageResultEnum.SUCCESS) {
      return {
        value: StageResultEnum.SUCCESS,
        label: t('common.success'),
      };
    }

    if (status === StageResultEnum.FAIL) {
      return {
        value: StageResultEnum.FAIL,
        label: t('common.fail'),
      };
    }

    return {
      value: OpportunityStatusEnum.END,
      label: t('opportunity.end'),
    };
  });

  const workflowList = computed<SelectOption[]>(() => {
    return [...props.baseSteps, endStage.value];
  });

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

  async function loadStageDetail() {
    try {
      currentStatus.value = await getOptStageDetail(sourceId.value);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
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

  watch(
    () => showOptOverviewDrawer.value,
    (val) => {
      if (val) {
        loadStageDetail();
      }
    }
  );
</script>
