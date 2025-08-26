<template>
  <CrmCard hide-footer>
    <div class="grid grid-cols-3 gap-[16px]">
      <div class="dashboard-card">
        <div class="dashboard-card-header">
          <div class="flex items-center gap-[8px]">
            <div class="bg-[var(--text-n9)] p-[8px]"><CrmSvgIcon name="dataease" width="24px" height="24px" /></div>
            <div class="flex flex-col gap-[4px]">
              <div class="text-[var(--text-n1)]">DataEase</div>
              <div class="text-[12px] text-[var(--text-n4)]">{{ t('system.business.DE.description') }}</div>
            </div>
          </div>
        </div>
        <div class="dashboard-card-bottom">
          <n-popover trigger="hover" placement="bottom" :disabled="DEConfig?.deLinkIntegration">
            <template #trigger>
              <n-button type="primary" text size="small" :disabled="!DEConfig?.deLinkIntegration" @click="jump('link')">
                {{ t('system.business.DE.link') }}
              </n-button>
            </template>
            <div class="flex items-center gap-[12px]">
              <div class="text-[12px]">{{ t('dashboard.unConfig') }}</div>
              <n-button type="primary" size="small" text @click="goConfig">{{ t('dashboard.goConfig') }}</n-button>
            </div>
          </n-popover>
          <n-popover trigger="hover" placement="bottom" :disabled="DEConfig?.deModuleEmbedding">
            <template #trigger>
              <n-button
                type="primary"
                text
                size="small"
                :disabled="!DEConfig?.deModuleEmbedding"
                @click="jump('embedModule')"
              >
                {{ t('system.business.DE.embedModule') }}
              </n-button>
            </template>
            <div class="flex items-center gap-[12px]">
              <div class="text-[12px]">{{ t('dashboard.unConfig') }}</div>
              <n-button type="primary" size="small" text @click="goConfig">{{ t('dashboard.goConfig') }}</n-button>
            </div>
          </n-popover>
        </div>
      </div>
    </div>
  </CrmCard>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { NButton, NPopover } from 'naive-ui';

  import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { ConfigSynchronization } from '@lib/shared/models/system/business';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmSvgIcon from '@/components/pure/crm-svg/index.vue';

  import { getConfigSynchronization } from '@/api/modules';

  import { AppRouteEnum, DashboardRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();
  const router = useRouter();

  function jump(action: 'link' | 'embedModule') {
    if (action === 'link') {
      router.push({ name: DashboardRouteEnum.DASHBOARD_LINK });
    } else if (action === 'embedModule') {
      router.push({ name: DashboardRouteEnum.DASHBOARD_MODULE });
    }
  }

  const configList = ref<ConfigSynchronization[]>([]);
  const DEConfig = computed(() => configList.value.find((item) => item.type === CompanyTypeEnum.DATA_EASE));

  async function init() {
    try {
      configList.value = await getConfigSynchronization();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function goConfig() {
    router.push({ name: AppRouteEnum.SYSTEM_BUSINESS });
  }

  onBeforeMount(() => {
    init();
  });
</script>

<style lang="less" scoped>
  .dashboard-card {
    padding: 24px;
    border: 1px solid var(--text-n8);
    border-radius: 6px;
    .dashboard-card-header {
      @apply flex items-start justify-between;

      margin-bottom: 8px;
      padding-bottom: 16px;
    }
    .dashboard-card-bottom {
      @apply flex items-center;

      gap: 12px;
    }
  }
</style>
