<template>
  <div class="flex gap-[16px] overflow-hidden">
    <div class="flex flex-col items-center gap-[4px]">
      <div class="flex h-[22px] w-[8px] items-center">
        <div class="h-[8px] w-full rounded-[999px] border border-[var(--primary-8)]"></div>
      </div>
      <div class="flex flex-1 justify-center">
        <div class="h-full w-[1px] bg-[var(--primary-8)]"></div>
      </div>
    </div>
    <div class="flex flex-1 flex-col gap-[8px] overflow-hidden">
      <div class="flex h-[24px] items-center justify-between gap-[16px]">
        <div class="flex items-center gap-[8px]">
          <div>{{ getShowTime(item) }} </div>
          <div class="text-[14px] font-semibold text-[var(--text-n1)]">
            {{ (!isPlan ? item.followMethod : item.method) ?? '-' }}
          </div>
        </div>
        <div v-if="isPlan" class="flex items-center">
          <van-dropdown-menu class="status-select-menu" @click.stop>
            <van-dropdown-item
              v-model="status"
              :disabled="props.readonly"
              :options="statusOptions"
              @change="changeStatus"
            />
          </van-dropdown-menu>
        </div>
      </div>
      <div class="flex flex-1 flex-col gap-[12px] rounded-[var(--border-radius-large)] bg-[var(--text-n10)] p-[16px]">
        <div class="flex items-center justify-between gap-[16px]">
          <CrmAvatar :is-word="item.owner !== userStore.userInfo.id" :text="item.ownerName" />
          <div class="flex flex-1 flex-wrap items-center overflow-hidden">
            <div class="one-line-text flex-1 text-[16px] font-semibold">{{ item.ownerName }}</div>
            <div v-if="!props.readonly" class="flex items-center gap-[16px]">
              <CrmTextButton icon="iconicon_delete" color="var(--error-red)" icon-size="16px" @click="emit('delete')" />
              <CrmTextButton
                v-if="!isPlan || (isPlan && item.status !== CustomerFollowPlanStatusEnum.CANCELLED)"
                icon="iconicon_handwritten_signature"
                color="var(--primary-8)"
                icon-size="16px"
                @click="emit('edit')"
              />
            </div>
          </div>
        </div>
        <div class="one-line-text rounded-[var(--border-radius-mini)] bg-[var(--text-n9)] p-[12px] text-[12px]">
          {{ item.content }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import dayjs from 'dayjs';

  import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FollowDetailItem, StatusTagKey } from '@lib/shared/models/customer';

  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';

  import { statusMap } from '@/config/follow';
  import useUserStore from '@/store/modules/user';

  const props = defineProps<{
    item: any;
    type: 'plan' | 'record';
    readonly?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'edit'): void;
    (e: 'delete'): void;
    (e: 'change'): void;
  }>();

  const { t } = useI18n();
  const userStore = useUserStore();

  const status = defineModel<StatusTagKey>('value', {
    default: CustomerFollowPlanStatusEnum.PREPARED,
  });

  const isPlan = computed(() => {
    return props.type === 'plan';
  });

  function getShowTime(item: FollowDetailItem) {
    const time = 'estimatedTime' in item ? item.estimatedTime : item.followTime;
    return time ? dayjs(time).format('YYYY-MM-DD') : '-';
  }

  const statusOptions = computed(() =>
    Object.values(statusMap).map((e) => ({
      text: t(e.label),
      value: e.value,
    }))
  );

  function changeStatus() {
    emit('change');
  }
</script>

<style lang="less">
  .status-select-menu {
    .van-dropdown-menu__bar {
      padding-right: 16px;
      height: 24px !important;
      border-radius: 4px !important;
      box-shadow: none !important;
      .van-dropdown-menu__title {
        .van-ellipsis {
          font-size: 14px !important;
        }
      }
    }
  }
</style>
