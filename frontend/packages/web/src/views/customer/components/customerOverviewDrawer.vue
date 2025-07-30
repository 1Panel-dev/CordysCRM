<template>
  <CrmOverviewDrawer
    v-model:show="show"
    v-model:active-tab="activeTab"
    v-model:cached-list="cachedList"
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
          v-else-if="['followRecord', 'followPlan'].includes(activeTab)"
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
        />
        <CrmHeaderTable
          v-else-if="activeTab === 'headRecord'"
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
            is-customer-tab
            :readonly="collaborationType === 'READ_ONLY' || props.readonly"
          />
        </CrmCard>
        <collaborator
          v-else-if="activeTab === 'collaborator'"
          :source-id="props.sourceId"
          :readonly="collaborationType === 'READ_ONLY' || props.readonly"
        />
      </div>
      <CrmMoveModal
        v-model:show="showMoveModal"
        :reason-key="ReasonTypeEnum.CUSTOMER_POOL_RS"
        :source-id="props.sourceId"
        :name="sourceName"
        type="warning"
        @refresh="() => handleSaved()"
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
  import FollowDetail from '@/components/business/crm-follow-detail/index.vue';
  import ContactTable from '@/components/business/crm-form-create-table/contactTable.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmHeaderTable from '@/components/business/crm-header-table/index.vue';
  import CrmMoveModal from '@/components/business/crm-move-modal/index.vue';
  import CrmOverviewDrawer from '@/components/business/crm-overview-drawer/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import collaborator from './collaborator.vue';
  import customerRelation from './customerRelation.vue';
  import opportunityTable from '@/views/opportunity/components/opportunityTable.vue';

  import { deleteCustomer, getCustomerHeaderList, updateCustomer } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

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
        permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
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
  const cachedList = ref([]);
  const tabList = computed<TabContentItem[]>(() => {
    const fullList = [
      {
        name: 'contact',
        tab: t('opportunity.contactInfo'),
        enable: true,
        allowClose: false,
      },
      {
        name: 'followRecord',
        tab: t('crmFollowRecord.followRecord'),
        enable: true,
        allowClose: true,
      },
      {
        name: 'followPlan',
        tab: t('common.plan'),
        enable: true,
        allowClose: true,
      },
      {
        name: 'headRecord',
        tab: t('common.headRecord'),
        enable: true,
        allowClose: true,
      },
      {
        name: 'relation',
        tab: t('customer.relation'),
        enable: true,
        allowClose: true,
      },
      {
        name: 'opportunityInfo',
        tab: t('customer.opportunityInfo'),
        enable: true,
        allowClose: true,
        permission: ['OPPORTUNITY_MANAGEMENT:READ'],
      },
      {
        name: 'collaborator',
        tab: t('customer.collaborator'),
        enable: true,
        allowClose: true,
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

  function handleDescriptionInit(_collaborationType?: CollaborationType, _sourceName?: string) {
    collaborationType.value = _collaborationType;
    sourceName.value = _sourceName || '';
  }
</script>

<style lang="less" scoped></style>
