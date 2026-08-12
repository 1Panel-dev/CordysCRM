<template>
  <div class="flex h-full flex-col">
    <div class="flex items-center justify-between gap-[12px] bg-[var(--text-n10)] px-[12px] py-[4px]">
      <CrmAvatar :is-word="false" @click="goMine" />
      <van-search
        v-if="lastScopedOptions?.length"
        v-model="keyword"
        shape="round"
        :placeholder="t('workbench.searchPlaceholder')"
        class="flex-1 !p-0"
        @click="goDuplicateCheck"
      />
      <CrmIcon v-if="hasValidApiKey" name="icon-bot" width="21px" height="21px" @click="goAgent" />
      <van-badge :dot="showBadge">
        <CrmIcon name="iconicon_notification" class="mt-[4px]" width="21px" height="21px" @click="goMineMessage" />
      </van-badge>
    </div>
    <div class="flex flex-1 flex-col overflow-auto">
      <div>
        <van-notice-bar
          v-if="useStore.userInfo.defaultPwd"
          :scrollable="false"
          wrapable
          mode="closeable"
          @close="showAlert = false"
        >
          <span>{{ t('mine.changePasswordTip') }}</span>
          <span class="ml-[8px] text-[var(--primary-8)]" @click="changePassword">{{ t('mine.changePassword') }}</span>
        </van-notice-bar>
        <CrmSegmentTabs
          v-model="activeWorkbenchTab"
          :options="workbenchTabOptions"
          @change="handleWorkbenchTabChange"
        />
        <van-cell-group
          v-if="activeWorkbenchTab === WorkbenchHomeTabEnum.DASHBOARD"
          class="px-[20px] py-[16px]"
          :border="false"
        >
          <van-cell :border="false" class="!py-0">
            <template #title>
              <div class="font-semibold text-[var(--text-n1)]">{{ t('workbench.task') }}</div>
            </template>
            <template #value>
              <div class="text-[var(--text-n4)]" @click="goTask()">
                {{ t('common.checkMore') }}
              </div>
            </template>
          </van-cell>
          <div class="mt-[12px] flex gap-[8px] px-[16px]">
            <div
              class="approval-card rounded-[var(--border-radius-small)] p-[12px]"
              @click="goTask(ApprovalListTypeEnum.PENDING)"
            >
              <div
                class="flex h-[24px] w-[24px] items-center justify-center rounded-[var(--border-radius-small)] bg-[var(--text-n10)]"
              >
                <CrmIcon name="iconicon_contract" width="16px" height="16px" color="var(--info-blue)" />
              </div>
              <div class="mt-[8px] flex items-baseline justify-between font-semibold text-[var(--text-n10)]">
                <div class="text-[14px]">{{ t('workbench.myApproval') }}</div>
                <div class="text-[16px]">{{ todoStatistic?.total }}</div>
              </div>
            </div>
            <div class="flex flex-1 flex-col gap-[4px]">
              <div class="task-card justify-between" @click="goTask(ApprovalListTypeEnum.COPIED)">
                <div class="flex items-center gap-[8px]">
                  <div class="task-icon border border-[var(--warning-yellow)]">
                    <CrmIcon name="iconicon_send" width="14px" height="14px" color="var(--warning-yellow)" />
                  </div>
                  <div>{{ t('workbench.copyToMe') }}</div>
                </div>
              </div>
              <div class="flex items-center gap-[4px]">
                <div class="task-card flex-1 gap-[8px]" @click="goTask(ApprovalListTypeEnum.INITIATED)">
                  <div class="task-icon border border-[var(--primary-8)]">
                    <CrmIcon name="iconicon_add" width="14px" height="14px" color="var(--primary-8)" />
                  </div>
                  <div class="text-[14px]">{{ t('workbench.myApply') }}</div>
                </div>
                <div class="task-card flex-1 gap-[8px]" @click="goTask(ApprovalListTypeEnum.APPROVAL)">
                  <div class="task-icon border border-[var(--success-green)]">
                    <CrmIcon name="iconicon_check_circle" width="14px" height="14px" color="var(--success-green)" />
                  </div>
                  <div class="text-[14px]">{{ t('workbench.myProcess') }}</div>
                </div>
              </div>
            </div>
          </div>
        </van-cell-group>
      </div>

      <template v-if="activeWorkbenchTab === WorkbenchHomeTabEnum.DASHBOARD">
        <van-cell-group class="my-[16px] px-[20px] py-[16px]">
          <van-cell :border="false" class="!py-0">
            <template #title>
              <div class="font-semibold text-[var(--text-n1)]">{{ t('workbench.quickEntry') }}</div>
            </template>
          </van-cell>
          <div class="flex flex-wrap">
            <div
              v-for="card of entryCardList"
              :key="card.name"
              v-permission="card.permission"
              class="quick-entry-card"
              @click="goCardRoute(card.name)"
            >
              <CrmIcon :name="card.icon" width="30px" height="30px" />
              <div class="text-[12px] text-[var(--text-n1)]">{{ card.label }}</div>
            </div>
          </div>
        </van-cell-group>
      </template>
      <div v-if="activeWorkbenchTab === WorkbenchHomeTabEnum.DASHBOARD" class="bg-[var(--text-n10)] py-[16px]">
        <van-cell-group class="flex-1 px-[8px]" :border="false">
          <div class="filter-buttons mb-[8px] flex flex-nowrap gap-[4px]">
            <van-button
              v-for="item of followTabList"
              :key="item.name"
              round
              size="small"
              class="!flex-1 !border-none !px-[16px] !py-[4px] !text-[14px]"
              :class="
                activeNameFollowType === item.name
                  ? '!bg-[var(--primary-7)] !text-[var(--primary-8)]'
                  : '!bg-[var(--text-n9)] !text-[var(--text-n1)]'
              "
              block
              @click="activeNameFollowType = item.name"
            >
              {{ item.tab }}
            </van-button>
          </div>
        </van-cell-group>
        <van-cell-group class="flex-1 px-[20px]" :border="false">
          <followRecordList v-if="activeNameFollowType === 'followRecord'" />
          <followPlanList v-if="activeNameFollowType === 'followPlan'" />
        </van-cell-group>
      </div>

      <SmartWorkbench v-else-if="activeWorkbenchTab === WorkbenchHomeTabEnum.SMART" />
    </div>
    <AiMobileEntryComposer v-if="showAiEntryComposer" @submit="goAiChat" />
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ApprovalListTypeEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { MessageCenterItem } from '@lib/shared/models/system/message';
  import type { TodoStatistic } from '@lib/shared/models/system/process';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmSegmentTabs from '@/components/pure/crm-segment-tabs/index.vue';
  import AiMobileEntryComposer from '@/components/business/ai-chat/components/AiMobileEntryComposer.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';
  import followPlanList from '@/views/workbench/follow/followPlanList.vue';
  import followRecordList from '@/views/workbench/follow/followRecordList.vue';
  import SmartWorkbench from '@/views/workbench/smart/index.vue';

  import { getTodoStatistic } from '@/api/modules';
  import useAppStore from '@/store/modules/app';
  import useLicenseStore from '@/store/modules/setting/license';
  import useUserStore from '@/store/modules/user';
  import showNoLicenseDialog from '@/utils/license';

  import { CommonRouteEnum, MineRouteEnum, WorkbenchRouteEnum } from '@/enums/routeEnum';

  import { lastScopedOptions } from './duplicateCheck/config';

  defineOptions({
    name: WorkbenchRouteEnum.WORKBENCH_INDEX,
  });

  const appStore = useAppStore();
  const licenseStore = useLicenseStore();
  const userStore = useUserStore();
  const { t } = useI18n();
  const router = useRouter();

  const useStore = useUserStore();

  const showAlert = ref(useStore.userInfo.defaultPwd);

  const activeNameFollowType = ref('followRecord');
  const followTabList = ref([
    {
      name: 'followRecord',
      tab: t('common.recordBusiness'),
      permission: [],
    },
    {
      name: 'followPlan',
      tab: t('common.planBusiness'),
      permission: [],
    },
  ]);

  enum WorkbenchHomeTabEnum {
    SMART = 'smart',
    DASHBOARD = 'dashboard',
  }

  function getDefaultWorkbenchTab(): WorkbenchHomeTabEnum {
    return licenseStore.hasLicense() ? WorkbenchHomeTabEnum.SMART : WorkbenchHomeTabEnum.DASHBOARD;
  }

  const activeWorkbenchTab = ref(getDefaultWorkbenchTab());
  const workbenchTabOptions = computed(() => [
    {
      label: t('workbench.smartWorkbench'),
      value: WorkbenchHomeTabEnum.SMART,
    },
    {
      label: t('workbench.myDashboard'),
      value: WorkbenchHomeTabEnum.DASHBOARD,
    },
  ]);

  function handleWorkbenchTabChange(value: string | number): void {
    if (value !== WorkbenchHomeTabEnum.SMART || licenseStore.hasLicense()) {
      return;
    }

    activeWorkbenchTab.value = WorkbenchHomeTabEnum.DASHBOARD;
    showNoLicenseDialog(t);
  }

  function changePassword() {
    router.push({
      name: MineRouteEnum.MINE_DETAIL,
      query: {
        type: 'resetPassWord',
      },
    });
  }

  const keyword = ref('');
  const entryCardList = [
    {
      icon: 'icon-newClue',
      label: t('common.newClue'),
      name: FormDesignKeyEnum.CLUE,
      permission: ['CLUE_MANAGEMENT:ADD'],
    },
    {
      icon: 'icon-newCustomer',
      label: t('common.newCustomer'),
      name: FormDesignKeyEnum.CUSTOMER,
      permission: ['CUSTOMER_MANAGEMENT:ADD'],
    },
    {
      icon: 'icon-newContact',
      label: t('common.newContact'),
      name: FormDesignKeyEnum.CUSTOMER_CONTACT,
      permission: ['CUSTOMER_MANAGEMENT_CONTACT:ADD'],
    },
    {
      icon: 'icon-newOpportunity',
      label: t('common.newOpportunity'),
      name: FormDesignKeyEnum.BUSINESS,
      permission: ['OPPORTUNITY_MANAGEMENT:ADD'],
    },
    // {
    //   icon: 'icon-newRecord',
    //   label: t('common.newFollowRecord'),
    //   name: 'followRecord',
    // },
    // {
    //   icon: 'icon-newPlan',
    //   label: t('common.newFollowPlan'),
    //   name: 'followPlan',
    // },
  ];

  function goMine() {
    router.push({ name: MineRouteEnum.MINE_INDEX });
  }

  function goMineMessage() {
    router.push({ name: MineRouteEnum.MINE_MESSAGE });
  }

  const hasValidApiKey = computed(() => userStore.apiKeyList.some((key) => !key.isExpire && key.enable));
  const showAiEntryComposer = computed(() => activeWorkbenchTab.value === WorkbenchHomeTabEnum.SMART);

  function goAgent() {
    router.push({ name: WorkbenchRouteEnum.WORKBENCH_AGENT });
  }

  function goAiChat(content?: string) {
    const prompt = content?.trim();

    router.push({
      name: WorkbenchRouteEnum.WORKBENCH_AI_CHAT,
      query: prompt
        ? {
            prompt,
          }
        : undefined,
    });
  }

  function goDuplicateCheck() {
    router.push({ name: WorkbenchRouteEnum.WORKBENCH_DUPLICATE_CHECK });
  }

  function goCardRoute(formKey: FormDesignKeyEnum) {
    router.push({ name: CommonRouteEnum.FORM_CREATE, query: { formKey } });
  }

  const showBadge = computed(() => {
    return !appStore.messageInfo.read;
  });

  const messageList = ref<MessageCenterItem[]>([]);

  watch(
    () => appStore.messageInfo.notificationDTOList,
    (val) => {
      messageList.value = val ?? [];
    }
  );

  const todoStatistic = ref<TodoStatistic>();
  async function initTodoStatistic() {
    try {
      todoStatistic.value = await getTodoStatistic();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function goTask(type?: ApprovalListTypeEnum) {
    localStorage.setItem('activeTaskType', type?.toString() || '');
    router.push({ name: WorkbenchRouteEnum.WORKBENCH_TASK });
  }

  onBeforeMount(() => {
    appStore.initMessage();
    appStore.connectSystemMessageSSE();
    appStore.initStageConfig();
    userStore.initApiKeyList();
    initTodoStatistic();
  });

  onActivated(() => {
    activeWorkbenchTab.value = getDefaultWorkbenchTab();
  });
</script>

<style lang="less" scoped>
  .approval-card {
    width: 114px;
    height: 80px;
    background-position: center;
    background-repeat: no-repeat;
    background-size: cover;
    background-image: url('@/assets/svg/approvalBg.svg'), linear-gradient(to left, #4d7ef2 5%, #1c60ff);
  }
  .task-card {
    @apply flex items-center;

    padding: 8px 4px;
    border-radius: var(--border-radius-small);
    background-color: var(--text-n9);
    .task-icon {
      @apply flex items-center;

      padding: 2px;
      border-radius: var(--border-radius-small);
    }
  }
  .quick-entry-card {
    @apply flex basis-1/4 flex-col items-center;

    gap: 8px;
    padding-top: 16px;
    padding-bottom: 8px;
    &:active {
      background-color: var(--text-n9);
    }
  }
  :deep(.workbench-follow-item) {
    .follow-item-content {
      padding: 16px 0 !important;
    }
  }
</style>

<style lang="less">
  .follow-view {
    .half-px-border-bottom();
  }
</style>
