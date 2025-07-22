<template>
  <!-- 先不上 -->
  <!-- <CrmCard hide-footer auto-height class="mb-[16px]">
    <div class="flex gap-[16px]">
      <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="segment" class="w-fit" />
      <n-date-picker v-if="activeTab === 'custom'" v-model:value="rangeTime" type="datetimerange" class="w-[360px]" />
    </div>
  </CrmCard> -->
  <n-scrollbar x-scrollable content-style="min-width: 1000px;height: 100%;width: 100%">
    <div class="workbench-card">
      <dataOverview />
      <div class="flex h-full w-full">
        <div class="h-full flex-1">
          <QuickAccess @refresh="refresh" />
          <CrmCard hide-footer :special-height="hasAnyPermission(quickAccessPermissionList) ? 177 : 160">
            <div class="title">
              <div class="title-name">{{ t('system.personal.plan') }}</div>
              <div class="title-right" @click="showPersonalInfo = true">{{ t('common.ViewMore') }}</div>
            </div>
            <FollowDetail
              :refresh-key="refreshKey"
              class="mt-[16px]"
              active-type="followPlan"
              wrapper-class="h-[calc(100%-38px)] !p-[0px] !pr-[1px]"
              :virtual-scroll-height="`${
                hasAnyPermission(quickAccessPermissionList) ? 'calc(100vh - 402px)' : 'calc(100vh - 235px)'
              }`"
              follow-api-key="myPlan"
              source-id="NULL"
              :any-permission="['CUSTOMER_MANAGEMENT:READ', 'OPPORTUNITY_MANAGEMENT:READ', 'CLUE_MANAGEMENT:READ']"
            />
          </CrmCard>
        </div>
        <CrmCard hide-footer no-content-padding class="ml-[16px] w-[400px]">
          <div class="h-full p-[24px]">
            <div class="title !mb-[8px]">
              <div class="title-name">{{ t('system.message.notify') }}</div>
              <div class="title-right" @click="showMessageDrawer = true">
                {{ t('common.ViewMore') }}
              </div>
            </div>
            <CrmMessageList
              ref="messageListRef"
              :message-list="messageList"
              virtual-scroll-height="calc(100vh - 168px)"
              key-field="id"
            />
          </div>
        </CrmCard>
        <PersonalInfoDrawer v-model:visible="showPersonalInfo" v-model:active-tab-value="personalTab" />
        <MessageDrawer v-model:show="showMessageDrawer" />
      </div>
    </div>
  </n-scrollbar>
</template>

<script setup lang="ts">
  // import { NDatePicker } from 'naive-ui';
  import { NScrollbar } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { PersonalEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  // import CrmTab from '@/components/pure/crm-tab/index.vue';
  import FollowDetail from '@/components/business/crm-follow-detail/index.vue';
  import CrmMessageList from '@/components/business/crm-message-list/index.vue';
  import dataOverview from './components/dataOverview.vue';
  import QuickAccess from './components/quickAccess.vue';
  import PersonalInfoDrawer from '@/views/system/business/components/personalInfoDrawer.vue';
  import MessageDrawer from '@/views/system/message/components/messageDrawer.vue';

  import { quickAccessList } from '@/config/workbench';
  import useAppStore from '@/store/modules/app';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();
  const appStore = useAppStore();

  // 先不上
  // const activeTab = ref('3');
  // const tabList = [
  //   {
  //     name: '3',
  //     tab: t('workbench.nearlyThreeDays'),
  //   },
  //   {
  //     name: '7',
  //     tab: t('workbench.nearlySevenDays'),
  //   },
  //   {
  //     name: '30',
  //     tab: t('workbench.lastOneMonth'),
  //   },
  //   {
  //     name: 'custom',
  //     tab: t('common.custom'),
  //   },
  // ];
  // const rangeTime = ref<[number, number]>();

  const quickAccessPermissionList = computed(() => quickAccessList.flatMap((item) => item.permission));

  // 计划
  const showPersonalInfo = ref<boolean>(false);
  const personalTab = ref(PersonalEnum.MY_PLAN);

  const refreshKey = ref(0);

  watch(
    () => showPersonalInfo.value,
    (val) => {
      if (!val) {
        refreshKey.value += 1;
      }
    }
  );

  function refresh(key: FormDesignKeyEnum) {
    if (key === FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS) {
      refreshKey.value += 1;
    }
  }

  // 消息
  const showMessageDrawer = ref(false);

  const messageList = computed(() => {
    return appStore.messageInfo.notificationDTOList;
  });

  onBeforeMount(() => {
    appStore.initMessage();
  });
</script>

<style lang="less" scoped>
  .title {
    display: flex;
    justify-content: space-between;
    .title-name {
      font-weight: 600;
    }
    .title-right {
      color: var(--text-n4);
      cursor: pointer;
      &:hover {
        color: var(--text-n1);
      }
    }
  }
  :deep(.crm-message-item) {
    padding: 8px 0;
  }
  .workbench-card {
    height: calc(100vh - 88px);
    @apply w-full overflow-auto;
    .crm-scroll-bar();
  }
</style>
