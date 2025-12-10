<template>
  <CrmDrawer v-model:show="visible" resizable no-padding :width="800" :footer="false" :title="title">
    <template #titleLeft>
      <div class="text-[14px] font-normal">
        <ContractStatus :status="detailInfo?.status ?? ContractStatusEnum.SIGNED" />
      </div>
    </template>
    <template v-if="detailInfo?.status !== ContractStatusEnum.VOID" #titleRight>
      <CrmButtonGroup class="gap-[12px]" :list="buttonList" not-show-divider @select="handleButtonClick" />
    </template>
    <div class="h-full bg-[var(--text-n9)] p-[16px]">
      <CrmCard no-content-padding hide-footer auto-height class="mb-[16px]">
        <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line" />
      </CrmCard>

      <CrmCard hide-footer :special-height="64" noContentBottomPadding>
        <!-- 需要用到 detailInfo 所以这里不用 v-if -->
        <div v-show="activeTab === 'contract'">
          <CrmFormDescription
            :form-key="FormDesignKeyEnum.CONTRACT_SNAPSHOT"
            :source-id="props.sourceId"
            :column="2"
            :refresh-key="refreshKey"
            label-width="auto"
            value-align="start"
            tooltip-position="top-start"
            readonly
            @openCustomerDetail="emit('showCustomerDrawer', detailInfo)"
            @init="handleInit"
          />
        </div>
        <template v-if="activeTab !== 'contract'">
          <PaymentTable
            :form-key="FormDesignKeyEnum.CONTRACT_CONTRACT_PAYMENT"
            :sourceId="props.sourceId"
            :sourceName="title"
            isContractTab
            :readonly="
              detailInfo?.status === ContractStatusEnum.VOID ||
              detailInfo?.approvalStatus === QuotationStatusEnum.APPROVING
            "
          />
        </template>
      </CrmCard>
    </div>
    <CrmFormCreateDrawer
      v-model:visible="formCreateDrawerVisible"
      :form-key="FormDesignKeyEnum.CONTRACT"
      :source-id="props.sourceId"
      need-init-detail
      :link-form-key="FormDesignKeyEnum.CONTRACT"
      @saved="() => handleSaved()"
    />

    <VoidReasonModal
      v-model:visible="showVoidReasonModal"
      :name="detailInfo?.name ?? ''"
      :sourceId="props.sourceId"
      @refresh="handleSaved"
    />
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { useMessage } from 'naive-ui';

  import { ArchiveStatusEnum, ContractStatusEnum } from '@lib/shared/enums/contractEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { QuotationStatusEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { ContractItem } from '@lib/shared/models/contract';
  import { CollaborationType } from '@lib/shared/models/customer';

  import CrmButtonGroup from '@/components/pure/crm-button-group/index.vue';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import VoidReasonModal from './voidReasonModal.vue';
  import ContractStatus from '@/views/contract/contract/components/contractStatus.vue';
  import PaymentTable from '@/views/contract/contractPaymentPlan/components/paymentTable.vue';

  import { approvalContract, archivedContract, deleteContract } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    sourceId: string;
  }>();
  const emit = defineEmits<{
    (e: 'refresh'): void;
    (e: 'showCustomerDrawer', row: ContractItem): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();
  const title = ref('');
  const detailInfo = ref();

  const activeTab = ref('contract');

  const tabList = computed(() =>
    [
      {
        name: 'contract',
        tab: t('module.contract'),
        permission: ['CONTRACT:READ'],
      },
      {
        name: 'payment',
        tab: t('module.paymentPlan'),
        permission: ['CONTRACT_PAYMENT_PLAN:READ'],
      },
    ].filter((item) => hasAnyPermission(item.permission))
  );

  const buttonList = computed(() => {
    if (detailInfo.value?.archivedStatus === ArchiveStatusEnum.ARCHIVED) {
      return [
        {
          key: 'unarchive',
          label: t('common.unarchive'),
          text: false,
          ghost: true,
          class: 'n-btn-outline-primary',
          permission: ['CONTRACT:ARCHIVE'],
        },
      ];
    }
    if (detailInfo.value?.approvalStatus === QuotationStatusEnum.APPROVING) {
      return [
        {
          label: t('common.pass'),
          key: 'pass',
          text: false,
          ghost: true,
          class: 'n-btn-outline-primary',
          permission: ['CONTRACT:APPROVAL'],
        },
        {
          label: t('common.unPass'),
          key: 'unPass',
          danger: true,
          text: false,
          ghost: true,
          class: 'n-btn-outline-primary',
          permission: ['CONTRACT:APPROVAL'],
        },
      ];
    }
    if (detailInfo.value?.approvalStatus === QuotationStatusEnum.APPROVED) {
      return [
        {
          key: 'archive',
          label: t('common.archive'),
          permission: ['CONTRACT:ARCHIVE'],
          text: false,
          ghost: true,
          class: 'n-btn-outline-primary',
        },
        {
          key: 'voided',
          label: t('common.voided'),
          permission: ['CONTRACT:VOIDED'],
          text: false,
          ghost: true,
          class: 'n-btn-outline-primary',
        },
      ];
    }
    return [
      {
        key: 'edit',
        label: t('common.edit'),
        permission: ['CONTRACT:UPDATE'],
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
        permission: ['CONTRACT:DELETE'],
      },
      {
        key: 'voided',
        label: t('common.voided'),
        permission: ['CONTRACT:VOIDED'],
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
      },
    ];
  });

  function handleInit(type?: CollaborationType, name?: string, detail?: Record<string, any>) {
    title.value = name || '';
    detailInfo.value = detail ?? {};
  }

  const formCreateDrawerVisible = ref(false);
  function handleEdit() {
    formCreateDrawerVisible.value = true;
  }

  const refreshKey = ref(0);
  function handleSaved() {
    refreshKey.value += 1;
    emit('refresh');
  }

  function handleDelete(row: ContractItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('common.deleteConfirmContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteContract(row.id);
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

  const showVoidReasonModal = ref(false);
  function handleVoided() {
    showVoidReasonModal.value = true;
  }

  async function handleArchive(id: string, status: string) {
    try {
      const isArchived = status === ArchiveStatusEnum.ARCHIVED;
      await archivedContract(id, isArchived ? ArchiveStatusEnum.UN_ARCHIVED : ArchiveStatusEnum.ARCHIVED);
      if (!isArchived) {
        Message.success(t('common.batchArchiveSuccess'));
      } else {
        Message.success(`${t('common.unarchive')}${t('common.success')}`);
      }
      handleSaved();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function handleApproval(approval = false) {
    const approvalStatus = approval ? QuotationStatusEnum.APPROVED : QuotationStatusEnum.UNAPPROVED;
    try {
      await approvalContract({
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
      case 'unarchive':
        handleArchive(props.sourceId, detailInfo.value.archivedStatus);
        break;
      case 'archive':
        handleArchive(props.sourceId, detailInfo.value.archivedStatus);
        break;
      case 'voided':
        handleVoided();
        break;
      case 'delete':
        handleDelete(detailInfo.value);
        break;
      default:
        break;
    }
  }
</script>
