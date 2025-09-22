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
    @button-select="handleButtonSelect"
    @saved="() => (refreshKey += 1)"
  >
    <template #distributePopContent>
      <TransferForm ref="distributeFormRef" v-model:form="distributeForm" />
    </template>
    <template #left>
      <div class="h-full overflow-hidden">
        <CrmFormDescription
          :form-key="FormDesignKeyEnum.CUSTOMER_OPEN_SEA"
          :source-id="props.sourceId"
          :hidden-fields="hiddenColumns"
          class="p-[16px_24px]"
          @init="handleDescriptionInit"
        />
      </div>
    </template>
    <template #right>
      <div class="h-full pt-[16px]">
        <CrmHeaderTable
          v-if="activeTab === 'headRecord'"
          :source-id="props.sourceId"
          :load-list-api="getCustomerHeaderList"
        />
        <FollowDetail
          v-else-if="['followRecord'].includes(activeTab)"
          :active-type="(activeTab as 'followRecord')"
          wrapper-class="h-[calc(100vh-162px)]"
          virtual-scroll-height="calc(100vh - 258px)"
          :follow-api-key="FormDesignKeyEnum.CUSTOMER_OPEN_SEA"
          :source-id="sourceId"
          :show-action="false"
          :refresh-key="refreshKey"
        />
      </div>
    </template>
  </CrmOverviewDrawer>
</template>

<script setup lang="ts">
  import { useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { CollaborationType } from '@lib/shared/models/customer';

  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import FollowDetail from '@/components/business/crm-follow-detail/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmHeaderTable from '@/components/business/crm-header-table/index.vue';
  import CrmOverviewDrawer from '@/components/business/crm-overview-drawer/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';

  import {
    assignOpenSeaCustomer,
    deleteOpenSeaCustomer,
    getCustomerHeaderList,
    pickOpenSeaCustomer,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import useOpenNewPage from '@/hooks/useOpenNewPage';

  import { CustomerRouteEnum } from '@/enums/routeEnum';

  const refreshKey = ref(0);

  const props = defineProps<{
    sourceId: string;
    poolId: string | number;
    readonly?: boolean;
    hiddenColumns: string[];
  }>();
  const emit = defineEmits<{
    (e: 'change'): void;
  }>();

  const { t } = useI18n();
  const { openModal } = useModal();
  const Message = useMessage();
  const { openNewPage } = useOpenNewPage();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  const claimLoading = ref(false);
  const distributeLoading = ref(false);
  const buttonList = computed<ActionsItem[]>(() => {
    if (props.readonly) {
      return [];
    }
    return [
      {
        label: t('common.claim'),
        key: 'claim',
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
        permission: ['CUSTOMER_MANAGEMENT_POOL:PICK'],
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
        permission: ['CUSTOMER_MANAGEMENT_POOL:ASSIGN'],
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
        danger: true,
        class: 'n-btn-outline-primary',
        permission: ['CUSTOMER_MANAGEMENT_POOL:DELETE'],
      },
    ];
  });

  const activeTab = ref('followRecord');
  const cachedList = ref([]);
  const tabList: TabContentItem[] = [
    {
      name: 'followRecord',
      tab: t('crmFollowRecord.followRecord'),
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
          await deleteOpenSeaCustomer(props.sourceId);
          Message.success(t('common.deleteSuccess'));
          emit('change');
          show.value = false;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  async function handleClaim(id: string) {
    try {
      claimLoading.value = true;
      await pickOpenSeaCustomer({
        customerId: id,
        poolId: props.poolId,
      });
      Message.success(t('common.claimSuccess'));
      emit('change');
      show.value = false;
      openNewPage(CustomerRouteEnum.CUSTOMER_INDEX, {
        id,
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      claimLoading.value = false;
    }
  }

  async function handleDistribute(id: string) {
    try {
      distributeLoading.value = true;
      await assignOpenSeaCustomer({
        customerId: id,
        assignUserId: distributeForm.value.owner,
      });
      Message.success(t('common.distributeSuccess'));
      emit('change');
      show.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      distributeLoading.value = false;
    }
  }

  function handleButtonSelect(key: string) {
    switch (key) {
      case 'delete':
        handleDelete();
        break;
      case 'pop-claim':
        handleClaim(props.sourceId);
        break;
      case 'pop-distribute':
        handleDistribute(props.sourceId);
        break;
      default:
        break;
    }
  }

  const sourceName = ref('');
  function handleDescriptionInit(_collaborationType?: CollaborationType, _sourceName?: string) {
    sourceName.value = _sourceName || '';
  }
</script>
