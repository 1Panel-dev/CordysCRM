<template>
  <CrmList
    v-if="filteredData.length"
    v-model:data="filteredData"
    :virtual-scroll-height="props.virtualScrollHeight"
    :key-field="props.keyField"
    :item-height="100"
    @reach-bottom="emit('reachBottom')"
  >
    <template #item="{ item }">
      <div class="crm-follow-record-item mr-[8px]">
        <div class="crm-follow-time-line">
          <div :class="`crm-follow-time-dot ${getFutureClass(item)}`"></div>
          <div class="crm-follow-time-line"></div>
        </div>
        <div class="mb-4 flex w-full flex-col gap-[16px]">
          <div class="crm-follow-record-title">
            <div class="flex items-center gap-[16px]">
              <!-- TODO 标签状态不确定有入口 xxw -->
              <div class="text-[var(--text-n1)]">{{ dayjs(item.followTime).format('YYYY-MM-DD HH:mm:ss') }}</div>
              <div class="crm-follow-record-method">{{ item.methodName }}</div>
            </div>

            <slot name="headerAction" :item="item"></slot>
          </div>

          <div class="crm-follow-record-base-info">
            <CrmDetailCard :description="description">
              <template #prefix>
                <div class="flex items-center gap-[8px]"> <CrmAvatar :size="24" /> {{ item.customerName }}</div>
              </template>
              <template v-for="ele in description" :key="ele.key" #[ele.key]="{ value }">
                <slot :name="ele.key" :value="value"></slot>
              </template>
            </CrmDetailCard>
          </div>
          <div class="crm-follow-record-content">{{ item.followContent }}</div>
        </div>
      </div>
    </template>
  </CrmList>
  <div v-else class="w-full bg-[var(--text-n9)] p-[16px] text-[var(--text-n4)]">
    {{ t('crmFollowRecord.noFollowRecord') }}
  </div>
</template>

<script setup lang="ts">
  import dayjs from 'dayjs';

  import type { Description } from '@/components/pure/crm-detail-card/index.vue';
  import CrmDetailCard from '@/components/pure/crm-detail-card/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  import useHighlight from './useHighlight';

  const { t } = useI18n();

  // TODO 类型
  export interface FollowRecordItem {
    id?: string;
    customerName: string; // 客户姓名
    followTime: string; // 跟进时间
    followUser: string; // 跟进人
    methodName: string; // 跟进方法
    contactsUser: string; // 联系人
    followContent: string; // 跟进内容
  }

  const props = defineProps<{
    keyField: string;
    description: Description[];
    virtualScrollHeight: string;
  }>();

  const emit = defineEmits<{
    (e: 'reachBottom'): void;
  }>();

  const listData = defineModel<FollowRecordItem[]>('data', {
    default: [],
  });

  const innerKeyWord = defineModel<string>('keyword', {
    default: '',
  });

  const { searchKeyword, highlightContent, resetHighlight } = useHighlight([
    '.crm-follow-record-content',
    '.crm-follow-record-method',
  ]);

  const filteredData = computed(() => {
    if (!searchKeyword.value) return listData.value;
    return listData.value.filter((item) => {
      return Object.values(item).some((value) => {
        if (typeof value === 'string') {
          return value.toLowerCase().includes(searchKeyword.value.toLowerCase());
        }
        return false;
      });
    });
  });

  watch(
    () => innerKeyWord.value,
    (val) => {
      searchKeyword.value = val;
    }
  );

  watch(
    () => searchKeyword.value,
    (val) => {
      if (val) {
        highlightContent();
      } else {
        resetHighlight();
      }
    }
  );

  function getFutureClass(item: FollowRecordItem) {
    const currentTime = new Date();
    const followDate = new Date(item.followTime);
    const isFuture = followDate.getTime() > currentTime.getTime();
    return isFuture ? 'crm-follow-dot-future' : '';
  }
</script>

<style scoped lang="less">
  .crm-follow-record-item {
    @apply flex gap-4;
    .crm-follow-time-line {
      padding-top: 12px;
      width: 8px;

      @apply flex flex-col items-center justify-center gap-2;
      .crm-follow-time-dot {
        width: 8px;
        height: 8px;
        border: 2px solid var(--text-n7);
        border-radius: 50%;
        flex-shrink: 0;
        &.crm-follow-dot-future {
          border-color: var(--primary-8);
        }
      }
      .crm-follow-time-line {
        width: 2px;
        background: var(--text-n8);
        @apply h-full;
      }
    }
    .crm-follow-record-title {
      @apply flex items-center justify-between gap-4;
      .crm-follow-record-method {
        color: var(--text-n1);
        @apply font-medium;
      }
    }
    .crm-follow-record-content {
      padding: 12px;
      border-radius: var(--border-radius-small);
      background: var(--text-n9);
    }
  }
</style>
