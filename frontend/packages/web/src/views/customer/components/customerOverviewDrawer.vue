<template>
  <CrmDrawer v-model:show="show" resizable no-padding :footer="false" :title="sourceName" :view-size="formViewSize">
    <template #titleRight>
      <CrmOperationButton
        :group-list="buttonList"
        class="gap-[12px]"
        :not-show-divider="true"
        @pop-update="handleTransferPopUpdate"
        @select="handleButtonSelect"
      >
        <template #transferPopContent>
          <TransferForm
            ref="transferFormRef"
            v-model:form="transferForm"
            :module-type="ModuleConfigEnum.CUSTOMER_MANAGEMENT"
          />
        </template>
      </CrmOperationButton>
    </template>
    <div class="h-full bg-[var(--text-n9)] p-[16px]">
      <CrmCard v-if="showDetailTabs" no-content-padding hide-footer auto-height class="mb-[16px]">
        <CrmTab v-model:active-tab="activeTab" no-content :tab-list="displayTabList" type="line">
          <template #suffix>
            <CrmTabSetting
              v-if="showDetailTabs"
              :tab-list="enabledDetailTabList"
              :setting-key="`${FormDesignKeyEnum.CUSTOMER}-settingKey`"
              @init="initTabList"
            />
          </template>
        </CrmTab>
      </CrmCard>
      <CrmCard contentHeight="100%" hide-footer :special-height="showDetailTabs ? 64 : 0" no-content-padding>
        <div v-show="activeTab === 'customer'" class="h-full overflow-hidden">
          <CrmFormDescription
            ref="descriptionRef"
            :form-key="FormDesignKeyEnum.CUSTOMER"
            :source-id="props.sourceId"
            :refresh-key="refreshKey"
            class="p-[24px]"
            :column="2"
            label-width="auto"
            value-align="start"
            tooltip-position="top-start"
            :readonly="!hasAnyPermission(['CUSTOMER_MANAGEMENT:UPDATE'])"
            @init="handleDescriptionInit"
            @refresh="emit('refresh')"
          />
        </div>
        <div v-if="activeTab === 'contact'" class="h-full px-[24px] pt-[24px]">
          <ContactTable
            :refresh-key="refreshKey"
            :source-id="props.sourceId"
            :initial-source-name="sourceName"
            :readonly="collaborationType === 'READ_ONLY' || props.readonly"
            :form-key="FormDesignKeyEnum.CUSTOMER_CONTACT"
          />
        </div>
        <FollowDetail
          v-if="['followRecord', 'followPlan'].includes(activeTab) && show"
          :active-type="(activeTab as 'followRecord' | 'followPlan')"
          wrapper-class="h-full"
          virtual-scroll-height="calc(100vh - 254px)"
          :follow-api-key="FormDesignKeyEnum.CUSTOMER"
          :source-id="props.sourceId"
          :refresh-key="refreshKey"
          :initial-source-name="sourceName"
          :show-add="
            collaborationType !== 'READ_ONLY' && hasAnyPermission(['CUSTOMER_MANAGEMENT:UPDATE']) && !props.readonly
          "
          :show-action="
            collaborationType !== 'READ_ONLY' && hasAnyPermission(['CUSTOMER_MANAGEMENT:UPDATE']) && !props.readonly
          "
          :parentFormKey="FormDesignKeyEnum.CUSTOMER"
        />
        <div v-if="activeTab === 'headRecord'" class="h-full px-[24px] pt-[24px]">
          <CrmHeaderTable
            :form-key="FormDesignKeyEnum.CUSTOMER_OPEN_SEA"
            :source-id="props.sourceId"
            :load-list-api="getCustomerHeaderList"
          />
        </div>
        <div v-if="activeTab === 'relation'" class="h-full px-[24px] pt-[24px]">
          <customerRelation
            :source-id="props.sourceId"
            :readonly="collaborationType === 'READ_ONLY' || props.readonly"
          />
        </div>
        <div v-if="activeTab === 'opportunityInfo'" class="h-full px-[24px] pt-[24px]">
          <opportunityTable
            :source-id="props.sourceId"
            :customer-name="sourceName"
            is-customer-tab
            :form-key="FormDesignKeyEnum.CUSTOMER_OPPORTUNITY"
            :readonly="collaborationType === 'READ_ONLY' || props.readonly"
          />
        </div>
        <div v-if="activeTab === 'collaborator'" class="h-full px-[24px] pt-[24px]">
          <collaborator :source-id="props.sourceId" :readonly="collaborationType === 'READ_ONLY' || props.readonly" />
        </div>
        <div v-if="activeTab === 'contract'" class="h-full p-[24px]">
          <ContractTimeline :form-key="FormDesignKeyEnum.CONTRACT" :source-id="props.sourceId" />
        </div>
        <div v-if="activeTab === 'contractPayment'" class="h-full p-[24px]">
          <ContractTimeline :form-key="FormDesignKeyEnum.CONTRACT_PAYMENT" :source-id="props.sourceId" />
        </div>
        <div v-if="activeTab === 'contractPaymentRecord'" class="h-full p-[24px]">
          <ContractTimeline :form-key="FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD" :source-id="props.sourceId" />
        </div>
        <div v-if="activeTab === 'invoice'" class="h-full p-[24px]">
          <ContractTimeline :form-key="FormDesignKeyEnum.INVOICE" :source-id="props.sourceId" />
        </div>
        <div v-if="activeTab === 'order'" class="h-full px-[24px] pt-[24px]">
          <OrderTable
            :formKey="FormDesignKeyEnum.CUSTOMER_ORDER"
            :sourceId="props.sourceId"
            isCustomerTab
            :readonly="collaborationType === 'READ_ONLY' || props.readonly"
            @open-contract-drawer="handleOpenContractDrawer"
          />
        </div>
        <template v-for="item in customDetailTabTableList" :key="String(item.tab.name)">
          <div v-if="activeTab === item.tab.name" class="h-full px-[24px] pt-[24px]">
            <component :is="item.table.component" v-bind="item.table.props" />
          </div>
        </template>
      </CrmCard>
      <CrmMoveModal
        v-model:show="showMoveModal"
        :reason-key="ReasonTypeEnum.CUSTOMER_POOL_RS"
        :source-id="props.sourceId"
        :name="sourceName"
        type="warning"
        @refresh="handleRemoveSuccess"
      />
      <ContractDetailDrawer
        v-model:visible="showContractDetailDrawer"
        :sourceId="activeSourceId"
        @showCustomerDrawer="handleOpenCustomerDrawer"
      />
    </div>
    <CrmFormCreateDrawer
      v-model:visible="formCreateDrawerVisible"
      :form-key="FormDesignKeyEnum.CUSTOMER"
      :source-id="props.sourceId"
      need-init-detail
      @saved="handleSaved"
    />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ModuleConfigEnum, ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { CollaborationType } from '@lib/shared/models/customer';
  import type { FormConfig, FormViewSize } from '@lib/shared/models/system/module';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import ContactTable from '@/components/business/crm-form-create-table/contactTable.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmHeaderTable from '@/components/business/crm-header-table/index.vue';
  import CrmMoveModal from '@/components/business/crm-move-modal/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmTabSetting from '@/components/business/crm-tab-setting/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import collaborator from './collaborator.vue';
  import customerRelation from './customerRelation.vue';
  import ContractTimeline from '@/views/contract/contract/components/contractTimeline.vue';
  import ContractDetailDrawer from '@/views/contract/contract/components/detail.vue';
  import opportunityTable from '@/views/opportunity/components/opportunityTable.vue';
  import OrderTable from '@/views/order/order/components/orderTable.vue';

  import { deleteCustomer, getCustomerHeaderList, updateCustomer } from '@/api/modules';
  import useFormDetailTabAvailability from '@/hooks/useFormDetailTabAvailability';
  import useFormDetailTabs from '@/hooks/useFormDetailTabs';
  import useFormDetailTabTable from '@/hooks/useFormDetailTabTable';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const FollowDetail = defineAsyncComponent(() => import('@/components/business/crm-follow-detail/index.vue'));

  const props = defineProps<{
    sourceId: string;
    readonly?: boolean;
  }>();
  const emit = defineEmits<{
    (e: 'saved'): void;
    (e: 'deleted'): void;
    (e: 'transfer'): void;
    (e: 'refresh'): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  const refreshKey = ref(0);
  const formConfig = ref<FormConfig>();
  const transferLoading = ref(false);
  const collaborationType = ref<CollaborationType>();
  const sourceName = ref('');
  const descriptionRef = ref<InstanceType<typeof CrmFormDescription>>();
  const buttonList = computed<ActionsItem[]>(() => {
    if (collaborationType.value || props.readonly) {
      return [];
    }
    return [
      {
        label: t('common.edit'),
        key: 'edit',
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
        permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
      },
      {
        label: t('common.transfer'),
        key: 'transfer',
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
        permission: ['CUSTOMER_MANAGEMENT:TRANSFER'],
        popConfirmProps: {
          loading: transferLoading.value,
          title: t('common.transfer'),
          positiveText: t('common.confirm'),
          iconType: 'primary',
        },
        popSlotContent: 'transferPopContent',
      },
      {
        label: t('customer.moveToOpenSea'),
        key: 'moveToOpenSea',
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
        permission: ['CUSTOMER_MANAGEMENT:RECYCLE'],
      },
      {
        label: t('common.delete'),
        key: 'delete',
        text: false,
        ghost: true,
        danger: true,
        class: 'n-btn-outline-primary',
        permission: ['CUSTOMER_MANAGEMENT:DELETE'],
      },
    ];
  });

  const activeTab = ref('customer');
  const staticTabList = computed<TabContentItem[]>(() => {
    const fullList = [
      {
        name: 'followRecord',
        tab: t('crmFollowRecord.followRecord'),
        enable: true,
        internalKey: 'CUSTOMER_FOLLOW_RECORD',
      },
      {
        name: 'contact',
        tab: t('opportunity.contactInfo'),
        enable: true,
        permission: ['CUSTOMER_MANAGEMENT_CONTACT:READ'],
        internalKey: 'CUSTOMER_CONTACT',
      },
      {
        name: 'followPlan',
        tab: t('common.plan'),
        enable: true,
        internalKey: 'CUSTOMER_FOLLOW_PLAN',
      },
      {
        name: 'headRecord',
        tab: t('common.headRecord'),
        enable: true,
        internalKey: 'CUSTOMER_OWNER_RECORD',
      },
      {
        name: 'relation',
        tab: t('customer.relation'),
        enable: true,
        internalKey: 'CUSTOMER_RELATION',
      },
      {
        name: 'opportunityInfo',
        tab: t('customer.opportunityInfo'),
        enable: true,
        permission: ['OPPORTUNITY_MANAGEMENT:READ'],
        internalKey: 'CUSTOMER_OPPORTUNITY',
      },
      {
        name: 'collaborator',
        tab: t('customer.collaborator'),
        enable: true,
        internalKey: 'CUSTOMER_COLLABORATION',
      },
      {
        name: 'contract',
        tab: t('module.contract'),
        enable: true,
        permission: ['CONTRACT:READ'],
        internalKey: 'CUSTOMER_CONTRACT',
      },
      {
        name: 'contractPayment',
        tab: t('module.paymentPlan'),
        enable: true,
        permission: ['CONTRACT_PAYMENT_PLAN:READ'],
        internalKey: 'CUSTOMER_PAYMENT_PLAN',
      },
      {
        name: 'contractPaymentRecord',
        tab: t('module.paymentRecord'),
        enable: true,
        permission: ['CONTRACT_PAYMENT_RECORD:READ'],
        internalKey: 'CUSTOMER_PAYMENT_RECORD',
      },
      {
        name: 'invoice',
        tab: t('module.invoice'),
        enable: true,
        permission: ['CONTRACT_INVOICE:READ'],
        internalKey: 'CUSTOMER_INVOICE',
      },
      {
        name: 'order',
        tab: t('module.order'),
        enable: true,
        permission: ['ORDER:READ'],
        internalKey: 'CUSTOMER_ORDER',
      },
    ];
    if (collaborationType.value) {
      return fullList.filter((item) => item.name !== 'collaborator');
    }
    return fullList;
  });
  const { availableDetailTabIds } = useFormDetailTabAvailability(formConfig, FormDesignKeyEnum.CUSTOMER);
  const { customDetailTabList, enabledDetailTabList } = useFormDetailTabs(
    formConfig,
    staticTabList,
    availableDetailTabIds
  );
  const showDetailTabs = computed(() => enabledDetailTabList.value.length > 0);
  const { getDetailTabTable } = useFormDetailTabTable();
  const customDetailTabTableList = computed(() =>
    customDetailTabList.value.flatMap((tab) => {
      const table = getDetailTabTable(tab.detailTab, props.sourceId, FormDesignKeyEnum.CUSTOMER);
      return table ? [{ tab, table }] : [];
    })
  );
  const settingTabList = ref<TabContentItem[]>([]);
  const displayTabList = computed<TabContentItem[]>(() => [
    {
      name: 'customer',
      tab: t('crmFormDesign.customer'),
      enable: true,
    },
    ...settingTabList.value,
  ]);

  function initTabList(list: TabContentItem[]) {
    settingTabList.value = list;
  }

  watch(
    () => displayTabList.value,
    (list) => {
      if (!list.some((item) => item.name === activeTab.value)) {
        activeTab.value = list[0]?.name as string;
      }
    }
  );

  const transferFormRef = ref<InstanceType<typeof TransferForm>>();
  const formCreateDrawerVisible = ref(false);
  const transferForm = ref<any>({
    owner: null,
    belongToPublicPool: null,
  });

  function resetTransferForm() {
    transferForm.value = {
      owner: null,
      belongToPublicPool: null,
    };
  }

  function handleTransferPopUpdate(key: string, visible: boolean) {
    if (key === 'transfer' && visible) {
      resetTransferForm();
    }
  }

  // 转移
  async function transfer() {
    try {
      transferLoading.value = true;
      await updateCustomer({
        id: props.sourceId,
        owner: transferForm.value.owner,
      });
      Message.success(t('common.transferSuccess'));
      resetTransferForm();
      descriptionRef.value?.initFormDescription();
      emit('transfer');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      transferLoading.value = false;
    }
  }

  // 删除
  function handleDelete() {
    openModal({
      type: 'error',
      title: t('customer.deleteTitleTip'),
      content: t('customer.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteCustomer(props.sourceId);
          Message.success(t('common.deleteSuccess'));
          emit('deleted');
          show.value = false;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 移入公海
  const showMoveModal = ref(false);
  function handleMoveToPublicPool() {
    showMoveModal.value = true;
  }

  function handleButtonSelect(key: string) {
    if (key === 'edit') {
      formCreateDrawerVisible.value = true;
    } else if (key === 'delete') {
      handleDelete();
    } else if (key === 'pop-transfer') {
      transfer();
    } else if (key === 'moveToOpenSea') {
      handleMoveToPublicPool();
    }
  }

  function handleSaved() {
    refreshKey.value += 1;
    emit('saved');
  }

  const formViewSize = ref<FormViewSize>('large');
  function handleDescriptionInit(
    _collaborationType?: CollaborationType,
    _sourceName?: string,
    detail?: Record<string, any>,
    config?: FormConfig
  ) {
    collaborationType.value = _collaborationType;
    sourceName.value = _sourceName || '';
    formConfig.value = config;
    formViewSize.value = config?.viewSize || 'large';
  }

  const showContractDetailDrawer = ref(false);
  const activeSourceId = ref<string>('');
  function handleOpenContractDrawer(params: { id: string }) {
    activeSourceId.value = params.id;
    showContractDetailDrawer.value = true;
  }

  function handleRemoveSuccess() {
    show.value = false;
    emit('deleted');
  }

  function handleOpenCustomerDrawer() {
    showContractDetailDrawer.value = false;
  }
</script>

<style lang="less" scoped></style>
