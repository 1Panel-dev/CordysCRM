<template>
  <CrmOverviewDrawer
    v-model:show="show"
    v-model:active-tab="activeTab"
    v-model:cached-list="cachedList"
    :tab-list="tabList"
    :button-list="buttonList"
    :source-id="sourceId"
    :title="props.detail?.name"
    :form-key="FormDesignKeyEnum.CLUE"
    :show-tab-setting="true"
    @button-select="handleSelect"
  >
    <template #left>
      <CrmFormDescription :form-key="FormDesignKeyEnum.CLUE" :source-id="sourceId" />
    </template>
    <template #transferPopContent>
      <TransferForm ref="transferFormRef" v-model:form="transferForm" class="mt-[16px] w-[320px]" />
    </template>
    <template #rightTop>
      <CrmWorkflowCard
        v-model:status="currentStatus"
        class="mb-[16px]"
        :show-confirm-status="true"
        :workflow-list="workflowList"
        :source-id="sourceId"
      />
    </template>
    <template #right>
      <FollowDetail
        v-if="['followRecord', 'followPlan'].includes(activeTab)"
        v-model:data="followList"
        v-model:active-status="activeStatus"
        class="mt-[16px]"
        :loading="followLoading"
        :show-title="activeTab === 'followRecord'"
        :type="activeTab"
        wrapper-class="h-[calc(100vh-258px)]"
        virtual-scroll-height="calc(100vh - 290px)"
        @reach-bottom="handleReachBottom"
        @search="() => loadFollowList()"
        @cancel-plan="handleCancelPlan"
        @handle-edit="handleEditFollow"
      />

      <!-- TODO getUserList接口换了 -->
      <HeaderTable
        v-if="activeTab === 'headRecord'"
        class="mt-[16px] h-[calc(100vh-258px)]"
        :source-id="sourceId"
        :load-list-api="getUserList"
      />

      <!-- TODO 确认后刷新数据 -->
      <CrmFormCreateDrawer
        v-model:visible="formDrawerVisible"
        :form-key="realFormKey"
        :source-id="realFollowSourceId"
      />
    </template>
  </CrmOverviewDrawer>
</template>

<script setup lang="ts">
  import { useMessage } from 'naive-ui';

  import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import type { ClueListItem } from '@lib/shared/models/clue';
  import type { FollowDetailItem, TransferParams } from '@lib/shared/models/customer';
  import type { WorkflowStepItem } from '@lib/shared/models/opportunity';

  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import FollowDetail from '@/components/business/crm-follow-detail/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import HeaderTable from '@/components/business/crm-form-create-table/headerTable.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmOverviewDrawer from '@/components/business/crm-overview-drawer/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import CrmWorkflowCard from '@/components/business/crm-workflow-card/index.vue';

  import {
    batchTransferClue,
    cancelClueFollowPlan,
    deleteClue,
    getClueFollowPlanList,
    getClueFollowRecordList,
  } from '@/api/modules/clue';
  import { getUserList } from '@/api/modules/system/org';
  import { defaultTransferForm } from '@/config/opportunity';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import { characterLimit } from '@/utils';

  const props = defineProps<{
    detail?: ClueListItem;
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

  const sourceId = computed(() => props.detail?.id ?? '');

  const transferForm = ref<TransferParams>({
    ...defaultTransferForm,
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
      label: t('common.followPlan'),
      key: 'followPlan',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
    {
      label: t('crmFollowRecord.followRecord'),
      key: 'followRecord',
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
      popSlotName: 'transferPopTitle',
      popSlotContent: 'transferPopContent',
    },
    {
      label: t('clue.convertToCustomer'),
      key: 'convertToCustomer',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
    {
      label: t('clue.convertToOpportunity'),
      key: 'convertToOpportunity',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
    {
      label: t('common.delete'),
      key: 'delete',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
  ];

  // 转移
  const transferFormRef = ref<InstanceType<typeof TransferForm>>();
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
          transferForm.value = { ...defaultTransferForm };
          show.value = false;
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
      title: t('common.deleteConfirmTitle', { name: characterLimit(props.detail?.name) }),
      content: t('clue.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteClue(sourceId.value);
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

  const formDrawerVisible = ref(false);
  const realFormKey = ref<FormDesignKeyEnum>(FormDesignKeyEnum.FOLLOW_RECORD_CLUE);

  function handleSelect(key: string) {
    switch (key) {
      case 'pop-transfer':
        handleTransfer();
        break;
      case 'delete':
        handleDelete();
        break;
      case 'convertToCustomer':
        // TODO 参数
        realFormKey.value = FormDesignKeyEnum.CUSTOMER;
        formDrawerVisible.value = true;
        break;
      case 'convertToOpportunity':
        realFormKey.value = FormDesignKeyEnum.BUSINESS;
        formDrawerVisible.value = true;
        break;
      default:
        break;
    }
  }

  // 进度条 TODO 从后端获取数据
  const currentStatus = ref<string>('purchasing');
  const workflowList = ref<WorkflowStepItem[]>([]);

  // tab
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

  // 跟进记录/跟进计划
  const pageNation = ref({
    total: 0,
    pageSize: 10,
    current: 1,
  });
  const followList = ref<FollowDetailItem[]>([]);
  const activeStatus = ref(CustomerFollowPlanStatusEnum.ALL);
  const followLoading = ref<boolean>(false);

  async function loadFollowList() {
    try {
      followLoading.value = true;
      const params = {
        sourceId: sourceId.value,
        current: pageNation.value.current || 1,
        pageSize: pageNation.value.pageSize,
      };

      let res;
      if (activeTab.value === 'followPlan') {
        res = await getClueFollowPlanList({
          ...params,
          status: activeStatus.value,
        });
      } else {
        res = await getClueFollowRecordList(params);
      }

      followList.value = res.list || [];
      pageNation.value.total = res.total;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      followLoading.value = false;
    }
  }

  function handleReachBottom() {
    pageNation.value.current += 1;
    if (pageNation.value.current > Math.ceil(pageNation.value.total / pageNation.value.pageSize)) {
      return;
    }
    loadFollowList();
  }

  // 取消计划
  async function handleCancelPlan(item: FollowDetailItem) {
    try {
      await cancelClueFollowPlan(item.id);
      Message.success(t('common.cancelSuccess'));
      loadFollowList();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  // 编辑跟进内容
  const realFollowSourceId = ref<string | undefined>('');
  function handleEditFollow(item: FollowDetailItem) {
    realFormKey.value =
      activeTab.value === 'followRecord' ? FormDesignKeyEnum.FOLLOW_RECORD_CLUE : FormDesignKeyEnum.FOLLOW_PLAN_CLUE;
    realFollowSourceId.value = item.id;
    formDrawerVisible.value = true;
  }

  watch(
    () => activeTab.value,
    (val) => {
      if (['followPlan', 'followRecord'].includes(val)) {
        loadFollowList();
      }
    }
  );

  watch(
    () => show.value,
    (val) => {
      if (val) {
        loadFollowList();
      }
    }
  );
</script>
