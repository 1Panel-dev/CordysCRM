<template>
  <n-spin :show="loading" class="min-h-[300px]">
    <CrmList
      v-if="list.length"
      v-model:data="list"
      :virtual-scroll-height="props.virtualScrollHeight"
      :key-field="props.keyField"
      :item-height="114"
      :mode="props.messageList ? 'static' : 'remote'"
      @reach-bottom="handleReachBottom"
    >
      <template #item="{ item }">
        <div class="crm-message-item py-[8px]">
          <div class="crm-message-item-content h-full w-full gap-[24px] p-[16px]">
            <div class="mb-[8px] flex w-full items-center justify-between gap-[24px]">
              <div class="flex items-center gap-[8px] overflow-hidden">
                <CrmTag theme="light" :type="item.type === SystemMessageTypeEnum.SYSTEM_NOTICE ? 'info' : 'warning'">
                  {{
                    item.type === SystemMessageTypeEnum.SYSTEM_NOTICE
                      ? t('system.message.system')
                      : t('system.message.announcement')
                  }}
                </CrmTag>
                <n-badge class="overflow-hidden" dot :show="item.status === SystemMessageStatusEnum.UNREAD">
                  <n-tooltip :delay="300">
                    <template #trigger>
                      <div
                        :class="`one-line-text  message-title--${
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
              <n-button
                v-if="item.status === SystemMessageStatusEnum.UNREAD"
                type="primary"
                text
                class="set-read-button flex-shrink-0"
                @click="setMessageRead(item)"
              >
                {{ t('system.message.setRead') }}
              </n-button>
            </div>
            <div class="flex flex-col pl-[48px]">
              <div :class="getMessageContentClass(item)" @click="goDetail(item)">
                {{
                  item.type === SystemMessageTypeEnum.SYSTEM_NOTICE
                    ? item.contentText
                    : parseMessageContent(item)?.content ?? '-'
                }}
                <span
                  v-if="item.type === SystemMessageTypeEnum.ANNOUNCEMENT_NOTICE"
                  class="ml-[8px] cursor-pointer text-[var(--primary-8)]"
                  @click="goUrl(item.contentText)"
                >
                  {{ parseMessageContent(item)?.renameUrl ?? parseMessageContent(item)?.url }}
                </span>
              </div>
              <div class="text-[var(--text-n4)]">
                {{ dayjs(item.createTime).format('YYYY-MM-DD HH:mm:ss') }}
              </div>
            </div>
          </div>
        </div>
      </template>
    </CrmList>
    <div
      v-else-if="props.messageList ? !list.length : !loading && finished"
      class="w-full p-[16px] text-center text-[var(--text-n4)]"
    >
      {{ props.emptyText || t('common.noData') }}
    </div>
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
  import useOpenNewPage from '@/hooks/useOpenNewPage';
  import useAppStore from '@/store/modules/app';
  import { hasAnyPermission } from '@/utils/permission';

  import { AppRouteEnum } from '@/enums/routeEnum';

  const appStore = useAppStore();
  const { openNewPage } = useOpenNewPage();

  const { t } = useI18n();

  interface MessageDetailAction {
    permission: string[];
    action: (id: string) => void;
  }

  const props = defineProps<{
    keyField: string;
    virtualScrollHeight: string;
    emptyText?: string;
    loadParams?: MessageCenterSubsetParams;
    messageList?: MessageCenterItem[];
  }>();

  const emit = defineEmits<{
    (e: 'refreshCount'): void;
  }>();

  function openNewPageQuotation(id: string) {
    openNewPage(AppRouteEnum.OPPORTUNITY_QUOTATION, {
      id,
    });
  }

  function openNewPageContract(id: string) {
    openNewPage(AppRouteEnum.CONTRACT_INDEX, {
      id,
    });
  }

  function openNewPageContractPaymentPlan(id: string) {
    openNewPage(AppRouteEnum.CONTRACT_PAYMENT, {
      id,
    });
  }

  const permissionConfig = {
    OPPORTUNITY_QUOTATION_READ: ['OPPORTUNITY_QUOTATION:READ'],
    CONTRACT_READ: ['CONTRACT:READ'],
    CONTRACT_PAYMENT_PLAN_READ: ['CONTRACT_PAYMENT_PLAN:READ'],
  };

  const messageDetailConfig: Record<string, MessageDetailAction> = {
    BUSINESS_QUOTATION_EXPIRED: {
      permission: permissionConfig.OPPORTUNITY_QUOTATION_READ,
      action: openNewPageQuotation,
    },
    BUSINESS_QUOTATION_EXPIRING: {
      permission: permissionConfig.OPPORTUNITY_QUOTATION_READ,
      action: openNewPageQuotation,
    },
    CONTRACT_EXPIRED: {
      permission: permissionConfig.CONTRACT_READ,
      action: openNewPageContract,
    },
    CONTRACT_EXPIRING: {
      permission: permissionConfig.CONTRACT_READ,
      action: openNewPageContract,
    },
    CONTRACT_PAYMENT_EXPIRING: {
      permission: permissionConfig.CONTRACT_PAYMENT_PLAN_READ,
      action: openNewPageContractPaymentPlan,
    },
    CONTRACT_PAYMENT_EXPIRED: {
      permission: permissionConfig.CONTRACT_PAYMENT_PLAN_READ,
      action: openNewPageContractPaymentPlan,
    },
  };

  function getMessageContentClass(item: MessageCenterItem) {
    if (messageDetailConfig[item.operation] && hasAnyPermission(messageDetailConfig[item.operation].permission)) {
      return 'cursor-pointer text-[var(--primary-8)]';
    }

    return `message-title--${item.status === SystemMessageStatusEnum.UNREAD ? 'normal' : 'read'}`;
  }

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

  const finished = ref(false);
  async function loadMessageList(refresh = true) {
    try {
      if (!props.loadParams) return;
      loading.value = true;

      if (refresh) {
        finished.value = false;
        pageNation.value.current = 1;
        list.value = [];
      }
      const res = await getNotificationList({
        current: pageNation.value.current || 1,
        pageSize: pageNation.value.pageSize,
        keyword: innerKeyword.value,
        ...props.loadParams,
      });
      if (res) {
        list.value = list.value.concat(res.list);
        pageNation.value.total = res.total;
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
      finished.value = true;
    }
  }

  async function setMessageRead(item: MessageCenterItem) {
    if (item.status === SystemMessageStatusEnum.READ) return;
    try {
      await setNotificationRead(item.id);
      loadMessageList();
      appStore.initMessage();
      emit('refreshCount');
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
    loadMessageList(false);
  }

  function goUrl(context: string) {
    const url = JSON.parse(context)?.url;
    if (url) {
      window.open(url, '_blank');
    }
  }

  function goDetail(item: MessageCenterItem) {
    if (!hasAnyPermission(messageDetailConfig[item.operation]?.permission ?? [])) return;
    messageDetailConfig[item.operation]?.action?.(item.resourceId);
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
      .set-read-button {
        opacity: 0;
      }
      &:hover {
        .set-read-button {
          opacity: 1;
        }
      }
    }
  }
</style>
