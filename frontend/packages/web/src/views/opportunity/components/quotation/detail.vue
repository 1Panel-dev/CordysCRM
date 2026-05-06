<template>
  <CrmDrawer v-model:show="visible" resizable no-padding :width="800" :footer="false" :title="detailInfo?.name ?? ''">
    <template #titleLeft>
      <div class="text-[14px]b flex items-center gap-[8px] font-normal">
        <CrmApprovalStatus v-if="isShowApprovalStatus" :status="detailInfo?.approvalStatus || ProcessStatusEnum.NONE" />
        <CrmTag
          theme="light"
          :type="detailInfo?.status && detailInfo?.status === QuotationStatusEnum.VOIDED ? 'default' : 'info'"
        >
          {{
            detailInfo?.status && detailInfo?.status === QuotationStatusEnum.VOIDED
              ? t('common.voided')
              : t('common.normal')
          }}
        </CrmTag>
      </div>
    </template>
    <template #titleRight>
      <CrmOperationButton
        class="gap-[12px]"
        :not-show-divider="true"
        :group-list="detailActions.groupList"
        :more-list="detailActions.moreList"
        @select="handleSelect"
      >
        <template #more>
          <n-button type="primary" ghost class="n-btn-outline-primary">
            {{ t('common.more') }}
            <CrmIcon class="ml-[8px]" type="iconicon_chevron_down" :size="16" />
          </n-button>
        </template>
      </CrmOperationButton>
    </template>
    <CrmFormDescription
      ref="formDescriptionRef"
      :form-key="FormDesignKeyEnum.OPPORTUNITY_QUOTATION_SNAPSHOT"
      :source-id="props.sourceId"
      :column="2"
      :refresh-key="refreshKey"
      label-width="auto"
      value-align="start"
      tooltip-position="top-start"
      class="p-[16px]"
      readonly
      @init="handleInit"
    />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { QuotationStatusEnum } from '@lib/shared/enums/opportunityEnum';
  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { CollaborationType } from '@lib/shared/models/customer';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmApprovalStatus from '@/components/business/crm-approval/components/crm-approval-status.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import { approvalQuotation, deleteQuotation, revokeQuotation, voidQuotation } from '@/api/modules';
  import { quotationDataActionMap } from '@/config/opportunity';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useModal from '@/hooks/useModal';
  import useOpenNewPage from '@/hooks/useOpenNewPage';
  import { useUserStore } from '@/store';

  import { FullPageEnum } from '@/enums/routeEnum';

  const { openModal } = useModal();
  const { openNewPage } = useOpenNewPage();

  const useStore = useUserStore();
  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    sourceId: string;
  }>();

  const emit = defineEmits<{
    (e: 'edit', sourceId: string): void;
    (e: 'refresh'): void;
    (e: 'remove'): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const refreshKey = ref(0);
  const title = ref('');
  const detailInfo = ref();

  function handleInit(type?: CollaborationType, name?: string, detail?: Record<string, any>) {
    title.value = name || '';
    detailInfo.value = detail ?? {};
  }

  function handleDownload() {
    openNewPage(FullPageEnum.FULL_PAGE_EXPORT_QUOTATION, { id: props.sourceId });
  }

  function handleSavedRefresh() {
    refreshKey.value += 1;
    emit('refresh');
  }

  const formDescriptionRef = ref<InstanceType<typeof CrmFormDescription> | null>(null);
  async function handleApproval(approval = false) {
    const approvalStatus = approval ? ProcessStatusEnum.APPROVED : ProcessStatusEnum.UNAPPROVED;
    const { name, opportunityId, moduleFields = [], products = [] } = detailInfo.value;
    try {
      await approvalQuotation({
        id: props.sourceId,
        name: name ?? '',
        approvalStatus,
        opportunityId: opportunityId ?? '',
        moduleFormConfigDTO: formDescriptionRef.value?.moduleFormConfig,
        moduleFields,
        products,
      });
      Message.success(approval ? t('common.approvedSuccess') : t('common.unApprovedSuccess'));
      handleSavedRefresh();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function handleVoid() {
    openModal({
      type: 'error',
      title: t('opportunity.quotation.voidTitleTip', { name: characterLimit(detailInfo.value.name ?? '') }),
      content: t('opportunity.quotation.invalidContentTip'),
      positiveText: t('common.confirmVoid'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await voidQuotation(props.sourceId);
          Message.success(t('common.voidSuccess'));
          handleSavedRefresh();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  async function handleRevoke() {
    try {
      await revokeQuotation(props.sourceId);
      Message.success(t('common.revokeSuccess'));
      handleSavedRefresh();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function handleDelete() {
    openModal({
      type: 'error',
      title: t('opportunity.quotation.deleteTitleTip', { name: characterLimit(detailInfo.value.name ?? '') }),
      content: t('opportunity.quotation.deleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteQuotation(props.sourceId);
          Message.success(t('common.deleteSuccess'));
          visible.value = false;
          emit('remove');
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleSelect(key: string) {
    switch (key) {
      case 'edit':
        emit('edit', props.sourceId);
        visible.value = false;
        break;
      case 'review':
        // todo 提审 xinxinwu
        break;
      case 'pass':
        handleApproval(true);
        break;
      case 'unPass':
        handleApproval();
        break;
      case 'voided':
        handleVoid();
        break;
      case 'revoke':
        handleRevoke();
        break;
      case 'download':
        handleDownload();
        break;
      case 'delete':
        handleDelete();
        break;
      default:
        break;
    }
  }
  const { initApprovalPermission, resolveRowOperation, enableApproval } = useApprovalOperation<Record<string, any>>({
    formType: FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
    dataActionMap: quotationDataActionMap,
    isDetail: true,
    identityResolver: {
      isApplicant: (row, currentUserId) => row.createUser === currentUserId,
      isApprover: (row, currentUserId) =>
        // todo xinxinwu 不确定审批人如何返回
        // row.isApprover,
        true,
    },
    specialActionFilter: (row, actionKeys) => {
      if (row.status === QuotationStatusEnum.VOIDED) {
        // todo xinxinwu 状态历史数据
        return actionKeys.filter((key) => key === 'delete');
      }
      return actionKeys;
    },
  });

  const detailActions = computed<{
    groupList: ActionsItem[];
    moreList: ActionsItem[];
  }>(() => {
    if (!detailInfo.value) {
      return { groupList: [], moreList: [] };
    }

    const detailAction = resolveRowOperation(detailInfo.value);
    return {
      ...detailAction,
      groupList: detailAction.groupList.map((e) => {
        return {
          ...e,
          text: false,
          ghost: true,
        };
      }),
    };
  });
  const isShowApprovalStatus = computed(() => {
    return detailInfo.value?.status !== QuotationStatusEnum.VOIDED && enableApproval.value;
  });

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        initApprovalPermission();
      }
    }
  );
</script>

<style scoped></style>
