<template>
  <CrmDrawer v-model:show="visible" resizable no-padding :width="800" :footer="false" :title="title">
    <template #titleLeft>
      <div class="text-[14px] font-normal">
        {{ stageName }}
      </div>
    </template>
    <template #titleRight>
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
            @openCustomerDetail="emit('showCustomerDrawer', $event)"
            @init="handleInit"
          />
        </div>
        <template v-if="activeTab === 'payment'">
          <PaymentTable
            :form-key="FormDesignKeyEnum.CONTRACT_CONTRACT_PAYMENT"
            :sourceId="props.sourceId"
            :sourceName="title"
            isContractTab
            :readonly="
              detailInfo?.stage === ContractStatusEnum.VOID ||
              detailInfo?.approvalStatus === QuotationStatusEnum.APPROVING
            "
          />
        </template>
        <template v-if="activeTab === 'paymentRecord'">
          <PaymentRecordTable
            :form-key="FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD"
            :sourceId="props.sourceId"
            :sourceName="title"
            isContractTab
            :readonly="
              detailInfo?.stage === ContractStatusEnum.VOID ||
              detailInfo?.approvalStatus === QuotationStatusEnum.APPROVING
            "
          />
        </template>
      </CrmCard>
    </div>
    <CrmFormCreateDrawer
      v-model:visible="formCreateDrawerVisible"
      :source-id="activeSourceId"
      :form-key="activeFormKey"
      :need-init-detail="needInitDetail"
      :initial-source-name="initialSourceName"
      :link-form-key="FormDesignKeyEnum.CONTRACT"
      :link-form-info="linkFormInfo"
      @saved="() => handleSaved()"
    />
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { useMessage } from 'naive-ui';

  import { ContractStatusEnum } from '@lib/shared/enums/contractEnum';
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
  import PaymentTable from '@/views/contract/contractPaymentPlan/components/paymentTable.vue';
  import PaymentRecordTable from '@/views/contract/contractPaymentRecord/components/paymentTable.vue';

  import { approvalContract, deleteContract, revokeContract } from '@/api/modules';
  import { contractStatusOptions } from '@/config/contract';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useModal from '@/hooks/useModal';
  import { useUserStore } from '@/store';
  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    sourceId: string;
  }>();
  const emit = defineEmits<{
    (e: 'refresh'): void;
    (e: 'showCustomerDrawer', params: { customerId: string; inCustomerPool: boolean; poolId: string }): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const useStore = useUserStore();
  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();
  const title = ref('');
  const detailInfo = ref();

  const stageName = computed(() => {
    return contractStatusOptions.find((item) => item.value === detailInfo.value?.stage)?.label;
  });

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
      {
        name: 'paymentRecord',
        tab: t('module.paymentRecord'),
        permission: ['CONTRACT_PAYMENT_RECORD:READ'],
      },
    ].filter((item) => hasAnyPermission(item.permission))
  );

  const buttonList = computed(() => {
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
          permission: ['CONTRACT:DELETE'],
        },
      ];
    }
    if (detailInfo.value?.approvalStatus === QuotationStatusEnum.APPROVED) {
      return [
        {
          label: t('contract.payment'),
          key: 'paymentRecord',
          permission: ['CONTRACT:PAYMENT'],
          text: false,
          ghost: true,
          class: 'n-btn-outline-primary',
          disabled: !detailInfo.value.amount,
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
    ];
  });

  function handleInit(type?: CollaborationType, name?: string, detail?: Record<string, any>) {
    title.value = name || '';
    detailInfo.value = detail ?? {};
  }

  const formCreateDrawerVisible = ref(false);
  const needInitDetail = ref(true);
  const initialSourceName = ref('');
  const activeFormKey = ref(FormDesignKeyEnum.CONTRACT);
  const activeSourceId = ref('');

  function handleEdit() {
    needInitDetail.value = true;
    initialSourceName.value = '';
    activeFormKey.value = FormDesignKeyEnum.CONTRACT;
    activeSourceId.value = props.sourceId;
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

  async function handleRevoke() {
    try {
      await revokeContract(props.sourceId);
      Message.success(t('common.revokeSuccess'));
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

  // 回款
  const linkFormInfo = ref();
  const { initFormDetail, initFormConfig, linkFormFieldMap } = useFormCreateApi({
    formKey: ref(FormDesignKeyEnum.CONTRACT),
    sourceId: activeSourceId,
  });
  async function handlePaymentRecord(row: ContractItem) {
    activeSourceId.value = row.id;
    initialSourceName.value = row.name;
    needInitDetail.value = false;
    activeFormKey.value = FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD;
    await initFormConfig();
    await initFormDetail(false, true);
    linkFormInfo.value = linkFormFieldMap.value;
    formCreateDrawerVisible.value = true;
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
      case 'paymentRecord':
        handlePaymentRecord(detailInfo.value);
        break;
      case 'delete':
        handleDelete(detailInfo.value);
        break;
      default:
        break;
    }
  }
</script>
