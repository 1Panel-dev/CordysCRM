<template>
  <CrmOverviewDrawer
    ref="crmOverviewDrawerRef"
    v-model:show="show"
    v-model:active-tab="activeTab"
    :tab-list="tabList"
    :button-list="buttonList"
    :title="sourceName"
    :form-key="FormDesignKeyEnum.CUSTOMER"
    :source-id="props.sourceId"
    show-tab-setting
    @button-select="handleButtonSelect"
    @saved="handleSaved"
  >
    <template #transferPopContent>
      <TransferForm
        ref="transferFormRef"
        v-model:form="transferForm"
        :module-type="ModuleConfigEnum.CUSTOMER_MANAGEMENT"
      />
    </template>
    <template #left>
      <div class="h-full overflow-hidden">
        <CrmFormDescription
          ref="descriptionRef"
          :form-key="FormDesignKeyEnum.CUSTOMER"
          :source-id="props.sourceId"
          :refresh-key="refreshKey"
          class="p-[16px_24px]"
          :column="layout === 'vertical' ? 3 : undefined"
          :label-width="layout === 'vertical' ? 'auto' : undefined"
          :value-align="layout === 'vertical' ? 'start' : undefined"
          @init="handleDescriptionInit"
        />
      </div>
    </template>
    <template #right>
      <div class="h-full pt-[16px]">
        <ContactTable
          v-if="activeTab === 'contact'"
          :refresh-key="refreshKey"
          :source-id="props.sourceId"
          :initial-source-name="sourceName"
          :readonly="collaborationType === 'READ_ONLY' || props.readonly"
          :form-key="FormDesignKeyEnum.CUSTOMER_CONTACT"
        />
        <FollowDetail
          v-else-if="['followRecord', 'followPlan'].includes(activeTab) && show"
          :active-type="(activeTab as 'followRecord'| 'followPlan')"
          wrapper-class="h-[calc(100vh-162px)]"
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
        <CrmHeaderTable
          v-else-if="activeTab === 'headRecord'"
          :form-key="FormDesignKeyEnum.CUSTOMER_OPEN_SEA"
          :source-id="props.sourceId"
          :load-list-api="getCustomerHeaderList"
        />
        <customerRelation
          v-else-if="activeTab === 'relation'"
          :source-id="props.sourceId"
          :readonly="collaborationType === 'READ_ONLY' || props.readonly"
        />
        <CrmCard v-else-if="activeTab === 'opportunityInfo'" no-content-bottom-padding hide-footer>
          <opportunityTable
            :source-id="props.sourceId"
            :customer-name="sourceName"
            is-customer-tab
            :form-key="FormDesignKeyEnum.CUSTOMER_OPPORTUNITY"
            :readonly="collaborationType === 'READ_ONLY' || props.readonly"
          />
        </CrmCard>
        <collaborator
          v-else-if="activeTab === 'collaborator'"
          :source-id="props.sourceId"
          :readonly="collaborationType === 'READ_ONLY' || props.readonly"
        />
        <ContractTimeline
          v-else-if="activeTab === 'contract'"
          :form-key="FormDesignKeyEnum.CONTRACT"
          :source-id="props.sourceId"
        />
        <ContractTimeline
          v-else-if="activeTab === 'contractPayment'"
          :form-key="FormDesignKeyEnum.CONTRACT_PAYMENT"
          :source-id="props.sourceId"
        />
        <ContractTimeline
          v-else-if="activeTab === 'contractPaymentRecord'"
          :form-key="FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD"
          :source-id="props.sourceId"
        />
      </div>
      <CrmMoveModal
        v-model:show="showMoveModal"
        :reason-key="ReasonTypeEnum.CUSTOMER_POOL_RS"
        :source-id="props.sourceId"
        :name="sourceName"
        type="warning"
        @refresh="refresh"
      />
    </template>
  </CrmOverviewDrawer>
</template>

<script setup lang="ts">
  import { useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ModuleConfigEnum, ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { CollaborationType } from '@lib/shared/models/customer';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import ContactTable from '@/components/business/crm-form-create-table/contactTable.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmHeaderTable from '@/components/business/crm-header-table/index.vue';
  import CrmMoveModal from '@/components/business/crm-move-modal/index.vue';
  import CrmOverviewDrawer from '@/components/business/crm-overview-drawer/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import collaborator from './collaborator.vue';
  import customerRelation from './customerRelation.vue';
  import ContractTimeline from '@/views/contract/contract/components/contractTimeline.vue';
  import opportunityTable from '@/views/opportunity/components/opportunityTable.vue';

  import { deleteCustomer, getCustomerHeaderList, updateCustomer } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const FollowDetail = defineAsyncComponent(() => import('@/components/business/crm-follow-detail/index.vue'));

  const props = defineProps<{
    sourceId: string;
    readonly?: boolean;
  }>();
  const emit = defineEmits<{
    (e: 'saved'): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  const crmOverviewDrawerRef = ref<InstanceType<typeof CrmOverviewDrawer>>();
  const layout = computed(() => crmOverviewDrawerRef.value?.layout);

  const refreshKey = ref(0);
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

  const activeTab = ref('contact');
  const tabList = computed<TabContentItem[]>(() => {
    const fullList = [
      {
        name: 'followRecord',
        tab: t('crmFollowRecord.followRecord'),
        enable: true,
      },
      {
        name: 'contact',
        tab: t('opportunity.contactInfo'),
        enable: true,
      },
      {
        name: 'followPlan',
        tab: t('common.plan'),
        enable: true,
      },
      {
        name: 'headRecord',
        tab: t('common.headRecord'),
        enable: true,
      },
      {
        name: 'relation',
        tab: t('customer.relation'),
        enable: true,
      },
      {
        name: 'opportunityInfo',
        tab: t('customer.opportunityInfo'),
        enable: true,
        permission: ['OPPORTUNITY_MANAGEMENT:READ'],
      },
      {
        name: 'collaborator',
        tab: t('customer.collaborator'),
        enable: true,
      },
      {
        name: 'contract',
        tab: t('module.contract'),
        enable: true,
        permission: ['CONTRACT:READ'],
      },
      {
        name: 'contractPayment',
        tab: t('module.paymentPlan'),
        enable: true,
        permission: ['CONTRACT_PAYMENT_PLAN:READ'],
      },
      {
        name: 'contractPaymentRecord',
        tab: t('module.paymentRecord'),
        enable: true,
        permission: ['CONTRACT_PAYMENT_RECORD:READ'],
      },
    ];
    if (collaborationType.value) {
      return fullList.filter((item) => item.name !== 'collaborator');
    }
    return fullList;
  });

  const transferFormRef = ref<InstanceType<typeof TransferForm>>();
  const transferForm = ref<any>({
    owner: null,
    belongToPublicPool: null,
  });

  // 转移
  async function transfer() {
    try {
      transferLoading.value = true;
      await updateCustomer({
        id: props.sourceId,
        owner: transferForm.value.owner,
      });
      Message.success(t('common.transferSuccess'));
      descriptionRef.value?.initFormDescription();
      emit('saved');
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
          emit('saved');
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
    if (key === 'delete') {
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

  function refresh() {
    emit('saved');
    show.value = false;
  }

  function handleDescriptionInit(_collaborationType?: CollaborationType, _sourceName?: string) {
    collaborationType.value = _collaborationType;
    sourceName.value = _sourceName || '';
  }
</script>

<style lang="less" scoped></style>
