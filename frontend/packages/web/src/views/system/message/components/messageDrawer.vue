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
        <n-switch v-model:value="unReadEnable" :rubber-band="false" size="small" @update:value="changeHandler" />
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
        <div class="p-[24px]">
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
            <div>{{ item.label }}</div>
            <div v-if="item.count !== undefined" class="text-[var(--text-n4)]">{{ item.count }}</div>
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
              @update:value="refresh"
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
          @open-approval-todo="(formType) => emit('openApprovalTodo', formType)"
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

  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import { SystemMessageStatusEnum, SystemMessageTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { MessageCenterSubsetParams } from '@lib/shared/models/system/message';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmMessageList from '@/components/business/crm-message-list/index.vue';

  import { getNotificationCount, setAllNotificationRead } from '@/api/modules';
  import useAppStore from '@/store/modules/app';
  import { findRouteByName, hasAnyPermission } from '@/utils/permission';

  import { AppRouteEnum } from '@/enums/routeEnum';

  const appStore = useAppStore();

  const { t } = useI18n();

  const router = useRouter();

  const showMessageDrawer = defineModel<boolean>('show', {
    required: true,
  });
  const emit = defineEmits<{
    (e: 'openApprovalTodo', formType: string): void;
  }>();

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

  const moduleCategoryConfig: Partial<Record<ModuleConfigEnum, { resourceType: string; label: string }>> = {
    [ModuleConfigEnum.CUSTOMER_MANAGEMENT]: { resourceType: 'CUSTOMER', label: 'system.message.customerMessage' },
    [ModuleConfigEnum.FOLLOW_MANAGEMENT]: { resourceType: 'FOLLOW', label: 'module.followManagement' },
    [ModuleConfigEnum.CLUE_MANAGEMENT]: { resourceType: 'CLUE', label: 'menu.clue' },
    [ModuleConfigEnum.BUSINESS_MANAGEMENT]: { resourceType: 'OPPORTUNITY', label: 'module.businessManagement' },
    [ModuleConfigEnum.CONTRACT]: { resourceType: 'CONTRACT', label: 'module.contract' },
    [ModuleConfigEnum.ORDER]: { resourceType: 'ORDER', label: 'module.order' },
    [ModuleConfigEnum.PRODUCT_MANAGEMENT]: { resourceType: 'PRODUCT', label: 'module.productManagement' },
    [ModuleConfigEnum.CUSTOM_FORM]: { resourceType: 'CUSTOM_FORM', label: 'module.customForm' },
  };

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
    if (activeTab.value === 'MENTION') return SystemMessageTypeEnum.COMMENT_MENTIONED;
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

  function searchData(val: string) {
    keyword.value = val;
    nextTick(() => {
      messageListRef.value?.loadMessageList();
    });
  }

  const messageCount = ref<Record<string, string>>({});

  function getModuleRouteName(moduleKey: string) {
    const routeNames: Partial<Record<ModuleConfigEnum, string>> = {
      [ModuleConfigEnum.CUSTOMER_MANAGEMENT]: 'account',
      [ModuleConfigEnum.FOLLOW_MANAGEMENT]: 'followManagement',
      [ModuleConfigEnum.CLUE_MANAGEMENT]: 'leadManagement',
      [ModuleConfigEnum.BUSINESS_MANAGEMENT]: 'opportunity',
      [ModuleConfigEnum.PRODUCT_MANAGEMENT]: 'product',
      [ModuleConfigEnum.SYSTEM_SETTINGS]: 'system',
      [ModuleConfigEnum.DASHBOARD]: 'dashboard',
      [ModuleConfigEnum.AGENT]: 'agent',
      [ModuleConfigEnum.CONTRACT]: 'contract',
      [ModuleConfigEnum.ORDER]: 'order',
      [ModuleConfigEnum.TENDER]: 'tender',
      [ModuleConfigEnum.CUSTOM_FORM]: 'customForm',
    };
    return routeNames[moduleKey as ModuleConfigEnum] ?? moduleKey;
  }

  function getCategoryCount(isPublic: boolean) {
    if (activeTab.value === 'MENTION') return undefined;
    if (isPublic) return messageCount.value[SystemMessageTypeEnum.ANNOUNCEMENT_NOTICE];
    return messageCount.value.total;
  }

  const messageTypeList = computed(() => {
    const isPublic = activeTab.value === 'PUBLIC';
    const allCategory = {
      value: '',
      label: isPublic ? t('system.message.announcement') : t('system.message.allMessage'),
      count: getCategoryCount(isPublic),
    };
    if (isPublic) return [allCategory];

    return [
      allCategory,
      {
        value: 'APPROVAL',
        label: t('system.message.settings.category.todo'),
        count: activeTab.value === 'MENTION' ? undefined : messageCount.value.APPROVAL ?? 0,
      },
      ...appStore.moduleConfigList.flatMap((module) => {
        const category = moduleCategoryConfig[module.moduleKey as ModuleConfigEnum];
        const route = findRouteByName(getModuleRouteName(module.moduleKey));
        if ((!module.enable && activeTab.value !== 'MENTION') || !category) return [];
        if (activeTab.value !== 'MENTION' && (!route || !hasAnyPermission(route.meta?.permissions as string[]))) {
          return [];
        }
        return [
          {
            value: category.resourceType,
            label: t(category.label),
            count: activeTab.value === 'MENTION' ? undefined : messageCount.value[category.resourceType] ?? 0,
          },
        ];
      }),
    ];
  });

  async function initMessageCount() {
    try {
      const result = await getNotificationCount({
        type: loadParams.value.type,
        status: '',
        resourceType: activeMessageType.value,
        createTime: null,
        endTime: null,
        triggerType: loadParams.value.triggerType,
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

  function resetMessageFilters() {
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
    activeMessageType.value = '';
    triggerType.value = '';
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
  :deep(.n-tabs-rail) {
    width: 100%;
  }
</style>
