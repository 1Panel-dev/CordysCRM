<template>
  <div :class="`crm-follow-detail ${props.noPadding ? '' : 'p-[24px]'} ${props.wrapperClass}`">
    <div class="mb-[16px] flex items-center justify-between">
      <div v-if="props.showTitle" class="font-medium text-[var(--text-n1)]">
        {{ t('crmFollowRecord.followRecord') }}
      </div>
      <CrmTab
        v-if="props.type === 'followPlan'"
        v-model:active-tab="activeStatus"
        no-content
        :tab-list="statusTabList"
        type="segment"
        @change="() => emit('search')"
      >
      </CrmTab>
      <CrmSearchInput
        v-if="props.showSearchInput"
        v-model:value="followKeyword"
        :placeholder="t('common.byKeywordSearch')"
        class="!w-[240px]"
        @search="() => emit('search')"
      />
    </div>
    <n-spin :show="props.loading" class="h-full">
      <FollowRecord
        v-model:data="data"
        v-model:keyword="followKeyword"
        :virtual-scroll-height="`${props.virtualScrollHeight || '1000px'}`"
        :get-description-fun="getDescriptionFun"
        key-field="id"
        :empty-text="
          props.type === 'followPlan' ? t('crmFollowRecord.noFollowPlan') : t('crmFollowRecord.noFollowRecord')
        "
        @reach-bottom="() => emit('reachBottom')"
      >
        <template #headerAction="{ item }">
          <div class="flex items-center gap-[12px]">
            <n-button
              v-if="props.type === 'followPlan' && item.status !== CustomerFollowPlanStatusEnum.CANCELLED"
              type="primary"
              text
              @click="cancelPlan(item)"
            >
              {{ t('common.cancelPlan') }}
            </n-button>
            <n-button type="primary" text @click="handleEdit(item)">
              {{ t('common.edit') }}
            </n-button>
          </div>
        </template>
        <template #createTime="{ descItem }">
          <div class="flex items-center gap-[8px]">
            {{ dayjs(descItem.value).format('YYYY-MM-DD HH:mm:ss') }}
          </div>
        </template>
        <template #updateTime="{ descItem }">
          <div class="flex items-center gap-[8px]">
            {{ dayjs(descItem.value).format('YYYY-MM-DD HH:mm:ss') }}
          </div>
        </template>
      </FollowRecord>
    </n-spin>
  </div>
</template>

<script lang="ts" setup>
  import { NButton, NSpin, TabPaneProps } from 'naive-ui';
  import dayjs from 'dayjs';

  import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
  import type { FollowDetailItem } from '@lib/shared/models/customer';

  import type { Description } from '@/components/pure/crm-detail-card/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import FollowRecord from './followRecord.vue';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  export type ActiveType = 'followPlan' | 'followRecord';

  interface FollowDetailProps {
    type: string; // 跟进记录|跟进计划
    showSearchInput?: boolean; // 是否显示检索框
    showTitle?: boolean; // 是否显示标题
    noPadding?: boolean; // 无边距
    virtualScrollHeight?: string; // 虚拟高度
    wrapperClass?: string;
    loading: boolean;
  }

  const props = withDefaults(defineProps<FollowDetailProps>(), {
    showSearchInput: true,
    noPadding: false,
  });

  const emit = defineEmits<{
    (e: 'search'): void;
    (e: 'handleEdit', item: FollowDetailItem): void;
    (e: 'cancelPlan', item: FollowDetailItem): void;
    (e: 'reachBottom'): void;
  }>();

  const data = defineModel<FollowDetailItem[]>('data', {
    required: true,
    default: [],
  });

  const activeStatus = defineModel<string | number>('activeStatus', {
    default: '',
  });

  const innerKeyword = defineModel<string>('keyword', {
    default: '',
  });

  // 跟进计划状态
  const statusTabList = ref<TabPaneProps[]>([
    {
      name: CustomerFollowPlanStatusEnum.ALL,
      tab: t('common.all'),
    },
    {
      name: CustomerFollowPlanStatusEnum.PREPARED,
      tab: t('common.notStarted'),
    },
    {
      name: CustomerFollowPlanStatusEnum.UNDERWAY,
      tab: t('common.inProgress'),
    },
    {
      name: CustomerFollowPlanStatusEnum.COMPLETED,
      tab: t('common.completed'),
    },
    {
      name: CustomerFollowPlanStatusEnum.CANCELLED,
      tab: t('common.canceled'),
    },
  ]);

  // TODO 统一处理
  const descriptionList: Description[] = [
    {
      key: 'department',
      label: t('org.department'),
      value: 'department',
    },
    {
      key: 'contactName',
      label: t('common.contact'),
      value: 'contactName',
    },
    {
      key: 'phone',
      label: t('common.phoneNumber'),
      value: 'phone',
    },
    {
      key: 'createTime',
      label: t('common.createTime'),
      value: 'createTime',
    },
    {
      key: 'updateTime',
      label: t('common.updateTime'),
      value: 'updateTime',
    },
    {
      key: 'updateUser',
      label: t('common.updateUserName'),
      value: 'updateUserName',
    },
  ];

  // TODO 统一处理部分自定义字段
  function getDescriptionFun(item: FollowDetailItem) {
    return (descriptionList.map((desc: Description) => ({
      ...desc,
      value: item[desc.key as keyof FollowDetailItem],
    })) || []) as Description[];
  }

  // 取消计划
  function cancelPlan(item: FollowDetailItem) {
    emit('cancelPlan', item);
  }

  // 编辑记录
  function handleEdit(item: FollowDetailItem) {
    emit('handleEdit', item);
  }

  const followKeyword = ref<string>('');

  watch(
    () => innerKeyword.value,
    (val) => {
      if (val && !props.showSearchInput) {
        followKeyword.value = val;
      }
    }
  );
</script>

<style lang="less" scoped>
  .crm-follow-detail {
    border-radius: @border-radius-medium;
    background: var(--text-n10);
  }
</style>
