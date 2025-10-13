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

  import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
  import useLocale from '@lib/shared/locale/useLocale';
  import { hasToken, setLoginExpires, setLoginType } from '@lib/shared/method/auth';
  import { getQueryVariable, getUrlParameterWidthRegExp } from '@lib/shared/method/index';
  import type { Result } from '@lib/shared/types/axios';

  import CrmSysUpgradeTip from '@/components/pure/crm-sys-upgrade-tip/index.vue';

  import { getThirdOauthCallback } from '@/api/modules';
  import useLoading from '@/hooks/useLoading';
  import useUser from '@/hooks/useUser';
  import useAppStore from '@/store/modules/app';
  import { setFavicon, watchStyle, watchTheme } from '@/utils/theme';

  import useDiscreteApi from './hooks/useDiscreteApi';
  import { WHITE_LIST } from './router/constants';
  import useLicenseStore from './store/modules/setting/license';
  import useUserStore from './store/modules/user';

  const { goUserHasPermissionPage, logout } = useUser();

  const { setLoading } = useLoading();
  const { message } = useDiscreteApi();
  const router = useRouter();

  const userStore = useUserStore();
  const licenseStore = useLicenseStore();
  const { currentLocale } = useLocale(message.loading);
  const appStore = useAppStore();

  const naiveUILocale = computed(() => {
    return currentLocale.value === 'zh-CN' ? zhCN : enUS;
  });

  const naiveUIDateLocale = computed(() => {
    return currentLocale.value === 'zh-CN' ? dateZhCN : dateEnUS;
  });

  async function handleOauthLogin(type: string, loginType: CompanyTypeEnum) {
    try {
      const code = getQueryVariable('code');
      if (code) {
        const res = await getThirdOauthCallback(code, type);
        const boolean = userStore.qrCodeLogin(res.data.data);
        if (boolean) {
          setLoginExpires();
          setLoginType(loginType);

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
      if ((error as Result).code === 100500) {
        router.replace({ name: 'login' });
      }
      if ((error as Result).code === 401) {
        logout();
      }
    }
  }

  onBeforeMount(async () => {
    const isWXWork = navigator.userAgent.includes('wxwork');
    const isDingTalk = navigator.userAgent.includes('dingtalk') || navigator.userAgent.includes('aliapp(dingtalk');
    if (!hasToken()) {
      if (isWXWork) {
        await handleOauthLogin('wecom', CompanyTypeEnum.WE_COM_OAUTH2);
      } else if (isDingTalk) {
        await handleOauthLogin('ding-talk', CompanyTypeEnum.DINGTALK_OAUTH2);
      }
    }

    if (WHITE_LIST.find((el) => window.location.hash.split('#')[1].includes(el.path)) === undefined) {
      await userStore.checkIsLogin(isWXWork || isDingTalk);
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
  watchStyle(appStore.pageConfig.style, appStore.pageConfig);
  watchTheme(appStore.pageConfig.theme, appStore.pageConfig);

  onBeforeMount(() => {
    try {
      licenseStore.getValidateLicense();
      const { isLoginPage } = useUser();
      if (licenseStore.hasLicense() && !isLoginPage()) {
        appStore.initPageConfig();
        if (appStore.pageConfig.icon[0]?.url) {
          setFavicon(appStore.pageConfig.icon[0]?.url);
        }
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  });

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
