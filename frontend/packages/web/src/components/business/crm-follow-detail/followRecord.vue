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
                :disabled="!props.getDisabledFun(item) || !!item.converted"
                @change="() => emit('change', item)"
              />
              <CrmTag v-if="item.status && item.converted"> {{ t('common.hasConvertToRecord') }} </CrmTag>
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
                <slot
                  v-if="
                    ['customerName', 'clueName'].includes(ele.key) &&
                    props.type === 'followPlan' &&
                    !props.disabledOpenDetail
                  "
                  name="customerName"
                >
                  <CrmTableButton @click="goDetail(ele.key, item)">
                    {{ ele.value }}
                    <template #trigger> {{ ele.value }} </template>
                  </CrmTableButton>
                </slot>
                <slot v-else :name="ele.key" :desc-item="descItem" :item="item"></slot>
              </template>
            </CrmDetailCard>
          </div>
          <div class="crm-follow-record-content" v-html="item.content.replace(/\n/g, '<br />')"></div>
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
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CustomerFollowPlanListItem, FollowDetailItem } from '@lib/shared/models/customer';

  import type { Description } from '@/components/pure/crm-detail-card/index.vue';
  import CrmDetailCard from '@/components/pure/crm-detail-card/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import StatusTagSelect from './statusTagSelect.vue';

  import useOpenNewPage from '@/hooks/useOpenNewPage';

  import { ClueRouteEnum, CustomerRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();
  const props = defineProps<{
    type: 'followRecord' | 'followPlan';
    keyField: string;
    getDescriptionFun: (item: FollowDetailItem) => Description[];
    getDisabledFun: (item: FollowDetailItem) => boolean;
    virtualScrollHeight: string;
    emptyText?: string;
    disabledOpenDetail?: boolean;
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

  const { openNewPage } = useOpenNewPage();
  function goDetail(key: string, item: FollowDetailItem) {
    if (key === 'clueName') {
      if (item.poolId) {
        openNewPage(ClueRouteEnum.CLUE_MANAGEMENT_POOL, {
          id: item.clueId,
          name: item.clueName,
          poolId: item.poolId,
        });
      } else {
        openNewPage(ClueRouteEnum.CLUE_MANAGEMENT, {
          id: item.clueId,
          transitionType: undefined,
          name: item.clueName,
        });
      }
    } else if (item.poolId) {
      openNewPage(CustomerRouteEnum.CUSTOMER_OPEN_SEA, {
        id: item.customerId,
        poolId: item.poolId,
      });
    } else {
      openNewPage(CustomerRouteEnum.CUSTOMER_INDEX, {
        id: item.customerId,
      });
    }
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
