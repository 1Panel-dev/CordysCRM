<template>
  <CrmDrawer v-model:show="showOptOverviewDrawer" resizable no-padding :footer="false" :view-size="formViewSize">
    <template #title>
      <n-tooltip trigger="hover" :delay="300" :disabled="!titleName">
        <template #trigger>
          <div class="flex gap-[4px] overflow-hidden">
            <div class="one-line-text flex-1 text-[var(--text-n1)]">{{ titleName }}</div>
            <div v-if="subTitleName" class="flex text-[var(--text-n4)]">
              (
              <div class="one-line-text max-w-[300px]">{{ subTitleName }}</div>
              )
            </div>
          </div>
        </template>
        {{ `${titleName}${subTitleName ? `(${subTitleName})` : ''}` }}
      </n-tooltip>
    </template>
    <template #titleRight>
      <CrmButtonGroup
        class="gap-[12px]"
        :list="buttonList"
        not-show-divider
        @pop-update="handleTransferPopUpdate"
        @select="handleSelect"
      >
        <template #transferPopContent>
          <TransferForm ref="transferFormRef" v-model:form="transferForm" class="mt-[16px] w-[320px]" />
        </template>
      </CrmButtonGroup>
    </template>
    <div class="h-full bg-[var(--text-n9)] p-[16px]">
      <CrmWorkflowCard
        v-model:stage="currentStatus"
        :formKey="FormDesignKeyEnum.BUSINESS"
        :show-confirm-status="true"
        class="mb-[16px]"
        :stageConfig="stageConfig"
        is-limit-back
        :failure-reason="lastFailureReason"
        :back-stage-permission="['OPPORTUNITY_MANAGEMENT:UPDATE', 'OPPORTUNITY_MANAGEMENT:RESIGN']"
        :source-id="sourceId"
        :operation-permission="['OPPORTUNITY_MANAGEMENT:UPDATE']"
        :update-api="updateOptStage"
        @load-detail="refreshList"
      />
      <CrmCard v-if="showDetailTabs" no-content-padding hide-footer auto-height class="mb-[16px]">
        <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line">
          <template #suffix>
            <CrmTabSetting
              v-if="showDetailTabs"
              :tab-list="enabledDetailTabList"
              :setting-key="`${FormDesignKeyEnum.BUSINESS}-settingKey`"
              @init="initTabList"
            />
          </template>
        </CrmTab>
      </CrmCard>
      <CrmCard contentHeight="100%" hide-footer :special-height="showDetailTabs ? 170 : 90" no-content-padding>
        <div v-show="activeTab === 'opportunity'" class="h-full overflow-hidden">
          <CrmFormDescription
            :form-key="FormDesignKeyEnum.BUSINESS"
            :source-id="sourceId"
            :refresh-key="refreshKey"
            class="p-[24px]"
            :column="2"
            label-width="auto"
            value-align="start"
            tooltip-position="top-start"
            :readonly="!hasAnyPermission(['OPPORTUNITY_MANAGEMENT:UPDATE'])"
            @init="handleDescriptionInit"
            @open-customer-detail="emit('openCustomerDrawer', $event)"
          />
        </div>
        <FollowDetail
          v-if="['followRecord', 'followPlan'].includes(activeTab)"
          :refresh-key="refreshKey"
          :active-type="(activeTab as 'followRecord' | 'followPlan')"
          wrapper-class="h-full"
          virtual-scroll-height="calc(100vh - 382px)"
          :follow-api-key="FormDesignKeyEnum.BUSINESS"
          :source-id="sourceId"
          :initial-source-name="initialSourceName"
          :show-add="hasAnyPermission(['OPPORTUNITY_MANAGEMENT:UPDATE'])"
          :show-action="hasAnyPermission(['OPPORTUNITY_MANAGEMENT:UPDATE'])"
          :parentFormKey="FormDesignKeyEnum.BUSINESS"
        />
        <div v-if="activeTab === 'contact'" class="h-full px-[24px] pt-[24px]">
          <ContactTable
            :form-key="FormDesignKeyEnum.BUSINESS_CONTACT"
            :refresh-key="refreshKey"
            readonly
            :source-id="sourceId"
          />
        </div>
        <div v-if="activeTab === 'quotation'" class="h-full px-[24px] pt-[24px]">
          <quotationTable
            :form-key="FormDesignKeyEnum.OPPORTUNITY_QUOTATION"
            :source-id="sourceId"
            :refresh-key="refreshKey"
            :source-name="titleName"
          />
        </div>
        <template v-for="item in customDetailTabTableList" :key="String(item.tab.name)">
          <div v-if="activeTab === item.tab.name" class="h-full px-[24px] pt-[24px]">
            <component :is="item.table.component" v-bind="item.table.props" />
          </div>
        </template>
      </CrmCard>
    </div>
    <CrmFormCreateDrawer
      v-model:visible="formCreateDrawerVisible"
      :form-key="FormDesignKeyEnum.BUSINESS"
      :source-id="sourceId"
      need-init-detail
      @saved="refreshList"
    />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { NTooltip, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { CollaborationType, TransferParams } from '@lib/shared/models/customer';
  import type { OpportunityItem, OpportunityStageConfig } from '@lib/shared/models/opportunity';
  import type { FormConfig, FormViewSize } from '@lib/shared/models/system/module';

  import CrmButtonGroup from '@/components/pure/crm-button-group/index.vue';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import FollowDetail from '@/components/business/crm-follow-detail/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import ContactTable from '@/components/business/crm-form-create-table/contactTable.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmTabSetting from '@/components/business/crm-tab-setting/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import CrmWorkflowCard from '@/components/business/crm-workflow-card/index.vue';
  import quotationTable from './quotation/quotationTable.vue';

  import { deleteOpt, getOpportunityStageConfig, transferOpt, updateOptStage } from '@/api/modules';
  import { defaultTransferForm } from '@/config/opportunity';
  import useFormDetailTabAvailability from '@/hooks/useFormDetailTabAvailability';
  import useFormDetailTabs from '@/hooks/useFormDetailTabs';
  import useFormDetailTabTable from '@/hooks/useFormDetailTabTable';
  import useModal from '@/hooks/useModal';
  import { hasAllPermission, hasAnyPermission } from '@/utils/permission';

  const { openModal } = useModal();

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    detail?: Partial<OpportunityItem>;
  }>();

  const emit = defineEmits<{
    (e: 'refresh'): void;
    (e: 'remove'): void;
    (e: 'openCustomerDrawer', params: { customerId: string; inCustomerPool: boolean; poolId: string }): void;
  }>();

  const showOptOverviewDrawer = defineModel<boolean>('show', {
    required: true,
  });

  const transferForm = ref<TransferParams>({
    owner: null,
    ids: [],
  });

  const stageConfig = ref<OpportunityStageConfig>();
  async function initStageConfig() {
    try {
      stageConfig.value = await getOpportunityStageConfig();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const sourceId = computed(() => props.detail?.id ?? '');
  const refreshKey = ref(0);
  const lastFailureReason = ref('');
  const currentStatus = ref<string>(stageConfig.value?.stageConfigList[0]?.id || '');
  const isSuccess = computed(
    () =>
      currentStatus.value === stageConfig.value?.stageConfigList.find((e) => e.type === 'END' && e.rate === '100')?.id
  );
  const isFail = computed(
    () => currentStatus.value === stageConfig.value?.stageConfigList.find((e) => e.type === 'END' && e.rate === '0')?.id
  );

  const transferLoading = ref(false);

  const buttonList = computed<ActionsItem[]>(() => {
    const transferAction: ActionsItem[] = [
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
        popSlotName: 'transferPopTitle',
        popSlotContent: 'transferPopContent',
        permission: ['OPPORTUNITY_MANAGEMENT:TRANSFER'],
      },
    ];

    const editAction: ActionsItem[] = [
      {
        label: t('common.edit'),
        key: 'edit',
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
        permission: ['OPPORTUNITY_MANAGEMENT:UPDATE'],
      },
    ];

    const deleteAction: ActionsItem[] = [
      {
        label: t('common.delete'),
        key: 'delete',
        text: false,
        ghost: true,
        danger: true,
        class: 'n-btn-outline-primary',
        permission: ['OPPORTUNITY_MANAGEMENT:DELETE'],
      },
    ];

    if (isFail.value) {
      return [...transferAction, ...deleteAction];
    }

    if (isSuccess.value) {
      return hasAllPermission(['OPPORTUNITY_MANAGEMENT:UPDATE', 'OPPORTUNITY_MANAGEMENT:RESIGN'])
        ? [...editAction, ...transferAction, ...deleteAction]
        : [...transferAction, ...deleteAction];
    }

    return [...editAction, ...transferAction, ...deleteAction];
  });

  const formConfig = ref<FormConfig>();
  const activeTab = ref('opportunity');
  const staticTabList: TabContentItem[] = [
    {
      name: 'followRecord',
      tab: t('crmFollowRecord.followRecord'),
      enable: true,
      internalKey: 'OPPORTUNITY_FOLLOW_RECORD',
    },
    {
      name: 'followPlan',
      tab: t('common.plan'),
      enable: true,
      internalKey: 'OPPORTUNITY_FOLLOW_PLAN',
    },
    {
      name: 'contact',
      tab: t('opportunity.contactInfo'),
      enable: true,
      permission: ['CUSTOMER_MANAGEMENT_CONTACT:READ'],
      internalKey: 'OPPORTUNITY_CONTACT',
    },
    {
      name: 'quotation',
      tab: t('opportunity.quotation'),
      enable: true,
      permission: ['OPPORTUNITY_QUOTATION:READ'],
      internalKey: 'OPPORTUNITY_QUOTATION',
    },
  ];
  const { availableDetailTabIds } = useFormDetailTabAvailability(formConfig, FormDesignKeyEnum.BUSINESS);
  const { customDetailTabList, enabledDetailTabList } = useFormDetailTabs(
    formConfig,
    staticTabList,
    availableDetailTabIds
  );
  const showDetailTabs = computed(() => enabledDetailTabList.value.length > 0);
  const { getDetailTabTable } = useFormDetailTabTable();
  const customDetailTabTableList = computed(() =>
    customDetailTabList.value.flatMap((tab) => {
      const table = getDetailTabTable(tab.detailTab, props.detail?.id, FormDesignKeyEnum.BUSINESS);
      return table ? [{ tab, table }] : [];
    })
  );
  const settingTabList = ref<TabContentItem[]>([]);
  const tabList = computed<TabContentItem[]>(() => [
    {
      name: 'opportunity',
      tab: t('crmFormDesign.opportunity'),
      enable: true,
    },
    ...settingTabList.value,
  ]);

  function initTabList(list: TabContentItem[]) {
    settingTabList.value = list;
  }

  watch(
    () => tabList.value,
    (list) => {
      if (!list.some((item) => item.name === activeTab.value)) {
        activeTab.value = list[0]?.name as string;
      }
    }
  );

  const titleName = ref('');
  const subTitleName = ref('');
  const initialSourceName = ref('');
  const formCreateDrawerVisible = ref(false);

  // 转移
  const transferFormRef = ref<InstanceType<typeof TransferForm>>();

  function resetTransferForm() {
    transferForm.value = { ...defaultTransferForm };
  }

  function handleTransferPopUpdate(key: string, show: boolean) {
    if (key === 'transfer' && show) {
      resetTransferForm();
    }
  }

  function handleTransfer(done?: () => void) {
    transferFormRef.value?.formRef?.validate(async (error) => {
      if (!error) {
        try {
          transferLoading.value = true;
          await transferOpt({
            ...transferForm.value,
            ids: [sourceId.value],
          });
          Message.success(t('common.transferSuccess'));
          resetTransferForm();
          showOptOverviewDrawer.value = false;
          done?.();
          emit('refresh');
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          transferLoading.value = false;
        }
      }
    });
  }

  // 删除
  function handleDelete() {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(titleName.value) }),
      content: t('opportunity.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteOpt(sourceId.value);
          Message.success(t('common.deleteSuccess'));
          showOptOverviewDrawer.value = false;
          emit('remove');
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  function handleSelect(key: string, done?: () => void) {
    switch (key) {
      case 'edit':
        formCreateDrawerVisible.value = true;
        break;
      case 'pop-transfer':
        handleTransfer(done);
        break;
      case 'delete':
        handleDelete();
        break;
      default:
        break;
    }
  }

  function refreshList() {
    refreshKey.value += 1;
    emit('refresh');
  }

  const formViewSize = ref<FormViewSize>('large');
  function handleDescriptionInit(
    _collaborationType?: CollaborationType,
    _sourceName?: string,
    detail?: Record<string, any>,
    config?: FormConfig
  ) {
    formConfig.value = config;
    if (detail) {
      const { customerName, customerId, name, stage, failureReason } = detail;
      // 商机阶段
      currentStatus.value = stage;
      // 用于回显跟进类型、商机、商机对应客户
      titleName.value = _sourceName || '';
      subTitleName.value = customerName;
      lastFailureReason.value = failureReason;
      initialSourceName.value =
        JSON.stringify({
          name,
          customerName,
          customerId,
        }) || '';
    }
    formViewSize.value = config?.viewSize || 'large';
  }

  watch(
    () => showOptOverviewDrawer.value,
    (val) => {
      if (val) {
        initStageConfig();
      }
    }
  );
</script>
