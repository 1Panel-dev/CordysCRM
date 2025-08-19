import { useRouter } from 'vue-router';

import { getQueryVariable, getUrlParameterWidthRegExp } from '@lib/shared/method';
import { setLoginExpires, setLoginType } from '@lib/shared/method/auth';
import type { Result } from '@lib/shared/types/axios';

import { getThirdConfigByType, getWeComOauthCallback } from '@/api/modules';
import useUserStore from '@/store/modules/user';

import { AppRouteEnum } from '@/enums/routeEnum';

export default function useLogin() {
  const userStore = useUserStore();
  const router = useRouter();

  function isWeComBrowser() {
    const ua = window.navigator.userAgent.toLowerCase();
    return ua.includes('wxwork'); // 企业微信 UA 一定包含 "wxwork"
  }

  async function weComAuthLogin(code: string) {
    try {
      const res = await getWeComOauthCallback(code);
      const success = userStore.setLoginInfo(res.data.data);
      if (success) {
        setLoginExpires();
        setLoginType('WE_COM_OAUTH2');
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

  async function oAuthLogin() {
    try {
      if (!isWeComBrowser()) {
        return router.replace({ name: 'login' });
      }
      const code = getQueryVariable('code');
      if (code) {
        await weComAuthLogin(code);
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
        const res = await getThirdConfigByType('WE_COM_OAUTH2');
        const redirectUrl = `${window.location.origin}/mobile`;
        const url = `https://open.weixin.qq.com/connect/oauth2/authorize?appid=${
          res.corpId
        }&response_type=code&redirect_uri=${encodeURIComponent(redirectUrl)}&scope=snsapi_privateinfo&agentid=${
          res.agentId
        }#wechat_redirect`;
        window.location.replace(url);
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      if ((error as Result).code === 401) {
        router.replace('login');
      }
    }
  }

  return {
    oAuthLogin,
  };
}
