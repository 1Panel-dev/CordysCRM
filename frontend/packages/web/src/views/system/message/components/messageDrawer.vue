<template>
  <CrmDrawer
    v-model:show="showMessageDrawer"
    no-padding
    :title="t('system.message.notify')"
    width="1200"
    :footer="false"
  >
    <div class="message-wrapper">
      <div class="message-count">
        <div class="p-[24px]">
          <CrmTab
            v-model:active-tab="activeTab"
            class="mb-[16px]"
            type="segment"
            no-content
            :tab-list="tabList"
            @change="handleChangeType"
          />
          <div
            v-for="item of messageTypeList"
            :key="item.value"
            :class="`flex h-[42px] cursor-pointer items-center justify-between rounded px-[16px] ${
              activeMessageType === item.value ? 'bg-[var(--primary-7)] text-[var(--primary-8)]' : ''
            }`"
            @click="changeMessageType(item.value)"
          >
            <div>{{ item.label }}</div>
            <div class="text-[var(--text-n4)]">{{ item.count }}</div>
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
              v-model:value="timeDays"
              class="w-[100px]"
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
          <div class="mb-[8px] flex items-center gap-[8px]">
            <n-switch v-model:value="unReadEnable" :rubber-band="false" @update:value="changeHandler" />
            {{ t('system.message.unreadOnly') }}
            <n-divider v-permission="['SYSTEM_NOTICE:UPDATE']" class="!mx-0" vertical />
            <n-button v-permission="['SYSTEM_NOTICE:UPDATE']" text type="primary" @click="setAllMessageStatus">
              <CrmIcon class="mr-[4px]" type="iconicon_browse" :size="16" />
              {{ t('system.message.markAllAsRead') }}
            </n-button>
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

  import {
    SystemMessageStatusEnum,
    SystemMessageTypeEnum,
    SystemResourceMessageTypeEnum,
  } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { MessageCenterSubsetParams } from '@lib/shared/models/system/message';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmMessageList from '@/components/business/crm-message-list/index.vue';

  import { getNotificationCount, setAllNotificationRead } from '@/api/modules';
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
      name: SystemMessageTypeEnum.SYSTEM_NOTICE,
      tab: t('system.message.systemMessage'),
    },
    {
      name: SystemMessageTypeEnum.ANNOUNCEMENT_NOTICE,
      tab: t('system.message.announcement'),
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

  const loadParams = computed<MessageCenterSubsetParams>(() => {
    const timeRange = getTimeRange.value;
    return {
      type: activeTab.value,
      resourceType: activeMessageType.value,
      status: unReadEnable.value ? SystemMessageStatusEnum.UNREAD : '',
      endTime: timeRange?.endTime ?? null,
      createTime: timeRange?.startTime ?? null,
    };
  });

  function searchData(val: string) {
    keyword.value = val;
    messageListRef.value?.loadMessageList();
  }

  const messageCount = ref<Record<string, string>>({});

  const messageTypeList = computed(() => {
    const enabledModuleKeys = new Set(
      appStore.moduleConfigList.filter((module) => module.enable).map((module) => module.moduleKey.toUpperCase())
    );

    const isAnnouncementTab = activeTab.value === SystemMessageTypeEnum.ANNOUNCEMENT_NOTICE;

    const allMessage = [
      {
        value: '',
        label: t('system.message.allMessage'),
        count: isAnnouncementTab
          ? messageCount.value[SystemMessageTypeEnum.ANNOUNCEMENT_NOTICE]
          : messageCount.value?.total,
      },
    ];

    const baseMessageTypes = [
      {
        value: SystemResourceMessageTypeEnum.CLUE,
        label: t('menu.clue'),
        count: messageCount.value[SystemResourceMessageTypeEnum.CLUE] || 0,
      },
      {
        value: SystemResourceMessageTypeEnum.CUSTOMER,
        label: t('system.message.customerMessage'),
        count: messageCount.value[SystemResourceMessageTypeEnum.CUSTOMER] || 0,
      },
      {
        value: SystemResourceMessageTypeEnum.OPPORTUNITY,
        label: t('system.message.opportunityMessage'),
        count: messageCount.value[SystemResourceMessageTypeEnum.OPPORTUNITY] || 0,
      },
    ];

    if (isAnnouncementTab) {
      return allMessage;
    }
    return [...allMessage, ...baseMessageTypes.filter(({ value }) => enabledModuleKeys.has(value) || value)];
  });

  async function initMessageCount() {
    try {
      const result = await getNotificationCount({
        type: activeTab.value,
        status: '',
        resourceType: activeMessageType.value,
        createTime: null,
        endTime: null,
      });
      if (result) {
        result.forEach(({ key, count }) => {
          messageCount.value[key] = count > 99 ? `99+` : `${count ?? 0}`;
        });
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

  function changeMessageType(value: string) {
    activeMessageType.value = value;
    refresh();
  }

  function handleChangeType() {
    activeMessageType.value = '';
    refresh(true);
  }

  function changeHandler(val: boolean | string) {
    range.value = undefined;
    if (val !== 'custom') {
      refresh();
    }
  }

  function confirmTimePicker(
    value: number | [number, number] | null,
    _formattedValue: string | [string, string] | null
  ) {
    range.value = value;
    refresh();
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
        appStore.initModuleConfig();
        initMessageCount();
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
      @apply h-full;
    }
    .message-footer {
      position: absolute;
      bottom: 0;
      width: 268px;
      height: 56px;
    }
    .message-content {
      width: calc(100% - 268px);
      @apply h-full;
    }
  }
</style>
