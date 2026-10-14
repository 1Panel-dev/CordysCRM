import { useRouter } from 'vue-router';
import { showFailToast } from 'vant';
import { AxiosResponse } from 'axios';

import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { isDingTalkBrowser, isLarkBrowser, isWeComBrowser } from '@lib/shared/method';
import { clearToken, setLoginExpires, setLoginType } from '@lib/shared/method/auth';
import { ThirdPartyResourceConfig } from '@lib/shared/models/system/business';
import type { Result } from '@lib/shared/types/axios';

import { getOauthState, getThirdConfigByType, getThirdOauthCallback } from '@/api/modules';
import useUserStore from '@/store/modules/user';

import { AppRouteEnum } from '@/enums/routeEnum';

const platformConfig = {
  // 企业微信
  'wecom': {
    detect: isWeComBrowser,
    platformConfigType: CompanyTypeEnum.WECOM,
    authLoginType: CompanyTypeEnum.WE_COM_OAUTH2,
    type: 'wecom',
    codeKey: 'code',
    authUrl: (config: Record<string, any>, state: string) => {
      const redirectUrl = `${window.location.origin}/mobile`;
      return `https://open.weixin.qq.com/connect/oauth2/authorize?appid=${
        config.corpId
      }&response_type=code&redirect_uri=${encodeURIComponent(redirectUrl)}&scope=snsapi_privateinfo&agentid=${
        config.agentId
      }&state=${encodeURIComponent(state)}#wechat_redirect`;
    },
  },
  // 钉钉
  'ding-talk': {
    detect: isDingTalkBrowser,
    platformConfigType: CompanyTypeEnum.DINGTALK,
    authLoginType: CompanyTypeEnum.DINGTALK_OAUTH2,
    type: 'ding-talk',
    codeKey: 'authCode',
    authUrl: (config: Record<string, any>, state: string) => {
      const redirectUrl = `${window.location.origin}/mobile`;
      return `https://login.dingtalk.com/oauth2/auth?redirect_uri=${encodeURIComponent(
        redirectUrl
      )}&response_type=code&client_id=${config.agentId}&scope=openid corpid&state=${encodeURIComponent(
        state
      )}&prompt=consent&corpid=${config.corpId}`;
    },
  },
  'lark': {
    detect: isLarkBrowser,
    platformConfigType: CompanyTypeEnum.LARK,
    authLoginType: CompanyTypeEnum.LARK_OAUTH2,
    type: 'lark-mobile',
    codeKey: 'code',
    authUrl: (config: Record<string, any>, state: string) => {
      const redirectUrl = `${window.location.origin}/mobile`;
      return `https://open.feishu.cn/open-apis/authen/v1/authorize?app_id=${
        config.agentId
      }&redirect_uri=${encodeURIComponent(redirectUrl)}&state=${encodeURIComponent(state)}`;
    },
  },
} as const;

export default function useLogin() {
  const userStore = useUserStore();
  const router = useRouter();
  const { t } = useI18n();

  async function thirdAuthLogin(code: string, state: string, type: string, loginType: CompanyTypeEnum) {
    try {
      const res = await getThirdOauthCallback(code, type, state);
      const success = userStore.setLoginInfo(res.data.data);
      if (success) {
        setLoginExpires();
        setLoginType(loginType);
        const { redirect, ...othersQuery } = router.currentRoute.value.query;
        await router.replace({
          name: (redirect as string) || AppRouteEnum.WORKBENCH,
          query: {
            ...othersQuery,
          },
        });
      } else {
        throw new Error(t('login.form.login.errMsg'));
      }
    } catch (error) {
      clearToken();
      const detail = error instanceof Error ? undefined : (error as Result)?.message;
      showFailToast(detail || t('login.form.login.errMsg'));
      await router.replace({ name: 'login' });
    }
  }

  async function handleThirdAuthLogin(platformKey: keyof typeof platformConfig): Promise<boolean> {
    const platform = platformConfig[platformKey];
    const url = new URL(window.location.href);
    const code = url.searchParams.get(platform.codeKey);
    const state = url.searchParams.get('state') ?? '';
    const authParams = ['code', 'authCode', 'state', 'error', 'error_description'];
    const authorizationError = url.searchParams.has('error');
    if (authParams.some((param) => url.searchParams.has(param))) {
      // 缺失 state 交给后端拒绝，不在回调后补发；清理参数必须早于路由跳转。
      authParams.forEach((param) => url.searchParams.delete(param));
      window.history.replaceState(window.history.state, document.title, url.toString());
      if (authorizationError || !code) throw new Error(t('login.form.login.errMsg'));
      await thirdAuthLogin(code, state, platform.type, platform.authLoginType);
      return true;
    }

    clearToken();
    const res = await getThirdConfigByType<AxiosResponse<Result<ThirdPartyResourceConfig>>>(
      platform.platformConfigType,
      true
    );
    if (!res?.data?.data?.config) throw new Error(t('login.form.login.errMsg'));
    const { data } = res.data;
    const oauthState = await getOauthState(platform.type);
    if (!oauthState) throw new Error(t('login.form.login.errMsg'));
    const redirectUrl = platform.authUrl(data.config, oauthState);
    window.location.replace(redirectUrl);
    // 浏览器跳转尚未卸载当前页面时，调用方必须停止初始化和业务请求。
    return false;
  }

  async function oAuthLogin(): Promise<boolean> {
    try {
      const platformKeys = Object.keys(platformConfig) as (keyof typeof platformConfig)[];
      const flow = new URLSearchParams(window.location.search).get('state')?.split('.')[0];
      const platformKey =
        platformKeys.find((key) => platformConfig[key].type === flow) ||
        platformKeys.find((key) => platformConfig[key].detect());
      // 没有检测到三方平台企业微信、钉钉浏览器直接跳转到登录
      if (!platformKey) {
        await router.replace({ name: 'login' });
        return true;
      }
      // 检测到任一平台走第三方认证登录
      return await handleThirdAuthLogin(platformKey);
    } catch (error) {
      clearToken();
      const detail = error instanceof Error ? undefined : (error as Result)?.message;
      showFailToast(detail || t('login.form.login.errMsg'));
      await router.replace({ name: 'login' });
      return true;
    }
  }

  return {
    oAuthLogin,
  };
}
