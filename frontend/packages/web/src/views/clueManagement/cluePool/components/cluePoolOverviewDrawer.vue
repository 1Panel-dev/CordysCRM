<template>
  <CrmOverviewDrawer
    ref="crmOverviewDrawerRef"
    v-model:show="show"
    v-model:active-tab="activeTab"
    :tab-list="tabList"
    :button-list="buttonList"
    :source-id="sourceId"
    :title="props.detail?.name"
    :form-key="FormDesignKeyEnum.CLUE_POOL"
    :show-tab-setting="true"
    @button-select="handleSelect"
    @saved="() => (refreshKey += 1)"
  >
    <template #left>
      <div class="h-full overflow-hidden">
        <CrmFormDescription
          :form-key="FormDesignKeyEnum.CLUE_POOL"
          :source-id="sourceId"
          class="p-[16px_24px]"
          :hidden-fields="hiddenColumns"
          :column="layout === 'vertical' ? 3 : undefined"
          :label-width="layout === 'vertical' ? 'auto' : undefined"
          :value-align="layout === 'vertical' ? 'start' : undefined"
        />
      </div>
    </template>
    <template #distributePopContent>
      <TransferForm ref="distributeFormRef" v-model:form="distributeForm" class="mt-[16px] w-[320px]" />
    </template>
    <template #right>
      <div class="h-full pt-[16px]">
        <FollowDetail
          v-if="['followRecord'].includes(activeTab)"
          :active-type="(activeTab as 'followRecord')"
          wrapper-class="h-[calc(100vh-162px)]"
          virtual-scroll-height="calc(100vh - 258px)"
          :follow-api-key="FormDesignKeyEnum.CLUE_POOL"
          :source-id="sourceId"
          :show-action="false"
          :refresh-key="refreshKey"
        />
        <CrmHeaderTable
          v-if="activeTab === 'headRecord'"
          :form-key="FormDesignKeyEnum.CLUE_POOL"
          :source-id="sourceId"
          :load-list-api="getClueHeaderList"
        />
      </div>
    </template>
  </CrmOverviewDrawer>
</template>

<script setup lang="ts">
  import { useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { CluePoolListItem } from '@lib/shared/models/clue';
  import type { TransferParams } from '@lib/shared/models/customer';

  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import FollowDetail from '@/components/business/crm-follow-detail/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmHeaderTable from '@/components/business/crm-header-table/index.vue';
  import CrmOverviewDrawer from '@/components/business/crm-overview-drawer/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';

  import { assignClue, deleteCluePool, getClueHeaderList, pickClue } from '@/api/modules';
  import { defaultTransferForm } from '@/config/opportunity';
  import useModal from '@/hooks/useModal';
  import useOpenNewPage from '@/hooks/useOpenNewPage';

  import { ClueRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    detail?: Partial<CluePoolListItem>;
    poolId: string;
    hiddenColumns: string[];
  }>();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  const emit = defineEmits<{
    (e: 'refresh'): void;
  }>();

  const { openModal } = useModal();
  const { t } = useI18n();
  const Message = useMessage();
  const { openNewPage } = useOpenNewPage();

  const crmOverviewDrawerRef = ref<InstanceType<typeof CrmOverviewDrawer>>();
  const layout = computed(() => crmOverviewDrawerRef.value?.layout);

  const sourceId = computed(() => props.detail?.id ?? '');

  const refreshKey = ref(0);

  const claimLoading = ref(false);
  const distributeLoading = ref(false);

  const buttonList: ActionsItem[] = [
    {
      label: t('common.claim'),
      key: 'claim',
      permission: ['CLUE_MANAGEMENT_POOL:PICK'],
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
      popConfirmProps: {
        loading: claimLoading.value,
        title: t('clue.claimOverviewTip'),
        positiveText: t('common.claim'),
        iconType: 'primary',
      },
    },
    {
      label: t('common.distribute'),
      key: 'distribute',
      permission: ['CLUE_MANAGEMENT_POOL:ASSIGN'],
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
      permission: ['CLUE_MANAGEMENT_POOL:DELETE'],
      text: false,
      ghost: true,
      danger: true,
      class: 'n-btn-outline-primary',
    },
  ];

  const distributeFormRef = ref<InstanceType<typeof TransferForm>>();
  const distributeForm = ref<TransferParams>({
    ...defaultTransferForm,
  });
  function handleDistribute() {
    distributeFormRef.value?.formRef?.validate(async (error) => {
      if (!error) {
        try {
          distributeLoading.value = true;
          await assignClue({
            assignUserId: distributeForm.value.owner || '',
            clueId: sourceId.value,
          });
          Message.success(t('common.distributeSuccess'));
          distributeForm.value = { ...defaultTransferForm };
          emit('refresh');
          show.value = false;
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          distributeLoading.value = false;
        }
      }
    });
  }

  // 删除
  function handleDelete() {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(props.detail?.name) }),
      content: t('clue.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteCluePool(sourceId.value);
          Message.success(t('common.deleteSuccess'));
          show.value = false;
          emit('refresh');
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  // 领取
  async function handleClaim() {
    try {
      claimLoading.value = true;
      await pickClue({
        clueId: sourceId.value,
        poolId: props.poolId,
      });
      Message.success(t('common.claimSuccess'));
      emit('refresh');
      show.value = false;
      openNewPage(ClueRouteEnum.CLUE_MANAGEMENT, {
        id: sourceId.value,
        transitionType: props.detail?.transitionType,
        name: props.detail?.name,
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      claimLoading.value = false;
    }
  }

  function handleSelect(key: string) {
    switch (key) {
      case 'pop-claim':
        handleClaim();
        break;
      case 'pop-distribute':
        handleDistribute();
        break;
      case 'delete':
        handleDelete();
        break;
      default:
        break;
    }
  }

  // tab
  const activeTab = ref('followRecord');
  const tabList: TabContentItem[] = [
    {
      name: 'followRecord',
      tab: t('crmFollowRecord.followRecord'),
      enable: true,
    },
    {
      name: 'headRecord',
      tab: t('common.previousOwnerRecord'),
      enable: true,
    },
  ];
</script>
