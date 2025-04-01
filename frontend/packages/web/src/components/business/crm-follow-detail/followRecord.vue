<template>
  <CrmList
    v-if="filteredData.length"
    v-model:data="filteredData"
    :virtual-scroll-height="props.virtualScrollHeight"
    :key-field="props.keyField"
    :item-height="100"
    mode="remote"
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
              <StatusTag v-if="item.status" :status="item.status" />
              <div class="text-[var(--text-n1)]">{{ getShowTime(item) }}</div>
              <div class="crm-follow-record-method">
                {{ (props.type === 'followRecord' ? item.recordMethod : item.planMethod) ?? '-' }}
              </div>
            </div>

            <slot name="headerAction" :item="item"></slot>
          </div>

          <div class="crm-follow-record-base-info">
            <CrmDetailCard :description="props.getDescriptionFun(item)">
              <template #prefix>
                <div class="flex items-center gap-[8px]">
                  <CrmAvatar :size="24" :word="item.ownerName" />
                  <n-tooltip :delay="300">
                    <template #trigger>
                      <div class="one-line-text max-w-[300px]">{{ item.ownerName }} </div>
                    </template>
                    {{ item.ownerName }}
                  </n-tooltip>
                </div>
              </template>
              <template v-for="ele in props.getDescriptionFun(item)" :key="ele.key" #[ele.key]="{ item: descItem }">
                <slot :name="ele.key" :desc-item="descItem" :item="item"></slot>
              </template>
            </CrmDetailCard>
          </div>
          <div class="crm-follow-record-content">{{ item.content }}</div>
        </div>
      </div>
    </template>
  </CrmList>
  <div v-else class="w-full p-[16px] text-center text-[var(--text-n4)]">
    {{ props.emptyText }}
  </div>
</template>

<script setup lang="ts">
  import { NTooltip } from 'naive-ui';
  import dayjs from 'dayjs';

  import type { FollowDetailItem } from '@lib/shared/models/customer';

  import type { Description } from '@/components/pure/crm-detail-card/index.vue';
  import CrmDetailCard from '@/components/pure/crm-detail-card/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';
  import StatusTag from './statusTag.vue';

  import useHighlight from './useHighlight';

  const props = defineProps<{
    type: 'followRecord' | 'followPlan';
    keyField: string;
    getDescriptionFun: (item: FollowDetailItem) => Description[];
    virtualScrollHeight: string;
    emptyText?: string;
  }>();

  const emit = defineEmits<{
    (e: 'reachBottom'): void;
  }>();

  const listData = defineModel<FollowDetailItem[]>('data', {
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
    const keyword = searchKeyword.value.toLowerCase();
    return listData.value.filter((item) =>
      Object.values(item).some((value) => typeof value === 'string' && value.toLowerCase().includes(keyword))
    );
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

  function getFutureClass(item: FollowDetailItem) {
    const time = 'estimatedTime' in item ? item.estimatedTime : item.followTime;
    return new Date(time).valueOf() > Date.now() ? 'crm-follow-dot-future' : '';
  }

  function getShowTime(item: FollowDetailItem) {
    const time = 'estimatedTime' in item ? item.estimatedTime : item.followTime;
    return time ? dayjs(time).format('YYYY-MM-DD') : '-';
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
