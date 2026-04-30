<template>
  <CrmDrawer v-model:show="visible" resizable no-padding :width="800" :footer="false" :title="detailInfo?.name ?? ''">
    <template #titleLeft>
      <div v-if="enableApproval" class="text-[14px] font-normal">
        <CrmApprovalStatus :status="detailInfo?.approvalStatus ?? ProcessStatusEnum.NONE" />
      </div>
    </template>
    <template #titleRight>
      <CrmOperationButton
        class="gap-[12px]"
        :not-show-divider="true"
        :group-list="detailActions.groupList"
        :more-list="detailActions.moreList"
        @select="handleSelect"
      >
        <template #more>
          <n-button type="primary" ghost class="n-btn-outline-primary">
            {{ t('common.more') }}
            <CrmIcon class="ml-[8px]" type="iconicon_chevron_down" :size="16" />
          </n-button>
        </template>
      </CrmOperationButton>
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
  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { CollaborationType } from '@lib/shared/models/customer';
  import { OpportunityStageConfig } from '@lib/shared/models/opportunity';
  import { OrderItem } from '@lib/shared/models/order';
  import { CluePoolItem } from '@lib/shared/models/system/module';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmApprovalStatus from '@/components/business/crm-approval-status/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmWorkflowCard from '@/components/business/crm-workflow-card/index.vue';
  import ContractDetailDrawer from '@/views/contract/contract/components/detail.vue';
  import customerOverviewDrawer from '@/views/customer/components/customerOverviewDrawer.vue';
  import openSeaOverviewDrawer from '@/views/customer/components/openSeaOverviewDrawer.vue';

  import { deleteOrder, getOpenSeaOptions, getOrderStatusConfig, updateOrderStage } from '@/api/modules';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
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

  const orderDetailDataActionMap = {
    edit: {
      key: 'edit',
      label: t('common.edit'),
      permission: ['ORDER:UPDATE'],
    },
    download: {
      label: t('common.download'),
      key: 'download',
      permission: ['ORDER:DOWNLOAD'],
    },
    delete: {
      label: t('common.delete'),
      key: 'delete',
      danger: true,
      permission: ['ORDER:DELETE'],
    },
  };
  // todo 订单现在没有审批状态，所以暂时不生效，需要和后台确认
  const { initApprovalPermission, resolveRowOperation, enableApproval } = useApprovalOperation<OrderItem>({
    formType: FormDesignKeyEnum.ORDER,
    dataActionMap: orderDetailDataActionMap,
    isDetail: true,
    specialActionFilter: (_row, actionKeys) => {
      return actionKeys.filter((key) => !['review', 'revoke', 'pass', 'unPass'].includes(key));
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
    const filteredGroupList = props.readonly
      ? detailAction.groupList.filter((item) => !['edit', 'delete'].includes(item.key as string))
      : detailAction.groupList;
    const filteredMoreList = props.readonly
      ? detailAction.moreList.filter((item) => !['edit', 'delete'].includes(item.key as string))
      : detailAction.moreList;

    return {
      groupList: filteredGroupList.map((e) => {
        return {
          ...e,
          text: false,
          ghost: true,
          class: 'n-btn-outline-primary',
        };
      }),
      moreList: filteredMoreList,
    };
  });

  const formCreateDrawerVisible = ref(false);
  function handleEdit() {
    formCreateDrawerVisible.value = true;
  }

  function handleDownload(id: string) {
    openNewPage(FullPageEnum.FULL_PAGE_EXPORT_ORDER, { id });
  }

  function handleApproval(isPass?: boolean) {
    // todo 审批
  }
  function handleRevoke() {
    // todo 撤销
  }

  function handleSelect(key: string) {
    // todo 缺少审批那些
    switch (key) {
      case 'pass':
        handleApproval(true);
        break;
      case 'unPass':
        handleApproval();
        break;
      case 'review':
        // todo 提审 xinxinwu
        break;
      case 'revoke':
        handleRevoke();
        break;
      case 'edit':
        handleEdit();
        break;
      case 'download':
        handleDownload(props.sourceId);
        break;
      case 'delete':
        handleDelete(detailInfo.value);
        break;
      default:
        break;
    }
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

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        initApprovalPermission();
      }
    }
  );
</script>
