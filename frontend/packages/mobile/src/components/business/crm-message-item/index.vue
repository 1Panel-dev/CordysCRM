<template>
  <div class="crm-message-item">
    <div class="mb-[4px] flex flex-nowrap items-center justify-between">
      <div class="flex w-[calc(100%-60px)] flex-nowrap items-center gap-[4px]">
        <van-tag
          :color="isSystemMessage ? 'var(--info-5)' : 'var(--warning-5)'"
          :text-color="isSystemMessage ? 'var(--info-blue)' : 'var(--warning-yellow)'"
          class="flex-shrink-0 !p-[2px_8px]"
        >
          {{ item.type === SystemMessageTypeEnum.SYSTEM_NOTICE ? t('common.system') : t('common.announcementMessage') }}
        </van-tag>
        <div class="flex max-w-[calc(100%-40px)] items-center">
          <van-badge :dot="item.status === SystemMessageStatusEnum.UNREAD" class="w-full">
            <div
              :class="` one-line-text w-full crm-message-item-title--${
                item.status === SystemMessageStatusEnum.UNREAD ? 'normal' : 'read'
              } font-medium`"
            >
              {{
                item.type === SystemMessageTypeEnum.SYSTEM_NOTICE ? t('common.systemNotification') : item.subject ?? '-'
              }}
            </div>
          </van-badge>
        </div>
      </div>
      <div
        v-if="props.item.status === SystemMessageStatusEnum.UNREAD"
        class="flex-shrink-0 text-[var(--primary-8)]"
        @click="setMessageRead"
      >
        {{ t('common.signRead') }}
      </div>
    </div>
    <div class="flex flex-col gap-[4px] text-[12px]">
      <div :class="`break-words message-title--${item.status === SystemMessageStatusEnum.UNREAD ? 'normal' : 'read'}`">
        {{ item.type === SystemMessageTypeEnum.SYSTEM_NOTICE ? item.contentText : parseMessageContent?.content ?? '-' }}
        <span
          v-if="item.type === SystemMessageTypeEnum.ANNOUNCEMENT_NOTICE"
          class="ml-[8px] cursor-pointer text-[var(--primary-8)]"
          @click="goUrl"
        >
          {{ parseMessageContent?.renameUrl ?? parseMessageContent?.url }}
        </span>
      </div>
      <div class="text-[var(--text-n2)]">
        {{ dayjs(item.createTime).format('YYYY-MM-DD HH:mm:ss') }}
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import dayjs from 'dayjs';

  import { SystemMessageStatusEnum, SystemMessageTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { openDocumentLink } from '@lib/shared/method/index';
  import type { MessageCenterItem } from '@lib/shared/models/system/message';

  import { setNotificationRead } from '@/api/modules';
  import useAppStore from '@/store/modules/app';

  const appStore = useAppStore();
  const { t } = useI18n();

  const props = defineProps<{
    item: MessageCenterItem;
  }>();

  const emit = defineEmits<{
    (e: 'loadList'): void;
  }>();

  const isSystemMessage = computed(() => props.item.type === SystemMessageTypeEnum.SYSTEM_NOTICE);

  const parseMessageContent = computed(() => JSON.parse(props.item.contentText || '{}'));

  // TODO 跳转过去可能回不来要加提示
  function goUrl() {
    const url = parseMessageContent.value?.url;
    if (url) {
      openDocumentLink(url);
    }
  }

  async function setMessageRead() {
    if (props.item.status === SystemMessageStatusEnum.READ) return;
    try {
      await setNotificationRead(props.item.id);
      appStore.initMessage();
      emit('loadList');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
</script>

<style lang="less" scoped>
  .crm-message-item {
    padding: 12px 20px;
    .half-px-border-bottom();
  }
  .crm-message-item-title {
    &--normal {
      color: var(--text-n1);
    }
    &--read {
      color: var(--text-n4);
    }
  }
</style>
