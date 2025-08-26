<template>
  <!-- special-height 64是tab的高度和margin -->
  <CrmCard hide-footer :special-height="licenseStore.expiredDuring ? 128 : 64" :loading="loading">
    <div v-if="integrationList.length" class="flex flex-wrap gap-[16px]">
      <div
        v-for="item of integrationList"
        :key="item.type"
        class="flex h-[140px] w-[380px] flex-col justify-between rounded-[6px] border border-solid border-[var(--text-n8)] bg-[var(--text-n10)] p-[24px]"
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
            <div class="flex justify-between">
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
        <div v-else-if="item.type === CompanyTypeEnum.SQLBot" class="flex justify-between">
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
        <div v-else class="flex justify-between">
          <div class="flex items-center gap-[8px]">
            <n-tooltip :disabled="item.response.verify">
              <template #trigger>
                <n-switch
                  size="small"
                  :rubber-band="false"
                  :value="item.response.qrcodeEnable"
                  :disabled="!item.hasConfig || !item.response.verify || !hasAnyPermission(['SYSTEM_SETTING:UPDATE'])"
                  @update:value="handleChangeEnable(item, 'qrcodeEnable')"
                />
              </template>
              {{ t('system.business.notConfiguredTip') }}
            </n-tooltip>
            <div class="text-[12px]">{{ t('system.business.tab.scanLogin') }}</div>
          </div>
          <div class="flex items-center gap-[8px]">
            <n-switch
              size="small"
              :rubber-band="false"
              :value="item.response.weComEnable"
              :disabled="!item.hasConfig || !item.response.verify || !hasAnyPermission(['SYSTEM_SETTING:UPDATE'])"
              @update:value="handleChangeEnable(item, 'weComEnable')"
            />
            <div class="text-[12px]">{{ t('system.message.enterpriseWeChatNotice') }}</div>
          </div>

          <div class="flex items-center gap-[8px]">
            <n-tooltip :disabled="item.response.verify">
              <template #trigger>
                <n-switch
                  size="small"
                  :rubber-band="false"
                  :value="item.response.syncEnable"
                  :disabled="!item.hasConfig || !item.response.verify || !hasAnyPermission(['SYSTEM_SETTING:UPDATE'])"
                  @update:value="handleChangeEnable(item, 'syncEnable')"
                />
              </template>
              {{ t('system.business.notConfiguredTip') }}
            </n-tooltip>
            <div class="text-[12px]">{{ t('system.business.tab.syncOrganization') }}</div>
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
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import EditIntegrationModal from './editIntegrationModal.vue';

  import {
    getConfigSynchronization,
    syncDE,
    testConfigSynchronization,
    updateConfigSynchronization,
  } from '@/api/modules';
  import useLicenseStore from '@/store/modules/setting/license';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();
  const Message = useMessage();
  const licenseStore = useLicenseStore();

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
  ];

  const integrationList = ref<IntegrationItem[]>([]);

  const loading = ref(false);
  async function initSyncList() {
    try {
      loading.value = true;
      const res = await getConfigSynchronization();
      const configMap = new Map(res.map((item) => [item.type, item]));
      integrationList.value = allIntegrations
        .filter((item) =>
          [CompanyTypeEnum.WECOM, CompanyTypeEnum.DATA_EASE, CompanyTypeEnum.SQLBot].includes(item.type)
        )
        .map((item) => {
          const config = configMap.get(item.type);
          return {
            ...item,
            hasConfig: Boolean(config?.appSecret),
            response: {
              qrcodeEnable: config?.qrcodeEnable ?? false,
              syncEnable: config?.syncEnable ?? false,
              weComEnable: config?.weComEnable ?? false,
              sqlBotBoardEnable: config?.sqlBotBoardEnable ?? false,
              sqlBotChatEnable: config?.sqlBotChatEnable ?? false,
              type: item.type,
              ...config,
            },
          };
        });
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
    key: 'syncEnable' | 'qrcodeEnable' | 'deBoardEnable' | 'weComEnable' | 'sqlBotBoardEnable' | 'sqlBotChatEnable'
  ) {
    try {
      loading.value = true;
      updateConfigSynchronization({ ...item.response, [key]: !item.response[key] })
        .then(async () => {
          Message.success(item.response[key] ? t('common.disableSuccess') : t('common.enableSuccess'));
          await initSyncList();
          if (item.response[key]) {
            removeScript(CompanyTypeEnum.SQLBot);
          } else {
            await loadScript(item.response.appSecret as string, { identifier: CompanyTypeEnum.SQLBot });
          }
        })
        .catch(() => {
          item.response.verify = false;
          item.response.qrcodeEnable = true;
          item.response.syncEnable = true;
          item.response.deBoardEnable = true;
          item.response.weComEnable = true;
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
          item.response.qrcodeEnable = true;
          item.response.syncEnable = true;
          item.response.deBoardEnable = true;
          item.response.weComEnable = true;
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

  onBeforeMount(() => {
    initSyncList();
  });
</script>
