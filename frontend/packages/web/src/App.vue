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

  import useLocale from '@lib/shared/locale/useLocale';
  import { setLoginExpires, setLoginType } from '@lib/shared/method/auth';
  import { getQueryVariable, getUrlParameterWidthRegExp } from '@lib/shared/method/index';
  import type { Result } from '@lib/shared/types/axios';

  import CrmSysUpgradeTip from '@/components/pure/crm-sys-upgrade-tip/index.vue';

  import { getWeComOauthCallback } from '@/api/modules';
  import useLoading from '@/hooks/useLoading';
  import useUser from '@/hooks/useUser';
  import useAppStore from '@/store/modules/app';

  import useDiscreteApi from './hooks/useDiscreteApi';
  import { WHITE_LIST } from './router/constants';
  import useUserStore from './store/modules/user';

  const { goUserHasPermissionPage, logout } = useUser();

  const { setLoading } = useLoading();
  const { message } = useDiscreteApi();

  const userStore = useUserStore();
  const { currentLocale } = useLocale(message.loading);
  const appStore = useAppStore();

  const naiveUILocale = computed(() => {
    return currentLocale.value === 'zh-CN' ? zhCN : enUS;
  });

  const naiveUIDateLocale = computed(() => {
    return currentLocale.value === 'zh-CN' ? dateZhCN : dateEnUS;
  });

  async function handleOauthLogin() {
    try {
      const code = getQueryVariable('code');
      if (code) {
        const res = await getWeComOauthCallback(code);
        const boolean = userStore.qrCodeLogin(res.data.data);
        if (boolean) {
          setLoginExpires();
          setLoginType('WE_COM_OAUTH2');

          goUserHasPermissionPage();
          setLoading(false);
          await userStore.getAuthentication();
        }
        if (code) {
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
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      if ((error as Result).code === 401) {
        logout();
      }
    }
  }

  onBeforeMount(async () => {
    await handleOauthLogin();
    if (WHITE_LIST.find((el) => window.location.hash.split('#')[1].includes(el.path)) === undefined) {
      await userStore.checkIsLogin();
      appStore.setLoginLoading(false);
    }
  });

  onBeforeUnmount(() => {
    appStore.disconnectSystemMessageSSE();
  });

  function showUpdateMessage() {
    message.warning(() => h(CrmSysUpgradeTip), {
      duration: 0,
      closable: false,
    });
  }

  function adjustOSTheme() {
    const isMac = navigator.platform.toUpperCase().includes('MAC');
    if (!isMac) {
      document.documentElement.style.setProperty('--text-n9', 'var(--text-n8)');
    }
  }

  onMounted(() => {
    adjustOSTheme();
    window.onerror = (_message) => {
      if (typeof _message === 'string' && _message.includes('Failed to fetch dynamically imported')) {
        showUpdateMessage();
      }
    };

    window.onunhandledrejection = (event: PromiseRejectionEvent) => {
      if (
        event &&
        event.reason &&
        event.reason.message &&
        event.reason.message.includes('Failed to fetch dynamically imported')
      ) {
        showUpdateMessage();
      }
    };
  });
</script>
