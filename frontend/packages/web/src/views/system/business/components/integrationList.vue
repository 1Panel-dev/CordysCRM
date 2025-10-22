<template>
  <CrmCard hide-footer auto-height>
    <div class="content-title mb-[16px]">{{ t('system.business.authenticationSettings.collaborativeSoftware') }}</div>
    <CrmTab
      v-model:active-tab="activePlatformTab"
      class="mb-[8px]"
      no-content
      :tab-list="tabList"
      type="segment"
      :before-leave="handleBeforeLeave"
    />
    <div v-if="platFormIntegrationList.length" class="grid gap-[16px] xl:grid-cols-2 2xl:grid-cols-3">
      <div
        v-for="item of platFormIntegrationList"
        :key="item.type"
        class="flex h-[140px] flex-col justify-between rounded-[6px] border border-solid border-[var(--text-n8)] bg-[var(--text-n10)] p-[24px]"
      >
        <div class="flex">
          <div class="mr-[8px] flex h-[40px] w-[40px] items-center justify-center rounded-[2px] bg-[var(--text-n9)]">
            <CrmSvgIcon
              v-if="[CompanyTypeEnum.DATA_EASE, CompanyTypeEnum.SQLBot].includes(item.type as CompanyTypeEnum)"
              :name="item.logo"
              width="24px"
              height="24px"
            />
            <CrmIcon v-else :type="item.logo" :size="24"></CrmIcon>
          </div>
          <div class="flex-1">
            <div class="flex justify-between gap-[8px]">
              <div>
                <span class="mr-[8px] font-medium">{{ item.title }}</span>
                <CrmTag v-if="!item.hasConfig" theme="light" size="small" custom-class="px-[4px]">
                  {{ t('system.business.notConfigured') }}
                </CrmTag>
                <CrmTag
                  v-else-if="item.hasConfig && item.response.verify === false"
                  theme="light"
                  type="error"
                  size="small"
                  custom-class="px-[4px]"
                >
                  {{ t('common.fail') }}
                </CrmTag>
                <CrmTag
                  v-else-if="item.hasConfig && item.response.verify === null"
                  theme="light"
                  type="warning"
                  size="small"
                  custom-class="px-[4px]"
                >
                  {{ t('common.unVerify') }}
                </CrmTag>
                <CrmTag v-else theme="light" type="success" size="small" custom-class="px-[4px]">
                  {{ t('common.success') }}
                </CrmTag>
              </div>

              <div>
                <n-button
                  v-if="item.type === CompanyTypeEnum.DATA_EASE && item.response.deModuleEmbedding"
                  v-permission="['SYSTEM_SETTING:UPDATE']"
                  size="small"
                  type="default"
                  class="outline--secondary mr-[8px] px-[8px]"
                  @click="handleSyncDE()"
                >
                  {{ t('common.sync') }}
                </n-button>
                <n-button
                  v-permission="['SYSTEM_SETTING:UPDATE']"
                  size="small"
                  type="default"
                  class="outline--secondary mr-[8px] px-[8px]"
                  @click="handleEdit(item)"
                >
                  {{ t('common.config') }}
                </n-button>
                <n-button
                  :disabled="!item.hasConfig"
                  size="small"
                  type="default"
                  class="outline--secondary px-[8px]"
                  @click="testLink(item)"
                >
                  {{ t('system.business.mailSettings.testLink') }}
                </n-button>
              </div>
            </div>
            <p class="text-[12px] text-[var(--text-n4)]">{{ item.description }}</p>
          </div>
        </div>

        <div class="flex justify-between gap-[8px]">
          <div class="flex items-center gap-[8px]">
            <n-tooltip :disabled="item.response.verify">
              <template #trigger>
                <n-switch
                  size="small"
                  :rubber-band="false"
                  :value="item.response.startEnable"
                  :disabled="!item.hasConfig || !item.response.verify || !hasAnyPermission(['SYSTEM_SETTING:UPDATE'])"
                  @update:value="handleChangeEnable(item, 'startEnable')"
                />
              </template>
              {{ t('system.business.notConfiguredTip') }}
            </n-tooltip>
            <div class="text-[12px]">{{ t('system.business.authenticationSettings.syncUser') }}</div>
            <n-tooltip trigger="hover">
              <template #trigger>
                <CrmIcon
                  type="iconicon_help_circle"
                  :size="16"
                  class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
                />
              </template>
              <template #default>
                <div>
                  <div>{{ t('system.business.authenticationSettings.syncUsersToolTitle', { type: item.title }) }}</div>
                  <div>{{ t('system.business.authenticationSettings.syncUsersOpenTip', { type: item.title }) }}</div>
                  <div>{{ t('system.business.authenticationSettings.syncUsersTipContent', { type: item.title }) }}</div>
                </div>
              </template>
            </n-tooltip>
          </div>
        </div>
      </div>
    </div>
  </CrmCard>
  <CrmCard class="my-[16px]" hide-footer auto-height :loading="loading">
    <div class="content-title">{{ t('system.business.authenticationSettings.openSourceDataTools') }}</div>
    <div v-if="integrationList.length" class="grid gap-[16px] xl:grid-cols-2 2xl:grid-cols-3">
      <div
        v-for="item of integrationList"
        :key="item.type"
        class="flex h-[140px] flex-col justify-between rounded-[6px] border border-solid border-[var(--text-n8)] bg-[var(--text-n10)] p-[24px]"
      >
        <div class="flex">
          <div class="mr-[8px] flex h-[40px] w-[40px] items-center justify-center rounded-[2px] bg-[var(--text-n9)]">
            <CrmSvgIcon
              v-if="[CompanyTypeEnum.DATA_EASE, CompanyTypeEnum.SQLBot].includes(item.type as CompanyTypeEnum)"
              :name="item.logo"
              width="24px"
              height="24px"
            />
            <CrmIcon v-else :type="item.logo" :size="24"></CrmIcon>
          </div>
          <div class="flex-1">
            <div class="flex justify-between gap-[8px]">
              <div>
                <span class="mr-[8px] font-medium">{{ item.title }}</span>
                <CrmTag v-if="!item.hasConfig" theme="light" size="small" custom-class="px-[4px]">
                  {{ t('system.business.notConfigured') }}
                </CrmTag>
                <CrmTag
                  v-else-if="item.hasConfig && item.response.verify === false"
                  theme="light"
                  type="error"
                  size="small"
                  custom-class="px-[4px]"
                >
                  {{ t('common.fail') }}
                </CrmTag>
                <CrmTag
                  v-else-if="item.hasConfig && item.response.verify === null"
                  theme="light"
                  type="warning"
                  size="small"
                  custom-class="px-[4px]"
                >
                  {{ t('common.unVerify') }}
                </CrmTag>
                <CrmTag v-else theme="light" type="success" size="small" custom-class="px-[4px]">
                  {{ t('common.success') }}
                </CrmTag>
              </div>

              <div>
                <n-button
                  v-if="item.type === CompanyTypeEnum.DATA_EASE && item.response.deModuleEmbedding"
                  v-permission="['SYSTEM_SETTING:UPDATE']"
                  size="small"
                  type="default"
                  class="outline--secondary mr-[8px] px-[8px]"
                  @click="handleSyncDE()"
                >
                  {{ t('common.sync') }}
                </n-button>
                <n-button
                  v-permission="['SYSTEM_SETTING:UPDATE']"
                  size="small"
                  type="default"
                  class="outline--secondary mr-[8px] px-[8px]"
                  @click="handleEdit(item)"
                >
                  {{ t('common.config') }}
                </n-button>
                <n-button
                  :disabled="!item.hasConfig"
                  size="small"
                  type="default"
                  class="outline--secondary px-[8px]"
                  @click="testLink(item)"
                >
                  {{ t('system.business.mailSettings.testLink') }}
                </n-button>
              </div>
            </div>
            <p class="text-[12px] text-[var(--text-n4)]">{{ item.description }}</p>
          </div>
        </div>
        <div v-if="item.type === CompanyTypeEnum.DATA_EASE" class="flex items-center gap-[8px]">
          <n-tooltip :disabled="item.response.verify">
            <template #trigger>
              <n-switch
                size="small"
                :rubber-band="false"
                :value="item.response.deBoardEnable"
                :disabled="!item.hasConfig || !item.response.verify || !hasAnyPermission(['SYSTEM_SETTING:UPDATE'])"
                @update:value="handleChangeEnable(item, 'deBoardEnable')"
              />
            </template>
            {{ t('system.business.notConfiguredTip') }}
          </n-tooltip>
          <div class="text-[12px]">{{ t('common.dashboard') }}</div>
        </div>
        <div v-else-if="item.type === CompanyTypeEnum.SQLBot" class="flex justify-between gap-[8px]">
          <!--          <div class="flex items-center gap-[8px]">
            <n-tooltip :disabled="item.response.verify">
              <template #trigger>
                <n-switch
                  size="small"
                  :rubber-band="false"
                  :value="item.response.sqlBotBoardEnable"
                  :disabled="!item.hasConfig || !item.response.verify || !hasAnyPermission(['SYSTEM_SETTING:UPDATE'])"
                  @update:value="handleChangeEnable(item, 'sqlBotBoardEnable')"
                />
              </template>
              {{ t('system.business.notConfiguredTip') }}
            </n-tooltip>
            <div class="text-[12px]">{{ t('common.dashboard') }}</div>
          </div>-->
          <div class="flex items-center gap-[8px]">
            <n-tooltip :disabled="item.response.verify">
              <template #trigger>
                <n-switch
                  size="small"
                  :rubber-band="false"
                  :value="item.response.sqlBotChatEnable"
                  :disabled="!item.hasConfig || !item.response.verify || !hasAnyPermission(['SYSTEM_SETTING:UPDATE'])"
                  @update:value="handleChangeEnable(item, 'sqlBotChatEnable')"
                />
              </template>
              {{ t('system.business.notConfiguredTip') }}
            </n-tooltip>
            <div class="text-[12px]">{{ t('system.business.SQLBot.switch') }}</div>
          </div>
        </div>
      </div>
    </div>
  </CrmCard>
  <CrmCard hide-footer auto-height>
    <div class="content-title mb-[16px]">{{ t('system.business.agent.agentTitle') }}</div>
    <div v-if="agentIntegrationList.length" class="grid gap-[16px] xl:grid-cols-2 2xl:grid-cols-3">
      <div
        v-for="item of agentIntegrationList"
        :key="item.type"
        class="flex h-[140px] flex-col justify-between rounded-[6px] border border-solid border-[var(--text-n8)] bg-[var(--text-n10)] p-[24px]"
      >
        <div class="flex">
          <div class="mr-[8px] flex h-[40px] w-[40px] items-center justify-center rounded-[2px] bg-[var(--text-n9)]">
            <CrmSvgIcon
              v-if="[CompanyTypeEnum.MAXKB].includes(item.type as CompanyTypeEnum)"
              :name="item.logo"
              width="24px"
              height="24px"
            />
            <CrmIcon v-else :type="item.logo" :size="24"></CrmIcon>
          </div>
          <div class="flex-1">
            <div class="flex justify-between gap-[8px]">
              <div>
                <span class="mr-[8px] font-medium">{{ item.title }}</span>
                <CrmTag v-if="!item.hasConfig" theme="light" size="small" custom-class="px-[4px]">
                  {{ t('system.business.notConfigured') }}
                </CrmTag>
                <CrmTag
                  v-else-if="item.hasConfig && item.response.verify === false"
                  theme="light"
                  type="error"
                  size="small"
                  custom-class="px-[4px]"
                >
                  {{ t('common.fail') }}
                </CrmTag>
                <CrmTag
                  v-else-if="item.hasConfig && item.response.verify === null"
                  theme="light"
                  type="warning"
                  size="small"
                  custom-class="px-[4px]"
                >
                  {{ t('common.unVerify') }}
                </CrmTag>
                <CrmTag v-else theme="light" type="success" size="small" custom-class="px-[4px]">
                  {{ t('common.success') }}
                </CrmTag>
              </div>
              <div>
                <n-button
                  v-permission="['SYSTEM_SETTING:UPDATE']"
                  size="small"
                  type="default"
                  class="outline--secondary mr-[8px] px-[8px]"
                  @click="handleEdit(item)"
                >
                  {{ t('common.config') }}
                </n-button>
                <n-button
                  :disabled="!item.hasConfig"
                  size="small"
                  type="default"
                  class="outline--secondary px-[8px]"
                  @click="testLink(item)"
                >
                  {{ t('system.business.mailSettings.testLink') }}
                </n-button>
              </div>
            </div>
            <p class="text-[12px] text-[var(--text-n4)]">{{ item.description }}</p>
          </div>
        </div>
        <div class="flex justify-between gap-[8px]">
          <div class="flex items-center gap-[8px]">
            <n-tooltip :disabled="item.response.verify">
              <template #trigger>
                <n-switch
                  size="small"
                  :rubber-band="false"
                  :value="item.response.mkEnable"
                  :disabled="!item.hasConfig || !item.response.verify || !hasAnyPermission(['SYSTEM_SETTING:UPDATE'])"
                  @update:value="handleChangeEnable(item, 'mkEnable')"
                />
              </template>
              {{ t('system.business.notConfiguredTip') }}
            </n-tooltip>
            <div class="text-[12px]">{{ t('module.agent') }}</div>
          </div>
        </div>
      </div>
    </div>
  </CrmCard>

  <EditIntegrationModal
    v-model:show="showEditIntegrationModal"
    :title="currentTitle"
    :integration="currentIntegration as ConfigSynchronization"
    @init-sync="editDone"
  />
