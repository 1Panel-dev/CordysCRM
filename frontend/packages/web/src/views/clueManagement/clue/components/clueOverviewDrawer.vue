<template>
  <CrmOverviewDrawer
    v-model:show="show"
    v-model:active-tab="activeTab"
    v-model:cached-list="cachedList"
    :tab-list="tabList"
    :button-list="buttonList"
    :button-more-list="buttonMoreList"
    :source-id="sourceId"
    :title="sourceName"
    :form-key="FormDesignKeyEnum.CLUE"
    :show-tab-setting="false"
    @button-select="handleSelect"
    @saved="() => (refreshKey += 1)"
  >
    <template #left>
      <div class="h-full overflow-hidden">
        <CrmFormDescription
          :refresh-key="refreshKey"
          :form-key="FormDesignKeyEnum.CLUE"
          :source-id="sourceId"
          class="p-[16px_24px]"
          @init="handleDescriptionInit"
        />
      </div>
    </template>
    <template #transferPopContent>
      <TransferForm ref="transferFormRef" v-model:form="transferForm" class="mt-[16px] w-[320px]" />
    </template>
    <!-- TODO 先不要了 -->
    <!-- <template #rightTop>
      <CrmWorkflowCard
        v-model:stage="currentStatus"
        v-model:last-stage="lastStage"
        class="mb-[16px]"
        show-error-btn
        :readonly="isConverted"
        :base-steps="workflowList"
        :source-id="sourceId"
        :update-api="updateClueStatus"
        :operation-permission="['CLUE_MANAGEMENT:UPDATE']"
        @load-detail="loadDetail"
      />
    </template> -->
    <template #right>
      <div class="h-full pt-[16px]">
        <FollowDetail
          v-if="['followRecord', 'followPlan'].includes(activeTab)"
          :active-type="(activeTab as 'followRecord'| 'followPlan')"
          wrapper-class="h-[calc(100vh-162px)]"
          virtual-scroll-height="calc(100vh - 254px)"
          :follow-api-key="FormDesignKeyEnum.CLUE"
          :initial-source-name="sourceName"
          :show-add="hasAnyPermission(['CLUE_MANAGEMENT:UPDATE'])"
          :source-id="sourceId"
          :show-action="showAction"
        />

        <CrmHeaderTable v-if="activeTab === 'headRecord'" :source-id="sourceId" :load-list-api="getClueHeaderList" />
      </div>
      <CrmFormCreateDrawer
        v-model:visible="formDrawerVisible"
        :other-save-params="otherFollowRecordSaveParams"
        :form-key="realFormKey"
        :initial-source-name="sourceName"
        @saved="closeAndRefresh"
      />
    </template>
  </CrmOverviewDrawer>
</template>

<script setup lang="ts">
  import { SelectOption, useMessage } from 'naive-ui';

  // import { ClueStatusEnum } from '@lib/shared/enums/clueEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  // import { StageResultEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { ClueListItem } from '@lib/shared/models/clue';
  import type { CollaborationType, TransferParams } from '@lib/shared/models/customer';

  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import FollowDetail from '@/components/business/crm-follow-detail/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmHeaderTable from '@/components/business/crm-header-table/index.vue';
  import CrmOverviewDrawer from '@/components/business/crm-overview-drawer/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';

  // import CrmWorkflowCard from '@/components/business/crm-workflow-card/index.vue';
  import { batchTransferClue, deleteClue, getClue, getClueHeaderList, updateClueStatus } from '@/api/modules';
  // import { clueBaseSteps } from '@/config/clue';
  import { defaultTransferForm } from '@/config/opportunity';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

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
  const sourceName = ref('');
  const refreshKey = ref(0);

  const transferForm = ref<TransferParams>({
    ...defaultTransferForm,
  });
  const transferLoading = ref(false);

  function closeAndRefresh() {
    show.value = false;
    emit('refresh');
  }

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
          closeAndRefresh();
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
      title: t('common.deleteConfirmTitle', { name: characterLimit(sourceName.value) }),
      content: t('clue.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteClue(sourceId.value);
          Message.success(t('common.deleteSuccess'));
          closeAndRefresh();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  const formDrawerVisible = ref(false);
  const realFormKey = ref<FormDesignKeyEnum>(FormDesignKeyEnum.FOLLOW_RECORD_CLUE);
  const otherFollowRecordSaveParams = computed(() => ({ clueId: sourceId.value }));
  function handleSelect(key: string) {
    switch (key) {
      case 'pop-transfer':
        handleTransfer();
        break;
      case 'delete':
        handleDelete();
        break;
      case 'convertToCustomer':
        realFormKey.value = FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER;
        formDrawerVisible.value = true;
        break;
      default:
        break;
    }
  }

  // const currentStatus = ref<string>(ClueStatusEnum.NEW);
  // const lastStage = ref<string>(ClueStatusEnum.NEW);
  const showAction = computed(() =>
    // currentStatus.value !== StageResultEnum.FAIL &&
    // currentStatus.value !== StageResultEnum.SUCCESS &&
    hasAnyPermission(['CLUE_MANAGEMENT:UPDATE'])
  );
  // TODO 先不要了
  // const workflowList: SelectOption[] = [
  //   ...clueBaseSteps,
  //   {
  //     value: StageResultEnum.SUCCESS,
  //     label: t('common.success'),
  //   },
  // ];
  // watch(
  //   () => props.detail,
  //   () => {
  //     if (props.detail) {
  //       currentStatus.value = props.detail.stage;
  //       lastStage.value = props.detail.lastStage;
  //     }
  //   }
  // );
  // TODO 先不要了
  // async function loadDetail() {
  //   try {
  //     const result = await getClue(sourceId.value);
  //     currentStatus.value = result.stage;
  //     lastStage.value = result.lastStage;
  //     emit('refresh');
  //   } catch (error) {
  //     // eslint-disable-next-line no-console
  //     console.log(error);
  //   }
  // }

  const isConverted = computed(
    () => props.detail?.transitionType && ['CUSTOMER'].includes(props.detail.transitionType)
  );

  const buttonList = computed<ActionsItem[]>(() => {
    if (isConverted.value) {
      return [];
    }
    return [
      {
        label: t('common.edit'),
        key: 'edit',
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
        permission: ['CLUE_MANAGEMENT:UPDATE'],
      },
      {
        label: t('common.transfer'),
        key: 'transfer',
        permission: ['CLUE_MANAGEMENT:UPDATE'],
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
    ];
  });

  const buttonMoreList = computed<ActionsItem[]>(() => {
    if (isConverted.value) {
      return [];
    }
    return [
      // TODO 先不要了
      // ...(currentStatus.value !== StageResultEnum.FAIL
      //   ? [
      //       {
      //         label: t('clue.convertToCustomer'),
      //         key: 'convertToCustomer',
      //         permission: ['CLUE_MANAGEMENT:READ', 'CUSTOMER_MANAGEMENT:ADD'],
      //         allPermission: true,
      //       },
      //     ]
      //   : []),
      {
        label: t('clue.convertToCustomer'),
        key: 'convertToCustomer',
        permission: ['CLUE_MANAGEMENT:READ', 'CUSTOMER_MANAGEMENT:ADD'],
        allPermission: true,
      },
      {
        type: 'divider',
      },
      {
        label: t('common.delete'),
        key: 'delete',
        permission: ['CLUE_MANAGEMENT:DELETE'],
        danger: true,
      },
    ];
  });

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
      tab: t('common.plan'),
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

  function handleDescriptionInit(_collaborationType?: CollaborationType, _sourceName?: string) {
    sourceName.value = _sourceName || '';
  }
</script>
