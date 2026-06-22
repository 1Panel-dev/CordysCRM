<template>
  <CrmDrawer
    v-model:show="visible"
    resizable
    no-padding
    :width="800"
    :footer="false"
    :title="detailInfo?.name ?? ''"
    :view-size="formViewSize"
  >
    <template #titleLeft>
      <div class="text-[14px]b flex items-center gap-[8px] font-normal">
        <CrmApprovalStatus v-if="isShowApprovalStatus" :status="detailInfo?.approvalStatus || ProcessStatusEnum.NONE" />
        <CrmTag theme="light" :type="detailInfo?.invalid ? 'default' : 'info'">
          {{ detailInfo?.invalid ? t('common.voided') : t('common.normal') }}
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
    <CrmApprovalDetail
      :form-key="FormDesignKeyEnum.OPPORTUNITY_QUOTATION"
      :source-id="props.sourceId"
      :refresh-key="approvalDetailRefreshKey"
      :approval-status="detailInfo?.approvalStatus"
      @saveApproval="handleSaveApproval"
    >
      <template #left="{ fieldPermissions }">
        <CrmFormDescription
          ref="formDescriptionRef"
          :form-key="FormDesignKeyEnum.OPPORTUNITY_QUOTATION_SNAPSHOT"
          :source-id="props.sourceId"
          :column="2"
          :refresh-key="refreshKey"
          :fieldPermissions="fieldPermissions"
          :otherSaveParams="{
            updateType: 'approval',
            approvalTaskId: props.approvalTaskId,
          }"
          label-width="auto"
          value-align="start"
          tooltip-position="top-start"
          :readonly="!hasApprovalScopedPermission(detailInfo, ['OPPORTUNITY_QUOTATION:UPDATE'])"
          @init="handleInit"
        />
      </template>
    </CrmApprovalDetail>
  </CrmDrawer>
  <CrmFormCreateDrawer
    v-model:visible="formCreateDrawerVisible"
    :form-key="activeFormKey"
    :source-id="props.sourceId"
    :need-init-detail="needInitDetail"
    :initial-source-name="initialSourceName"
    :other-save-params="otherSaveParams"
    @saved="handleFormCreateSaved"
  />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { CollaborationType } from '@lib/shared/models/customer';
  import type { FormConfig, FormViewSize } from '@lib/shared/models/system/module';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmApprovalDetail from '@/components/business/crm-approval/components/crm-approval-detail.vue';
  import CrmApprovalStatus from '@/components/business/crm-approval/components/crm-approval-status.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import { deleteQuotation, voidQuotation } from '@/api/modules';
  import { quotationDataActionMap } from '@/config/opportunity';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useApprovalResourceAction from '@/hooks/useApprovalResourceAction';
  import useModal from '@/hooks/useModal';
  import useOpenNewPage from '@/hooks/useOpenNewPage';

  import { FullPageEnum } from '@/enums/routeEnum';

  const { openModal } = useModal();
  const { openNewPage } = useOpenNewPage();

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    sourceId: string;
    approvalTaskId?: string;
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
  const approvalDetailRefreshKey = ref(0);
  const title = ref('');
  const detailInfo = ref();
  const formViewSize = ref<FormViewSize>('large');

  function handleInit(type?: CollaborationType, name?: string, detail?: Record<string, any>, config?: FormConfig) {
    title.value = name || '';
    detailInfo.value = detail ?? {};
    formViewSize.value = config?.viewSize || 'large';
  }

  function handleDownload() {
    openNewPage(FullPageEnum.FULL_PAGE_EXPORT_QUOTATION, { id: props.sourceId });
  }

  function handleSavedRefresh() {
    refreshKey.value += 1;
    emit('refresh');
  }

  const { reviewByResourceId, revokeByResourceId } = useApprovalResourceAction({
    formKey: FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
  });

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

  function handleRevoke() {
    revokeByResourceId(props.sourceId, {
      onSuccess: () => {
        handleSavedRefresh();
      },
    });
  }

  function handleReview() {
    reviewByResourceId(props.sourceId, {
      onSuccess: () => {
        handleSavedRefresh();
      },
    });
  }

  const formCreateDrawerVisible = ref(false);
  const initialSourceName = ref('');
  const needInitDetail = ref(false);
  const activeFormKey = ref(FormDesignKeyEnum.OPPORTUNITY_QUOTATION);
  const otherSaveParams = ref<Record<string, any>>({
    id: '',
  });

  function handleFormCreateSaved(_res?: any, isUpdateReview?: boolean) {
    if (isUpdateReview) {
      approvalDetailRefreshKey.value += 1;
    }
    refreshKey.value += 1;
    emit('refresh');
  }

  const { initApprovalPermission, resolveRowOperation, deleteExecute, hasApprovalScopedPermission } =
    useApprovalOperation<Record<string, any>>({
      formType: FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
      dataActionMap: quotationDataActionMap,
      isDetail: true,
      identityResolver: {
        isApplicant: (row, currentUserId) => row.createUser === currentUserId,
      },
      shouldUseRolePermissionOnly: (row) => row.invalid,
      specialActionFilter: (row, actionKeys) => {
        if (row.invalid) {
          return actionKeys.filter((key) => key === 'delete');
        }
        return actionKeys;
      },
    });

  function handleDelete() {
    openModal({
      type: 'error',
      title: t('opportunity.quotation.deleteTitleTip', { name: characterLimit(detailInfo.value.name ?? '') }),
      content: t('opportunity.quotation.deleteContentTip'),
      positiveText: deleteExecute.value ? t('crm.approval.confirmAndSubmitReview') : t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteQuotation(props.sourceId);
          Message.success(deleteExecute.value ? t('common.reviewSuccess') : t('common.deleteSuccess'));
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
        activeFormKey.value = FormDesignKeyEnum.OPPORTUNITY_QUOTATION;
        needInitDetail.value = true;
        otherSaveParams.value.id = props.sourceId;
        formCreateDrawerVisible.value = true;
        break;
      case 'review':
        handleReview();
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
          class: 'n-btn-outline-primary',
        };
      }),
    };
  });
  const isShowApprovalStatus = computed(() => {
    return !detailInfo.value?.invalid;
  });

  const formDescriptionRef = ref<InstanceType<typeof CrmFormDescription>>();
  async function handleSaveApproval(callback: () => Promise<any>, hasFieldPermission: boolean) {
    if (hasFieldPermission) {
      formDescriptionRef.value?.handleFormChange(async () => {
        await callback();
        refreshKey.value += 1;
        emit('refresh');
      });
    } else {
      await callback();
      refreshKey.value += 1;
      emit('refresh');
    }
  }

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
