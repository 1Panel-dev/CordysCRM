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
        <div class="flex h-[24px] items-center gap-[8px]">
          <CrmTag
            v-if="item.converted && isPlan"
            plain
            :tag="t('common.hasConvertToRecord')"
            text-color="var(--primary-8)"
            color="var(--primary-8)"
          />
          <div>{{ getShowTime(item) }} </div>
          <div class="text-[14px] font-semibold text-[var(--text-n1)]">
            {{ (!isPlan ? item.followMethod : item.method) ?? '-' }}
          </div>
        </div>
        <div v-if="isPlan" class="flex items-center">
          <van-dropdown-menu class="status-select-menu" @click.stop>
            <van-dropdown-item
              v-model="status"
              :disabled="props.readonly || item.converted || !isOwner(item)"
              :options="statusOptions"
              @change="changeStatus"
            />
          </van-dropdown-menu>
        </div>
      </div>
      <div class="follow-item-content rounded-[var(--border-radius-large)]">
        <div class="flex items-center justify-between gap-[16px]">
          <CrmAvatar :is-word="item.owner !== userStore.userInfo.id" :text="item.ownerName" />
          <div class="flex flex-1 flex-wrap items-center overflow-hidden">
            <div class="one-line-text flex-1 text-[16px] font-semibold">{{ item.ownerName }}</div>
          </div>
        </div>
        <div class="one-line-text rounded-[var(--border-radius-mini)] bg-[var(--text-n9)] p-[12px] text-[12px]">
          {{ item.content }}
        </div>
        <div :class="`flex items-center ${isShowAction ? 'justify-between' : 'justify-end'}`">
          <div v-if="isShowAction" class="flex items-center gap-[16px]">
            <CrmTextButton icon="iconicon_delete" color="var(--error-red)" icon-size="16px" @click="emit('delete')" />
            <CrmTextButton
              icon="iconicon_handwritten_signature"
              color="var(--primary-8)"
              icon-size="16px"
              @click="emit('edit')"
            />
            <!-- TODO先不上 -->
            <!-- <CrmTextButton
                icon="iconicon_login"
                color="var(--primary-8)"
                icon-size="16px"
                @click="emit('convertToRecord')"
              /> -->
          </div>
          <!-- 评论入口 -->
          <div class="mx-[4px] flex items-center text-[12px] text-[var(--text-n4)]">
            <CrmIcon
              name="iconicon_chat"
              width="14px"
              height="14px"
              color="var(--text-n4)"
              class="mr-[4px]"
              @click.stop="openComment(item)"
            />
            <div>{{ displayCommentCount }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import dayjs from 'dayjs';

  import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatBadgeCount } from '@lib/shared/method';
  import type { FollowDetailItem, StatusTagKey } from '@lib/shared/models/customer';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';

  import { statusMap } from '@/config/follow';
  import useUserStore from '@/store/modules/user';
  import { hasAnyPermission } from '@/utils/permission';

  import { CommonRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    item: any;
    type: 'plan' | 'record';
    formKey?: string;
    sourceId?: string;
    sourceName?: string;
    readonly?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'edit'): void;
    (e: 'delete'): void;
    (e: 'change'): void;
  }>();

  const { t } = useI18n();
  const userStore = useUserStore();
  const router = useRouter();

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

  const isOwner = (item: FollowDetailItem) => item.owner === userStore.userInfo?.id;

  const isShowAction = computed(() => !props.readonly && isOwner(props.item));

  const displayCommentCount = computed(() => formatBadgeCount(props.item.commentCount));

  function changeStatus() {
    emit('change');
  }

  function openComment(item: FollowDetailItem) {
    router.push({
      name: CommonRouteEnum.FOLLOW_COMMENT,
      query: {
        id: item.id,
        type: props.type,
        formKey: props.formKey,
        sourceId: props.sourceId,
        sourceName: props.sourceName,
        commentCount: item.commentCount || 0,
        readonly: String(props.readonly || !isOwner(item)),
      },
    });
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

<style scoped lang="less">
  .follow-item-content {
    gap: 12px;
    padding: 16px;
    background: var(--text-n10);
    @apply flex flex-1 flex-col;
  }
</style>
