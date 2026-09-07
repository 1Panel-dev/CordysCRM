<template>
  <CrmDrawer
    v-model:show="visible"
    resizable
    no-padding
    :width="800"
    :footer="false"
    :title="title"
    :view-size="formViewSize"
  >
    <template #titleLeft>
      <CrmApprovalStatus :status="detailInfo?.approvalStatus ?? ProcessStatusEnum.NONE" />
    </template>
    <template #titleRight>
      <CrmOperationButton
        class="gap-[12px]"
        :not-show-divider="true"
        :group-list="detailActions.groupList"
        :more-list="detailActions.moreList"
        @select="handleButtonClick"
      >
        <template #more>
          <n-button type="primary" ghost class="n-btn-outline-primary">
            {{ t('common.more') }}
            <CrmIcon class="ml-[8px]" type="iconicon_chevron_down" :size="16" />
          </n-button>
        </template>
      </CrmOperationButton>
    </template>
    <div class="h-full bg-[var(--text-n9)] px-[16px] pt-[16px]">
      <CrmApprovalDetail
        :form-key="props.customFormId"
        :source-id="props.sourceId"
        :refresh-key="props.refreshId"
        :approval-status="detailInfo?.approvalStatus ?? ProcessStatusEnum.NONE"
        @saveApproval="handleSaveApproval"
        @refresh="emit('refresh')"
      >
        <template #left="{ fieldPermissions, taskNode }">
          <CrmFormDescription
            ref="formDescriptionRef"
            :form-key="FormDesignKeyEnum.CUSTOM_FORM"
            :source-id="props.sourceId"
            :column="3"
            :refresh-key="props.refreshId"
            label-width="auto"
            value-align="start"
            tooltip-position="top-start"
            :readonly="!isAdmin"
            :fieldPermissions="fieldPermissions"
            :otherSaveParams="{
              updateType: 'approval',
              approvalTaskId: props.approvalTaskId || taskNode?.taskId,
              customFormId: props.customFormId,
            }"
            :customFormId="props.customFormId"
            @init="handleInit"
          />
        </template>
      </CrmApprovalDetail>
    </div>
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { NButton, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { CollaborationType } from '@lib/shared/models/customer';
  import type { FormConfig, FormViewSize } from '@lib/shared/models/system/module';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmApprovalDetail from '@/components/business/crm-approval/components/crm-approval-detail.vue';
  import CrmApprovalStatus from '@/components/business/crm-approval/components/crm-approval-status.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import { deleteCustomFormData } from '@/api/modules';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useApprovalResourceAction from '@/hooks/useApprovalResourceAction';
  import useModal from '@/hooks/useModal';

  const props = defineProps<{
    sourceId: string;
    refreshId?: number;
    customFormId: string;
    approvalTaskId?: string;
  }>();
  const emit = defineEmits<{
    (e: 'edit', sourceId: string): void;
    (e: 'refresh'): void;
    (e: 'delete'): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const { t } = useI18n();
  const { openModal } = useModal();
  const Message = useMessage();
  const title = ref('');
  const isAdmin = ref(false);
  const formViewSize = ref<FormViewSize>('large');
  const detailInfo = ref<Record<string, any>>();
  const approvalFormKey = computed(() => props.customFormId || '');
  const customFormActionMap: Record<string, ActionsItem> = {
    edit: {
      label: t('common.edit'),
      key: 'edit',
      permission: [],
    },
    delete: {
      label: t('common.delete'),
      key: 'delete',
      permission: [],
      danger: true,
    },
  };
  const { initApprovalPermission, resolveRowOperation, deleteExecute } = useApprovalOperation<Record<string, any>>({
    formType: approvalFormKey,
    dataActionMap: customFormActionMap,
    isDetail: true,
    specialActionFilter: (row, actionKeys) => {
      if (row.isAdmin) {
        return actionKeys;
      }

      return actionKeys.filter((key) => !['edit', 'delete'].includes(key));
    },
    shouldUseRolePermissionOnly: () => true,
  });
  const { reviewByResourceId, revokeByResourceId } = useApprovalResourceAction({
    formKey: approvalFormKey,
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
      groupList: detailAction.groupList.map((e) => ({
        ...e,
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
      })),
    };
  });

  function handleInit(type?: CollaborationType, name?: string, detail?: Record<string, any>, config?: FormConfig) {
    title.value = name || '';
    isAdmin.value = !!detail?.isAdmin;
    detailInfo.value = detail;
    formViewSize.value = config?.viewSize || 'large';
  }

  const formDescriptionRef = ref<InstanceType<typeof CrmFormDescription>>();
  async function handleSaveApproval(callback: () => Promise<any>, hasFieldPermission: boolean) {
    if (hasFieldPermission) {
      formDescriptionRef.value?.handleFormChange(callback);
      return;
    }

    await callback();
  }

  // 删除
  function handleDelete() {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(title.value) }),
      content: t('common.deleteConfirmContent'),
      positiveText: deleteExecute.value ? t('crm.approval.confirmAndSubmitReview') : t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteCustomFormData(props.sourceId);
          Message.success(deleteExecute.value ? t('common.reviewSuccess') : t('common.deleteSuccess'));
          emit('delete');
          visible.value = false;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleButtonClick(key: string) {
    switch (key) {
      case 'edit':
        emit('edit', props.sourceId);
        break;
      case 'delete':
        handleDelete();
        break;
      case 'review':
        reviewByResourceId(props.sourceId, {
          onSuccess: () => emit('refresh'),
        });
        break;
      case 'revoke':
        revokeByResourceId(props.sourceId, {
          onSuccess: () => emit('refresh'),
        });
        break;
      default:
        break;
    }
  }

  watch(
    () => props.customFormId,
    () => {
      initApprovalPermission();
    },
    {
      immediate: true,
    }
  );
</script>
