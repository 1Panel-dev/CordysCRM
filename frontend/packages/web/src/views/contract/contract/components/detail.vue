<template>
  <CrmDrawer v-model:show="visible" resizable no-padding :footer="false" :title="title" :view-size="formViewSize">
    <template #titleLeft>
      <div class="text-[14px]b flex items-center gap-[8px] font-normal">
        <CrmApprovalStatus :status="detailInfo?.approvalStatus || ProcessStatusEnum.NONE" />
      </div>
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
    <div class="h-full bg-[var(--text-n9)] p-[16px]">
      <CrmWorkflowCard
        v-model:stage="currentStatus"
        class="mb-[16px]"
        :stageConfig="stageConfig"
        :formKey="FormDesignKeyEnum.CONTRACT"
        is-limit-back
        is-no-resign-flow
        :readonly="!canUpdateStage"
        :back-stage-permission="['CONTRACT:STAGE']"
        :source-id="sourceId"
        :operation-permission="['CONTRACT:STAGE']"
        :update-api="updateContractStage"
        @load-detail="handleSaved()"
      />
      <CrmCard no-content-padding hide-footer auto-height class="mb-[16px]">
        <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line" />
      </CrmCard>

      <CrmCard contentHeight="100%" hide-footer :special-height="170" no-content-padding>
        <!-- 需要用到 detailInfo 所以这里不用 v-if -->
        <div v-show="activeTab === 'contract'" class="h-full">
          <CrmApprovalDetail
            :form-key="FormDesignKeyEnum.CONTRACT"
            :source-id="props.sourceId"
            :refresh-key="approvalDetailRefreshKey"
            :approval-status="detailInfo?.approvalStatus"
            @saveApproval="handleSaveApproval"
          >
            <template #left="{ fieldPermissions }">
              <CrmFormDescription
                ref="formDescriptionRef"
                :form-key="FormDesignKeyEnum.CONTRACT_SNAPSHOT"
                :source-id="props.sourceId"
                :column="2"
                :refresh-key="refreshKey"
                label-width="auto"
                value-align="start"
                tooltip-position="top-start"
                :readonly="!hasAnyPermission(['CONTRACT:UPDATE'])"
                :isContractTableDetail="props.isContractTableDetail"
                :fieldPermissions="fieldPermissions"
                :otherSaveParams="{
                  updateType: 'approval',
                  approvalTaskId: props.approvalTaskId,
                }"
                @openCustomerDetail="emit('showCustomerDrawer', $event)"
                @openOpportunityDetail="openOpportunityDetail"
                @openQuotationDetail="openQuotationDetail"
                @init="handleInit"
              />
            </template>
          </CrmApprovalDetail>
        </div>
        <div v-if="activeTab === 'payment'" class="h-full p-[24px]">
          <PaymentTable
            :form-key="FormDesignKeyEnum.CONTRACT_CONTRACT_PAYMENT"
            :sourceId="props.sourceId"
            :sourceName="title"
            isContractTab
          />
        </div>
        <div v-if="activeTab === 'paymentRecord'" class="h-full p-[24px]">
          <PaymentRecordTable
            :form-key="FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD"
            :sourceId="props.sourceId"
            :sourceName="title"
            isContractTab
            @refresh="handleSaved()"
          />
        </div>
        <div v-if="activeTab === 'invoice'" class="h-full p-[24px]">
          <InvoiceTable
            :sourceId="props.sourceId"
            :sourceName="title"
            is-contract-tab
            @open-business-title-drawer="showBusinessTitleDetail"
          />
        </div>
        <div v-if="activeTab === 'order'" class="h-full p-[24px]">
          <OrderTable
            :formKey="FormDesignKeyEnum.CONTRACT_ORDER"
            :sourceId="props.sourceId"
            :sourceName="title"
            is-contract-tab
            @open-customer-drawer="emit('showCustomerDrawer', $event)"
          />
        </div>
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
      @saved="handleFormCreateSaved"
      @review="handleFormReview"
    />
    <QuotationDetailDrawer
      v-model:visible="showQuotationDetailDrawer"
      :source-id="activeQuotationSourceId"
      @edit="handleEditQuotation"
      @refresh="handleSaved()"
    />
    <OptOverviewDrawer
      v-model:show="showOptOverviewDrawer"
      :detail="activeOpportunity"
      @refresh="handleSaved()"
      @open-customer-drawer="emit('showCustomerDrawer', $event)"
    />
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { NButton, useMessage } from 'naive-ui';

  import { ContractStatusEnum } from '@lib/shared/enums/contractEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { ContractItem } from '@lib/shared/models/contract';
  import { CollaborationType } from '@lib/shared/models/customer';
  import { OpportunityStageConfig } from '@lib/shared/models/opportunity';
  import type { FormConfig, FormViewSize } from '@lib/shared/models/system/module';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmApprovalDetail from '@/components/business/crm-approval/components/crm-approval-detail.vue';
  import CrmApprovalStatus from '@/components/business/crm-approval/components/crm-approval-status.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmWorkflowCard from '@/components/business/crm-workflow-card/index.vue';
  import PaymentTable from '@/views/contract/contractPaymentPlan/components/paymentTable.vue';
  import PaymentRecordTable from '@/views/contract/contractPaymentRecord/components/paymentTable.vue';
  import InvoiceTable from '@/views/contract/invoice/components/invoiceTable.vue';
  import OptOverviewDrawer from '@/views/opportunity/components/optOverviewDrawer.vue';
  import QuotationDetailDrawer from '@/views/opportunity/components/quotation/detail.vue';
  import OrderTable from '@/views/order/order/components/orderTable.vue';

  import { deleteContract, getContractStatusConfig, updateContractStage } from '@/api/modules';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useApprovalResourceAction from '@/hooks/useApprovalResourceAction';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    sourceId: string;
    isContractTableDetail?: boolean;
    approvalTaskId?: string;
  }>();
  const emit = defineEmits<{
    (e: 'refresh'): void;
    (e: 'delete'): void;
    (e: 'showCustomerDrawer', params: { customerId: string; inCustomerPool: boolean; poolId: string }): void;
    (e: 'openBusinessTitleDrawer', params: { id: string }): void;
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
      {
        name: 'paymentRecord',
        tab: t('module.paymentRecord'),
        permission: ['CONTRACT_PAYMENT_RECORD:READ'],
      },
      {
        name: 'invoice',
        tab: t('module.invoice'),
        permission: ['CONTRACT_INVOICE:READ'],
      },
      {
        name: 'order',
        tab: t('module.order'),
        permission: ['ORDER:READ'],
      },
    ].filter((item) => hasAnyPermission(item.permission))
  );

  function createContractDetailActionMap(row: ContractItem) {
    return {
      edit: {
        label: t('common.edit'),
        key: 'edit',
        permission: ['CONTRACT:UPDATE'],
      },
      paymentRecord: {
        label: t('contract.payment'),
        key: 'paymentRecord',
        permission: ['CONTRACT:PAYMENT'],
        disabled: !row.amount || row.alreadyPayAmount >= row.amount,
        tooltipContent: row.alreadyPayAmount >= row.amount ? t('contract.noPaymentRequired') : undefined,
      },
      delete: {
        label: t('common.delete'),
        key: 'delete',
        danger: true,
        permission: ['CONTRACT:DELETE'],
      },
    };
  }

  const { initApprovalPermission, resolveRowOperation, enableApproval, deleteExecute, hasApprovalScopedPermission } =
    useApprovalOperation<ContractItem>({
      formType: FormDesignKeyEnum.CONTRACT,
      dataActionMap: createContractDetailActionMap,
      isDetail: true,
      identityResolver: {
        isApplicant: (row, currentUserId) => row.createUser === currentUserId,
      },
      specialActionFilter: (row, actionKeys) => {
        if (row.stage !== ContractStatusEnum.VOID) {
          return actionKeys;
        }

        return actionKeys.filter((key) => {
          if (key === 'paymentRecord') {
            return false;
          }

          if (!enableApproval.value && key === 'edit') {
            return false;
          }

          return true;
        });
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
  const stageConfig = ref<OpportunityStageConfig>();
  const currentStatus = ref<string>(stageConfig.value?.stageConfigList[0]?.id || '');
  const formViewSize = ref<FormViewSize>('large');
  function handleInit(type?: CollaborationType, name?: string, detail?: Record<string, any>, config?: FormConfig) {
    title.value = name || '';
    detailInfo.value = detail ?? {};
    formViewSize.value = config?.viewSize || 'large';
    if (detail) {
      currentStatus.value = detail.stage;
    }
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
  const approvalDetailRefreshKey = ref(0);
  function handleSaved() {
    refreshKey.value += 1;
    emit('refresh');
  }

  function handleFormCreateSaved(_res: any, isUpdateReview?: boolean) {
    if (isUpdateReview) {
      approvalDetailRefreshKey.value += 1;
    }
    handleSaved();
  }

  const { reviewByFormResult, reviewByResourceId, revokeByResourceId } = useApprovalResourceAction({
    formKey: FormDesignKeyEnum.CONTRACT,
  });

  const canUpdateStage = computed(() => {
    if (!detailInfo.value) {
      return false;
    }

    return hasApprovalScopedPermission(detailInfo.value, ['CONTRACT:STAGE']);
  });

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

  function handleDelete(row: ContractItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('common.deleteConfirmContent'),
      positiveText: deleteExecute.value ? t('crm.approval.confirmAndSubmitReview') : t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteContract(row.id);
          Message.success(deleteExecute.value ? t('common.reviewSuccess') : t('common.deleteSuccess'));
          visible.value = false;
          emit('delete');
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
        handleSaved();
      },
    });
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

  const showQuotationDetailDrawer = ref(false);
  const activeQuotationSourceId = ref('');
  function openQuotationDetail(params: { id: string }) {
    showQuotationDetailDrawer.value = true;
    activeQuotationSourceId.value = params.id;
  }

  function handleEditQuotation(id: string) {
    activeFormKey.value = FormDesignKeyEnum.OPPORTUNITY_QUOTATION;
    activeSourceId.value = id;
    needInitDetail.value = true;
    linkFormInfo.value = undefined;
    formCreateDrawerVisible.value = true;
  }

  const showOptOverviewDrawer = ref<boolean>(false);
  const activeOpportunity = ref();
  function openOpportunityDetail(params: { id: string }) {
    showOptOverviewDrawer.value = true;
    activeOpportunity.value = {
      id: params.id,
    };
  }

  async function handleButtonClick(actionKey: string) {
    switch (actionKey) {
      case 'edit':
        handleEdit();
        break;
      case 'revoke':
        handleRevoke();
        break;
      case 'review':
        handleReview();
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

  async function initStageConfig() {
    try {
      stageConfig.value = await getContractStatusConfig();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        initStageConfig();
      }
    }
  );

  function showBusinessTitleDetail(params: { id: string }) {
    emit('openBusinessTitleDrawer', params);
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
