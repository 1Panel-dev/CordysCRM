<template>
  <n-spin :show="loading">
    <CrmList
      v-model:data="list"
      :virtual-scroll-height="props.virtualScrollHeight"
      :key-field="props.keyField"
      :item-height="114"
      @reach-bottom="handleReachBottom"
    >
      <template #item="{ item }">
        <div class="crm-message-item p-[8px]">
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
                <div :class="`message-title--${item.status === SystemMessageStatusEnum.UNREAD ? 'normal' : 'read'}`">
                  {{ item.contentText }}
                </div>
                <div :class="`message-title--${item.status === SystemMessageStatusEnum.UNREAD ? 'normal' : 'read'}`">
                  {{ dayjs(item.createTime).format('YYYY-MM-DD HH:mm:ss') }}
                </div>
              </div>
            </div>
            <n-button
              v-if="item.status === SystemMessageStatusEnum.UNREAD"
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
  import type { MessageCenterItem, MessageCenterSubsetParams } from '@lib/shared/models/system/message';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';

  import { getNotificationList, setNotificationRead } from '@/api/modules/system/message';
  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const props = defineProps<{
    keyField: string;
    virtualScrollHeight: string;
    emptyText?: string;
    loadParams: MessageCenterSubsetParams;
  }>();

  const innerKeyword = defineModel<string>('keyword', {
    required: true,
    default: null,
  });

  const list = ref<MessageCenterItem[]>([]);
  const loading = ref(false);

  const pageNation = ref({
    total: 0,
    pageSize: 10,
    current: 1,
  });

  async function setMessageRead(item: MessageCenterItem) {
    if (item.status === SystemMessageStatusEnum.READ) return;
    try {
      await setNotificationRead(item.id);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function loadMessageList() {
    try {
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

  function handleReachBottom() {
    pageNation.value.current += 1;
    if (pageNation.value.current > Math.ceil(pageNation.value.total / pageNation.value.pageSize)) {
      return;
    }
    loadMessageList();
  }

  onBeforeMount(() => {
    loadMessageList();
  });

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
