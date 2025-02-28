<template>
  <CrmDrawer v-model:show="showModal" width="100%" :footer="false" no-padding :show-back="true">
    <template #title>
      <n-tooltip trigger="hover" :delay="300" :disabled="!detail.name">
        <template #trigger>
          <div class="flex flex-nowrap items-center gap-[8px]">
            <div class="one-line-text font-medium text-[var(--text-n1)]">{{ detail.name }}</div>
            <div class="one-line-text font-medium text-[var(--text-n4)]">({{ detail.customerName }})</div>
          </div>
        </template>
        {{ `${detail.name}(${detail.customerName})` }}
      </n-tooltip>
    </template>
    <template #titleRight>
      <div class="flex items-center gap-[12px]">
        <n-button type="primary" ghost class="n-btn-outline-primary" @click="handleEdit">
          {{ t('common.edit') }}
        </n-button>
        <n-button type="primary" ghost class="n-btn-outline-primary" @click="handleFollowPlan">
          {{ t('common.followPlan') }}
        </n-button>
        <n-button type="primary" ghost class="n-btn-outline-primary" @click="handleFollowRecord">
          {{ t('crmFollowRecord.followRecord') }}
        </n-button>
        <n-button type="primary" ghost class="n-btn-outline-primary" @click="handleTransfer">
          {{ t('common.transfer') }}
        </n-button>
        <n-button type="primary" ghost class="n-btn-outline-primary" @click="handleDelete">
          {{ t('common.delete') }}
        </n-button>
      </div>
    </template>
    <div class="relative h-full w-full">
      <CrmSplitPanel :max="0.5" :min="0.2" :default-size="0.2">
        <template #1>
          <div class="opportunity-detail-info"> </div>
        </template>
        <template #2>
          <div class="opportunity-detail">
            <n-scrollbar :style="{ 'max-height': 'calc(100vh - 65px)' }">
              <div class="bg-[var(--text-n10)]">
                <div class="relative">
                  <CrmTab v-model:active-tab="activeTab" no-content :tab-list="showTabList" type="line"> </CrmTab>
                  <div class="tab-setting">
                    <CrmTabSetting v-model:cached-list="cachedList" :tab-list="tabList" :setting-key="'settingKey'" />
                  </div>
                  <div v-if="activeTab === 'overview'" class="bg-[var(--text-n10)] p-[16px]">
                    <CrmWorkFlowCard v-model:status="currenStatus" :workflow-list="workflowList">
                      <template #action>
                        <n-button class="mr-[12px]" type="primary" @click="updateStage">
                          {{ t('common.updateToCurrentProgress') }}
                        </n-button>
                      </template>
                    </CrmWorkFlowCard>
                  </div>
                </div>
              </div>
              <div class="mt-[16px]">
                <CrmCollapse v-if="activeTab === 'overview'" name-key="headInfo" :title="t('opportunity.headInfo')">
                  <BaseInfoList :list="headInfoList" />
                </CrmCollapse>
              </div>
              <div class="my-[16px]">
                <CrmCollapse
                  v-if="activeTab === 'overview'"
                  name-key="contactInfo"
                  :title="t('opportunity.contactInfo')"
                >
                  <BaseInfoList :list="contactInfoList" />
                </CrmCollapse>
              </div>
              <FollowDetail :type="(activeTab as ActiveType)" />
              <div v-if="activeTab === 'headRecord'" class="h-[calc(100vh-162px)]">
                <HeadList />
              </div>
            </n-scrollbar>
          </div>
        </template>
      </CrmSplitPanel>

      <TransferModal
        v-model:show="showTransferModal"
        :module-type="ModuleConfigEnum.BUSINESS_MANAGEMENT"
        :is-batch="isBatchTransfer"
        :opt-ids="optIds"
      />
    </div>
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { NButton, NScrollbar, NTooltip, TabPaneProps, useMessage } from 'naive-ui';

  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';

  import CrmCollapse from '@/components/pure/crm-collapse/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmTabSetting from '@/components/business/crm-tab-setting/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import CrmWorkFlowCard from '@/components/business/crm-workflow-card/index.vue';
  import BaseInfoList from './baseInfoList.vue';
  import FollowDetail from './followDetail.vue';
  import HeadList from './headList.vue';
  import TransferModal from './transferModal.vue';

  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import { characterLimit } from '@/utils';

  import type { ActiveType } from './followDetail.vue';

  const { openModal } = useModal();

  const { t } = useI18n();
  const Message = useMessage();

  const showModal = defineModel<boolean>('show', {
    required: true,
  });

  const activeTab = ref('overview');

  // TODO ts类型
  const detail = ref<any>({
    name: '商机名称',
    customerName: '客户名称',
  });

  const cachedList = ref([]);

  const tabList = ref<TabContentItem[]>([
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
  ]);

  const showTabList = computed(() => {
    return cachedList.value.reduce((acc: TabPaneProps[], e: TabContentItem) => {
      if (e.enable) {
        acc.push({ name: e.name, tab: e.tab });
      }
      return acc;
    }, []);
  });

  const currenStatus = ref('end');
  // TODO 类型 假数据 等待联调
  const workflowList = ref([
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

  // 更新阶段
  function updateStage() {}

  // TODO 类型 假数据 等待联调
  const headInfoList = ref<any[]>([
    {
      name: '负责人',
      id: 'head',
      children: [
        {
          id: '87780',
          name: 'xinxinx',
          current: true,
        },
        {
          id: '87780',
          name: 'xinxin1xinxinx',
          current: false,
        },
        {
          id: '87780',
          name: 'xinxin2',
          current: false,
        },
      ],
    },
    {
      name: '部门',
      id: 'dep',
      children: [
        {
          id: '135',
          name: '架构部',
        },
        {
          id: '136',
          name: '架构部',
        },
        {
          id: '137',
          name: '架构部',
        },
      ],
    },
    {
      name: '区域',
      id: 'area',
      children: [
        {
          id: '1371',
          name: '南区',
        },
        {
          id: '1372',
          name: '南区',
        },
        {
          id: '1373',
          name: '南区',
        },
      ],
    },
    {
      name: '所属公海',
      id: 'openSea',
      children: [
        {
          id: '1372111',
          name: '南区公海',
        },
        {
          id: '137211312411',
          name: '南区公海',
        },
        {
          id: '13721113154135135',
          name: '南区公海',
        },
      ],
    },
    {
      name: '归属天数',
      id: 'belongDays',
      children: [
        {
          id: '1372111315413513511e',
          name: '18011',
        },
        {
          id: '137e51e35',
          name: '18011',
        },
        {
          id: '137211e135135',
          name: '18011',
        },
      ],
    },
    {
      name: '剩余天数',
      id: 'remainDays',
      children: [
        {
          id: '125234',
          name: '180',
        },
        {
          id: '1252341245235',
          name: '180',
        },
        {
          id: '125232352354',
          name: '180',
        },
      ],
    },
  ]);
  // TODO 类型 假数据 等待联调
  const contactInfoList = ref([
    {
      name: '姓名',
      id: 'name',
      children: [
        {
          id: '87780',
          name: 'xinxin',
        },
      ],
    },
    {
      name: '手机号',
      id: 'phone',
      children: [
        {
          id: '135',
          name: '2141352135623626',
        },
      ],
    },
    {
      name: '邮箱',
      id: 'email',
      children: [
        {
          id: '135',
          name: '2141352135623626',
        },
      ],
    },
  ]);

  // 编辑
  function handleEdit() {}

  // 跟进计划
  function handleFollowPlan() {}

  // 跟进记录
  function handleFollowRecord() {}

  const showTransferModal = ref<boolean>(false);

  const isBatchTransfer = ref<boolean>(true);

  const optIds = ref<string[]>([]);
  // 转移
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
</script>

<style lang="less" scoped>
  .opportunity-detail {
    padding: 16px;
    height: calc(100vh - 66px);
    border-radius: var(--border-radius-large);
    background: var(--text-n9);
    .tab-setting {
      @apply absolute right-4 top-2;
    }
  }
</style>
