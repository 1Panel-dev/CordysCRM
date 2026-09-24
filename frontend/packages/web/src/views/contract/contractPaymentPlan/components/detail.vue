<template>
  <CrmDrawer v-model:show="visible" resizable no-padding :width="800" :footer="false" :view-size="formViewSize">
    <template #titleLeft>
      <div class="text-[14px] font-normal">
        <ContractStatus :status="detailInfo?.planStatus ?? ContractPaymentPlanEnum.PENDING" />
      </div>
    </template>
    <template v-if="!props.readonly" #titleRight>
      <n-button
        v-permission="['CONTRACT_PAYMENT_PLAN:UPDATE']"
        type="primary"
        ghost
        class="n-btn-outline-primary"
        @click="handleEdit(props.sourceId)"
      >
        {{ t('common.edit') }}
      </n-button>
      <n-button
        v-permission="['CONTRACT_PAYMENT_PLAN:DELETE']"
        type="error"
        ghost
        class="n-btn-outline-error ml-[12px]"
        @click="handleDelete(detailInfo)"
      >
        {{ t('common.delete') }}
      </n-button>
    </template>
    <div class="h-full bg-[var(--text-n9)] px-[16px] pt-[16px]">
      <CrmCard v-if="showDetailTabs" no-content-padding hide-footer auto-height class="mb-[16px]">
        <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line">
          <template #suffix>
            <CrmTabSetting
              v-if="showDetailTabs"
              :tab-list="enabledDetailTabList"
              :setting-key="`${FormDesignKeyEnum.CONTRACT_PAYMENT}-settingKey`"
              @init="initTabList"
            />
          </template>
        </CrmTab>
      </CrmCard>
      <CrmCard contentHeight="100%" hide-footer :special-height="showDetailTabs ? 80 : 0" no-content-padding>
        <div v-show="activeTab === 'paymentPlan'" class="h-full p-[24px]">
          <CrmFormDescription
            ref="formDescriptionRef"
            :form-key="FormDesignKeyEnum.CONTRACT_PAYMENT"
            :source-id="props.sourceId"
            :column="2"
            :refresh-key="refreshKey"
            label-width="auto"
            value-align="start"
            tooltip-position="top-start"
            :readonly="!hasAnyPermission(['CONTRACT_PAYMENT_PLAN:UPDATE'])"
            @init="handleInit"
            @open-contract-detail="emit('openContractDrawer', $event)"
            @refresh="emit('refresh')"
          />
        </div>
        <template v-for="item in customDetailTabTableList" :key="String(item.tab.name)">
          <div v-if="activeTab === item.tab.name" class="h-full px-[24px] pt-[24px]">
            <component :is="item.table.component" v-bind="item.table.props" hideBoard />
          </div>
        </template>
      </CrmCard>
    </div>

    <CrmFormCreateDrawer
      v-model:visible="formCreateDrawerVisible"
      :form-key="FormDesignKeyEnum.CONTRACT_PAYMENT"
      :source-id="props.sourceId"
      need-init-detail
      :link-form-key="FormDesignKeyEnum.CONTRACT_PAYMENT"
      @saved="() => handleSaved()"
    />
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { NButton, useMessage } from 'naive-ui';

  import { ContractPaymentPlanEnum } from '@lib/shared/enums/contractEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { CollaborationType } from '@lib/shared/models/customer';
  import type { FormConfig, FormViewSize } from '@lib/shared/models/system/module';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmTabSetting from '@/components/business/crm-tab-setting/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import ContractStatus from './contractPaymentStatus.vue';

  import { deletePaymentPlan } from '@/api/modules';
  import useFormDetailTabAvailability from '@/hooks/useFormDetailTabAvailability';
  import useFormDetailTabs from '@/hooks/useFormDetailTabs';
  import useFormDetailTabTable from '@/hooks/useFormDetailTabTable';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission.js';

  const props = defineProps<{
    sourceId: string;
    readonly?: boolean;
  }>();
  const emit = defineEmits<{
    (e: 'refresh'): void;
    (e: 'delete'): void;
    (e: 'openContractDrawer', params: { id: string }): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();
  const detailInfo = ref();
  const formConfig = ref<FormConfig>();
  const formViewSize = ref<FormViewSize>('large');

  function handleInit(type?: CollaborationType, name?: string, detail?: Record<string, any>, config?: FormConfig) {
    detailInfo.value = detail;
    formConfig.value = config;
    formViewSize.value = config?.viewSize || 'large';
  }

  const activeTab = ref('paymentPlan');
  const { availableDetailTabIds } = useFormDetailTabAvailability(formConfig, FormDesignKeyEnum.CONTRACT_PAYMENT);
  const { customDetailTabList, enabledDetailTabList } = useFormDetailTabs(formConfig, [], availableDetailTabIds);
  const showDetailTabs = computed(() => enabledDetailTabList.value.length > 0);
  const { getDetailTabTable } = useFormDetailTabTable();
  const customDetailTabTableList = computed(() =>
    customDetailTabList.value.flatMap((tab) => {
      const table = getDetailTabTable(tab.detailTab, props.sourceId, FormDesignKeyEnum.CONTRACT_PAYMENT);
      return table ? [{ tab, table }] : [];
    })
  );
  const settingTabList = ref<TabContentItem[]>([]);
  const tabList = computed<TabContentItem[]>(() => [
    {
      name: 'paymentPlan',
      tab: t('module.paymentPlan'),
      enable: true,
      permission: ['CONTRACT_PAYMENT_PLAN:READ'],
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

  const formDescriptionRef = ref<InstanceType<typeof CrmFormDescription>>();
  watch(
    () => activeTab.value,
    () => {
      if (activeTab.value === 'customer') {
        formDescriptionRef.value?.initFormDescription();
      }
    }
  );

  const refreshKey = ref(0);
  function handleSaved() {
    refreshKey.value += 1;
    emit('refresh');
  }

  function handleDelete(row: any) {
    openModal({
      type: 'error',
      title: t('system.personal.confirmDelete'),
      content: t('common.deleteConfirmContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deletePaymentPlan(row.id);
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
  function handleEdit(id: string) {
    formCreateDrawerVisible.value = true;
  }
</script>
