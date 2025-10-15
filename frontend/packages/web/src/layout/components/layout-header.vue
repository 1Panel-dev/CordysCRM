<template>
  <n-layout-header class="flex" bordered>
    <div class="flex max-w-[300px] px-[24px] py-[14px]">
      <img :src="innerLogo" class="h-[28px]" />
    </div>
    <div class="flex flex-1 items-center justify-between px-[16px]">
      <CrmTopMenu />
      <div v-if="!props.isPreview" class="flex items-center gap-[8px]">
        <CrmButtonGroup not-show-divider class="gap-[8px]" :list="appStore.getNavTopConfigList">
          <template #searchSlot>
            <n-button v-if="showSearch" class="p-[8px]" quaternary @click="showDuplicateCheckDrawer = true">
              <template #icon>
                <CrmIcon type="iconicon_search-outline_outlined" :size="16" />
              </template>
            </n-button>
            <span v-else></span>
          </template>
          <template #eventSlot>
            <n-button class="p-[8px]" quaternary @click="showFollowDrawer = true">
              <template #icon>
                <CrmIcon type="iconicon_data_plan" :size="16" />
              </template>
            </n-button>
          </template>
          <template #languageSlot>
            <n-popselect
              v-model:value="currentLocale"
              :options="LOCALE_OPTIONS"
              trigger="hover"
              @update-value="changeLanguage"
            >
              <n-button class="p-[8px]" quaternary>
                <template #icon>
                  <LanguageOutline />
                </template>
              </n-button>
            </n-popselect>
          </template>
          <template #alertsSlot>
            <n-button class="p-[8px]" quaternary @click="showMessage">
              <n-badge value="1" dot :show="showBadge">
                <CrmIcon type="iconicon-alarmclock" :size="16" />
              </n-badge>
            </n-button>
          </template>
          <template #agentSlot>
            <n-button class="p-[8px]" quaternary @click="showAgent">
              <CrmSvg name="icon_bot" :size="16" />
            </n-button>
          </template>
          <template #versionInfoSlot>
            <n-popover position="left" content-class="w-[320px]" class="!p-[16px]">
              <div class="flex flex-col gap-[8px]">
                <CrmSvg name="logo_CORDYS" height="22px" width="100px" />
                <div
                  class="flex cursor-pointer items-center gap-[8px] text-[14px] text-[var(--color-text-1)]"
                  @click="copyVersion(appStore.versionInfo.currentVersion)"
                >
                  <div class="text-[12px] leading-[20px]">
                    {{ t('settings.help.currentVersion') }}
                  </div>
                  <div class="font-semibold">
                    {{ appStore.versionInfo.currentVersion }} ({{ appStore.versionInfo.architecture }})
                  </div>
                </div>
                <div
                  class="flex cursor-pointer items-center gap-[8px] text-[14px] text-[var(--color-text-1)]"
                  @click="copyVersion(appStore.versionInfo.latestVersion)"
                >
                  <div class="text-[12px] leading-[20px]">
                    {{ t('settings.help.latestVersion') }}
                  </div>
                  <div class="font-semibold">{{ appStore.versionInfo.latestVersion }}</div>
                </div>
                <div
                  v-if="licenseStore.isEnterpriseVersion() && hasAnyPermission(['LICENSE:READ'])"
                  class="flex flex-col gap-[4px]"
                >
                  <n-divider class="!my-0" />
                  <div class="flex flex-col gap-[8px]">
                    <div class="flex items-center justify-between">
                      <div class="flex items-center gap-[8px]">
                        <div class="font-semibold text-[var(--text-n1)]">License</div>
                        <CrmTag tooltip-disabled :type="getLicenseStatus.status" theme="light">
                          {{ getLicenseStatus.title }}
                        </CrmTag>
                      </div>
                      <n-button v-permission="['LICENSE:EDIT']" text type="primary" @click="handleUpdate">
                        {{ t('common.update') }}
                      </n-button>
                    </div>
                    <div class="flex items-center gap-[8px]">
                      <div class="text-[12px] leading-[20px] text-[var(--text-n4)]">
                        {{ t('system.license.customerName') }}
                      </div>
                      <div class="font-semibold">{{ licenseStore?.licenseInfo?.corporation }}</div>
                    </div>
                    <div class="flex items-center gap-[8px]">
                      <div class="text-[12px] leading-[20px] text-[var(--text-n4)]">
                        {{ t('system.license.productionVersion') }}
                      </div>
                      <div class="font-semibold">
                        {{
                          licenseVersionMap[licenseStore?.licenseInfo?.edition as keyof typeof licenseVersionMap] ?? '-'
                        }}
                      </div>
                    </div>
                    <div class="flex items-center gap-[8px]">
                      <div class="text-[12px] leading-[20px] text-[var(--text-n4)]">
                        {{ t('system.license.LicenseAccountCount') }}
                      </div>
                      <div class="font-semibold">
                        {{ licenseStore?.licenseInfo?.count ?? '-' }}
                      </div>
                    </div>
                    <div class="flex items-center gap-[8px]">
                      <div class="text-[12px] leading-[20px] text-[var(--text-n4)]">
                        {{ t('system.license.authorizationTime') }}
                      </div>
                      <div class="font-semibold">{{ licenseStore?.licenseInfo?.expired ?? '-' }}</div>
                    </div>
                  </div>
                </div>
                <div class="flex items-center justify-between">
                  <div class="text-[12px] leading-[20px] text-[var(--text-n4)]">Cordys CRM</div>
                  <div class="text-[12px] leading-[20px] text-[var(--text-n4)]">
                    {{ appStore.versionInfo.copyright }}
                  </div>
                </div>
              </div>
              <template #trigger>
                <n-button class="p-[8px]" quaternary>
                  <template #icon>
                    <n-badge value="1" dot :show="appStore.versionInfo.hasNewVersion">
                      <CrmIcon type="iconicon_info_circle" :size="16" />
                    </n-badge>
                  </template>
                </n-button>
              </template>
            </n-popover>
          </template>
          <template #helpSlot>
            <CrmMoreAction placement="bottom-end" :options="moreActions" @select="selectMoreActions">
              <n-button class="p-[8px]" quaternary>
                <template #icon>
                  <CrmIcon type="iconicon_help_circle" :size="16" />
                </template>
              </n-button>
            </CrmMoreAction>
          </template>
        </CrmButtonGroup>
      </div>
    </div>
    <MessageDrawer v-model:show="showMessageDrawer" />
    <licenseDrawer v-model:visible="showLicenseDrawer" />
    <Suspense>
      <CrmDuplicateCheckDrawer v-model:visible="showDuplicateCheckDrawer" />
    </Suspense>
  </n-layout-header>
  <agentDrawer v-model:visible="showAgentDrawer" />
  <CrmFollowDrawer v-model:visible="showFollowDrawer" />
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import { useClipboard } from '@vueuse/core';
  import { NBadge, NButton, NDivider, NLayoutHeader, NPopover, NPopselect, useMessage } from 'naive-ui';
  import { LanguageOutline } from '@vicons/ionicons5';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { LOCALE_OPTIONS } from '@lib/shared/locale';
  import useLocale from '@lib/shared/locale/useLocale';
  import { LocaleType } from '@lib/shared/types/global';

  import CrmButtonGroup from '@/components/pure/crm-button-group/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSvg from '@/components/pure/crm-svg/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import { lastScopedOptions } from '@/components/business/crm-duplicate-check-drawer/config';
  import CrmDuplicateCheckDrawer from '@/components/business/crm-duplicate-check-drawer/index.vue';
  import CrmTopMenu from '@/components/business/crm-top-menu/index.vue';
  import licenseDrawer from '@/views/system/license/licenseDrawer.vue';
  import MessageDrawer from '@/views/system/message/components/messageDrawer.vue';

  import { changeLocaleBackEnd } from '@/api/modules';
  import { defaultPlatformLogo } from '@/config/business';
  import useAppStore from '@/store/modules/app';
  import useLicenseStore from '@/store/modules/setting/license';
  import useUserStore from '@/store/modules/user';
  import { hasAnyPermission } from '@/utils/permission';

  import { WorkbenchRouteEnum } from '@/enums/routeEnum';

  const agentDrawer = defineAsyncComponent(() => import('@/components/business/crm-agent-drawer/index.vue'));
  const CrmFollowDrawer = defineAsyncComponent(() => import('@/components/business/crm-follow-drawer/index.vue'));

  const route = useRoute();

  const { success, warning, loading } = useMessage();
  const { t } = useI18n();
  const { changeLocale, currentLocale } = useLocale(loading);

  const appStore = useAppStore();
  const userStore = useUserStore();
  const licenseStore = useLicenseStore();

  const props = defineProps<{
    isPreview?: boolean;
    logo?: string;
  }>();

  function changeLanguage(locale: LocaleType) {
    changeLocaleBackEnd(locale);
    changeLocale(locale);
  }

  const showBadge = computed(() => {
    return !appStore.messageInfo.read;
  });

  const showMessageDrawer = ref(false);
  function showMessage() {
    showMessageDrawer.value = true;
  }

  const showLicenseDrawer = ref(false);
  function handleUpdate() {
    showLicenseDrawer.value = true;
  }
  const getLicenseStatus = computed<{
    title: string;
    status: 'default' | 'error' | 'warning' | 'primary' | 'info' | 'success';
  }>(() => {
    switch (licenseStore.licenseInfo?.status) {
      case 'valid':
        return {
          title: t('system.license.valid'),
          status: 'success',
        };
      case 'expired':
        return {
          title: t('system.license.invalid'),
          status: 'error',
        };
      default:
        return {
          title: t('system.license.failure'),
          status: 'error',
        };
    }
  });
  const showAgentDrawer = ref(false);
  function showAgent() {
    showAgentDrawer.value = true;
  }

  const showSearch = computed(() => lastScopedOptions.value.length);
  const showDuplicateCheckDrawer = ref(false);

  const showFollowDrawer = ref(false);

  const { copy, isSupported } = useClipboard({ legacy: true });
  function copyVersion(version: string) {
    if (isSupported) {
      copy(version);
      success(t('common.copySuccess'));
    } else {
      warning(t('common.copyNotSupport'));
    }
  }

  const moreActions: ActionsItem[] = [
    {
      label: t('settings.help.doc'),
      key: 'helpDoc',
      iconType: 'iconicon_help_circle',
    },
  ];

  function selectMoreActions() {
    window.open(appStore.pageConfig.helpDoc, '_blank');
  }

  const licenseVersionMap: Record<string, string> = {
    Standard: t('system.license.LicenseStandard'),
    Enterprise: t('system.license.LicenseEnterprise'),
    Professional: t('system.license.LicenseProfessional'),
  };

  onBeforeMount(() => {
    appStore.getVersion();
    if (route.name !== WorkbenchRouteEnum.WORKBENCH_INDEX) {
      appStore.initMessage();
    }
    appStore.connectSystemMessageSSE(userStore.showSystemNotify);
    appStore.showSQLBot();
  });

  const innerLogo = computed(() =>
    props.isPreview && props.logo ? props.logo : appStore.pageConfig.logoPlatform[0]?.url ?? defaultPlatformLogo
  );
</script>

<style lang="less" scoped></style>
