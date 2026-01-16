<template>
  <CrmDrawer v-model:show="visible" resizable no-padding :width="800" :footer="false">
    <template #titleLeft>
      <div class="text-[14px] font-normal">
        <contractInvoiceStatus :status="detailInfo?.approvalStatus ?? ContractInvoiceStatusEnum.APPROVING" />
      </div>
    </template>
    <template v-if="!props.readonly" #titleRight>
      <CrmButtonGroup class="gap-[12px]" :list="buttonList" not-show-divider @select="handleButtonClick" />
    </template>
    <div class="h-full bg-[var(--text-n9)] px-[16px] pt-[16px]">
      <CrmCard hide-footer>
        <div class="flex-1">
          <CrmFormDescription
            :form-key="FormDesignKeyEnum.INVOICE"
            :source-id="props.sourceId"
            :column="2"
            :refresh-key="refreshKey"
            label-width="auto"
            value-align="start"
            tooltip-position="top-start"
            @init="handleInit"
            @open-contract-detail="emit('openContractDrawer', $event)"
          />
        </div>
      </CrmCard>
    </div>

    <CrmFormCreateDrawer
      v-model:visible="formCreateDrawerVisible"
      :form-key="FormDesignKeyEnum.INVOICE"
      :source-id="props.sourceId"
      need-init-detail
      :link-form-key="FormDesignKeyEnum.INVOICE"
      @saved="() => handleSaved()"
    />
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { useMessage } from 'naive-ui';

  import { ContractInvoiceStatusEnum } from '@lib/shared/enums/contractEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { CollaborationType } from '@lib/shared/models/customer';

  import CrmButtonGroup from '@/components/pure/crm-button-group/index.vue';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import contractInvoiceStatus from './contractInvoiceStatus.vue';

  import { approvalInvoiced, deleteInvoiced, revokeInvoiced } from '@/api/modules';
  import { deleteInvoiceContentMap } from '@/config/contract';
  import useModal from '@/hooks/useModal';
  import useUserStore from '@/store/modules/user';

  const props = defineProps<{
    sourceId: string;
    readonly?: boolean;
  }>();
  const emit = defineEmits<{
    (e: 'refresh'): void;
    (e: 'openContractDrawer', params: { id: string }): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const useStore = useUserStore();
  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();
  const detailInfo = ref();

  function handleInit(type?: CollaborationType, name?: string, detail?: Record<string, any>) {
    detailInfo.value = detail;
  }

  const buttonList = computed(() => {
    if (detailInfo.value?.approvalStatus === ContractInvoiceStatusEnum.APPROVING) {
      return [
        {
          label: t('common.pass'),
          key: 'pass',
          text: false,
          ghost: true,
          class: 'n-btn-outline-primary',
          permission: ['CONTRACT_INVOICE:APPROVAL'],
        },
        {
          label: t('common.unPass'),
          key: 'unPass',
          danger: true,
          text: false,
          ghost: true,
          class: 'n-btn-outline-primary',
          permission: ['CONTRACT_INVOICE:APPROVAL'],
        },
        ...(detailInfo.value?.createUser === useStore.userInfo.id
          ? [
              {
                label: t('common.revoke'),
                key: 'revoke',
                text: false,
                ghost: true,
                class: 'n-btn-outline-primary',
              },
            ]
          : []),
        {
          label: t('common.delete'),
          key: 'delete',
          text: false,
          ghost: true,
          danger: true,
          class: 'n-btn-outline-primary',
          permission: ['CONTRACT_INVOICE:DELETE'],
        },
      ];
    }
    if (detailInfo.value?.approvalStatus === ContractInvoiceStatusEnum.APPROVED) {
      return [
        {
          label: t('common.delete'),
          key: 'delete',
          text: false,
          ghost: true,
          danger: true,
          class: 'n-btn-outline-primary',
          permission: ['CONTRACT_INVOICE:DELETE'],
        },
      ];
    }
    return [
      {
        key: 'edit',
        label: t('common.edit'),
        permission: ['CONTRACT_INVOICE:UPDATE'],
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
      },
      {
        label: t('common.delete'),
        key: 'delete',
        text: false,
        ghost: true,
        danger: true,
        class: 'n-btn-outline-primary',
        permission: ['CONTRACT_INVOICE:DELETE'],
      },
    ];
  });

  const refreshKey = ref(0);
  function handleSaved() {
    refreshKey.value += 1;
    emit('refresh');
  }

  function handleDelete(row: any) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: row.name }),
      content: deleteInvoiceContentMap[row.approvalStatus as ContractInvoiceStatusEnum],
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteInvoiced(row.id);
          Message.success(t('common.deleteSuccess'));
          visible.value = false;
          emit('refresh');
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
    const approvalStatus = approval ? ContractInvoiceStatusEnum.APPROVED : ContractInvoiceStatusEnum.UNAPPROVED;
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
</script>
