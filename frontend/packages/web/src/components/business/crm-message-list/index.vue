<template>
  <n-spin :show="loading">
    <CrmList
      v-model:data="list"
      :virtual-scroll-height="props.virtualScrollHeight"
      :key-field="props.keyField"
      :item-height="114"
      :mode="props.messageList ? 'static' : 'remote'"
      @reach-bottom="handleReachBottom"
    >
      <template #item="{ item }">
        <div class="crm-message-item py-[8px]">
          <div class="crm-message-item-content flex h-full w-full justify-between gap-[24px] p-[16px]">
            <div>
              <div class="mb-[8px] flex w-full items-center gap-[8px]">
                <CrmTag theme="light" :type="item.type === SystemMessageTypeEnum.SYSTEM_NOTICE ? 'info' : 'warning'">
                  {{
                    item.type === SystemMessageTypeEnum.SYSTEM_NOTICE
                      ? t('system.message.system')
                      : t('system.message.announcement')
                  }}
                </CrmTag>
                <n-badge :dot="item.status === SystemMessageStatusEnum.UNREAD">
                  <n-tooltip :delay="300">
                    <template #trigger>
                      <div
                        :class="` w-full message-title--${
                          item.status === SystemMessageStatusEnum.UNREAD ? 'normal' : 'read'
                        } font-medium`"
                      >
                        {{
                          item.type === SystemMessageTypeEnum.SYSTEM_NOTICE
                            ? t('system.message.systemNotification')
                            : item.subject
                        }}
                      </div>
                    </template>
                    {{
                      item.type === SystemMessageTypeEnum.SYSTEM_NOTICE
                        ? t('system.message.systemNotification')
                        : item.subject
                    }}
                  </n-tooltip>
                </n-badge>
              </div>
              <div class="flex flex-col pl-[48px]">
                <div
                  :class="`flex message-title--${item.status === SystemMessageStatusEnum.UNREAD ? 'normal' : 'read'}`"
                >
                  {{
                    item.type === SystemMessageTypeEnum.SYSTEM_NOTICE
                      ? item.contentText
                      : parseMessageContent(item)?.content ?? '-'
                  }}
                  <div
                    v-if="item.type === SystemMessageTypeEnum.ANNOUNCEMENT_NOTICE"
                    class="ml-[8px] cursor-pointer text-[var(--primary-8)]"
                    @click="goUrl(item.contentText)"
                  >
                    {{ parseMessageContent(item)?.renameUrl ?? parseMessageContent(item)?.url }}
                  </div>
                </div>
                <div :class="`message-title--${item.status === SystemMessageStatusEnum.UNREAD ? 'normal' : 'read'}`">
                  {{ dayjs(item.createTime).format('YYYY-MM-DD HH:mm:ss') }}
                </div>
              </div>
            </div>
            <n-button
              v-if="item.status === SystemMessageStatusEnum.UNREAD && hasAnyPermission(['SYSTEM_NOTICE:UPDATE'])"
              type="primary"
              text
              class="flex-shrink-0"
              @click="setMessageRead(item)"
            >
              {{ t('system.message.setRead') }}
            </n-button>
          </div>
        </div>
      </template>
    </CrmList>
  </n-spin>
</template>

<script lang="ts" setup>
  import { NBadge, NButton, NSpin, NTooltip } from 'naive-ui';
  import dayjs from 'dayjs';

  import { SystemMessageStatusEnum, SystemMessageTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { MessageCenterItem, MessageCenterSubsetParams } from '@lib/shared/models/system/message';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';

  import { getNotificationList, setNotificationRead } from '@/api/modules';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();

  const props = defineProps<{
    keyField: string;
    virtualScrollHeight: string;
    emptyText?: string;
    loadParams?: MessageCenterSubsetParams;
    messageList?: MessageCenterItem[];
  }>();

  const innerKeyword = defineModel<string>('keyword', {
    required: false,
    default: null,
  });

  const list = ref<MessageCenterItem[]>([]);
  const loading = ref(false);

  const pageNation = ref({
    total: 0,
    pageSize: 10,
    current: 1,
  });

  function parseMessageContent(item: MessageCenterItem) {
    return JSON.parse(item.contentText || '{}');
  }

  async function loadMessageList() {
    try {
      if (!props.loadParams) return;
      loading.value = true;
      const res = await getNotificationList({
        current: pageNation.value.current || 1,
        pageSize: pageNation.value.pageSize,
        keyword: innerKeyword.value,
        ...props.loadParams,
      });
      if (res) {
        list.value = res.list;
        pageNation.value.total = res.total;
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function setMessageRead(item: MessageCenterItem) {
    if (item.status === SystemMessageStatusEnum.READ) return;
    try {
      await setNotificationRead(item.id);
      loadMessageList();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function handleReachBottom() {
    pageNation.value.current += 1;
    if (pageNation.value.current > Math.ceil(pageNation.value.total / pageNation.value.pageSize)) {
      return;
    }
    loadMessageList();
  }

  function goUrl(context: string) {
    const url = JSON.parse(context)?.url;
    if (url) {
      window.open(url, '_blank');
    }
  }

  onBeforeMount(() => {
    if (props.messageList) return;
    loadMessageList();
  });

  watch(
    () => props.messageList,
    (val) => {
      if (val) {
        list.value = val;
      }
    }
  );

  defineExpose({
    loadMessageList,
  });
</script>

<style lang="less" scoped>
  .crm-message-item {
    background: var(--text-n10);
    .crm-message-item-content {
      min-height: 108px;
      border-radius: @border-radius-medium;
      background: var(--text-n9);
      .message-title {
        &--normal {
          color: var(--text-n1);
        }
        &--read {
          color: var(--text-n4);
        }
      }
    }
  }
</style>
