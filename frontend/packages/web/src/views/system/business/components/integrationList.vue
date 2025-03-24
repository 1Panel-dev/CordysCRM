<template>
  <!-- special-height 64是tab的高度和margin -->
  <CrmCard hide-footer :special-height="64" :loading="loading">
    <div v-if="integrationList.length" class="flex flex-wrap gap-[16px]">
      <div
        v-for="item of integrationList"
        :key="item.type"
        class="flex h-[140px] w-[380px] flex-col justify-between rounded-[6px] border border-solid border-[var(--text-n8)] bg-[var(--text-n10)] p-[24px]"
      >
        <div class="flex">
          <div class="mr-[8px] flex h-[40px] w-[40px] items-center justify-center rounded-[2px] bg-[var(--text-n9)]">
            <CrmIcon :type="item.logo" :size="24"></CrmIcon>
          </div>
          <div class="flex-1">
            <div class="flex justify-between">
              <div>
                <span class="mr-[8px] font-medium">{{ item.title }}</span>
                <CrmTag v-if="!item.hasConfig" theme="light" size="small">
                  {{ t('system.business.notConfigured') }}
                </CrmTag>
                <CrmTag v-else-if="item.hasConfig && !item.response.verify" theme="light" type="error" size="small">
                  {{ t('common.fail') }}
                </CrmTag>
                <CrmTag v-else theme="light" type="success" size="small">
                  {{ t('common.success') }}
                </CrmTag>
              </div>

              <div>
                <n-button
                  v-permission="['SYSTEM_SETTING:UPDATE']"
                  size="small"
                  type="default"
                  class="outline--secondary mr-[8px]"
                  @click="handleEdit(item)"
                >
                  {{ t('common.edit') }}
                </n-button>
                <n-tooltip :disabled="item.hasConfig">
                  <template #trigger>
                    <n-button
                      :disabled="!item.hasConfig"
                      size="small"
                      type="default"
                      class="outline--secondary"
                      @click="testLink(item)"
                    >
                      {{ t('system.business.mailSettings.testLink') }}
                    </n-button>
                  </template>
                  {{ t('system.business.notConfiguredTip') }}
                </n-tooltip>
              </div>
            </div>
            <p class="text-[12px] text-[var(--text-n4)]">{{ item.description }}</p>
          </div>
        </div>
        <div class="flex justify-between">
          <div class="flex items-center gap-[8px]">
            <n-tooltip :disabled="item.hasConfig">
              <template #trigger>
                <n-switch
                  size="small"
                  :value="item.response.qrcodeEnable"
                  :disabled="(!item.hasConfig && !item.response.verify) || !hasAnyPermission(['SYSTEM_SETTING:UPDATE'])"
                  @update:value="handleChangeEnable(item, 'qrcodeEnable')"
                />
              </template>
              {{ t('system.business.notConfiguredTip') }}
            </n-tooltip>
            <div class="text-[12px]">{{ t('system.business.tab.scanLogin') }}</div>
          </div>

          <div class="flex items-center gap-[8px]">
            <n-tooltip :disabled="item.hasConfig">
              <template #trigger>
                <n-switch
                  size="small"
                  :value="item.response.syncEnable"
                  :disabled="!item.hasConfig || !hasAnyPermission(['SYSTEM_SETTING:UPDATE'])"
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
    @init-sync="initSyncList"
  />
</template>

<script setup lang="ts">
  import { useI18n } from 'vue-i18n';
  import { NButton, NSwitch, NTooltip, useMessage } from 'naive-ui';

  import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
  import type { ConfigSynchronization, IntegrationItem } from '@lib/shared/models/system/business';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import EditIntegrationModal from './editIntegrationModal.vue';

  import {
    getConfigSynchronization,
    testConfigSynchronization,
    updateConfigSynchronization,
  } from '@/api/modules/system/business';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();
  const Message = useMessage();

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
  ];

  const integrationList = ref<IntegrationItem[]>([]);

  const loading = ref(false);
  async function initSyncList() {
    try {
      loading.value = true;
      const res = await getConfigSynchronization();
      const configMap = new Map(res.map((item) => [item.type, item]));
      integrationList.value = allIntegrations
        .filter((item) => [CompanyTypeEnum.WECOM].includes(item.type))
        .map((item) => {
          const config = configMap.get(item.type);
          return {
            ...item,
            hasConfig: Boolean(config?.appSecret),
            response: {
              qrcodeEnable: config?.qrcodeEnable ?? true,
              syncEnable: config?.syncEnable ?? true,
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

  async function handleChangeEnable(item: IntegrationItem, key: 'syncEnable' | 'qrcodeEnable') {
    try {
      loading.value = true;
      await updateConfigSynchronization({ ...item.response, [key]: !item.response[key] });
      Message.success(item.response[key] ? t('common.disableSuccess') : t('common.enableSuccess'));
      initSyncList();
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  async function testLink(item: IntegrationItem) {
    try {
      const res = await testConfigSynchronization(item.response);
      if (!res) {
        item.response.verify = res;
        item.response.qrcodeEnable = true;
        item.response.syncEnable = true;
      }
      Message.success(t('org.testConnectionSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  onBeforeMount(() => {
    initSyncList();
  });
</script>
