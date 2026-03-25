<template>
  <CrmDrawer v-model:show="visible" resizable no-padding :width="800" :footer="false" :title="detailInfo?.name ?? ''">
    <template #titleRight>
      <n-button
        v-if="!props.readonly"
        v-permission="['ORDER:UPDATE']"
        type="primary"
        ghost
        class="n-btn-outline-primary"
        @click="handleEdit()"
      >
        {{ t('common.edit') }}
      </n-button>
      <n-button
        v-permission="['ORDER:DOWNLOAD']"
        type="primary"
        ghost
        class="n-btn-outline-primary ml-[12px]"
        @click="handleDownload(props.sourceId)"
      >
        {{ t('common.download') }}
      </n-button>
      <n-button
        v-if="!props.readonly"
        v-permission="['ORDER:DELETE']"
        type="error"
        ghost
        class="n-btn-outline-error ml-[12px]"
        @click="handleDelete(detailInfo)"
      >
        {{ t('common.delete') }}
      </n-button>
    </template>
    <div class="h-full bg-[var(--text-n9)] px-[16px] pt-[16px]">
      <CrmWorkflowCard
        v-model:stage="currentStatus"
        class="mb-[16px]"
        :stage-config-list="stageConfig?.stageConfigList || []"
        is-limit-back
        is-order
        :back-stage-permission="['ORDER:UPDATE']"
        :source-id="sourceId"
        :operation-permission="['ORDER:UPDATE']"
        :update-api="updateOrderStage"
        :afoot-roll-back="stageConfig?.afootRollBack"
        :end-roll-back="stageConfig?.endRollBack"
        @load-detail="handleSaved()"
      />
      <CrmCard hide-footer>
        <div class="flex-1">
          <CrmFormDescription
            :form-key="FormDesignKeyEnum.ORDER_SNAPSHOT"
            :source-id="props.sourceId"
            :column="2"
            :refresh-key="refreshKey"
            label-width="auto"
            value-align="start"
            tooltip-position="top-start"
            readonly
            @init="handleInit"
            @open-contract-detail="handleOpenContractDrawer"
            @open-customer-detail="handleOpenCustomerDrawer"
          />
        </div>
      </CrmCard>
    </div>

    <CrmFormCreateDrawer
      v-model:visible="formCreateDrawerVisible"
      :form-key="FormDesignKeyEnum.ORDER"
      :source-id="props.sourceId"
      need-init-detail
      :link-form-key="FormDesignKeyEnum.ORDER"
      @saved="() => handleSaved()"
    />
    <ContractDetailDrawer
      v-model:visible="showContractDetailDrawer"
      :sourceId="activeSourceId"
      @show-customer-drawer="handleOpenCustomerDrawer"
    />
    <customerOverviewDrawer v-model:show="showCustomerOverviewDrawer" :source-id="activeCustomerSourceId" />
    <openSeaOverviewDrawer
      v-model:show="showCustomerOpenseaOverviewDrawer"
      :source-id="activeCustomerSourceId"
      :pool-id="poolId"
      :hidden-columns="hiddenColumns"
    />
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { NButton, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { CollaborationType } from '@lib/shared/models/customer';
  import { OpportunityStageConfig } from '@lib/shared/models/opportunity';
  import { OrderItem } from '@lib/shared/models/order';
  import { CluePoolItem } from '@lib/shared/models/system/module';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmWorkflowCard from '@/components/business/crm-workflow-card/index.vue';
  import ContractDetailDrawer from '@/views/contract/contract/components/detail.vue';
  import customerOverviewDrawer from '@/views/customer/components/customerOverviewDrawer.vue';
  import openSeaOverviewDrawer from '@/views/customer/components/openSeaOverviewDrawer.vue';

  import { deleteOrder, getOpenSeaOptions, getOrderStatusConfig, updateOrderStage } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import useOpenNewPage from '@/hooks/useOpenNewPage';
  import { hasAnyPermission } from '@/utils/permission';

  import { FullPageEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    sourceId: string;
    readonly?: boolean;
  }>();
  const emit = defineEmits<{
    (e: 'refresh'): void;
    (e: 'delete'): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();
  const detailInfo = ref();
  const { openNewPage } = useOpenNewPage();

  const stageConfig = ref<OpportunityStageConfig>();
  async function initStageConfig() {
    try {
      stageConfig.value = await getOrderStatusConfig();
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

  const currentStatus = ref<string>(stageConfig.value?.stageConfigList[0]?.id || '');

  function handleInit(type?: CollaborationType, name?: string, detail?: Record<string, any>) {
    detailInfo.value = detail;
    if (detail) {
      currentStatus.value = detail.stage;
    }
  }

  const refreshKey = ref(0);
  function handleSaved() {
    refreshKey.value += 1;
    emit('refresh');
  }

  async function handleDelete(row: OrderItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('common.deleteConfirmContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteOrder(row.id);
          Message.success(t('common.deleteSuccess'));
          visible.value = false;
          emit('delete');
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  const formCreateDrawerVisible = ref(false);
  function handleEdit() {
    formCreateDrawerVisible.value = true;
  }

  function handleDownload(id: string) {
    openNewPage(FullPageEnum.FULL_PAGE_EXPORT_ORDER, { id });
  }

  const showContractDetailDrawer = ref(false);
  const activeSourceId = ref<string>('');
  function handleOpenContractDrawer(params: { id: string }) {
    activeSourceId.value = params.id;
    showContractDetailDrawer.value = true;
  }

  const showCustomerOverviewDrawer = ref(false);
  const showCustomerOpenseaOverviewDrawer = ref(false);
  const poolId = ref<string>('');
  const activeCustomerSourceId = ref<string>('');
  function handleOpenCustomerDrawer(params: { customerId: string; inCustomerPool: boolean; poolId: string }) {
    activeCustomerSourceId.value = params.customerId;
    if (params.inCustomerPool) {
      showCustomerOpenseaOverviewDrawer.value = true;
      poolId.value = params.poolId;
    } else {
      showCustomerOverviewDrawer.value = true;
    }
  }

  const openSeaOptions = ref<CluePoolItem[]>([]);

  async function initOpenSeaOptions() {
    if (hasAnyPermission(['CUSTOMER_MANAGEMENT_POOL:READ'])) {
      const res = await getOpenSeaOptions();
      openSeaOptions.value = res;
    }
  }

  const hiddenColumns = computed<string[]>(() => {
    const openSeaSetting = openSeaOptions.value.find((item) => item.id === poolId.value);
    return openSeaSetting?.fieldConfigs.filter((item) => !item.enable).map((item) => item.fieldId) || [];
  });

  onBeforeMount(() => {
    initOpenSeaOptions();
  });
</script>
