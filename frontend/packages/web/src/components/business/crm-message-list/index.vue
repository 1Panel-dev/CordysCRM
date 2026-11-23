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
              <div class="flex min-w-0 items-center gap-[8px]">
                <CrmTag v-if="getMessageTag(item)" theme="light" :type="getMessageTag(item)?.type">
                  {{ getMessageTag(item)?.label }}
                </CrmTag>
                <n-badge class="crm-message-item__badge" dot :show="item.status === SystemMessageStatusEnum.UNREAD">
                  <n-tooltip :delay="300">
                    <template #trigger>
                      <div
                        :class="`one-line-text message-title--${
                          item.status === SystemMessageStatusEnum.UNREAD ? 'normal' : 'read'
                        } font-medium`"
                      >
                        {{ item.subject || t('system.message.systemNotification') }}
                      </div>
                    </template>
                    {{ item.subject || t('system.message.systemNotification') }}
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
              <div :class="`message-title--${item.status === SystemMessageStatusEnum.UNREAD ? 'normal' : 'read'}`">
                <template v-for="(part, index) in getMessageContentParts(item)" :key="index">
                  <n-button
                    v-if="part.isResourceName"
                    class="!h-auto !p-0"
                    type="primary"
                    text
                    @click.stop="openResourceDetail(item)"
                  >
                    {{ part.text }}
                  </n-button>
                  <span v-else>{{ part.text }}</span>
                </template>
                <span
                  v-if="item.type === SystemMessageTypeEnum.ANNOUNCEMENT_NOTICE"
                  class="ml-[8px] cursor-pointer text-[var(--primary-8)]"
                  @click.stop="goUrl(item.contentText)"
                >
                  {{ parseMessageContent(item)?.renameUrl ?? parseMessageContent(item)?.url }}
                </span>
              </div>
              <div class="mt-[8px] flex items-center justify-between gap-[12px] text-[12px] text-[var(--text-n4)]">
                <span class="one-line-text">{{ getResourceTypeLabel(item.resourceType) }}</span>
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
  <DetailDrawer
    v-model:show="showFollowDetailDrawer"
    :form-key="followDetailFormKey"
    :source-id="followDetailSourceId"
    :source-name="followDetailSourceName"
    :refresh-key="followDetailRefreshKey"
    readonly
  />
  <ClueOverviewDrawer
    v-if="resourceDrawerType === 'CLUE'"
    v-model:show="showResourceDetailDrawer"
    :detail="resourceDetail"
  />
  <CustomerOverviewDrawer
    v-if="resourceDrawerType === 'CUSTOMER'"
    v-model:show="showResourceDetailDrawer"
    :source-id="activeResourceItem?.resourceId ?? ''"
    readonly
  />
  <OptOverviewDrawer
    v-if="resourceDrawerType === 'OPPORTUNITY'"
    v-model:show="showResourceDetailDrawer"
    :detail="resourceDetail"
    readonly
  />
  <ContractDetailDrawer
    v-if="resourceDrawerType === 'CONTRACT'"
    v-model:visible="showResourceDetailDrawer"
    :source-id="activeResourceItem?.resourceId ?? ''"
    readonly
  />
  <OrderDetailDrawer
    v-if="resourceDrawerType === 'ORDER'"
    v-model:visible="showResourceDetailDrawer"
    :source-id="activeResourceItem?.resourceId ?? ''"
    readonly
  />
</template>

<script lang="ts" setup>
  import { NBadge, NButton, NSpin, NTooltip } from 'naive-ui';
  import dayjs from 'dayjs';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { SystemMessageStatusEnum, SystemMessageTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { MessageCenterItem, MessageCenterSubsetParams } from '@lib/shared/models/system/message';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import DetailDrawer from '@/components/business/crm-follow-drawer/components/detailDrawer.vue';
  import ClueOverviewDrawer from '@/views/clueManagement/clue/components/clueOverviewDrawer.vue';
  import ContractDetailDrawer from '@/views/contract/contract/components/detail.vue';
  import CustomerOverviewDrawer from '@/views/customer/components/customerOverviewDrawer.vue';
  import OptOverviewDrawer from '@/views/opportunity/components/optOverviewDrawer.vue';
  import OrderDetailDrawer from '@/views/order/order/components/detail.vue';

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
    action: (item: MessageCenterItem) => void;
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
    (e: 'resourceTypesChange', resourceTypes: string[]): void;
    (e: 'openApprovalTodo'): void;
  }>();

  function openNewPageQuotation(item: MessageCenterItem) {
    openNewPage(AppRouteEnum.OPPORTUNITY_QUOTATION, {
      id: item.resourceId,
    });
  }

  function openNewPageContract(item: MessageCenterItem) {
    openNewPage(AppRouteEnum.CONTRACT_INDEX, {
      id: item.resourceId,
    });
  }

  function openNewPageContractPaymentPlan(item: MessageCenterItem) {
    openNewPage(AppRouteEnum.CONTRACT_PAYMENT, {
      id: item.resourceId,
    });
  }

  const showFollowDetailDrawer = ref(false);
  const followDetailSourceId = ref('');
  const followDetailSourceName = ref('');
  const followDetailRefreshKey = ref(0);
  const followDetailFormKey = ref(FormDesignKeyEnum.FOLLOW_RECORD);
  const activeResourceItem = ref<MessageCenterItem>();
  const resourceDrawerType = ref('');
  const showResourceDetailDrawer = ref(false);
  const resourceDetail = computed(() =>
    activeResourceItem.value ? { id: activeResourceItem.value.resourceId } : undefined
  );

  function openFollowDetail(
    item: MessageCenterItem,
    formKey: FormDesignKeyEnum.FOLLOW_RECORD | FormDesignKeyEnum.FOLLOW_PLAN
  ) {
    followDetailSourceId.value = item.resourceId;
    followDetailSourceName.value = item.resourceName || '';
    followDetailFormKey.value = formKey;
    followDetailRefreshKey.value += 1;
    showFollowDetailDrawer.value = true;
  }

  function openFollowRecordDetail(item: MessageCenterItem) {
    openFollowDetail(item, FormDesignKeyEnum.FOLLOW_RECORD);
  }

  function openFollowPlanDetail(item: MessageCenterItem) {
    openFollowDetail(item, FormDesignKeyEnum.FOLLOW_PLAN);
  }
  const followPermission = ['CLUE_MANAGEMENT:READ', 'CUSTOMER_MANAGEMENT:READ', 'OPPORTUNITY_MANAGEMENT:READ'];

  const resourceTypeLabelMap: Record<string, string> = {
    CLUE: 'system.message.settings.category.lead',
    CLUE_POOL: 'system.message.cluePoolMessage',
    CUSTOMER: 'system.message.customerMessage',
    CUSTOMER_POOL: 'system.message.openSeaMessage',
    CUSTOMER_CONTACT: 'system.message.contactMessage',
    OPPORTUNITY: 'system.message.opportunityMessage',
    CONTRACT: 'module.contract',
    ORDER: 'module.order',
    FOLLOW_RECORD: 'system.message.settings.category.followRecord',
    FOLLOW_PLAN: 'system.message.settings.category.followPlan',
    CONTRACT_PAYMENT_PLAN: 'system.message.settings.category.paymentPlan',
    CONTRACT_PAYMENT_RECORD: 'system.message.settings.category.paymentRecord',
    CONTRACT_INVOICE: 'system.message.settings.category.invoice',
    BUSINESS_TITLE: 'system.message.settings.category.quotation',
  };

  function getResourceTypeLabel(resourceType: string) {
    const labelKey = resourceTypeLabelMap[resourceType.toUpperCase()];
    return labelKey ? t(labelKey) : '';
  }

  function getMessageTag(item: MessageCenterItem) {
    if (item.type === SystemMessageTypeEnum.ANNOUNCEMENT_NOTICE) {
      return { label: t('system.message.announcement'), type: 'warning' as const };
    }
    if (item.operation.toUpperCase().includes('MENTION') || item.triggerType?.toUpperCase() === 'MENTION') {
      return { label: t('system.message.atMe'), type: 'primary' as const };
    }
    const triggerType = item.triggerType?.toUpperCase();
    if (triggerType === 'ACTION') {
      return { label: t('system.message.triggerType.action'), type: 'info' as const };
    }
    if (triggerType === 'FIELD' || triggerType === 'CONDITION') {
      return { label: t('system.message.triggerType.condition'), type: 'success' as const };
    }
    return undefined;
  }

  const permissionConfig = {
    OPPORTUNITY_QUOTATION_READ: ['OPPORTUNITY_QUOTATION:READ'],
    CONTRACT_READ: ['CONTRACT:READ'],
    CONTRACT_PAYMENT_PLAN_READ: ['CONTRACT_PAYMENT_PLAN:READ'],
  };

  const followCommentDetailConfig = [
    {
      operation: 'FOLLOW_UP_RECORD_COMMENT',
      permission: followPermission,
      action: openFollowRecordDetail,
    },
    {
      operation: 'FOLLOW_UP_PLAN_COMMENT',
      permission: followPermission,
      action: openFollowPlanDetail,
    },
  ];

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

  function getMessageDetailAction(operation: string) {
    return (
      followCommentDetailConfig.find((item) => operation.includes(item.operation)) || messageDetailConfig[operation]
    );
  }

  function openResourceDetail(item: MessageCenterItem) {
    const resourceType = item.resourceType?.toUpperCase() ?? '';
    if (resourceType === 'APPROVAL') {
      emit('openApprovalTodo');
      return;
    }
    const drawerConfig: Record<string, { type: string; permission: string[] }> = {
      CLUE: { type: 'CLUE', permission: ['CLUE_MANAGEMENT:READ'] },
      CUSTOMER: { type: 'CUSTOMER', permission: ['CUSTOMER_MANAGEMENT:READ'] },
      OPPORTUNITY: { type: 'OPPORTUNITY', permission: ['OPPORTUNITY_MANAGEMENT:READ'] },
      CONTRACT: { type: 'CONTRACT', permission: ['CONTRACT:READ'] },
      ORDER: { type: 'ORDER', permission: ['ORDER:READ'] },
    };
    if (resourceType === 'FOLLOW_RECORD' || resourceType === 'FOLLOW_PLAN') {
      if (!hasAnyPermission(followPermission)) return;
      openFollowDetail(
        item,
        resourceType === 'FOLLOW_RECORD' ? FormDesignKeyEnum.FOLLOW_RECORD : FormDesignKeyEnum.FOLLOW_PLAN
      );
      return;
    }
    const config = drawerConfig[resourceType];
    if (config) {
      if (!hasAnyPermission(config.permission)) return;
      activeResourceItem.value = item;
      resourceDrawerType.value = config.type;
      showResourceDetailDrawer.value = true;
      return;
    }
    const detailAction = getMessageDetailAction(item.operation);
    if (detailAction && hasAnyPermission(detailAction.permission)) detailAction.action(item);
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
    try {
      return JSON.parse(item.contentText || '{}');
    } catch {
      return { content: item.contentText || item.content || '' };
    }
  }

  function getMessageContentParts(item: MessageCenterItem) {
    const content = String(parseMessageContent(item)?.content ?? item.content ?? item.contentText ?? '');
    const resourceName = item.resourceName?.trim();
    if (!resourceName) return [{ text: content }];

    const parts: { text: string; isResourceName?: boolean }[] = [];
    let remainingContent = content;
    let matchIndex = remainingContent.indexOf(resourceName);
    while (matchIndex >= 0) {
      if (matchIndex > 0) parts.push({ text: remainingContent.slice(0, matchIndex) });
      parts.push({ text: resourceName, isResourceName: true });
      remainingContent = remainingContent.slice(matchIndex + resourceName.length);
      matchIndex = remainingContent.indexOf(resourceName);
    }
    if (remainingContent) parts.push({ text: remainingContent });
    if (!parts.some((part) => part.isResourceName)) {
      parts.push({ text: ` ${resourceName}`, isResourceName: true });
    }
    return parts;
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
        pageNation.value.total = res.total;
        const { triggerType } = props.loadParams;
        const filteredList = res.list.filter((item) => {
          if (triggerType === 'MENTION') {
            return item.triggerType?.toUpperCase() === 'MENTION' || item.operation.toUpperCase().includes('MENTION');
          }
          if (!triggerType || !item.triggerType) return true;
          return item.triggerType.toUpperCase() === triggerType;
        });
        list.value = list.value.concat(filteredList);
        if (triggerType === 'MENTION') {
          emit('resourceTypesChange', [...new Set(list.value.map((item) => item.resourceType).filter(Boolean))]);
        }
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
    const url = parseMessageContent({ contentText: context } as MessageCenterItem)?.url;
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
  :deep(.crm-message-item__badge) {
    display: inline-flex;
    flex: 1;
    min-width: 0;
  }
</style>
