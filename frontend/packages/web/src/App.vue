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
  import { dateEnUS, dateZhCN, enUS, NConfigProvider, NDialogProvider, NMessageProvider, zhCN } from 'naive-ui';

  import { setLoginExpires, setToken } from '@lib/shared/method/auth';
  import { getQueryVariable } from '@lib/shared/method/index';

  import { getWeComOauthCallback } from '@/api/modules/system/login';
  import useLocale from '@/locale/useLocale';
  import { WHITE_LIST } from '@/router/constants';
  import useAppStore from '@/store/modules/app';

  import useUserStore from './store/modules/user';

  const userStore = useUserStore();
  const { currentLocale } = useLocale();
  const appStore = useAppStore();

  const naiveUILocale = computed(() => {
    return currentLocale.value === 'zh-CN' ? zhCN : enUS;
  });

  const naiveUIDateLocale = computed(() => {
    return currentLocale.value === 'zh-CN' ? dateZhCN : dateEnUS;
  });

   function handleOauthRedirect() {
    if (!WHITE_LIST.find((el) => window.location.hash.split('#')[1].includes(el.path))) {
      const TOKEN = getQueryVariable('_token');
      const CSRF = getQueryVariable('_csrf');
      if (TOKEN && CSRF) {
        setToken(window.atob(TOKEN), CSRF);
        setLoginExpires();
      }
      userStore.checkIsLogin();
      appStore.setLoginLoading(false);
    }
  }

  async function handleOauthLogin() {
    const code = getQueryVariable('code');
    await getWeComOauthCallback(code || '');
    handleOauthRedirect();
  }

  onBeforeMount(() => {
    handleOauthLogin();
  });
</script>
