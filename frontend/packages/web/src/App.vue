<template>
  <n-config-provider
    :theme-overrides="appStore.themeOverridesConfig"
    :locale="naiveUILocale"
    :date-locale="naiveUIDateLocale"
  >
    <n-message-provider>
      <n-dialog-provider>
        <RouterView />
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { dateEnUS, dateZhCN, enUS, NConfigProvider, NDialogProvider, NMessageProvider, zhCN } from 'naive-ui';

  import { setLoginExpires, setLoginType } from '@lib/shared/method/auth';
  import {getQueryVariable, getUrlParameterWidthRegExp} from '@lib/shared/method/index';

  import { getWeComOauthCallback } from '@/api/modules/system/login';
  import useLocale from '@/locale/useLocale';
  import { NO_RESOURCE_ROUTE_NAME, WHITE_LIST } from '@/router/constants';
  import useAppStore from '@/store/modules/app';

  import useUserStore from './store/modules/user';

  const router = useRouter();
  const userStore = useUserStore();
  const { currentLocale } = useLocale();
  const appStore = useAppStore();

  const naiveUILocale = computed(() => {
    return currentLocale.value === 'zh-CN' ? zhCN : enUS;
  });

  const naiveUIDateLocale = computed(() => {
    return currentLocale.value === 'zh-CN' ? dateZhCN : dateEnUS;
  });

  async function handleOauthLogin() {
    const code = getQueryVariable('code');
    if (code) {
      const weComCallback = getWeComOauthCallback(code);
      userStore.qrCodeLogin(await weComCallback);
      setLoginType('WE_COM');
      const { redirect, ...othersQuery } = router.currentRoute.value.query;
      setLoginExpires();
      router.push({
        name: (redirect as string) || NO_RESOURCE_ROUTE_NAME,
        query: {
          ...othersQuery,
        },
      });
    }
    if (code && getQueryVariable('state')) {
      const currentUrl = window.location.href;
      const url = new URL(currentUrl);
      getUrlParameterWidthRegExp('code');
      getUrlParameterWidthRegExp('state');
      url.searchParams.delete('code');
      url.searchParams.delete('state');
      const newUrl = url.toString();
      // 或者在不刷新页面的情况下更新URL（比如使用 History API）
      window.history.replaceState({}, document.title, newUrl);
    }

  }

  onBeforeMount(() => {
    handleOauthLogin();
  });
</script>
