<template>
  <CrmDrawer
    v-model:show="visible"
    :title="detailInfo?.name"
    resizable
    no-padding
    :width="800"
    :footer="false"
    :view-size="formViewSize"
  >
    <template #titleLeft>
      <div class="text-[14px] font-normal">
        <CrmApprovalStatus :status="detailInfo?.approvalStatus ?? ProcessStatusEnum.NONE" />
      </div>
    </template>
    <template v-if="!props.readonly" #titleRight>
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
      <CrmCard no-content-padding auto-height hide-footer>
        <div class="flex-1">
          <CrmApprovalDetail
            :form-key="FormDesignKeyEnum.INVOICE"
            :source-id="props.sourceId"
            :approval-status="detailInfo?.approvalStatus"
            @save-approval="handleSaveApproval"
          >
            <template #left="{ fieldPermissions }">
              <CrmFormDescription
                ref="formDescriptionRef"
                :form-key="FormDesignKeyEnum.INVOICE_SNAPSHOT"
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
                :readonly="!hasAnyPermission(['CONTRACT_INVOICE:UPDATE'])"
                @init="handleInit"
                @open-contract-detail="emit('openContractDrawer', $event)"
                @open-customer-detail="emit('openCustomerDrawer', $event)"
              />
            </template>
          </CrmApprovalDetail>
        </div>
      </CrmCard>
    </div>

    <CrmFormCreateDrawer
      v-model:visible="formCreateDrawerVisible"
      :form-key="FormDesignKeyEnum.INVOICE"
      :source-id="props.sourceId"
      need-init-detail
      @saved="() => handleSaved()"
      @review="handleFormReview"
    />
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { NButton, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { ContractInvoiceItem } from '@lib/shared/models/contract';
  import { CollaborationType } from '@lib/shared/models/customer';
  import type { FormConfig, FormViewSize } from '@lib/shared/models/system/module';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmApprovalDetail from '@/components/business/crm-approval/components/crm-approval-detail.vue';
  import CrmApprovalStatus from '@/components/business/crm-approval/components/crm-approval-status.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import { approvalInvoiced, deleteInvoiced } from '@/api/modules';
  import { deleteInvoiceContentMap } from '@/config/contract';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useApprovalResourceAction from '@/hooks/useApprovalResourceAction';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    sourceId: string;
    readonly?: boolean;
    approvalTaskId?: string;
  }>();
  const emit = defineEmits<{
    (e: 'refresh'): void;
    (e: 'delete'): void;
    (e: 'openContractDrawer', params: { id: string }): void;
    (e: 'openCustomerDrawer', params: { customerId: string; inCustomerPool: boolean; poolId: string }): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();

  const detailInfo = ref();
  const formViewSize = ref<FormViewSize>('large');

  function handleInit(type?: CollaborationType, name?: string, detail?: Record<string, any>, config?: FormConfig) {
    detailInfo.value = detail;
    formViewSize.value = config?.viewSize || 'large';
  }

  const invoiceDetailDataActionMap = {
    edit: {
      key: 'edit',
      label: t('common.edit'),
      permission: ['CONTRACT_INVOICE:UPDATE'],
    },
    delete: {
      label: t('common.delete'),
      key: 'delete',
      danger: true,
      permission: ['CONTRACT_INVOICE:DELETE'],
    },
  };

  const { initApprovalPermission, resolveRowOperation } = useApprovalOperation<ContractInvoiceItem>({
    formType: FormDesignKeyEnum.INVOICE,
    dataActionMap: invoiceDetailDataActionMap,
    isDetail: true,
    identityResolver: {
      isApplicant: (row, currentUserId) => row.createUser === currentUserId,
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
          class: 'n-btn-outline-primary',
        };
      }),
    };
  });

  const refreshKey = ref(0);
  function handleSaved() {
    refreshKey.value += 1;
    emit('refresh');
  }

  const { reviewByFormResult, reviewByResourceId, revokeByResourceId } = useApprovalResourceAction({
    formKey: FormDesignKeyEnum.INVOICE,
  });

  function handleDelete(row: ContractInvoiceItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: row.name }),
      content: deleteInvoiceContentMap[row.approvalStatus],
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteInvoiced(row.id);
          Message.success(t('common.deleteSuccess'));
          visible.value = false;
          emit('delete');
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  const formCreateDrawerVisible = ref(false);
  function handleEdit() {
    formCreateDrawerVisible.value = true;
  }

  async function handleApproval(approval = false) {
    const approvalStatus = approval ? ProcessStatusEnum.APPROVED : ProcessStatusEnum.UNAPPROVED;
    try {
      await approvalInvoiced({
        id: props.sourceId,
        approvalStatus,
      });
      Message.success(approval ? t('common.approvedSuccess') : t('common.unApprovedSuccess'));
      handleSaved();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

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

  function handleRevoke() {
    revokeByResourceId(props.sourceId, {
      onSuccess: () => {
        handleSaved();
      },
    });
  }

  function handleFormReview(res: any) {
    reviewByFormResult(res, {
      onSuccess: () => {
        handleSaved();
      },
    });
  }

  function handleReview() {
    reviewByResourceId(props.sourceId, {
      onSuccess: () => {
        handleSaved();
      },
    });
  }

  async function handleButtonClick(actionKey: string) {
    switch (actionKey) {
      case 'review':
        handleReview();
        break;
      case 'edit':
        handleEdit();
        break;
      case 'revoke':
        handleRevoke();
        break;
      case 'delete':
        handleDelete(detailInfo.value);
        break;
      default:
        break;
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