</template>

<script setup lang="ts">
  import { useI18n } from 'vue-i18n';
  import { NButton, NSwitch, NTooltip, useMessage } from 'naive-ui';

  import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
  import { loadScript, removeScript } from '@lib/shared/method/scriptLoader';
  import type { ConfigSynchronization, IntegrationItem } from '@lib/shared/models/system/business';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmSvgIcon from '@/components/pure/crm-svg/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import EditIntegrationModal from './editIntegrationModal.vue';

  import {
    getConfigSynchronization,
    switchThirdParty,
    syncDE,
    testConfigSynchronization,
    updateConfigSynchronization,
  } from '@/api/modules';
  import { platformType } from '@/config/business';
  import useModal from '@/hooks/useModal';
  import { useAppStore } from '@/store';
  import useLicenseStore from '@/store/modules/setting/license';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();
  const Message = useMessage();
  const licenseStore = useLicenseStore();
  const appStore = useAppStore();
  const { openModal } = useModal();
  const activePlatformTab = ref<CompanyTypeEnum>(CompanyTypeEnum.WECOM);

  const tabList = [
    { name: CompanyTypeEnum.WECOM, tab: t('system.business.WE_COM') },
    { name: CompanyTypeEnum.DINGTALK, tab: t('system.business.DING_TALK') },
    { name: CompanyTypeEnum.LARK, tab: t('system.business.LARK') },
  ];

  // 所有可用的集成平台配置
  const allIntegrations = [
    {
      type: CompanyTypeEnum.WECOM,
      title: t('system.business.WE_COM'),
      description: t('system.business.WE_COM.description'),
      logo: 'iconlogo_wechat-work',
    },
    {
      type: CompanyTypeEnum.DINGTALK,
      title: t('system.business.DING_TALK'),
      description: t('system.business.DING_TALK.description'),
      logo: 'iconlogo_dingtalk',
    },
    {
      type: CompanyTypeEnum.LARK,
      title: t('system.business.LARK'),
      description: t('system.business.LARK.description'),
      logo: 'iconlogo_lark',
    },
    {
      type: CompanyTypeEnum.INTERNAL,
      title: t('system.business.LARK_SUITE'),
      description: t('system.business.LARK.description'),
      logo: 'iconlogo_lark',
    },
    {
      type: CompanyTypeEnum.DATA_EASE,
      title: 'DataEase',
      description: t('system.business.DE.description'),
      logo: 'dataease',
    },
    {
      type: CompanyTypeEnum.SQLBot,
      title: 'SQLBot',
      description: t('system.business.SQLBot.description'),
      logo: 'SQLBot',
    },
    {
      type: CompanyTypeEnum.MAXKB,
      title: 'MaxKB',
      description: t('system.business.agent.agentMaxKBDescription'),
      logo: 'maxKB',
    },
  ];

  const originIntegrationList = ref<IntegrationItem[]>([]);
  const integrationList = ref<IntegrationItem[]>([]);

  const platFormIntegrationList = computed<IntegrationItem[]>(() =>
    originIntegrationList.value.filter((e) => e.type === activePlatformTab.value)
  );

  const agentIntegrationList = computed<IntegrationItem[]>(() =>
    originIntegrationList.value.filter((e) => e.type === CompanyTypeEnum.MAXKB)
  );

  const loading = ref(false);
  async function initSyncList() {
    try {
      loading.value = true;
      const res = await getConfigSynchronization();
      const configMap = new Map(res.map((item) => [item.type, item]));
      originIntegrationList.value = allIntegrations
        .filter((item) =>
          [...platformType, CompanyTypeEnum.DATA_EASE, CompanyTypeEnum.SQLBot, CompanyTypeEnum.MAXKB].includes(
            item.type
          )
        )
        .map((item) => {
          const config = configMap.get(item.type);
          return {
            ...item,
            hasConfig: Boolean(config?.appSecret),
            response: {
              sqlBotBoardEnable: config?.sqlBotBoardEnable ?? false,
              sqlBotChatEnable: config?.sqlBotChatEnable ?? false,
              startEnable: config?.startEnable ?? false,
              mkEnable: config?.mkEnable ?? false,
              type: item.type,
              ...config,
            },
          };
        });

      integrationList.value = originIntegrationList.value.filter(
        (e) => ![...platformType, CompanyTypeEnum.MAXKB].includes(e.type as CompanyTypeEnum)
      );
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  const showEditIntegrationModal = ref(false);

  const currentTitle = ref('');
  const currentIntegration = ref<ConfigSynchronization>();
  function handleEdit(item: IntegrationItem) {
    currentTitle.value = item.title;
    currentIntegration.value = { ...item.response };
    showEditIntegrationModal.value = true;
  }

  async function handleSyncDE() {
    try {
      loading.value = true;
      await syncDE();
      Message.success(t('org.syncSuccess'));
      await initSyncList();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function handleChangeEnable(
    item: IntegrationItem,
    key: 'deBoardEnable' | 'sqlBotBoardEnable' | 'sqlBotChatEnable' | 'startEnable' | 'mkEnable'
  ) {
    try {
      loading.value = true;
      updateConfigSynchronization({ ...item.response, [key]: !item.response[key] })
        .then(async () => {
          Message.success(item.response[key] ? t('common.disableSuccess') : t('common.enableSuccess'));
          await initSyncList();
          appStore.initThirdPartyResource();
          if (item.response[key]) {
            removeScript(CompanyTypeEnum.SQLBot);
          } else {
            await loadScript(item.response.appSecret as string, { identifier: CompanyTypeEnum.SQLBot });
          }
        })
        .catch(() => {
          item.response.verify = false;
          item.response.deBoardEnable = true;
          item.response.startEnable = false;
          item.response.mkEnable = false;
        })
        .finally(() => {
          loading.value = false;
        });
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  async function testLink(item: IntegrationItem) {
    try {
      testConfigSynchronization(item.response)
        .then((res) => {
          const isSuccess = res.data.data;
          if (isSuccess) {
            Message.success(t('org.testConnectionSuccess'));
          } else {
            Message.error(t('org.testConnectionError'));
          }
          initSyncList();
        })
        .catch(() => {
          item.response.verify = false;
          item.response.deBoardEnable = true;
          item.response.startEnable = false;
          item.response.mkEnable = false;
        });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function editDone() {
    await initSyncList();
    const sqlItem = integrationList.value.find((item) => item.type === CompanyTypeEnum.SQLBot);
    removeScript(CompanyTypeEnum.SQLBot);
    if (sqlItem && sqlItem.response.sqlBotChatEnable) {
      await loadScript(sqlItem.response.appSecret as string, { identifier: CompanyTypeEnum.SQLBot });
    }
  }

  async function initThirdPartyResource() {
    try {
      await appStore.initThirdPartyResource();
      activePlatformTab.value = appStore.activePlatformResource.syncResource;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function handleBeforeLeave(newVal: string | number, _o: string | number | null) {
    if (!newVal) return true;
    const currentPlatformName = allIntegrations.find((e) => e.type === newVal)?.title ?? '';

    return new Promise<boolean>((resolve) => {
      openModal({
        type: 'error',
        title: t('system.business.authenticationSettings.confirmTogglePlatform'),
        content: t('system.business.authenticationSettings.togglePlatformTip', { type: currentPlatformName }),
        positiveText: t('common.confirm'),
        negativeText: t('common.cancel'),

        onPositiveClick: async () => {
          try {
            await switchThirdParty(newVal as CompanyTypeEnum);
            initSyncList();
            appStore.initThirdPartyResource();
            Message.success(t('common.operationSuccess'));
            resolve(true);
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
            resolve(false);
          }
        },

        onNegativeClick: () => {
          resolve(false);
        },
      });
    });
  }

  onBeforeMount(() => {
    initThirdPartyResource();
    initSyncList();
  });
</script>

<style lang="less" scoped>
  .content-title {
    color: var(--text-n1);
    @apply mb-4 font-medium;
  }
</style>
