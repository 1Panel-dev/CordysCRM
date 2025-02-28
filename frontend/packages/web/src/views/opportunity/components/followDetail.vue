<template>
  <CrmCollapse
    v-if="['overview', 'followRecord', 'followPlan'].includes(props.type)"
    name-key="follow"
    :title="props.type === 'followPlan' ? '' : t('crmFollowRecord.followRecord')"
    :default-expand="props.type !== 'overview'"
    :show-expand-btn="props.type === 'overview'"
  >
    <template v-if="props.type === 'followPlan'" #header="{ collapsed }">
      <CrmTab
        v-if="!collapsed"
        v-model:active-tab="activePlanStatus"
        no-content
        :tab-list="statusTabList"
        type="segment"
        @click.stop
      >
      </CrmTab>
    </template>
    <template #headerExtraLeft="{ collapsed }">
      <CrmSearchInput v-if="!collapsed" v-model:value="followKeyword" class="!w-[240px]" @click.stop />
    </template>
    <CrmFollowRecord
      v-model:data="recordList"
      v-model:keyword="followKeyword"
      virtual-scroll-height="1000px"
      :description="descriptionList"
      key-field="id"
      @reach-bottom="reachBottom"
    >
      <template #headerAction="{ item }">
        <div class="flex items-center gap-[12px]">
          <!-- TOTO 根据时间判断，xxw 状态未结束才可以取消 -->
          <n-button v-if="props.type === 'followPlan'" type="primary" text @click="cancelPlan(item)">
            {{ t('common.cancelPlan') }}
          </n-button>
          <n-button type="primary" text @click="handleEdit(item)">
            {{ t('common.edit') }}
          </n-button>
        </div>
      </template>
    </CrmFollowRecord>
  </CrmCollapse>
</template>

<script lang="ts" setup>
  import { NButton } from 'naive-ui';
  import dayjs from 'dayjs';

  import CrmCollapse from '@/components/pure/crm-collapse/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import type { FollowRecordItem } from '@/components/business/crm-follow-record/index.vue';
  import CrmFollowRecord from '@/components/business/crm-follow-record/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  export type ActiveType = 'followPlan' | 'followRecord' | 'overview';
  const props = defineProps<{
    type: ActiveType;
  }>();

  const recordList = ref<FollowRecordItem[]>([
    {
      id: '111001',
      customerName: 'admin',
      followUser: '下雨',
      methodName: '线上',
      contactsUser: '张三',
      followTime: '72532152523',
      followContent: `上门拜访了客户，客户有意向购买，意向购买，`,
    },
  ]);

  const activePlanStatus = ref<string>('all');
  const followKeyword = ref<string>('');

  // 跟进计划状态
  const statusTabList = ref([
    {
      name: 'all',
      tab: t('common.all'),
    },
    {
      name: 'notStarted',
      tab: t('common.notStarted'),
    },
    {
      name: 'inProgress',
      tab: t('common.inProgress'),
    },
    {
      name: 'completed',
      tab: t('common.completed'),
    },
    {
      name: 'canceled',
      tab: t('common.canceled'),
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

  // TODO  取消计划 类型
  function cancelPlan(item: any) {}

  // 更新记录 类型
  function handleEdit(item: any) {}

  // 触底更新
  function reachBottom() {}
</script>

<style lang="less" scoped></style>
