<template>
  <CrmOverviewDrawer
    v-model:show="show"
    v-model:active-tab="activeTab"
    v-model:cached-list="cachedList"
    :tab-list="tabList"
    :button-list="buttonList"
    title="1"
    subtitle="2"
    :form-key="FormDesignKeyEnum.CUSTOMER"
    :source-id="props.sourceId"
    show-tab-setting
    @button-select="handleButtonSelect"
    @saved="() => (refreshKey += 1)"
  >
    <template #transferPopContent>
      <TransferForm
        ref="transferFormRef"
        v-model:form="transferForm"
        :module-type="ModuleConfigEnum.CUSTOMER_MANAGEMENT"
      />
    </template>
    <template #left>
      <div class="p-[16px_24px]">
        <CrmFormDescription :form-key="FormDesignKeyEnum.CUSTOMER" :source-id="props.sourceId" />
      </div>
    </template>
    <template #right>
      <div class="mt-[16px]">
        <div v-if="activeTab === 'overview'" class="mt-[16px] h-[100px] bg-[var(--text-n10)]"></div>
        <ContactTable v-else-if="activeTab === 'contact'" class="h-[calc(100vh-161px)]" is-overview />
        <FollowDetail
          v-else-if="['followRecord', 'followPlan'].includes(activeTab)"
          class="mt-[16px]"
          :active-type="(activeTab as 'followRecord'| 'followPlan')"
          wrapper-class="h-[calc(100vh-162px)]"
          virtual-scroll-height="calc(100vh - 254px)"
          :follow-api-key="FormDesignKeyEnum.CUSTOMER"
          :source-id="props.sourceId"
          :refresh-key="refreshKey"
        />
        <CrmHeaderTable
          v-else-if="activeTab === 'headRecord'"
          class="h-[calc(100vh-161px)]"
          :source-id="props.sourceId"
          :load-list-api="getCustomerHeaderList"
        />
        <customerRelation v-else-if="activeTab === 'relation'" :source-id="props.sourceId" />
        <collaborator v-else-if="activeTab === 'collaborator'" :source-id="props.sourceId" />
      </div>
    </template>
  </CrmOverviewDrawer>
</template>

<script setup lang="ts">
  import { useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';

  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import FollowDetail from '@/components/business/crm-follow-detail/index.vue';
  import ContactTable from '@/components/business/crm-form-create-table/contactTable.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmHeaderTable from '@/components/business/crm-header-table/index.vue';
  import CrmOverviewDrawer from '@/components/business/crm-overview-drawer/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import collaborator from './collaborator.vue';
  import customerRelation from './customerRelation.vue';

  import { deleteCustomer, getCustomerHeaderList, updateCustomer } from '@/api/modules/customer/index';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';

  const props = defineProps<{
    sourceId: string;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const show = defineModel<boolean>('show', {
    required: true,
  });
  const refreshKey = ref(0);
  const transferLoading = ref(false);
  const buttonList: ActionsItem[] = [
    {
      label: t('common.edit'),
      key: 'edit',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
      permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
    },
    {
      label: t('overviewDrawer.addContract'),
      key: 'addContract',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
      permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
    },
    {
      label: t('overviewDrawer.followRecord'),
      key: 'followRecord',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
      permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
    },
    {
      label: t('overviewDrawer.followPlan'),
      key: 'followPlan',
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
      label: t('common.delete'),
      key: 'delete',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
      permission: ['CUSTOMER_MANAGEMENT:DELETE'],
    },
  ];

  const activeTab = ref('contact');
  const cachedList = ref([]);
  const tabList: TabContentItem[] = [
    {
      name: 'contact',
      tab: t('opportunity.contactInfo'),
      enable: true,
      allowClose: true,
    },
    {
      name: 'followRecord',
      tab: t('crmFollowRecord.followRecord'),
      enable: true,
      allowClose: true,
    },
    {
      name: 'followPlan',
      tab: t('common.followPlan'),
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
      name: 'collaborator',
      tab: t('customer.collaborator'),
      enable: true,
      allowClose: true,
    },
  ];

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
          show.value = false;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleButtonSelect(key: string) {
    if (key === 'delete') {
      handleDelete();
    } else if (key === 'transfer') {
      transfer();
    }
  }
</script>

<style lang="less" scoped></style>
