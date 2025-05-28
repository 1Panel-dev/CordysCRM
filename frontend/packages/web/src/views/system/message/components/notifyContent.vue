<template>
  <div>
    <div class="notify-title">
      <div class="one-line-text flex items-center">
        <CrmIcon class="mr-[8px] text-[var(--warning-yellow)]" type="iconicon_info_circle_filled" :size="24" />
        <n-tooltip :delay="300">
          <template #trigger>
            <div class="one-line-text">{{ notifyItem?.subject }}</div>
          </template>
          {{ notifyItem?.subject }}
        </n-tooltip>
      </div>
    </div>
    <div class="notify-content py-[16px]">
      {{ parsedContent?.content }}
      <n-tooltip trigger="hover" :delay="300" :disabled="!(parsedContent.renameUrl || parsedContent.url)?.length">
        <template #trigger>
          <n-button class="inline-block" text type="primary" @click="goUrl">
            <div class="one-line-text max-w-[300px]">
              {{ parsedContent?.renameUrl || parsedContent?.url }}
            </div>
          </n-button>
        </template>
        {{ parsedContent?.renameUrl || parsedContent?.url }}
      </n-tooltip>
    </div>
    <div :class="`flex items-center ${total > 1 ? 'justify-between' : 'justify-end'}`">
      <div v-if="total > 1" class="flex items-center gap-[8px]">
        <CrmIcon
          :class="`${
            current === 1 ? 'cursor-not-allowed text-[var(--text-n6)]' : 'cursor-pointer text-[var(--text-n4)]'
          }`"
          type="iconicon_chevron_left"
          @click="prevMessage"
        />
        <span class="text-[var(--text-n1)]"> {{ current }}/{{ total }}</span>

        <CrmIcon
          :class="`${
            current === total ? 'cursor-not-allowed text-[var(--text-n6)]' : 'cursor-pointer text-[var(--text-n4)]'
          }`"
          type="iconicon_chevron_right"
          @click="nextMessage"
        />
      </div>
      <n-button
        v-if="hasAnyPermission(['SYSTEM_NOTICE:UPDATE']) && notifyItem?.status === SystemMessageStatusEnum.UNREAD"
        class="!bg-[var(--primary-8)]"
        type="primary"
        @click="setMessageRead"
      >
        {{ t('system.message.setRead') }}
      </n-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { NButton, NTooltip } from 'naive-ui';

  import { SystemMessageStatusEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { MessageCenterItem } from '@lib/shared/models/system/message';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { setNotificationRead } from '@/api/modules';
  import useAppStore from '@/store/modules/app';
  import { hasAnyPermission } from '@/utils/permission';

  const appStore = useAppStore();

  const { t } = useI18n();

  const emit = defineEmits<{
    (e: 'close'): void;
  }>();

  const current = ref(1);

  const total = computed(() => (appStore.messageInfo.announcementDTOList || []).length);

  const notifyItem = computed<MessageCenterItem | null>(() => {
    if ((appStore.messageInfo.announcementDTOList || []).length) {
      return appStore.messageInfo.announcementDTOList[current.value - 1];
    }
    return null;
  });

  const parsedContent = computed(() => JSON.parse(notifyItem.value?.contentText ?? '{}'));

  function prevMessage() {
    if (current.value > 1) {
      current.value -= 1;
    }
  }

  function nextMessage() {
    if (current.value < total.value) {
      current.value += 1;
    }
  }

  function goUrl() {
    const url = parsedContent.value?.url;
    if (url) {
      window.open(url, '_blank');
    }
  }

  async function setMessageRead() {
    if (notifyItem.value) {
      const { status, id } = notifyItem.value;
      if (status === SystemMessageStatusEnum.READ) return;
      try {
        await setNotificationRead(id);
        appStore.initMessage();
        emit('close');
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    }
  }
</script>

<style lang="less" scoped>
  .notify-title {
    font-size: 16px;
    color: var(--text-n1);
    @apply flex items-center justify-between font-semibold;
  }
  .notify-content {
    @apply overflow-y-auto break-all;

    max-height: 400px;
    .crm-scroll-bar();
  }
</style>

<style lang="less">
  .n-notification-main__content {
    padding-right: 8px !important;
  }
</style>
