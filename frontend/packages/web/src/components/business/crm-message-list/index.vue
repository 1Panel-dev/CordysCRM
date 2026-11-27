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
                <div v-if="item.type === SystemMessageTypeEnum.ANNOUNCEMENT_NOTICE" class="whitespace-pre-line">
                  {{ getMessageContent(item) }}
                </div>
                <template v-else>
                  <template v-for="(part, index) in getMessageContentParts(item)" :key="index">
                    <n-button
                      v-if="part.isResourceName && !isDeleteAction(item)"
                      class="!h-auto !p-0"
                      type="primary"
                      text
                      @click.stop="openResourceDetail(item)"
                    >
                      {{ part.text }}
                    </n-button>
                    <span v-else>{{ part.text }}</span>
                  </template>
                </template>
                <n-button
                  v-if="item.type === SystemMessageTypeEnum.ANNOUNCEMENT_NOTICE && parseMessageContent(item)?.url"
                  class="ml-[8px] !h-auto !p-0"
                  type="primary"
                  text
                  @click.stop="goUrl(item.contentText)"
                >
                  {{ parseMessageContent(item)?.renameUrl ?? parseMessageContent(item)?.url }}
                </n-button>
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
</template>

<script lang="ts" setup>
  import { NBadge, NButton, NSpin, NTooltip } from 'naive-ui';
  import dayjs from 'dayjs';

  import { MessageNotificationSubcategoryEnum } from '@lib/shared/enums/messageNotificationEnum';
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

  function openNewPageContractPaymentRecord(item: MessageCenterItem) {
    openNewPage(AppRouteEnum.CONTRACT_PAYMENT_RECORD, {
      recordId: item.resourceId,
    });
  }

  function openNewPageContractInvoice(item: MessageCenterItem) {
    openNewPage(AppRouteEnum.CONTRACT_INVOICE, {
      id: item.resourceId,
    });
  }

  function openNewPageContractBusinessTitle(item: MessageCenterItem) {
    openNewPage(AppRouteEnum.CONTRACT_BUSINESS_NAME, {
      id: item.resourceId,
    });
  }

  function openNewPageClue(item: MessageCenterItem) {
    openNewPage(AppRouteEnum.CLUE_MANAGEMENT_CLUE, { id: item.resourceId });
  }

  function openNewPageCluePool(item: MessageCenterItem) {
    openNewPage(AppRouteEnum.CLUE_MANAGEMENT_POOL, { id: item.resourceId });
  }

  function openNewPageCustomer(item: MessageCenterItem) {
    openNewPage(AppRouteEnum.CUSTOMER_INDEX, { id: item.resourceId });
  }

  function openNewPageCustomerPool(item: MessageCenterItem) {
    openNewPage(AppRouteEnum.CUSTOMER_OPEN_SEA, { id: item.resourceId });
  }

  function openNewPageCustomerContact(item: MessageCenterItem) {
    openNewPage(AppRouteEnum.CUSTOMER_CONTACT, { id: item.resourceId });
  }

  function openNewPageOpportunity(item: MessageCenterItem) {
    openNewPage(AppRouteEnum.OPPORTUNITY_OPT, { id: item.resourceId });
  }

  function openNewPageOrder(item: MessageCenterItem) {
    openNewPage(AppRouteEnum.ORDER_INDEX, { id: item.resourceId });
  }

  function openNewPageFollowRecord(item: MessageCenterItem) {
    openNewPage(AppRouteEnum.FOLLOW_MANAGEMENT_RECORD, { id: item.resourceId });
  }

  function openNewPageFollowPlan(item: MessageCenterItem) {
    openNewPage(AppRouteEnum.FOLLOW_MANAGEMENT_PLAN, { id: item.resourceId });
  }

  function openNewPageCustomForm(item: MessageCenterItem) {
    openNewPage(AppRouteEnum.CUSTOM_FORM_INDEX, {
      id: item.resourceId,
      customFormId: item.formType,
    });
  }

  function openNewPagePendingTodo(item: MessageCenterItem) {
    openNewPage(AppRouteEnum.WORKBENCH, {
      todoType: 'pending',
      resourceType: item.formType?.toUpperCase() === 'APPROVAL' ? '' : item.formType,
    });
  }

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

  function getNotificationSubcategory(formType?: string) {
    return Object.values(MessageNotificationSubcategoryEnum).find((subcategory) => subcategory === formType);
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
    if (triggerType === 'SCHEDULE') {
      return { label: t('system.message.triggerType.schedule'), type: 'warning' as const };
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
      permission: ['FOLLOW_RECORD:READ'],
      action: openNewPageFollowRecord,
    },
    {
      operation: 'FOLLOW_UP_PLAN_COMMENT',
      permission: ['FOLLOW_PLAN:READ'],
      action: openNewPageFollowPlan,
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

  const formTypeDetailConfig: Partial<Record<MessageNotificationSubcategoryEnum, MessageDetailAction>> = {
    [MessageNotificationSubcategoryEnum.CLUE]: {
      permission: ['CLUE_MANAGEMENT:READ'],
      action: openNewPageClue,
    },
    [MessageNotificationSubcategoryEnum.CUSTOMER]: {
      permission: ['CUSTOMER_MANAGEMENT:READ'],
      action: openNewPageCustomer,
    },
    [MessageNotificationSubcategoryEnum.CUSTOMER_CONTRACT]: {
      permission: ['CONTRACT:READ'],
      action: openNewPageContract,
    },
    [MessageNotificationSubcategoryEnum.CUSTOMER_CONTACT]: {
      permission: ['CUSTOMER_MANAGEMENT_CONTACT:READ'],
      action: openNewPageCustomerContact,
    },
    [MessageNotificationSubcategoryEnum.OPPORTUNITY]: {
      permission: ['OPPORTUNITY_MANAGEMENT:READ'],
      action: openNewPageOpportunity,
    },
    [MessageNotificationSubcategoryEnum.OPPORTUNITY_QUOTATION]: {
      permission: permissionConfig.OPPORTUNITY_QUOTATION_READ,
      action: openNewPageQuotation,
    },
    [MessageNotificationSubcategoryEnum.CONTRACT]: {
      permission: ['CONTRACT:READ'],
      action: openNewPageContract,
    },
    [MessageNotificationSubcategoryEnum.ORDER]: {
      permission: ['ORDER:READ'],
      action: openNewPageOrder,
    },
    [MessageNotificationSubcategoryEnum.FOLLOW_UP_RECORD]: {
      permission: ['FOLLOW_RECORD:READ'],
      action: openNewPageFollowRecord,
    },
    [MessageNotificationSubcategoryEnum.FOLLOW_UP_PLAN]: {
      permission: ['FOLLOW_PLAN:READ'],
      action: openNewPageFollowPlan,
    },
    [MessageNotificationSubcategoryEnum.CONTRACT_PAYMENT]: {
      permission: permissionConfig.CONTRACT_PAYMENT_PLAN_READ,
      action: openNewPageContractPaymentPlan,
    },
    [MessageNotificationSubcategoryEnum.CONTRACT_RECORD]: {
      permission: ['CONTRACT_PAYMENT_RECORD:READ'],
      action: openNewPageContractPaymentRecord,
    },
    [MessageNotificationSubcategoryEnum.CONTRACT_INVOICE]: {
      permission: ['CONTRACT_INVOICE:READ'],
      action: openNewPageContractInvoice,
    },
  };

  const resourceTypeDetailConfig: Record<string, MessageDetailAction> = {
    CLUE: { permission: ['CLUE_MANAGEMENT:READ'], action: openNewPageClue },
    CLUE_POOL: { permission: ['CLUE_MANAGEMENT_POOL:READ'], action: openNewPageCluePool },
    CUSTOMER: { permission: ['CUSTOMER_MANAGEMENT:READ'], action: openNewPageCustomer },
    CUSTOMER_POOL: { permission: ['CUSTOMER_MANAGEMENT_POOL:READ'], action: openNewPageCustomerPool },
    CUSTOMER_OPEN_SEA: { permission: ['CUSTOMER_MANAGEMENT_POOL:READ'], action: openNewPageCustomerPool },
    CUSTOMER_CONTACT: { permission: ['CUSTOMER_MANAGEMENT_CONTACT:READ'], action: openNewPageCustomerContact },
    OPPORTUNITY: { permission: ['OPPORTUNITY_MANAGEMENT:READ'], action: openNewPageOpportunity },
    BUSINESS: { permission: ['OPPORTUNITY_MANAGEMENT:READ'], action: openNewPageOpportunity },
    CONTRACT: { permission: ['CONTRACT:READ'], action: openNewPageContract },
    ORDER: { permission: ['ORDER:READ'], action: openNewPageOrder },
    FOLLOW_RECORD: { permission: ['FOLLOW_RECORD:READ'], action: openNewPageFollowRecord },
    FOLLOW_PLAN: { permission: ['FOLLOW_PLAN:READ'], action: openNewPageFollowPlan },
    CONTRACT_PAYMENT_PLAN: {
      permission: permissionConfig.CONTRACT_PAYMENT_PLAN_READ,
      action: openNewPageContractPaymentPlan,
    },
    CONTRACT_PAYMENT: {
      permission: permissionConfig.CONTRACT_PAYMENT_PLAN_READ,
      action: openNewPageContractPaymentPlan,
    },
    CONTRACT_PAYMENT_RECORD: {
      permission: ['CONTRACT_PAYMENT_RECORD:READ'],
      action: openNewPageContractPaymentRecord,
    },
    CONTRACT_RECORD: {
      permission: ['CONTRACT_PAYMENT_RECORD:READ'],
      action: openNewPageContractPaymentRecord,
    },
    CONTRACT_INVOICE: { permission: ['CONTRACT_INVOICE:READ'], action: openNewPageContractInvoice },
    INVOICE: { permission: ['CONTRACT_INVOICE:READ'], action: openNewPageContractInvoice },
    BUSINESS_TITLE: { permission: ['CONTRACT_BUSINESS_TITLE:READ'], action: openNewPageContractBusinessTitle },
    CUSTOM_FORM: { permission: ['CUSTOM_FORM_DATA:READ'], action: openNewPageCustomForm },
  };

  function getMessageDetailAction(operation: string) {
    return (
      followCommentDetailConfig.find((item) => operation.includes(item.operation)) || messageDetailConfig[operation]
    );
  }

  function openResourceDetail(item: MessageCenterItem) {
    const resourceTypes = [item.formType, item.resourceType].map((type) => type?.toUpperCase() ?? '').filter(Boolean);
    if (resourceTypes.includes('APPROVAL')) {
      openNewPagePendingTodo(item);
      return;
    }
    const subcategory = getNotificationSubcategory(item.formType);
    const formTypeAction = subcategory ? formTypeDetailConfig[subcategory] : undefined;
    if (formTypeAction) {
      if (hasAnyPermission(formTypeAction.permission)) formTypeAction.action(item);
      return;
    }
    const resourceTypeAction = item.resourceType
      ? resourceTypeDetailConfig[item.resourceType.toUpperCase()]
      : undefined;
    if (resourceTypeAction) {
      if (hasAnyPermission(resourceTypeAction.permission)) resourceTypeAction.action(item);
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

  function getMessageContent(item: MessageCenterItem) {
    return String(parseMessageContent(item)?.content ?? item.content ?? item.contentText ?? '');
  }

  function isDeleteAction(item: MessageCenterItem) {
    return /(?:^|_)DELETE(?:D)?(?:_|$)/i.test(item.operation ?? '');
  }

  function getMessageContentParts(item: MessageCenterItem) {
    const content = getMessageContent(item);
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
