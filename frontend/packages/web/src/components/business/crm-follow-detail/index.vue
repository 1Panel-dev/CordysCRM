<template>
  <div :class="`crm-follow-detail p-[24px] ${props.wrapperClass}`">
    <div class="mb-[16px] flex items-center justify-between">
      <div v-if="props.activeType === 'followRecord'" class="font-medium text-[var(--text-n1)]">
        {{ t('crmFollowRecord.followRecord') }}
      </div>
      <CrmTab
        v-if="props.activeType === 'followPlan'"
        v-model:active-tab="activeStatus"
        no-content
        :tab-list="statusTabList"
        type="segment"
        @change="() => loadFollowList()"
      >
      </CrmTab>
      <CrmSearchInput
        v-model:value="followKeyword"
        :placeholder="t('common.byKeywordSearch')"
        class="!w-[240px]"
        @search="(val) => searchData(val)"
      />
    </div>
    <n-spin :show="loading" class="h-full">
      <FollowRecord
        v-model:data="data"
        v-model:keyword="followKeyword"
        :virtual-scroll-height="`${props.virtualScrollHeight || '1000px'}`"
        :get-description-fun="getDescriptionFun"
        key-field="id"
        :type="props.activeType"
        :empty-text="
          props.activeType === 'followPlan' ? t('crmFollowRecord.noFollowPlan') : t('crmFollowRecord.noFollowRecord')
        "
        @reach-bottom="handleReachBottom"
      >
        <template #headerAction="{ item }">
          <div v-if="props.showAction" class="flex items-center gap-[12px]">
            <n-button
              v-if="props.activeType === 'followPlan' && item.status !== CustomerFollowPlanStatusEnum.CANCELLED"
              type="primary"
              text
              @click="handleCancelPlan(item)"
            >
              {{ t('common.cancelPlan') }}
            </n-button>
            <n-button
              v-if="
                props.activeType === 'followRecord' ||
                (props.activeType === 'followPlan' &&
                  ![CustomerFollowPlanStatusEnum.CANCELLED, CustomerFollowPlanStatusEnum.CANCELLED].includes(
                    item.status
                  ))
              "
              type="primary"
              text
              @click="handleEdit(item)"
            >
              {{ t('common.edit') }}
            </n-button>
            <n-button type="error" text @click="handleDelete(item)">
              {{ t('common.delete') }}
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
    <CrmFormCreateDrawer
      v-model:visible="formDrawerVisible"
      :form-key="realFormKey"
      :source-id="realFollowSourceId"
      need-init-detail
      @saved="() => loadFollowList()"
    />
  </div>
</template>

<script lang="ts" setup>
  import { NButton, NSpin, TabPaneProps } from 'naive-ui';
  import dayjs from 'dayjs';

  import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import type { FollowDetailItem } from '@lib/shared/models/customer';

  import type { Description } from '@/components/pure/crm-detail-card/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import FollowRecord from './followRecord.vue';

  import { useI18n } from '@/hooks/useI18n';

  import useFollowApi, { type followEnumType } from './useFollowApi';

  const { t } = useI18n();

  export type ActiveType = 'followPlan' | 'followRecord';

  interface FollowDetailProps {
    activeType: 'followRecord' | 'followPlan'; // 跟进记录|跟进计划
    followApiKey: followEnumType; // 跟进计划apiKey
    virtualScrollHeight?: string; // 虚拟高度
    wrapperClass?: string;
    sourceId: string; // 资源id
    refreshKey: number;
    showAction?: boolean; // 显示操作
  }

  const props = withDefaults(defineProps<FollowDetailProps>(), {
    showAction: true,
  });

  const formDrawerVisible = ref(false);

  const {
    data,
    loading,
    handleReachBottom,
    searchData,
    activeStatus,
    loadFollowList,
    handleCancelPlan,
    followKeyword,
    followFormKeyMap,
    handleDelete,
    getApiKey,
    initFollowFormConfig,
  } = useFollowApi({
    type: toRef(props, 'activeType'),
    followApiKey: props.followApiKey,
    sourceId: toRef(props, 'sourceId'),
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
      key: 'createUserName',
      label: t('common.creator'),
      value: 'createUserName',
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

  // 编辑记录或计划
  const realFormKey = ref<FormDesignKeyEnum>(FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER);
  const realFollowSourceId = ref<string | undefined>('');

  function handleEdit(item: FollowDetailItem) {
    realFormKey.value = followFormKeyMap[getApiKey(item) as keyof typeof followFormKeyMap]?.[
      props.activeType
    ] as FormDesignKeyEnum;
    realFollowSourceId.value = item.id;
    formDrawerVisible.value = true;
  }

  onBeforeMount(() => {
    initFollowFormConfig();
    loadFollowList();
  });

  watch(
    () => props.refreshKey,
    (val) => {
      if (val) {
        loadFollowList();
      }
    }
  );
</script>

<style lang="less" scoped>
  .crm-follow-detail {
    @apply overflow-hidden;

    border-radius: @border-radius-medium;
    background: var(--text-n10);
  }
</style>
