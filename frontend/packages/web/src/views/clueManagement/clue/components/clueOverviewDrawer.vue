<template>
  <CrmDrawer v-model:show="show" resizable no-padding :footer="false" :title="sourceName" :view-size="formViewSize">
    <template #titleRight>
      <CrmOperationButton
        :group-list="buttonList"
        class="gap-[12px]"
        :more-list="buttonMoreList"
        :not-show-divider="true"
        @pop-update="handleTransferPopUpdate"
        @select="handleSelect"
      >
        <template #more>
          <n-button type="primary" ghost class="n-btn-outline-primary">
            {{ t('common.more') }}
            <CrmIcon class="ml-[8px]" type="iconicon_chevron_down" :size="16" />
          </n-button>
        </template>
        <template #transferPopContent>
          <TransferForm ref="transferFormRef" v-model:form="transferForm" class="mt-[16px] w-[320px]" />
        </template>
      </CrmOperationButton>
    </template>
    <div class="h-full bg-[var(--text-n9)] p-[16px]">
      <CrmCard v-if="showDetailTabs" no-content-padding hide-footer auto-height class="mb-[16px]">
        <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line">
          <template #suffix>
            <CrmTabSetting
              v-if="showDetailTabs"
              :tab-list="enabledDetailTabList"
              :setting-key="`${FormDesignKeyEnum.CLUE}-settingKey`"
              @init="initTabList"
            />
          </template>
        </CrmTab>
      </CrmCard>
      <CrmCard contentHeight="100%" hide-footer :special-height="showDetailTabs ? 64 : 0" no-content-padding>
        <div v-show="activeTab === 'clue'" class="h-full overflow-hidden">
          <CrmFormDescription
            :refresh-key="refreshKey"
            :form-key="FormDesignKeyEnum.CLUE"
            :source-id="sourceId"
            class="p-[24px]"
            :column="2"
            label-width="auto"
            value-align="start"
            tooltip-position="top-start"
            :readonly="!hasAnyPermission(['CLUE_MANAGEMENT:UPDATE'])"
            @init="handleDescriptionInit"
            @open-customer-detail="emit('openCustomerDrawer', $event)"
            @refresh="emit('refresh')"
          />
        </div>
        <FollowDetail
          v-if="['followRecord', 'followPlan'].includes(activeTab)"
          :active-type="(activeTab as 'followRecord' | 'followPlan')"
          wrapper-class="h-full"
          virtual-scroll-height="calc(100vh - 254px)"
          :follow-api-key="FormDesignKeyEnum.CLUE"
          :initial-source-name="sourceName"
          :show-add="hasAnyPermission(['CLUE_MANAGEMENT:UPDATE'])"
          :source-id="sourceId"
          :show-action="showAction"
          :parentFormKey="FormDesignKeyEnum.CLUE"
        />
        <div v-if="activeTab === 'headRecord'" class="h-full px-[24px] pt-[24px]">
          <CrmHeaderTable
            :form-key="FormDesignKeyEnum.CLUE_POOL"
            :source-id="sourceId"
            :load-list-api="getClueHeaderList"
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
      :form-key="FormDesignKeyEnum.CLUE"
      :source-id="sourceId"
      need-init-detail
      @saved="handleSaved"
    />
  </CrmDrawer>
  <CrmMoveModal
    v-model:show="showMoveModal"
    :reason-key="ReasonTypeEnum.CLUE_POOL_RS"
    :source-id="sourceId"
    :name="sourceName"
    type="warning"
    @refresh="handleMovedSuccess"
  />
  <convertClueModal
    v-model:show="showConvertClueModal"
    :clue-id="sourceId"
    @success="emit('remove')"
    @finish="show = false"
  />
</template>

