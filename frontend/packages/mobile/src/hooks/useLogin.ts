import { useRouter } from 'vue-router';
import { AxiosResponse } from 'axios';

import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
import {getQueryVariable, getUrlParameterWidthRegExp, isDingTalBrowserk, isWeComBrowser} from '@lib/shared/method';
import { setLoginExpires, setLoginType } from '@lib/shared/method/auth';
import { ConfigSynchronization } from '@lib/shared/models/system/business';
import type { Result } from '@lib/shared/types/axios';

import { getThirdConfigByType, getThirdOauthCallback } from '@/api/modules';
import { AUTH_DISABLED_ROUTE_NAME } from '@/router/constants';
import useUserStore from '@/store/modules/user';

import { AppRouteEnum } from '@/enums/routeEnum';

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

    async function weComAuthLoginAndReplace(code: string) {
      if (code) {
          await thirdAuthLogin(code, 'wecom', CompanyTypeEnum.WE_COM_OAUTH2);
          const currentUrl = window.location.href;
          const url = new URL(currentUrl);
          getUrlParameterWidthRegExp('code');
          getUrlParameterWidthRegExp('state');
          url.searchParams.delete('code');
          url.searchParams.delete('state');
          const newUrl = url.toString();
          // 或者在不刷新页面的情况下更新URL（比如使用 History API）
          window.history.replaceState({}, document.title, newUrl);
      } else {
          const res = await getThirdConfigByType<AxiosResponse<Result<ConfigSynchronization>>>(
              CompanyTypeEnum.WE_COM_OAUTH2
          );
          if (res) {
              const { data } = res.data;
              const redirectUrl = `${window.location.origin}/mobile`;
              const url = `https://open.weixin.qq.com/connect/oauth2/authorize?appid=${
                  data.corpId
              }&response_type=code&redirect_uri=${encodeURIComponent(redirectUrl)}&scope=snsapi_privateinfo&agentid=${
                  data.agentId
              }#wechat_redirect`;
              window.location.replace(url);
          }
      }
  }

    async function dingAuthLoginAndReplace(code: string) {
        if (code) {
            await thirdAuthLogin(code, 'ding-talk', CompanyTypeEnum.DINGTALK_OAUTH2);
            const currentUrl = window.location.href;
            const url = new URL(currentUrl);
            getUrlParameterWidthRegExp('code');
            getUrlParameterWidthRegExp('state');
            url.searchParams.delete('code');
            url.searchParams.delete('state');
            const newUrl = url.toString();
            // 或者在不刷新页面的情况下更新URL（比如使用 History API）
            window.history.replaceState({}, document.title, newUrl);
        } else {
            const res = await getThirdConfigByType<AxiosResponse<Result<ConfigSynchronization>>>(
                CompanyTypeEnum.DINGTALK_OAUTH2
            );
            if (res) {
                const { data } = res.data;
                const redirectUrl = `${window.location.origin}/mobile`;
                const url = `https://login.dingtalk.com/oauth2/auth?redirect_uri=${encodeURIComponent(redirectUrl)}&response_type=code&client_id=${
                    data.agentId
                }&scope=openid corpid&state=dddd&prompt=consent&corpid=${
                    data.corpId
                }`;
                window.location.replace(url);
            }
        }
    }


  async function oAuthLogin() {
    try {
      if (!isWeComBrowser() && !isDingTalBrowserk()) {
        return router.replace({ name: 'login' });
      }
      const code = getQueryVariable('code');
      if (isWeComBrowser()) {
          await weComAuthLoginAndReplace(code || '');
      } else if (isDingTalBrowserk()) {
          await dingAuthLoginAndReplace(code || '');
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      if ((error as Result).code === 100500) {
        router.replace({ name: 'login' });
      }

      if ((error as Result).code === 401) {
        router.replace(AUTH_DISABLED_ROUTE_NAME);
      }
    }
  }



  return {
    oAuthLogin,
  };
}
