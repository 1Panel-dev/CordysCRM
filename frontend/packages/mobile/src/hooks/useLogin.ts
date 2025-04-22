import { useRouter } from 'vue-router';

import { getQueryVariable, getUrlParameterWidthRegExp } from '@lib/shared/method';
import { setLoginExpires, setLoginType } from '@lib/shared/method/auth';

import { getThirdConfigByType, getWeComOauthCallback } from '@/api/modules';
import useUserStore from '@/store/modules/user';

import { AppRouteEnum } from '@/enums/routeEnum';

export default function useLogin() {
  const userStore = useUserStore();
  const router = useRouter();

  async function oAuthLogin() {
    try {
      const code = getQueryVariable('code');
      if (code) {
        const weComCallback = await getWeComOauthCallback(code);
        const boolean = userStore.setLoginInfo(weComCallback);
        if (boolean) {
          setLoginExpires();
          setLoginType('WE_COM_OAUTH2');
          const { redirect, ...othersQuery } = router.currentRoute.value.query;
          await router.push({
            name: (redirect as string) || AppRouteEnum.WORKBENCH,
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
      } else {
        const res = await getThirdConfigByType('WE_COM_OAUTH2');
        const redirectUrl = `${window.location.origin}/mobile`;
        const url = `https://open.weixin.qq.com/connect/oauth2/authorize?appid=${
          res.corpId
        }&response_type=code&redirect_uri=${encodeURIComponent(redirectUrl)}&scope=snsapi_privateinfo&agentid=${
          res.agentId
        }#wechat_redirect`;
        window.location.href = url;
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  return {
    oAuthLogin,
  };
}
