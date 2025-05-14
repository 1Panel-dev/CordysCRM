<template>
  <n-layout-header class="flex" bordered>
    <div class="flex w-[200px] items-center gap-[4px] px-[30px] py-[12px]">
      <img :src="props.logo ?? '/logo.svg'" width="32px" height="32px" />
      <n-tooltip :delay="300">
        <template #trigger>
          <div class="one-line-text text-[20px] font-semibold leading-[32px]">
            {{ props.name ?? 'CORDYS' }}
          </div>
        </template>
        {{ props.name ?? 'CORDYS' }}
      </n-tooltip>
    </div>
    <div class="flex flex-1 items-center justify-between px-[16px]">
      <CrmTopMenu />
      <div class="flex items-center gap-[8px]">
        <CrmTag
          v-if="hasAnyPermission(['CUSTOMER_MANAGEMENT:READ', 'OPPORTUNITY_MANAGEMENT_READ', 'CLUE_MANAGEMENT_READ'])"
          theme="light"
          type="primary"
          class="cursor-pointer"
          @click="showDuplicateCheckDrawer = true"
        >
          {{ t('workbench.duplicateCheck') }}
        </CrmTag>
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
        <n-button class="p-[8px]" quaternary @click="showMessage">
          <n-badge value="1" dot :show="showBadge">
            <CrmIcon type="iconicon-alarmclock" :size="16" />
          </n-badge>
        </n-button>
        <n-popover position="left">
          <div
            class="flex cursor-pointer items-center gap-[4px] text-[14px] text-[var(--color-text-1)]"
            @click="copyVersion"
          >
            <div class="text-[var(--color-text-4)]">{{ t('settings.help.version') }}：</div>
            {{ appStore.version }}
          </div>
          <template #trigger>
            <n-button class="p-[8px]" quaternary>
              <template #icon>
                <InformationCircleOutline />
              </template>
            </n-button>
          </template>
        </n-popover>
      </div>
    </div>
    <MessageDrawer v-model:show="showMessageDrawer" />
    <CrmDuplicateCheckDrawer v-model:visible="showDuplicateCheckDrawer" />
  </n-layout-header>
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import { useClipboard } from '@vueuse/core';
  import { NBadge, NButton, NLayoutHeader, NPopover, NPopselect, NTooltip, useMessage } from 'naive-ui';
  import { InformationCircleOutline, LanguageOutline } from '@vicons/ionicons5';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { LOCALE_OPTIONS } from '@lib/shared/locale';
  import useLocale from '@lib/shared/locale/useLocale';
  import { LocaleType } from '@lib/shared/types/global';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmDuplicateCheckDrawer from '@/components/business/crm-duplicate-check-drawer/index.vue';
  import CrmTopMenu from '@/components/business/crm-top-menu/index.vue';
  import MessageDrawer from '@/views/system/message/components/messageDrawer.vue';

  import useAppStore from '@/store/modules/app';
  import useUserStore from '@/store/modules/user';
  import { hasAnyPermission } from '@/utils/permission';

  import { WorkbenchRouteEnum } from '@/enums/routeEnum';

  const route = useRoute();

  const props = defineProps<{
    logo?: string;
    name?: string;
  }>();

  const { success, warning, loading } = useMessage();
  const { t } = useI18n();
  const { changeLocale, currentLocale } = useLocale(loading);

  const appStore = useAppStore();
  const userStore = useUserStore();

  function changeLanguage(locale: LocaleType) {
    changeLocale(locale);
  }

  const showBadge = computed(() => {
    return !appStore.messageInfo.read;
  });

  const showMessageDrawer = ref(false);
  function showMessage() {
    showMessageDrawer.value = true;
  }

  const showDuplicateCheckDrawer = ref(false);

  const { copy, isSupported } = useClipboard({ legacy: true });
  function copyVersion() {
    if (isSupported) {
      copy(appStore.version);
      success(t('common.copySuccess'));
    } else {
      warning(t('common.copyNotSupport'));
    }
  }

  onBeforeMount(() => {
    appStore.getVersion();
    if (route.name !== WorkbenchRouteEnum.WORKBENCH_INDEX) {
      appStore.initMessage();
    }
    appStore.connectSystemMessageSSE(userStore.showSystemNotify);
  });
</script>

<style lang="less" scoped></style>
