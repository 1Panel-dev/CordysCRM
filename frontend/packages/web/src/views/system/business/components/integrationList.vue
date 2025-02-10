<template>
  <!-- special-height 64是tab的高度和margin -->
  <CrmCard hide-footer :special-height="64" :loading="loading">
    <div v-if="integrationList.length" class="flex flex-wrap gap-[16px]">
      <div
        v-for="item of integrationList"
        :key="item.response.type"
        class="flex h-[140px] w-[380px] flex-col justify-between rounded-[6px] bg-[var(--text-n9)] p-[24px]"
      >
        <div class="flex">
          <div class="mr-[8px] flex h-[40px] w-[40px] items-center justify-center rounded-[2px] bg-[var(--text-n10)]">
            <CrmIcon :type="item.logo" :size="24"></CrmIcon>
          </div>
          <div>
            <div>
              <span class="mr-[8px] font-medium">{{ item.title }}</span>
              <CrmTag v-if="!item.hasConfig" theme="light" size="small">
                {{ t('system.business.notConfigured') }}
              </CrmTag>
              <CrmTag v-else theme="light" type="success" size="small"> {{ t('system.business.effective') }} </CrmTag>
            </div>
            <p class="text-[12px] text-[var(--text-n4)]">{{ item.description }}</p>
          </div>
        </div>
        <div class="flex justify-between">
          <div>
            <n-tooltip :disabled="item.hasConfig">
              <template #trigger>
                <n-button :disabled="!item.hasConfig" size="small" type="default" class="outline--secondary mr-[8px]">
                  {{ t('system.business.mailSettings.testLink') }}
                </n-button>
              </template>
              {{ t('system.business.notConfiguredTip') }}
            </n-tooltip>

            <n-button size="small" type="default" class="outline--secondary" @click="handleEdit(item)">{{
              t('common.edit')
            }}</n-button>
          </div>

          <n-tooltip :disabled="item.hasConfig">
            <template #trigger>
              <n-switch
                :value="item.response.enable"
                :disabled="!item.hasConfig"
                @update:value="handleChangeEnable(item)"
              />
            </template>
            {{ t('system.business.notConfiguredTip') }}
          </n-tooltip>
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

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import EditIntegrationModal from './editIntegrationModal.vue';

  import { getConfigSynchronization, updateConfigSynchronization } from '@/api/modules/system/business';

  import { CompanyTypeEnum } from '@/enums/commonEnum';

  import type { ConfigSynchronization, IntegrationItem } from '@lib/shared/models/system/business';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    activeTab: string;
  }>();

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
      let res: ConfigSynchronization[] = [];
      if (props.activeTab === 'syncOrganization') {
        res = await getConfigSynchronization();
      }

      const configMap = new Map(res.map((item) => [item.type, item]));
      integrationList.value = allIntegrations
        .filter((item) => configMap.has(item.type))
        .map((item) => {
          const config = configMap.get(item.type)!;
          return {
            ...item,
            hasConfig: Boolean(config.appSecret),
            response: config,
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

  async function handleChangeEnable(item: IntegrationItem) {
    try {
      loading.value = true;
      await updateConfigSynchronization({ ...item.response, enable: !item.response.enable });
      Message.success(item.response.enable ? t('common.disableSuccess') : t('common.enableSuccess'));
      initSyncList();
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  onBeforeMount(() => {
    initSyncList();
  });
</script>
