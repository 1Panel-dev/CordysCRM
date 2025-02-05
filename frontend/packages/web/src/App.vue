<template>
  <n-config-provider :theme-overrides="themeOverridesConfig" :locale="naiveUILocale" :date-locale="naiveUIDateLocale">
    <n-message-provider>
      <n-dialog-provider>
        <RouterView />
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
  import { dateEnUS, dateZhCN, enUS, NConfigProvider, NDialogProvider, NMessageProvider, zhCN } from 'naive-ui';

  import useLocale from '@/locale/useLocale';
  import { getThemeOverrides } from '@/utils/themeOverrides';

  import type { GlobalThemeOverrides } from 'naive-ui';

  const { currentLocale } = useLocale();

  const themeOverridesConfig = ref<GlobalThemeOverrides>(getThemeOverrides());

  const naiveUILocale = computed(() => {
    return currentLocale.value === 'zh-CN' ? zhCN : enUS;
  });

  const naiveUIDateLocale = computed(() => {
    return currentLocale.value === 'zh-CN' ? dateZhCN : dateEnUS;
  });
</script>
