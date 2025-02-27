<template>
  <CrmDrawer
    v-model:show="showModal"
    width="100%"
    :title="t('module.businessManage.businessCloseRule')"
    :footer="false"
    no-padding
    :show-back="true"
  >
    <div class="relative h-full w-full">
      <CrmSplitPanel :max="0.5" :min="0.2" :default-size="0.2">
        <template #1>
          <div class="opportunity-detail-info"> </div>
        </template>
        <template #2>
          <n-scrollbar :style="{ 'max-height': '100%' }">
            <div class="opportunity-detail">
              <div class="bg-[var(--text-n10)]">
                <div class="relative p-[16px]">
                  <CrmTab v-model:active-tab="activeTab" :tab-list="showTabList" type="line"> </CrmTab>
                  <div class="tab-setting">
                    <CrmTabSetting v-model:cached-list="cachedList" :tab-list="tabList" :setting-key="'settingKey'" />
                  </div>
                  <CrmWorkFlowCard v-model:status="currenStatus" :workflow-list="workflowList">
                    <template #action>
                      <n-button class="mr-[12px]" type="primary" @click="updateStage">
                        {{ t('common.updateToCurrentProgress') }}
                      </n-button>
                    </template>
                  </CrmWorkFlowCard>
                </div>
              </div>
              <CrmCollapse class="mt-[16px]" name-key="headInfo" :title="t('opportunity.headInfo')">
                <BaseInfoList :list="headInfoList" />
              </CrmCollapse>
              <CrmCollapse class="mt-[16px]" name-key="contactInfo" :title="t('opportunity.contactInfo')">
                <BaseInfoList :list="contactInfoList" />
              </CrmCollapse>
              <CrmCollapse class="mt-[16px]" name-key="followRecord" :title="t('crmFollowRecord.followRecord')">
                <template #headerExtraLeft="{ collapsed }">
                  <CrmSearchInput v-if="!collapsed" v-model:value="followKeyword" class="!w-[240px]" @click.stop />
                </template>
                <div>
                  <CrmFollowRecord
                    v-model:data="recordList"
                    v-model:keyword="followKeyword"
                    virtual-scroll-height="1000px"
                    :description="descriptionList"
                    key-field="id"
                  >
                    <template #headerAction>
                      <n-button type="primary" text>
                        {{ t('common.edit') }}
                      </n-button>
                    </template>
                  </CrmFollowRecord>
                </div>
              </CrmCollapse>
            </div>
          </n-scrollbar>
        </template>
      </CrmSplitPanel>
    </div>
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { NButton, NScrollbar, TabPaneProps } from 'naive-ui';
  import dayjs from 'dayjs';

  import CrmCollapse from '@/components/pure/crm-collapse/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import type { FollowRecordItem } from '@/components/business/crm-follow-record/index.vue';
  import CrmFollowRecord from '@/components/business/crm-follow-record/index.vue';
  import CrmTabSetting from '@/components/business/crm-tab-setting/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import CrmWorkFlowCard from '@/components/business/crm-workflow-card/index.vue';
  import BaseInfoList from './baseInfoList.vue';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const showModal = defineModel<boolean>('show', {
    required: true,
  });

  const activeTab = ref('overview');

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
          name: 'xinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxin',
          current: true,
        },
        {
          id: '87780',
          name: 'xinxin1xinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinxinv',
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

  const followKeyword = ref<string>('');
  // TODO 类型 假数据 等待联调
  const recordList = ref<FollowRecordItem[]>([
    {
      id: '111001',
      customerName: 'admin',
      followUser: '下雨',
      methodName: '线上',
      contactsUser: '张三',
      followTime: '72532152523',
      followContent: `上门拜访了客户，客户有意向购买，意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户v比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比上门拜访了客户，客户有意向购买，比`,
    },
  ]);
  // TODO 类型 假数据 等待联调
  const descriptionList: any[] = [
    {
      key: 'path',
      label: '部门',
      value: '架构部',
    },
    {
      key: 'tags',
      label: '区域',
      value: '北区',
    },
    {
      key: 'relatedUser',
      label: '联系人',
      value: '张三',
    },
    {
      key: 'phone',
      label: '手机号',
      value: '689689679579',
    },
    {
      key: 'createdTime',
      label: '创建时间',
      value: dayjs(Date.now()).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      key: 'updateTime',
      label: '更新时间',
      value: dayjs(Date.now()).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      key: 'updateUser',
      label: '更新人',
      value: '王五',
    },
  ];
</script>

<style lang="less" scoped>
  .opportunity-detail {
    padding: 16px;
    background: var(--text-n9);
    .crm-scroll-bar();
    .tab-setting {
      @apply absolute right-0 top-2;
    }
  }
</style>
