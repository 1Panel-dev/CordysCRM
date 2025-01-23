<template>
  <CrmCard hide-footer>
    <div v-if="integrationList.length" class="flex flex-wrap gap-[16px]">
      <div
        v-for="item of integrationList"
        :key="item.key"
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
              <n-switch v-model:value="item.enable" :disabled="!item.hasConfig" />
            </template>
            {{ t('system.business.notConfiguredTip') }}
          </n-tooltip>
        </div>
      </div>
    </div>
  </CrmCard>

  <EditIntegrationModal v-model:show="showEditIntegrationModal" :integration="currentIntegration" />
</template>

<script setup lang="ts">
  import { useI18n } from 'vue-i18n';
  import { NButton, NSwitch, NTooltip } from 'naive-ui';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import EditIntegrationModal from './editIntegrationModal.vue';

  const { t } = useI18n();

  // TODO lmy 类型
  const integrationList = ref<any[]>([
    {
      key: 'WE_COM',
      title: t('system.business.WE_COM'),
      description: t('system.business.WE_COM.description'),
      logo: 'iconlogo_wechat-work',
      enable: false,
      hasConfig: false,
    },
    {
      key: 'DING_TALK',
      title: t('system.business.DING_TALK'),
      description: t('system.business.DING_TALK.description'),
      enable: true,
      logo: 'iconlogo_dingtalk',
      hasConfig: false,
    },
    {
      key: 'LARK',
      title: t('system.business.LARK'),
      description: t('system.business.LARK.description'),
      enable: false,
      logo: 'iconlogo_lark',
      hasConfig: true,
    },
    {
      key: 'LARK_SUITE',
      title: t('system.business.LARK_SUITE'),
      description: t('system.business.LARK.description'),
      enable: false,
      logo: 'iconlogo_lark',
      hasConfig: false,
    },
  ]);

  const showEditIntegrationModal = ref(false);
  // TODO lmy 类型
  const currentIntegration = ref<any>(null);

  // TODO lmy 类型
  function handleEdit(item: any) {
    currentIntegration.value = item;
    showEditIntegrationModal.value = true;
  }
</script>
