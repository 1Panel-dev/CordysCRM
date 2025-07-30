<template>
  <CrmList
    v-if="listData.length"
    v-model:data="listData"
    :virtual-scroll-height="props.virtualScrollHeight"
    :key-field="props.keyField"
    :item-height="100"
    mode="remote"
    @reach-bottom="emit('reachBottom')"
  >
    <template #item="{ item }">
      <div class="crm-follow-record-item">
        <div class="crm-follow-time-line">
          <div :class="`crm-follow-time-dot ${getFutureClass(item)}`"></div>
          <div class="crm-follow-time-line"></div>
        </div>
        <div class="mb-[24px] flex w-full flex-col gap-[16px]">
          <div class="crm-follow-record-title h-[32px]">
            <div class="flex items-center gap-[16px]">
              <StatusTagSelect
                v-if="item.status"
                v-model:status="item.status"
                :disabled="!props.getDisabledFun(item)"
                @change="() => emit('change', item)"
              />
              <div class="text-[var(--text-n1)]">{{ getShowTime(item) }}</div>
              <div class="crm-follow-record-method">
                {{ (props.type === 'followRecord' ? item.followMethod : item.method) ?? '-' }}
              </div>
            </div>

            <slot name="headerAction" :item="item"></slot>
          </div>

          <div class="crm-follow-record-base-info">
            <CrmDetailCard :description="props.getDescriptionFun(item)">
              <!-- TODO 先不要了 xinxin -->
              <!-- <template #prefix>
                <div class="flex items-center gap-[8px]">
                  <CrmAvatar :is-user="item.owner === userStore.userInfo.id" :size="24" :word="item.ownerName" />
                  <n-tooltip :delay="300">
                    <template #trigger>
                      <div class="one-line-text max-w-[300px]">{{ item.ownerName }} </div>
                    </template>
                    {{ item.ownerName || '-' }}
                  </n-tooltip>
                </div>
              </template> -->
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
  import dayjs from 'dayjs';

  import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
  import type { CustomerFollowPlanListItem, FollowDetailItem } from '@lib/shared/models/customer';

  import type { Description } from '@/components/pure/crm-detail-card/index.vue';
  import CrmDetailCard from '@/components/pure/crm-detail-card/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import StatusTagSelect from './statusTagSelect.vue';

  const props = defineProps<{
    type: 'followRecord' | 'followPlan';
    keyField: string;
    getDescriptionFun: (item: FollowDetailItem) => Description[];
    getDisabledFun: (item: FollowDetailItem) => boolean;
    virtualScrollHeight: string;
    emptyText?: string;
  }>();

  const emit = defineEmits<{
    (e: 'reachBottom'): void;
    (e: 'change', item: FollowDetailItem): void;
  }>();

  const listData = defineModel<FollowDetailItem[]>('data', {
    default: [],
  });

  function getFutureClass(item: FollowDetailItem) {
    if (props.type === 'followPlan') {
      const isNotFuture = [CustomerFollowPlanStatusEnum.CANCELLED, CustomerFollowPlanStatusEnum.COMPLETED].includes(
        (item as CustomerFollowPlanListItem).status
      );
      return isNotFuture ? '' : 'crm-follow-dot-future';
    }

    return new Date(item.followTime).getTime() > Date.now() ? 'crm-follow-dot-future' : '';
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
