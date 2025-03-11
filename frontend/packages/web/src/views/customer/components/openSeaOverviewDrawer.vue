<template>
  <CrmOverviewDrawer
    v-model:show="show"
    v-model:active-tab="activeTab"
    v-model:cached-list="tabList"
    :tab-list="tabList"
    :button-list="buttonList"
    title="1"
    subtitle="2"
    :form-key="FormDesignKeyEnum.CUSTOMER"
    :source-id="props.sourceId"
    @button-select="handleButtonSelect"
  >
    <template #distributePopContent>
      <TransferForm ref="distributeFormRef" v-model:form="distributeForm" />
    </template>
    <template #left>
      <CrmFormDescription :form-key="FormDesignKeyEnum.CUSTOMER" :source-id="props.sourceId" />
    </template>
    <template #right>
      <div v-if="activeTab === 'overview'" class="mt-[16px] h-[100px] bg-[var(--text-n10)]"></div>
    </template>
  </CrmOverviewDrawer>
</template>

<script setup lang="ts">
  import { useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmOverviewDrawer from '@/components/business/crm-overview-drawer/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';

  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';

  const props = defineProps<{
    sourceId: string;
  }>();

  const { t } = useI18n();
  const { openModal } = useModal();
  const Message = useMessage();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  const claimLoading = ref(false);
  const distributeLoading = ref(false);
  const buttonList: ActionsItem[] = [
    {
      label: t('common.claim'),
      key: 'claim',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
      popConfirmProps: {
        loading: claimLoading.value,
        title: t('customer.claimTip'),
        content: t('customer.claimTipContent'),
        positiveText: t('common.claim'),
        iconType: 'primary',
      },
    },
    {
      label: t('common.distribute'),
      key: 'distribute',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
      popConfirmProps: {
        loading: distributeLoading.value,
        title: t('common.distribute'),
        positiveText: t('common.confirm'),
        iconType: 'primary',
      },
      popSlotContent: 'distributePopContent',
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
  const tabList: TabContentItem[] = [
    {
      name: 'overview',
      tab: t('common.overview'),
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
  ];

  const distributeFormRef = ref<InstanceType<typeof TransferForm>>();
  const distributeForm = ref<any>({
    head: null,
  });

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
          // TODO: 联调
          Message.success(t('common.deleteSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleButtonSelect(key: string) {
    switch (key) {
      case 'delete':
        handleDelete();
        break;
      default:
        break;
    }
  }
</script>
