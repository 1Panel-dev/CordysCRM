<template>
  <!-- 先不上 -->
  <!-- <CrmCard hide-footer auto-height class="mb-[16px]">
    <div class="flex gap-[16px]">
      <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="segment" class="w-fit" />
      <n-date-picker v-if="activeTab === 'custom'" v-model:value="rangeTime" type="datetimerange" class="w-[360px]" />
    </div>
  </CrmCard> -->
  <div class="flex h-full flex-col overflow-hidden">
    <n-alert
      v-if="useStore.userInfo.defaultPwd"
      type="warning"
      closable
      class="mb-[16px]"
      @after-leave="showAlert = false"
    >
      <span>{{ t('system.personal.changePasswordTip') }}</span>
      <n-button class="ml-[8px]" text type="primary" @click="changePassword">
        {{ t('system.personal.changePassword') }}
      </n-button>
    </n-alert>
    <n-scrollbar x-scrollable content-style="min-width: 1000px;height: 100%;width: 100%">
      <div class="workbench-card" :style="{ height: `calc(100vh - ${!showAlert ? 88 : 154}px)` }">
        <dataOverviewIndex />
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
                  hasAnyPermission(quickAccessPermissionList)
                    ? `calc(100vh - ${!showAlert ? 402 : 464}px)`
                    : `calc(100vh - ${!showAlert ? 235 : 297}px)`
                }`"
                follow-api-key="myPlan"
                source-id="NULL"
                :any-permission="['CUSTOMER_MANAGEMENT:READ', 'OPPORTUNITY_MANAGEMENT:READ', 'CLUE_MANAGEMENT:READ']"
              />
            </CrmCard>
          </div>
          <div class="flex h-full flex-wrap">
            <CrmCard hide-footer no-content-padding auto-height class="ml-[16px] w-full">
              <div class="max-h-[352px] p-[24px]">
                <div class="title !mb-[8px]">
                  <div class="title-name">{{ t('workbench.dataOverview.myTasks') }}</div>
                </div>
                <div class="grid grid-cols-2 gap-[8px_16px]">
                  <div class="task-item" @click="openTaskDrawer('pending')">
                    <div class="task-icon bg-[var(--info-blue)]">
                      <CrmIcon type="iconicon_contract" :size="16" color="var(--text-n10)" />
                    </div>
                    <div class="flex flex-1 items-center justify-between">
                      {{ t('workbench.dataOverview.pendingApproval') }}
                      <div class="font-semibold text-[var(--primary-8)]">{{ pendingApprovalCount }}</div>
                    </div>
                  </div>
                  <div class="task-item" @click="openTaskDrawer('approved')">
                    <div class="task-icon bg-[var(--success-green)]">
                      <CrmIcon type="iconicon_check_circle" :size="16" color="var(--text-n10)" />
                    </div>
                    {{ t('workbench.dataOverview.approvedByMe') }}
                  </div>
                  <div class="task-item" @click="openTaskDrawer('initiated')">
                    <div class="task-icon bg-[var(--primary-8)]">
                      <CrmIcon type="iconicon_add" :size="16" color="var(--text-n10)" />
                    </div>
                    {{ t('workbench.dataOverview.initiatedByMe') }}
                  </div>
                  <div class="task-item" @click="openTaskDrawer('copied')">
                    <div class="task-icon bg-[var(--warning-yellow)]">
                      <CrmIcon type="iconicon_send" :size="16" color="var(--text-n10)" />
                    </div>
                    {{ t('workbench.dataOverview.copiedToMe') }}
                  </div>
                </div>
              </div>
            </CrmCard>
            <CrmCard hide-footer no-content-padding auto-height class="ml-[16px] mt-[16px] flex-1">
              <div class="p-[24px]">
                <div class="title !mb-[8px]">
                  <div class="title-name">{{ t('system.message.notify') }}</div>
                  <div class="title-right" @click="showMessageDrawer = true">
                    {{ t('common.ViewMore') }}
                  </div>
                </div>
                <CrmMessageList
                  ref="messageListRef"
                  :message-list="messageList"
                  :virtual-scroll-height="`calc(100vh - ${!showAlert ? 168 : 230}px)`"
                  key-field="id"
                />
              </div>
            </CrmCard>
          </div>
          <PersonalInfoDrawer v-model:visible="showPersonalInfo" v-model:active-tab-value="personalTab" />
          <MessageDrawer v-model:show="showMessageDrawer" />
        </div>
      </div>
    </n-scrollbar>

    <EditPasswordModal v-model:show="showEditPasswordModal" />
  </div>
  <CrmTaskDrawer v-model:show="showTaskDrawer" :type="activeTaskType" />
</template>

<script setup lang="ts">
  // import { NDatePicker } from 'naive-ui';
  import { NAlert, NButton, NScrollbar } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { PersonalEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  // import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import FollowDetail from '@/components/business/crm-follow-detail/index.vue';
  import CrmMessageList from '@/components/business/crm-message-list/index.vue';
  import CrmTaskDrawer from '@/components/business/crm-task-drawer/index.vue';
  import dataOverviewIndex from './components/dataOverviewIndex.vue';
  import QuickAccess from './components/quickAccess.vue';
  import EditPasswordModal from '@/views/system/business/components/editPasswordModal.vue';
  import PersonalInfoDrawer from '@/views/system/business/components/personalInfoDrawer.vue';
  import MessageDrawer from '@/views/system/message/components/messageDrawer.vue';

  import { quickAccessList } from '@/config/workbench';
  import { useAppStore, useUserStore } from '@/store';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();
  const appStore = useAppStore();
  const useStore = useUserStore();

  const showAlert = ref(useStore.userInfo.defaultPwd);
  const showEditPasswordModal = ref<boolean>(false);
  function changePassword() {
    showEditPasswordModal.value = true;
  }

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

  // 待办
  const pendingApprovalCount = ref(0);
  const showTaskDrawer = ref(false);
  const activeTaskType = ref<'pending' | 'approved' | 'initiated' | 'copied'>('pending');
  function openTaskDrawer(type: 'pending' | 'approved' | 'initiated' | 'copied') {
    activeTaskType.value = type;
    showTaskDrawer.value = true;
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
  .task-item {
    @apply flex cursor-pointer items-center;

    padding: 8px;
    height: 40px;
    border-radius: var(--border-radius-small);
    background-color: var(--text-n9);
    .task-icon {
      @apply flex items-center justify-center;

      margin-right: 8px;
      width: 24px;
      height: 24px;
      border-radius: 6px;
    }
  }
  :deep(.crm-message-item) {
    padding: 8px 0;
  }
  .workbench-card {
    @apply w-full overflow-auto;
    .crm-scroll-bar();
  }
  :deep(.n-alert__icon) {
    margin-top: 16px;
  }
</style>
