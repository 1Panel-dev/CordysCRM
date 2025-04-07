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

    <div class="py-[16px]">
      {{ parsedContent?.content }}
      <n-button class="!inline !text-[var(--primary-8)]" text type="primary" @click="goUrl">
        {{ parsedContent?.renameUrl ?? parsedContent?.url }}
      </n-button>
    </div>
    <div v-if="total > 1" :class="`flex items-center ${total > 1 ? 'justify-between' : 'justify-end'}`">
      <div class="flex items-center gap-[8px]">
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
        v-if="hasAnyPermission(['SYSTEM_NOTICE:UPDATE'])"
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
</style>

<style lang="less">
  .n-notification-main__content {
    padding-right: 8px !important;
  }
</style>
