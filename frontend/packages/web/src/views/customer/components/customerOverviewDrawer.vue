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
  >
    <template #transferPopContent>
      <TransferForm
        ref="transferFormRef"
        v-model:form="transferForm"
        :module-type="ModuleConfigEnum.CUSTOMER_MANAGEMENT"
      />
    </template>
    <template #left>
      <CrmFormDescription :form-key="FormDesignKeyEnum.CUSTOMER" :source-id="props.sourceId" />
    </template>
    <template #right>
      <div class="mt-[16px]">
        <div v-if="activeTab === 'overview'" class="mt-[16px] h-[100px] bg-[var(--text-n10)]"></div>
        <ContactTable v-else-if="activeTab === 'contact'" class="h-[calc(100vh-161px)]" is-overview />
        <HeaderTable
          v-else-if="activeTab === 'headRecord'"
          class="h-[calc(100vh-161px)]"
          :source-id="props.sourceId"
          :load-list-api="async()=>({} as any)"
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
  import ContactTable from '@/components/business/crm-form-create-table/contactTable.vue';
  import HeaderTable from '@/components/business/crm-form-create-table/headerTable.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmOverviewDrawer from '@/components/business/crm-overview-drawer/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import collaborator from './collaborator.vue';
  import customerRelation from './customerRelation.vue';

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

  const transferLoading = ref(false);
  const buttonList: ActionsItem[] = [
    {
      label: t('common.edit'),
      key: 'edit',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
    {
      label: t('overviewDrawer.addContract'),
      key: 'addContract',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
    {
      label: t('overviewDrawer.followRecord'),
      key: 'followRecord',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
    {
      label: t('overviewDrawer.followPlan'),
      key: 'followPlan',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
    {
      label: t('common.transfer'),
      key: 'transfer',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
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
    },
  ];

  const activeTab = ref('overview');
  const cachedList = ref([]);
  const tabList: TabContentItem[] = [
    {
      name: 'overview',
      tab: t('common.overview'),
      enable: true,
      allowClose: false,
    },
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
    head: null,
    belongToPublicPool: null,
  });

  // 转移
  async function transfer() {
    try {
      transferLoading.value = true;
      // TODO:
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
          // TODO:
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
