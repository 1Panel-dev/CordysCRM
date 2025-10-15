import { useRouter } from 'vue-router';
import { AxiosResponse } from 'axios';

import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
import { getQueryVariable, getUrlParameterWidthRegExp, isDingTalkBrowser, isWeComBrowser } from '@lib/shared/method';
import { setLoginExpires, setLoginType } from '@lib/shared/method/auth';
import { ConfigSynchronization } from '@lib/shared/models/system/business';
import type { Result } from '@lib/shared/types/axios';

import { getThirdConfigByType, getThirdOauthCallback } from '@/api/modules';
import { AUTH_DISABLED_ROUTE_NAME } from '@/router/constants';
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
    codeKeysParams: ['code', 'state'],
    authUrl: (config: ConfigSynchronization) => {
      const redirectUrl = `${window.location.origin}/mobile`;
      return `https://open.weixin.qq.com/connect/oauth2/authorize?appid=${
        config.corpId
      }&response_type=code&redirect_uri=${encodeURIComponent(redirectUrl)}&scope=snsapi_privateinfo&agentid=${
        config.agentId
      }#wechat_redirect`;
    },
  },
  // 钉钉
  'ding-talk': {
    detect: isDingTalkBrowser,
    platformConfigType: CompanyTypeEnum.DINGTALK,
    authLoginType: CompanyTypeEnum.DINGTALK_OAUTH2,
    type: 'ding-talk',
    codeKey: 'authCode',
    codeKeysParams: ['code', 'authCode', 'state'],
    authUrl: (config: ConfigSynchronization) => {
      const redirectUrl = `${window.location.origin}/mobile`;
      return `https://login.dingtalk.com/oauth2/auth?redirect_uri=${encodeURIComponent(
        redirectUrl
      )}&response_type=code&client_id=${config.agentId}&scope=openid corpid&state=ding&prompt=consent&corpid=${
        config.corpId
      }`;
    },
  },
} as const;

export default function useLogin() {
  const userStore = useUserStore();
  const router = useRouter();

  async function thirdAuthLogin(code: string, type: string, loginType: CompanyTypeEnum) {
    try {
      const res = await getThirdOauthCallback(code, type);
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
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      if ((error as Result).code === 401) {
        router.replace('login');
      }
    }
  }

  async function handleThirdAuthLogin(platformKey: keyof typeof platformConfig) {
    const platform = platformConfig[platformKey];
    const code = getQueryVariable(platform.codeKey) ?? '';
    if (code) {
      await thirdAuthLogin(code, platform.type, platform.authLoginType);

      // 清理参数
      const url = new URL(window.location.href);
      platform.codeKeysParams.forEach((param) => {
        getUrlParameterWidthRegExp(param);
        url.searchParams.delete(param);
      });
      // 或者在不刷新页面的情况下更新URL（比如使用 History API）
      window.history.replaceState({}, document.title, url.toString());
      return;
    }

    const res = await getThirdConfigByType<AxiosResponse<Result<ConfigSynchronization>>>(
      platform.platformConfigType,
      true
    );
    if (res) {
      const { data } = res.data;
      const redirectUrl = platform.authUrl(data);
      window.location.replace(redirectUrl);
    }
  }

  async function oAuthLogin() {
    try {
      const platformKey = Object.keys(platformConfig).find((key) =>
        platformConfig[key as keyof typeof platformConfig].detect()
      ) as keyof typeof platformConfig | undefined;
      // 没有检测到三方平台企业微信、钉钉浏览器直接跳转到登录
      if (!platformKey) {
        return router.replace({ name: 'login' });
      }
      // 检测到任一平台走第三方认证登录
      await handleThirdAuthLogin(platformKey);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      const err = error as Result;
      if (err.code === 100500) {
        router.replace({ name: 'login' });
      } else if (err.code === 401) {
        router.replace(AUTH_DISABLED_ROUTE_NAME);
      }
    }
  }

  return {
    oAuthLogin,
  };
}
