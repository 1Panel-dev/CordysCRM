<template>
  <n-config-provider
    :theme-overrides="appStore.themeOverridesConfig"
    :locale="naiveUILocale"
    :date-locale="naiveUIDateLocale"
  >
    <n-message-provider>
      <n-dialog-provider>
        <template v-if="appInitialized">
          <RouterView />
          <CrmLicenseFeatureNotice />
        </template>
        <n-spin v-else class="flex h-screen items-center justify-center" :description="t('login.form.loading')" />
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { dateEnUS, dateZhCN, enUS, NConfigProvider, NDialogProvider, NMessageProvider, NSpin, zhCN } from 'naive-ui';

  import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import useLocale from '@lib/shared/locale/useLocale';
  import { clearToken, hasToken, setLoginExpires, setLoginType } from '@lib/shared/method/auth';
  import type { Result } from '@lib/shared/types/axios';

  import CrmSysUpgradeTip from '@/components/pure/crm-sys-upgrade-tip/index.vue';
  import CrmLicenseFeatureNotice from '@/components/business/crm-license-feature-notice/index.vue';

  import { getOauthState, getThirdConfigByType, getThirdOauthCallback } from '@/api/modules';
  import useLoading from '@/hooks/useLoading';
  import useUser from '@/hooks/useUser';
  import useAppStore from '@/store/modules/app';
  import { setFavicon, watchStyle, watchTheme } from '@/utils/theme';

  import useDiscreteApi from './hooks/useDiscreteApi';
  import { WHITE_LIST } from './router/constants';
  import useLicenseStore from './store/modules/setting/license';
  import useUserStore from './store/modules/user';

  const { goUserHasPermissionPage } = useUser();

  const { setLoading } = useLoading();
  const { message } = useDiscreteApi();
  const { t } = useI18n();
  const router = useRouter();

  const userStore = useUserStore();
  const licenseStore = useLicenseStore();
  const { currentLocale } = useLocale(message.loading);
  const appStore = useAppStore();
  const appInitialized = ref(false);

  const naiveUILocale = computed(() => {
    return currentLocale.value === 'zh-CN' ? zhCN : enUS;
  });

  const naiveUIDateLocale = computed(() => {
    return currentLocale.value === 'zh-CN' ? dateZhCN : dateEnUS;
  });

  async function handleOauthLogin(
    type: 'wecom' | 'ding-talk' | 'lark',
    loginType: CompanyTypeEnum,
    isDingBrowser: boolean
  ): Promise<boolean> {
    const retryKey = `cordys.oauth2.workbench-retry.${type}`;
    try {
      const codeKey = isDingBrowser ? 'authCode' : 'code';
      const url = new URL(window.location.href);
      const authParams = ['code', 'authCode', 'state', 'error', 'error_description'];
      const code = url.searchParams.get(codeKey);
      const state = url.searchParams.get('state') ?? '';
      const authorizationError = url.searchParams.has('error');
      const hasCallback = authParams.some((param) => url.searchParams.has(param));
      if (hasCallback) {
        // 先清理一次性参数，避免刷新重放或覆盖后续路由。
        authParams.forEach((param) => url.searchParams.delete(param));
        window.history.replaceState(window.history.state, document.title, url.toString());
        if (authorizationError || !code) {
          throw new Error(t('login.form.login.errMsg'));
        }
        if (state.startsWith(`${type}.`)) {
          sessionStorage.removeItem(retryKey);
          const res = await getThirdOauthCallback(code, type, state);
          const boolean = userStore.qrCodeLogin(res.data.data);
          if (boolean) {
            setLoginExpires();
            setLoginType(loginType);

            await goUserHasPermissionPage();
            setLoading(false);
            await userStore.getAuthentication();
          } else {
            throw new Error(t('login.form.login.errMsg'));
          }
          return true;
        }

        // 仅客户端工作台旧链接允许丢弃旧 code 后重新授权一次。
        const ua = navigator.userAgent.toLowerCase();
        const isPlatformBrowser = {
          'wecom': ua.includes('wxwork'),
          'ding-talk': ua.includes('dingtalk'),
          'lark': ua.includes('feishu') || ua.includes('lark'),
        }[type];
        if (!isPlatformBrowser || sessionStorage.getItem(retryKey)) {
          throw new Error(t('login.form.login.errMsg'));
        }
      } else {
        sessionStorage.removeItem(retryKey);
        if (await userStore.isLogin(true)) {
          if (router.currentRoute.value.name === 'login') await goUserHasPermissionPage();
          return true;
        }
      }
      clearToken();
      const configType = {
        'wecom': CompanyTypeEnum.WECOM,
        'ding-talk': CompanyTypeEnum.DINGTALK,
        'lark': CompanyTypeEnum.LARK,
      }[type];
      const { config } = await getThirdConfigByType(configType);
      const redirectUrl = type === 'lark' ? config.redirectUrl : `${window.location.origin}/`;
      if (!config.agentId || !redirectUrl || (type !== 'lark' && !config.corpId)) {
        throw new Error(t('login.form.login.errMsg'));
      }
      if (new URL(redirectUrl).origin !== window.location.origin) {
        throw new Error(t('login.form.login.errMsg'));
      }
      // 与移动端一样，在跳转授权前签发 state，并由平台随授权码原样带回。
      const oauthState = await getOauthState(type);
      if (!oauthState) throw new Error(t('login.form.login.errMsg'));
      const params = new URLSearchParams({ redirect_uri: redirectUrl, response_type: 'code', state: oauthState });
      let authUrl: string;
      if (type === 'wecom') {
        params.set('appid', config.corpId);
        params.set('agentid', config.agentId);
        params.set('scope', 'snsapi_privateinfo');
        authUrl = `https://open.weixin.qq.com/connect/oauth2/authorize?${params}#wechat_redirect`;
      } else if (type === 'ding-talk') {
        params.set('client_id', config.agentId);
        params.set('scope', 'openid corpid');
        params.set('corpid', config.corpId);
        params.set('prompt', 'consent');
        authUrl = `https://login.dingtalk.com/oauth2/auth?${params}`;
      } else {
        params.set('client_id', config.agentId);
        authUrl = `https://passport.feishu.cn/suite/passport/oauth/authorize?${params}`;
      }
      if (hasCallback) sessionStorage.setItem(retryKey, '1');
      window.location.replace(authUrl);
      // 整页授权跳转期间继续显示加载状态，不能挂载扫码页或发起业务请求。
      return false;
    } catch (error) {
      sessionStorage.removeItem(retryKey);
      clearToken();
      const detail = error instanceof Error ? undefined : (error as Result)?.message;
      message.error(detail || t('login.form.login.errMsg'));
      await router.replace({ name: 'login' });
      return true;
    }
  }

  onBeforeMount(async () => {
    try {
      // 初始路由守卫会取消请求，等待其完成后再申请 state。
      await router.isReady();
      const ua = navigator.userAgent.toLowerCase();
      const params = new URLSearchParams(window.location.search);
      const flow = params.get('state')?.split('.')[0];
      const isWXWork = ua.includes('wxwork') || flow === 'wecom';
      const isDingTalk = ua.includes('dingtalk') || !!params.get('authCode') || flow === 'ding-talk';
      const isLark = ua.includes('feishu') || ua.includes('lark') || flow === 'lark';
      // 授权回调优先处理，不能因残留的本地 Token 被跳过。
      if (isWXWork) {
        if (!(await handleOauthLogin('wecom', CompanyTypeEnum.WE_COM_OAUTH2, false))) return;
      } else if (isDingTalk) {
        if (!(await handleOauthLogin('ding-talk', CompanyTypeEnum.DINGTALK_OAUTH2, true))) return;
      } else if (isLark) {
        if (!(await handleOauthLogin('lark', CompanyTypeEnum.LARK_OAUTH2, false))) return;
      } else if (!WHITE_LIST.some((route) => route.name === router.currentRoute.value.name)) {
        await userStore.checkIsLogin(true);
      }

      await appStore.initThirdPartyResource();
      // 认证与路由先完成，再加载需要登录态的配置，避免旧会话的 401 打断新 OAuth 会话。
      if (hasToken()) {
        await licenseStore.ensureLicenseValidated();
        if (licenseStore.hasLicense()) {
          await appStore.initPageConfig();
          if (appStore.pageConfig.icon[0]?.url) {
            setFavicon(appStore.pageConfig.icon[0]?.url);
          }
        }
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      appStore.setLoginLoading(false);
    }
    appInitialized.value = true;
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

  onMounted(() => {
    adjustOSTheme();
    window.onerror = (_message) => {
      if (
        typeof _message === 'string' &&
        (_message.includes('Failed to fetch dynamically imported') ||
          _message.includes('error loading dynamically imported module'))
      ) {
        showUpdateMessage();
      }
    };

    window.onunhandledrejection = (event: PromiseRejectionEvent) => {
      if (
        event &&
        event.reason &&
        event.reason.message &&
        (event.reason.message.includes('Failed to fetch dynamically imported') ||
          event.reason.message.includes('error loading dynamically imported module'))
      ) {
        showUpdateMessage();
      }
    };
  });
</script>
