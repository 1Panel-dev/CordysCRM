<template>
  <CrmDrawer v-model:show="visible" :title="detailInfo?.name" resizable no-padding :width="800" :footer="false">
    <template #titleLeft>
      <div v-if="enableApproval" class="text-[14px] font-normal">
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
      <CrmCard hide-footer>
        <div class="flex-1">
          <CrmFormDescription
            :form-key="FormDesignKeyEnum.INVOICE_SNAPSHOT"
            :source-id="props.sourceId"
            :column="2"
            :refresh-key="refreshKey"
            label-width="auto"
            value-align="start"
            tooltip-position="top-start"
            readonly
            @init="handleInit"
            @open-contract-detail="emit('openContractDrawer', $event)"
            @open-customer-detail="emit('openCustomerDrawer', $event)"
          />
        </div>
      </CrmCard>
    </div>

    <CrmFormCreateDrawer
      v-model:visible="formCreateDrawerVisible"
      :form-key="FormDesignKeyEnum.INVOICE"
      :source-id="props.sourceId"
      need-init-detail
      @saved="() => handleSaved()"
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

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmApprovalStatus from '@/components/business/crm-approval/components/crm-approval-status.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import { approvalInvoiced, deleteInvoiced, revokeInvoiced } from '@/api/modules';
  import { deleteInvoiceContentMap } from '@/config/contract';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useModal from '@/hooks/useModal';

  const props = defineProps<{
    sourceId: string;
    readonly?: boolean;
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

  function handleInit(type?: CollaborationType, name?: string, detail?: Record<string, any>) {
    detailInfo.value = detail;
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

  const { initApprovalPermission, resolveRowOperation, enableApproval } = useApprovalOperation<ContractInvoiceItem>({
    formType: FormDesignKeyEnum.INVOICE,
    dataActionMap: invoiceDetailDataActionMap,
    isDetail: true,
    identityResolver: {
      isApplicant: (row, currentUserId) => row.createUser === currentUserId,
      isApprover: () =>
        // todo xinxinwu 不确定审批人如何返回
        true,
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

  async function handleRevoke() {
    try {
      await revokeInvoiced(props.sourceId);
      Message.success(t('common.revokeSuccess'));
      handleSaved();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function handleButtonClick(actionKey: string) {
    switch (actionKey) {
      case 'pass':
        handleApproval(true);
        break;
      case 'unPass':
        handleApproval();
        break;
      case 'review':
        // todo 提审 xinxinwu
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
