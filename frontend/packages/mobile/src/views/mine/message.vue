<template>
  <CrmPageWrapper :title="t('common.message')">
    <div class="flex h-full flex-col overflow-hidden">
      <van-tabs v-model:active="activeName" border class="detail-tabs" @change="changeResourceTab">
        <van-tab v-for="tab of tabList" :key="tab.name" :name="tab.name">
          <template #title>
            <div class="text-[16px]" :class="activeName === tab.name ? 'text-[var(--primary-8)]' : ''">
              {{ tab.title }}
            </div>
          </template>
          <div class="flex h-full flex-col overflow-hidden">
            <div class="flex items-center gap-[12px] bg-[var(--text-n10)] p-[8px_16px]">
              <van-search
                v-model="keyword"
                shape="round"
                :placeholder="t('common.searchByName')"
                class="crm-search"
                @search="() => refreshMessageList()"
                @clear="() => refreshMessageList()"
              />
            </div>
            <div class="filter-buttons flex gap-2">
              <template v-for="item of messageButtons" :key="item.name">
                <van-badge :dot="item.count > 0">
                  <van-button
                    round
                    size="small"
                    class="!border-none !px-[16px] !py-[4px] !text-[14px]"
                    :class="
                      activeFilter === item.name
                        ? '!bg-[var(--primary-7)] !text-[var(--primary-8)]'
                        : '!bg-[var(--text-n9)] !text-[var(--text-n1)]'
                    "
                    @click="activeFilter = item.name"
                  >
                    {{ item.tab }}
                  </van-button>
                </van-badge>
              </template>
            </div>
          </div>
        </van-tab>
      </van-tabs>
      <CrmList
        ref="crmListRef"
        :list-params="listParams"
        class="p-[16px]"
        :item-gap="16"
        :keyword="keyword"
        :load-list-api="getNotificationList"
      >
        <template #item="{ item }">
          <CrmMessageItem :item="item" @load-list="() => refreshMessageList(true)" />
        </template>
      </CrmList>
    </div>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { SystemMessageTypeEnum, SystemResourceMessageTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmMessageItem from '@/components/business/crm-message-item/index.vue';

  import { getNotificationCount, getNotificationList } from '@/api/modules';
  import useAppStore from '@/store/modules/app';

  const appStore = useAppStore();
  const { t } = useI18n();

  const activeName = ref('');

  const tabList = [
    {
      name: '',
      title: t('mine.allMessage'),
    },
    {
      name: SystemMessageTypeEnum.SYSTEM_NOTICE,
      title: t('mine.systemMessage'),
    },
    {
      name: SystemMessageTypeEnum.ANNOUNCEMENT_NOTICE,
      title: t('mine.announcementMessage'),
    },
  ];

  const keyword = ref('');

  const activeFilter = ref('');

  const messageCount = ref<Record<string, number>>({});

  const messageButtons = computed(() => {
    const enabledModuleKeys = new Set(
      appStore.moduleConfigList.filter((module) => module.enable).map((module) => module.moduleKey.toUpperCase())
    );

    const isAnnouncementTab = activeName.value === SystemMessageTypeEnum.ANNOUNCEMENT_NOTICE;
    const allMessage = [
      {
        name: '',
        tab: t('mine.allMessage'),
        count: isAnnouncementTab
          ? messageCount.value[SystemMessageTypeEnum.ANNOUNCEMENT_NOTICE]
          : messageCount.value?.total,
      },
    ];

    const baseMessageTypes = [
      {
        name: SystemResourceMessageTypeEnum.CUSTOMER,
        tab: t('menu.customer'),
        count: messageCount.value[SystemResourceMessageTypeEnum.CUSTOMER] || 0,
      },
      {
        name: SystemResourceMessageTypeEnum.CLUE,
        tab: t('menu.clue'),
        count: messageCount.value[SystemResourceMessageTypeEnum.CLUE] || 0,
      },
      {
        name: SystemResourceMessageTypeEnum.OPPORTUNITY,
        tab: t('mine.opportunityMessage'),
        count: messageCount.value[SystemResourceMessageTypeEnum.OPPORTUNITY] || 0,
      },
    ];

    if (isAnnouncementTab) {
      return allMessage;
    }
    return [...allMessage, ...baseMessageTypes.filter(({ name }) => enabledModuleKeys.has(name) || name)];
  });

  const listParams = computed(() => {
    return {
      type: activeName.value,
      resourceType: activeName.value === SystemMessageTypeEnum.ANNOUNCEMENT_NOTICE ? '' : activeFilter.value,
      status: '',
      endTime: null,
      keyword: keyword.value,
      createTime: null,
    };
  });

  async function initSystemMessageCount() {
    try {
      const result = await getNotificationCount({
        type: '',
        status: '',
        resourceType: '',
        createTime: null,
        endTime: null,
      });

      if (result) {
        result.forEach(({ key, count }) => {
          messageCount.value[key] = count || 0;
        });
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const crmListRef = ref<InstanceType<typeof CrmList>>();

  function refreshMessageList(refreshCount = false) {
    nextTick(() => {
      crmListRef.value?.loadList(true);
    });
    if (refreshCount) {
      initSystemMessageCount();
    }
  }

  function changeResourceTab() {
    refreshMessageList();
  }

  watch(
    () => activeFilter.value,
    () => {
      refreshMessageList();
    }
  );

  onBeforeMount(() => {
    initSystemMessageCount();
  });
</script>

<style lang="less" scoped>
  .detail-tabs {
    :deep(.van-hairline--top-bottom) {
      margin-top: -0.5px;
    }
  }
  .filter-buttons {
    @apply flex;

    gap: 8px;
    padding: 8px 4px;
    background-color: var(--text-n10);
    .half-px-border-bottom();
  }
</style>
