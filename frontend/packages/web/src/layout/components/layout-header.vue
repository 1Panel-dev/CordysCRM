<template>
  <n-layout-header class="flex" bordered>
    <div class="flex w-[180px] items-center gap-[4px] px-[30px] py-[12px]">
      <img :src="props.logo ?? '/logo.png'" width="32px" height="32px" />
      <n-tooltip :delay="300">
        <template #trigger>
          <div class="one-line-text text-[20px] font-semibold leading-[32px]">
            {{ props.name ?? 'Cordys' }}
          </div>
        </template>
        {{ props.name ?? 'Cordys' }}
      </n-tooltip>
    </div>
    <div class="flex flex-1 items-center justify-between px-[16px]">
      <CrmTopMenu />
      <!-- <n-breadcrumb>
        <n-breadcrumb-item>
          <div class="text-[14px] leading-[22px]">一级页面</div>
        </n-breadcrumb-item>
        <n-breadcrumb-item>
          <div class="text-[14px] leading-[22px]">二级页面</div>
        </n-breadcrumb-item>
        <n-breadcrumb-item>
          <div class="text-[14px] leading-[22px]">三级页面</div>
        </n-breadcrumb-item>
      </n-breadcrumb> -->
      <div class="flex items-center gap-[8px]">
        <CrmTag theme="light" type="primary" @click="showDuplicateCheckDrawer = true">
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
      </div>
    </div>
    <MessageDrawer v-model:show="showMessageDrawer" />
    <CrmDuplicateCheckDrawer v-model:visible="showDuplicateCheckDrawer" />
  </n-layout-header>
</template>

<script setup lang="ts">
  import { NBadge, NButton, NLayoutHeader, NPopselect, NTooltip } from 'naive-ui';
  import { LanguageOutline } from '@vicons/ionicons5';

  import { LocaleType } from '@lib/shared/types/global';

  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmDuplicateCheckDrawer from '@/components/business/crm-duplicate-check-drawer/index.vue';
  import CrmTopMenu from '@/components/business/crm-top-menu/index.vue';
  import MessageDrawer from '@/views/system/message/components/messageDrawer.vue';

  import { useI18n } from '@/hooks/useI18n';
  import { LOCALE_OPTIONS } from '@/locale/index';
  import useLocale from '@/locale/useLocale';
  import useUserStore from '@/store/modules/user';

  const props = defineProps<{
    logo?: string;
    name?: string;
  }>();

  const { t } = useI18n();
  const { changeLocale, currentLocale } = useLocale();
  const userStore = useUserStore();

  function changeLanguage(locale: LocaleType) {
    changeLocale(locale);
  }

  const showBadge = computed(() => {
    return userStore.messageInfo.read;
  });

  const showMessageDrawer = ref(false);
  function showMessage() {
    showMessageDrawer.value = true;
  }

  const showDuplicateCheckDrawer = ref(false);
</script>

<style lang="less" scoped></style>
