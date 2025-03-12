<template>
  <CrmOverviewDrawer
    v-model:show="showOptOverviewDrawer"
    v-model:active-tab="activeTab"
    v-model:cached-list="cachedList"
    :tab-list="tabList"
    :button-list="buttonList"
    :title="detail.name"
    :subtitle="detail.customerName"
    :form-key="FormDesignKeyEnum.BUSINESS"
    @button-select="handleSelect"
  >
    <template #left>
      <CrmFormDescription :form-key="FormDesignKeyEnum.BUSINESS" :source-id="props.sourceId" />
    </template>
    <template #rightTop>
      <CrmWorkflowCard
        v-model:status="currentStatus"
        :show-confirm-status="true"
        class="mb-[16px]"
        :workflow-list="workflowList"
        :source-id="detail.id"
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
        wrapper-class="h-[calc(100vh-162px)]"
        virtual-scroll-height="calc(100vh - 194px)"
        @reach-bottom="handleReachBottom"
        @search="() => loadFollowList()"
        @cancel-plan="handleCancelPlan"
        @handle-edit="handleEditFollow"
      />

      <HeaderTable
        v-if="activeTab === 'headRecord'"
        class="mt-[16px] h-[calc(100vh-161px)]"
        :source-id="detail.id"
        :load-list-api="getUserList"
      />

      <CrmFormCreateDrawer
        v-model:visible="formDrawerVisible"
        :form-key="realFormKey"
        :source-id="realFollowSourceId"
      />
    </template>

    <template #transferPopContent>
      <TransferForm ref="transferFormRef" v-model:form="transferForm" class="mt-[16px] w-[320px]" />
    </template>
  </CrmOverviewDrawer>
</template>

<script setup lang="ts">
  import { useMessage } from 'naive-ui';

  import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
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
    cancelOptFollowPlan,
    deleteOpt,
    getOptFollowPlanList,
    getOptFollowRecordList,
    transferOpt,
  } from '@/api/modules/opportunity';
  import { getUserList } from '@/api/modules/system/org';
  import { defaultTransferForm } from '@/config/opportunity';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import { characterLimit } from '@/utils';

  const { openModal } = useModal();

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    sourceId: string;
  }>();

  const showOptOverviewDrawer = defineModel<boolean>('show', {
    required: true,
  });

  // TODO ts类型
  const detail = ref<any>({
    id: '101001',
    name: '商机名称',
    customerName: '客户名称',
  });

  const transferForm = ref<TransferParams>({
    owner: null,
    ids: [],
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
      label: t('common.delete'),
      key: 'delete',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
  ];

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

  const currentStatus = ref<string>('purchasing');
  const workflowList = ref<WorkflowStepItem[]>([
    {
      value: 'new',
      label: '新建',
      isError: false,
    },
    {
      value: 'demandConfirm',
      label: '需求明确',
      isError: false,
    },
    {
      value: 'Solution',
      label: '方案验证',
      isError: false,
    },
    {
      value: 'report',
      label: '立项汇报',
      isError: false,
    },
    {
      value: 'purchasing',
      label: '商务采购',
      isError: false,
    },
    {
      value: 'end',
      label: '完结',
      isError: false,
    },
  ]);

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
        sourceId: detail.value.id,
        current: pageNation.value.current || 1,
        pageSize: pageNation.value.pageSize,
      };

      let res;
      if (activeTab.value === 'followPlan') {
        res = await getOptFollowPlanList({
          ...params,
          status: activeStatus.value,
        });
      } else {
        res = await getOptFollowRecordList(params);
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

  const formDrawerVisible = ref(false);
  const realFormKey = ref<FormDesignKeyEnum>(FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS);

  // 取消计划
  async function handleCancelPlan(item: FollowDetailItem) {
    try {
      await cancelOptFollowPlan(item.id);
      Message.success(t('common.cancelSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  // 编辑跟进内容
  const realFollowSourceId = ref<string | undefined>('');
  function handleEditFollow(item: FollowDetailItem) {
    realFormKey.value =
      activeTab.value === 'followRecord'
        ? FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS
        : FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS;
    realFollowSourceId.value = item.id;
    formDrawerVisible.value = true;
  }

  // 转移
  const transferFormRef = ref<InstanceType<typeof TransferForm>>();
  function handleTransfer() {
    transferFormRef.value?.formRef?.validate(async (error) => {
      if (!error) {
        try {
          transferLoading.value = true;
          await transferOpt({
            ...transferForm.value,
            ids: [props.sourceId],
          });
          Message.success(t('common.transferSuccess'));
          transferForm.value = { ...defaultTransferForm };
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
      title: t('common.deleteConfirmTitle', { name: characterLimit(detail.value.name) }),
      content: t('opportunity.batchDeleteContentTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteOpt(detail.value.id);
          Message.success(t('common.deleteSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  function handleSelect(key: string) {
    switch (key) {
      case 'pop-transfer':
        handleTransfer();
        break;
      case 'delete':
        handleDelete();
        break;
      default:
        break;
    }
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
    () => showOptOverviewDrawer.value,
    (val) => {
      if (val) {
        loadFollowList();
      }
    }
  );
</script>
