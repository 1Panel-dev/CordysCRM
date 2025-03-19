<template>
  <CrmOverviewDrawer
    v-model:show="show"
    v-model:active-tab="activeTab"
    v-model:cached-list="cachedList"
    :tab-list="tabList"
    :button-list="buttonList"
    :source-id="sourceId"
    :title="props.detail?.name"
    :form-key="FormDesignKeyEnum.CLUE"
    :show-tab-setting="true"
    @button-select="handleSelect"
  >
    <template #left>
      <CrmFormDescription :form-key="FormDesignKeyEnum.CLUE" :source-id="sourceId" />
    </template>
    <template #transferPopContent>
      <TransferForm ref="transferFormRef" v-model:form="transferForm" class="mt-[16px] w-[320px]" />
    </template>
    <template #rightTop>
      <CrmWorkflowCard
        v-model:stage="currentStatus"
        v-model:last-stage="lastOptStage"
        class="mb-[16px]"
        show-error-btn
        :base-steps="workflowList"
        :source-id="sourceId"
        :update-api="updateClueStatus"
      />
    </template>
    <template #right>
      <FollowDetail
        v-if="['followRecord', 'followPlan'].includes(activeTab)"
        class="mt-[16px]"
        :active-type="(activeTab as 'followRecord'| 'followPlan')"
        wrapper-class="h-[calc(100vh-290px)]"
        virtual-scroll-height="calc(100vh - 322px)"
        :follow-api-key="FormDesignKeyEnum.CLUE"
        :source-id="sourceId"
      />

      <CrmHeaderTable
        v-if="activeTab === 'headRecord'"
        class="mt-[16px] h-[calc(100vh-258px)]"
        :source-id="sourceId"
        :load-list-api="getClueHeaderList"
      />

      <CrmFormCreateDrawer v-model:visible="formDrawerVisible" :form-key="realFormKey" />
    </template>
  </CrmOverviewDrawer>
</template>

<script setup lang="ts">
  import { SelectOption, useMessage } from 'naive-ui';

  import { ClueStatusEnum } from '@lib/shared/enums/clueEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { StageResultEnum } from '@lib/shared/enums/opportunityEnum';
  import type { ClueListItem } from '@lib/shared/models/clue';
  import type { TransferParams } from '@lib/shared/models/customer';

  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import FollowDetail from '@/components/business/crm-follow-detail/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmHeaderTable from '@/components/business/crm-header-table/index.vue';
  import CrmOverviewDrawer from '@/components/business/crm-overview-drawer/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import CrmWorkflowCard from '@/components/business/crm-workflow-card/index.vue';

  import { batchTransferClue, deleteClue, getClueHeaderList, updateClueStatus } from '@/api/modules/clue';
  import { defaultTransferForm } from '@/config/opportunity';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import { characterLimit } from '@/utils';

  const props = defineProps<{
    detail?: ClueListItem;
  }>();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  const emit = defineEmits<{
    (e: 'refresh'): void;
  }>();

  const { openModal } = useModal();
  const { t } = useI18n();
  const Message = useMessage();

  const sourceId = computed(() => props.detail?.id ?? '');

  const transferForm = ref<TransferParams>({
    ...defaultTransferForm,
  });
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
      label: t('clue.convertToCustomer'),
      key: 'convertToCustomer',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
    {
      label: t('clue.convertToOpportunity'),
      key: 'convertToOpportunity',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
    {
      label: t('common.delete'),
      key: 'delete',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
  ];

  // 转移
  const transferFormRef = ref<InstanceType<typeof TransferForm>>();
  function handleTransfer() {
    transferFormRef.value?.formRef?.validate(async (error) => {
      if (!error) {
        try {
          transferLoading.value = true;
          await batchTransferClue({
            ...transferForm.value,
            ids: [sourceId.value],
          });
          Message.success(t('common.transferSuccess'));
          transferForm.value = { ...defaultTransferForm };
          show.value = false;
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
      title: t('common.deleteConfirmTitle', { name: characterLimit(props.detail?.name) }),
      content: t('clue.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteClue(sourceId.value);
          Message.success(t('common.deleteSuccess'));
          show.value = false;
          emit('refresh');
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  const formDrawerVisible = ref(false);
  const realFormKey = ref<FormDesignKeyEnum>(FormDesignKeyEnum.FOLLOW_RECORD_CLUE);

  function handleSelect(key: string) {
    switch (key) {
      case 'pop-transfer':
        handleTransfer();
        break;
      case 'delete':
        handleDelete();
        break;
      case 'convertToCustomer':
        // TODO 参数
        realFormKey.value = FormDesignKeyEnum.CUSTOMER;
        formDrawerVisible.value = true;
        break;
      case 'convertToOpportunity':
        realFormKey.value = FormDesignKeyEnum.BUSINESS;
        formDrawerVisible.value = true;
        break;
      default:
        break;
    }
  }

  const currentStatus = ref<string>(props.detail?.stage || ClueStatusEnum.NEW);
  const lastOptStage = ref<string>(props.detail?.lastStage || ClueStatusEnum.NEW);
  const workflowList: SelectOption[] = [
    {
      value: ClueStatusEnum.NEW,
      label: t('common.newCreate'),
    },
    {
      value: ClueStatusEnum.FOLLOWING,
      label: t('clue.followingUp'),
    },
    {
      value: ClueStatusEnum.INTERESTED,
      label: t('clue.interested'),
    },
    {
      value: StageResultEnum.SUCCESS,
      label: t('common.success'),
    },
  ];

  // tab
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
</script>