<script setup lang="ts">
  import { NButton, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { ClueListItem } from '@lib/shared/models/clue';
  import type { CollaborationType, TransferParams } from '@lib/shared/models/customer';
  import type { FormConfig, FormViewSize } from '@lib/shared/models/system/module';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import FollowDetail from '@/components/business/crm-follow-detail/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmHeaderTable from '@/components/business/crm-header-table/index.vue';
  import CrmMoveModal from '@/components/business/crm-move-modal/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmTabSetting from '@/components/business/crm-tab-setting/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import convertClueModal from './convertClueModal.vue';

  import { batchTransferClue, deleteClue, getClueHeaderList } from '@/api/modules';
  import { defaultTransferForm } from '@/config/opportunity';
  import useFormDetailTabAvailability from '@/hooks/useFormDetailTabAvailability';
  import useFormDetailTabs from '@/hooks/useFormDetailTabs';
  import useFormDetailTabTable from '@/hooks/useFormDetailTabTable';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    detail?: Partial<ClueListItem>;
  }>();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  const emit = defineEmits<{
    (e: 'refresh'): void;
    (e: 'saved', res: any): void;
    (e: 'remove'): void;
    (e: 'openCustomerDrawer', params: { customerId: string; inCustomerPool: boolean; poolId: string }): void;
  }>();

  const { openModal } = useModal();
  const { t } = useI18n();
  const Message = useMessage();

  const sourceId = computed(() => props.detail?.id ?? '');
  const sourceName = ref('');
  const refreshKey = ref(0);
  const formConfig = ref<FormConfig>();
  const formCreateDrawerVisible = ref(false);

  const transferForm = ref<TransferParams>({
    ...defaultTransferForm,
  });
  const transferLoading = ref(false);

  function closeAndRefresh() {
    show.value = false;
    emit('refresh');
  }

  // 转移
  const transferFormRef = ref<InstanceType<typeof TransferForm>>();

  function resetTransferForm() {
    transferForm.value = { ...defaultTransferForm };
  }

  function handleTransferPopUpdate(key: string, visible: boolean) {
    if (key === 'transfer' && visible) {
      resetTransferForm();
    }
  }

  function handleTransfer() {
    transferFormRef.value?.formRef?.validate(async (error) => {
      if (!error) {
        try {
          transferLoading.value = true;
          await batchTransferClue({
            ...transferForm.value,
            ids: [sourceId.value],
          });
          Message.success(t('common.transferSuccess'));
          resetTransferForm();
          closeAndRefresh();
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
      title: t('common.deleteConfirmTitle', { name: characterLimit(sourceName.value) }),
      content: t('clue.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteClue(sourceId.value);
          Message.success(t('common.deleteSuccess'));
          emit('remove');
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  // 移入线索池
  const showMoveModal = ref(false);
  function handleMoveToLeadPool() {
    showMoveModal.value = true;
  }

  // 转换
  const showConvertClueModal = ref(false);
  function handleConvert() {
    showConvertClueModal.value = true;
  }

  function handleSelect(key: string) {
    switch (key) {
      case 'edit':
        formCreateDrawerVisible.value = true;
        break;
      case 'pop-transfer':
        handleTransfer();
        break;
      case 'delete':
        handleDelete();
        break;
      case 'convert':
        handleConvert();
        break;
      case 'moveIntoCluePool':
        handleMoveToLeadPool();
        break;
      default:
        break;
    }
  }

  const showAction = computed(() => hasAnyPermission(['CLUE_MANAGEMENT:UPDATE']));

  const isConverted = computed(
    () => props.detail?.transitionType && ['CUSTOMER'].includes(props.detail.transitionType)
  );

  const buttonList = computed<ActionsItem[]>(() => {
    if (isConverted.value) {
      return [];
    }
    return [
      {
        label: t('common.edit'),
        key: 'edit',
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
        permission: ['CLUE_MANAGEMENT:UPDATE'],
      },
      {
        label: t('clue.convert'),
        key: 'convert',
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
        permission: ['CLUE_MANAGEMENT:UPDATE'],
      },
      {
        label: t('clue.moveIntoCluePool'),
        key: 'moveIntoCluePool',
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
        permission: ['CLUE_MANAGEMENT:RECYCLE'],
      },
      {
        label: t('common.transfer'),
        key: 'transfer',
        permission: ['CLUE_MANAGEMENT:TRANSFER'],
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
      },
    ];
  });

  const buttonMoreList = computed<ActionsItem[]>(() => {
    if (isConverted.value) {
      return [];
    }
    return [
      {
        label: t('common.delete'),
        key: 'delete',
        permission: ['CLUE_MANAGEMENT:DELETE'],
        danger: true,
      },
    ];
  });

  // tab
  const activeTab = ref('clue');
  const staticTabList: TabContentItem[] = [
    {
      name: 'followRecord',
      tab: t('crmFollowRecord.followRecord'),
      enable: true,
      internalKey: 'CLUE_FOLLOW_RECORD',
    },
    {
      name: 'followPlan',
      tab: t('common.plan'),
      enable: true,
      internalKey: 'CLUE_FOLLOW_PLAN',
    },
    {
      name: 'headRecord',
      tab: t('common.headRecord'),
      enable: true,
      internalKey: 'CLUE_OWNER_RECORD',
    },
  ];
  const { availableDetailTabIds } = useFormDetailTabAvailability(formConfig, FormDesignKeyEnum.CLUE);
  const { customDetailTabList, enabledDetailTabList } = useFormDetailTabs(
    formConfig,
    staticTabList,
    availableDetailTabIds
  );
  const showDetailTabs = computed(() => enabledDetailTabList.value.length > 0);
  const { getDetailTabTable } = useFormDetailTabTable();
  const customDetailTabTableList = computed(() =>
    customDetailTabList.value.flatMap((tab) => {
      const table = getDetailTabTable(tab.detailTab, props.detail?.id, FormDesignKeyEnum.CLUE);
      return table ? [{ tab, table }] : [];
    })
  );
  const settingTabList = ref<TabContentItem[]>([]);
  const tabList = computed<TabContentItem[]>(() => [
    {
      name: 'clue',
      tab: t('crmFormDesign.clue'),
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

  function handleMovedSuccess() {
    show.value = false;
    emit('remove');
  }

  const formViewSize = ref<FormViewSize>('large');
  function handleSaved(res: any) {
    refreshKey.value += 1;
    emit('saved', res);
  }

  function handleDescriptionInit(
    _collaborationType?: CollaborationType,
    _sourceName?: string,
    detail?: Record<string, any>,
    config?: FormConfig
  ) {
    sourceName.value = _sourceName || '';
    formConfig.value = config;
    formViewSize.value = config?.viewSize || 'large';
  }
</script>
