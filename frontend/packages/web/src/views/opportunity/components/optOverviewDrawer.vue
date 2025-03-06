<template>
  <CrmOverviewDrawer
    v-model:show="showOptOverviewDrawer"
    v-model:active-tab="activeTab"
    v-model:cached-list="cachedList"
    :tab-list="tabList"
    :button-list="buttonList"
    :title="detail.name"
    :subtitle="detail.customerName"
    @button-select="handleSelect"
  >
    <template #right>
      <CrmWorkflowCard
        v-if="activeTab === 'overview'"
        v-model:status="currentStatus"
        :show-confirm-status="true"
        :workflow-list="workflowList"
        :source-id="detail.id"
      />

      <CrmCollapse
        v-if="activeTab === 'overview'"
        class="my-[16px]"
        name-key="followRecord"
        :title="t('crmFollowRecord.followRecord')"
      >
        <template #headerExtraLeft="{ collapsed }">
          <CrmSearchInput v-if="!collapsed" v-model:value="followRecordKeyword" class="!w-[240px]" @click.stop />
        </template>
        <FollowDetail
          v-model:data="followList"
          v-model:active-status="activeStatus"
          v-model:keyword="followRecordKeyword"
          :no-padding="true"
          :show-search-input="false"
          :type="activeTab"
          :loading="followLoading"
        />
      </CrmCollapse>

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
      />

      <HeaderTable
        v-if="activeTab === 'headRecord'"
        class="mt-[16px] h-[calc(100vh-161px)]"
        :source-id="detail.id"
        :load-list-api="getUserList"
      />

      <TransferModal
        v-model:show="showTransferModal"
        :module-type="ModuleConfigEnum.BUSINESS_MANAGEMENT"
        :is-batch="isBatchTransfer"
        :source-ids="[detail.id]"
      />
    </template>
  </CrmOverviewDrawer>
</template>

<script setup lang="ts">
  import { useMessage } from 'naive-ui';

  import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import type { FollowDetailItem } from '@lib/shared/models/customer';
  import type { WorkflowStepItem } from '@lib/shared/models/opportunity';

  import CrmCollapse from '@/components/pure/crm-collapse/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import FollowDetail from '@/components/business/crm-follow-detail/index.vue';
  import HeaderTable from '@/components/business/crm-form-create-table/headerTable.vue';
  import CrmOverviewDrawer from '@/components/business/crm-overview-drawer/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import TransferModal from '@/components/business/crm-transfer-modal/index.vue';
  import CrmWorkflowCard from '@/components/business/crm-workflow-card/index.vue';

  import { getOptFollowPlanList, getOptFollowRecordList } from '@/api/modules/opportunity';
  import { getUserList } from '@/api/modules/system/org';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import { characterLimit } from '@/utils';

  const { openModal } = useModal();

  const { t } = useI18n();
  const Message = useMessage();

  const showOptOverviewDrawer = defineModel<boolean>('show', {
    required: true,
  });

  // TODO ts类型
  const detail = ref<any>({
    id: '101001',
    name: '商机名称',
    customerName: '客户名称',
  });

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
    },
    {
      label: t('common.delete'),
      key: 'transfer',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
  ];

  const activeTab = ref('overview');
  const cachedList = ref([]);
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

  const followRecordKeyword = ref<string>('');
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

  // 编辑
  function handleEdit() {}

  // 跟进计划
  const showFollowPlanDrawer = ref<boolean>(false);
  function handleFollowPlan() {
    showFollowPlanDrawer.value = true;
  }

  // 跟进记录
  const showFollowRecordDrawer = ref(false);
  function handleFollowRecord() {
    showFollowRecordDrawer.value = true;
  }

  // 转移
  const showTransferModal = ref<boolean>(false);
  const isBatchTransfer = ref<boolean>(true);
  function handleTransfer() {
    isBatchTransfer.value = false;
    showTransferModal.value = true;
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
          // TODO
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
      case 'edit':
        handleEdit();
        break;
      case 'followPlan':
        handleFollowPlan();
        break;
      case 'followRecord':
        handleFollowRecord();
        break;
      case 'transfer':
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
        // loadFollowList();
      }
    }
  );

  watch(
    () => showOptOverviewDrawer.value,
    (val) => {
      if (val) {
        // loadFollowList();
      }
    }
  );
</script>
