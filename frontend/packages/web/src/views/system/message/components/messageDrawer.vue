<template>
  <CrmDrawer
    v-model:show="showMessageDrawer"
    no-padding
    :title="t('system.message.notify')"
    width="1200"
    :footer="false"
    @cancel="resetMessageFilters"
  >
    <template #titleRight>
      <div class="flex items-center gap-[8px]">
        <n-switch v-model:value="unReadEnable" :rubber-band="false" size="small" @update:value="refresh()" />
        <div class="text-[14px] font-normal">{{ t('system.message.unreadOnly') }}</div>
        <n-divider class="!mx-0" vertical />
        <n-button text type="primary" @click="setAllMessageStatus">
          <CrmIcon class="mr-[4px]" type="iconicon_browse" :size="16" />
          {{ t('system.message.markAllAsRead') }}
        </n-button>
      </div>
    </template>
    <div class="message-wrapper">
      <div class="message-count">
        <div class="min-h-0 flex-1 overflow-y-auto p-[24px]">
          <div class="w-full overflow-hidden">
            <CrmTab
              v-model:active-tab="activeTab"
              class="mb-[16px]"
              type="segment"
              no-content
              :tab-list="tabList"
              @change="handleChangeType"
            />
          </div>
          <div
            v-for="item of messageTypeList"
            :key="item.value"
            :class="`flex h-[42px] cursor-pointer items-center justify-between rounded px-[16px] ${
              activeMessageType === item.value ? 'bg-[var(--primary-7)] text-[var(--primary-8)]' : ''
            }`"
            @click="changeMessageType(item.value)"
          >
            <div class="truncate" :title="item.label">{{ item.label }}</div>
            <div class="ml-[8px] shrink-0 text-[var(--text-n4)]">{{ item.count }}</div>
          </div>
        </div>
        <div class="message-footer flex items-center px-[24px]">
          <n-button v-permission="['SYSTEM_NOTICE:READ']" text @click="goMessageSetting">
            <CrmIcon class="mr-[8px]" type="iconicon_set_up" :size="16" />
            {{ t('menu.settings.messageSetting') }}
          </n-button>
        </div>
      </div>

      <div class="message-content p-[24px]">
        <div class="mb-[16px] flex items-center justify-between">
          <div class="flex items-center gap-[8px]">
            <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
            <n-select
              v-model:value="triggerType"
              class="w-[160px]"
              :options="triggerTypeOptions"
              :disabled="activeTab === 'PUBLIC'"
              @update:value="refresh(true)"
            />
            <n-select
              v-model:value="timeDays"
              class="w-[180px]"
              :options="selectTimeOptions"
              @update:value="changeHandler"
            />
            <n-date-picker
              v-if="timeDays === 'custom'"
              v-model:value="range"
              class="w-[240px]"
              type="datetimerange"
              @confirm="confirmTimePicker"
            >
              <template #date-icon>
                <CrmIcon class="text-[var(--text-n4)]" type="iconicon_time" :size="16" />
              </template>
              <template #separator>
                <div class="text-[var(--text-n4)]">{{ t('common.to') }}</div>
              </template>
            </n-date-picker>
          </div>
        </div>
        <CrmMessageList
          ref="messageListRef"
          v-model:keyword="keyword"
          :load-params="loadParams"
          virtual-scroll-height="calc(100vh - 162px)"
          key-field="id"
          @refresh-count="() => initMessageCount()"
        />
      </div>
    </div>
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { useRouter } from 'vue-router';
  import { NButton, NDatePicker, NDivider, NSelect, NSwitch, SelectOption } from 'naive-ui';
  import dayjs from 'dayjs';

  import { SystemMessageStatusEnum, SystemMessageTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { MessageCenterSubsetParams, NotificationCategory } from '@lib/shared/models/system/message';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmMessageList from '@/components/business/crm-message-list/index.vue';

  import { getNotificationCategories, setAllNotificationRead } from '@/api/modules';
  import useAppStore from '@/store/modules/app';

  import { AppRouteEnum } from '@/enums/routeEnum';

  const appStore = useAppStore();

  const { t } = useI18n();

  const router = useRouter();

  const showMessageDrawer = defineModel<boolean>('show', {
    required: true,
  });
  const keyword = ref('');

  const tabList = [
    {
      name: '',
      tab: t('system.message.allMessages'),
    },
    {
      name: 'MENTION',
      tab: t('system.message.atMe'),
    },
    {
      name: 'PUBLIC',
      tab: t('system.message.public'),
    },
  ];

  const timeDays = ref('3');
  const unReadEnable = ref(false);
  const range = ref();

  const selectTimeOptions: SelectOption[] = [
    {
      value: '3',
      label: t('system.message.last3Days'),
    },
    {
      value: '5',
      label: t('system.message.last5Days'),
    },
    {
      value: '7',
      label: t('system.message.last7Days'),
    },
    {
      value: 'month',
      label: t('system.message.nearlyOneMonth'),
    },
    {
      value: 'custom',
      label: t('common.custom'),
    },
  ];
  const activeTab = ref('');
  const activeMessageType = ref('');
  const triggerType = ref('');
  const triggerTypeOptions = computed(() => [
    { label: t('system.message.triggerType.all'), value: '' },
    { label: t('system.message.triggerType.action'), value: 'ACTION' },
    { label: t('system.message.triggerType.condition'), value: 'FIELD' },
    { label: t('system.message.triggerType.schedule'), value: 'SCHEDULE' },
  ]);

  const messageListRef = ref<InstanceType<typeof CrmMessageList>>();

  const getTimeRange = computed<{ startTime: number; endTime: number } | null>(() => {
    if (timeDays.value === 'custom' && range.value?.length) {
      const [start, end] = range.value;
      return { startTime: start.valueOf(), endTime: end.valueOf() };
    }

    let startTime;

    if (timeDays.value === 'month') {
      startTime = dayjs().subtract(30, 'day').startOf('day');
    } else {
      const days = Number(timeDays.value);
      if (!Number.isNaN(days)) {
        startTime = dayjs().subtract(days, 'day').startOf('day');
      } else {
        return null;
      }
    }
    const endTime = dayjs().endOf('day');
    return { startTime: startTime.valueOf(), endTime: endTime.valueOf() };
  });

  function goMessageSetting() {
    router.push({
      name: AppRouteEnum.SYSTEM_MESSAGE,
    });
    showMessageDrawer.value = false;
  }

  function getActiveMessageType() {
    if (activeTab.value === 'PUBLIC') return SystemMessageTypeEnum.ANNOUNCEMENT_NOTICE;
    return '';
  }

  const loadParams = computed<MessageCenterSubsetParams>(() => {
    const timeRange = getTimeRange.value;
    return {
      type: getActiveMessageType(),
      resourceType: activeMessageType.value,
      status: unReadEnable.value ? SystemMessageStatusEnum.UNREAD : '',
      endTime: timeRange?.endTime ?? null,
      createTime: timeRange?.startTime ?? null,
      triggerType: activeTab.value === 'MENTION' ? 'MENTION' : triggerType.value,
    };
  });

  const resourceTypeLabelMap: Record<string, string> = {
    CLUE: 'system.message.settings.category.lead',
    CUSTOMER: 'system.message.customerMessage',
    OPPORTUNITY: 'system.message.opportunityMessage',
    CONTRACT: 'module.contract',
    ORDER: 'module.order',
    FOLLOW_RECORD: 'system.message.settings.category.followRecord',
    FOLLOW_PLAN: 'system.message.settings.category.followPlan',
    CONTRACT_PAYMENT_PLAN: 'system.message.settings.category.paymentPlan',
    CONTRACT_PAYMENT_RECORD: 'system.message.settings.category.paymentRecord',
    CONTRACT_INVOICE: 'system.message.settings.category.invoice',
    BUSINESS_TITLE: 'system.message.settings.category.quotation',
    APPROVAL: 'system.message.settings.category.todo',
    ANNOUNCEMENT_NOTICE: 'system.message.announcement',
    FOLLOW: 'module.followManagement',
    FOLLOW_UP: 'module.followManagement',
    PRODUCT: 'module.productManagement',
    CUSTOM_FORM: 'module.customForm',
    SYSTEM: 'menu.settings.messageSetting',
  };

  const categories = ref<NotificationCategory[]>([]);
  let categoryRequest = 0;

  const messageTypeList = computed(() => {
    const unread = categories.value.reduce((sum, item) => sum + item.unreadCount, 0);
    const total = categories.value.reduce((sum, item) => sum + item.totalCount, 0);
    const isPublic = activeTab.value === 'PUBLIC';
    const allCategory = {
      value: '',
      label: t(isPublic ? 'system.message.announcement' : 'system.message.allMessage'),
      count: `${unread}/${total}`,
    };
    if (isPublic) return [allCategory];

    return [
      allCategory,
      ...categories.value.flatMap(({ key, formName, unreadCount, totalCount }) => {
        if (!key) return [];
        const labelKey = resourceTypeLabelMap[key];
        return [
          {
            value: key,
            label: formName || (labelKey ? t(labelKey) : key),
            count: `${unreadCount}/${totalCount}`,
          },
        ];
      }),
    ];
  });

  async function initMessageCount() {
    const request = ++categoryRequest;
    try {
      const result = await getNotificationCategories({
        ...loadParams.value,
        keyword: keyword.value,
        resourceType: '',
        status: '',
      });
      if (request !== categoryRequest) return;
      categories.value = result ?? [];
      if (activeMessageType.value && !categories.value.some(({ key }) => key === activeMessageType.value)) {
        activeMessageType.value = '';
        nextTick(() => messageListRef.value?.loadMessageList());
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function refresh(initCount = false) {
    nextTick(() => {
      if (initCount) {
        initMessageCount();
      }
      messageListRef.value?.loadMessageList();
    });
  }

  function searchData(val: string) {
    keyword.value = val;
    refresh(true);
  }

  function resetMessageFilters() {
    categoryRequest += 1;
    categories.value = [];
    activeTab.value = '';
    activeMessageType.value = '';
    keyword.value = '';
    triggerType.value = '';
    timeDays.value = '3';
    unReadEnable.value = false;
    range.value = undefined;
  }

  function changeMessageType(value: string) {
    activeMessageType.value = value;
    refresh();
  }

  function handleChangeType() {
    categories.value = [];
    activeMessageType.value = '';
    triggerType.value = '';
    refresh(true);
  }

  function changeHandler(val: string) {
    range.value = undefined;
    if (val !== 'custom') {
      refresh(true);
    }
  }

  function confirmTimePicker(
    value: number | [number, number] | null,
    _formattedValue: string | [string, string] | null
  ) {
    range.value = value;
    refresh(true);
  }

  async function setAllMessageStatus() {
    try {
      await setAllNotificationRead();
      messageListRef.value?.loadMessageList();
      initMessageCount();
      appStore.initMessage();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => showMessageDrawer.value,
    (val) => {
      if (val) {
        refresh(true);
      }
    }
  );
</script>

<style lang="less" scoped>
  .message-wrapper {
    @apply flex h-full;
    .message-count {
      width: 268px;
      border-right: 1px solid var(--text-n8);
      @apply flex h-full flex-col;
    }
    .message-footer {
      height: 56px;
      @apply shrink-0;
    }
    .message-content {
      width: calc(100% - 268px);
      @apply h-full;
    }
  }
  :deep(.n-tabs-rail) {
    width: 100%;
  }
</style>
