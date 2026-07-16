import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  getKeyUrl,
  isLoginUrl,
  loginUrl,
  oauthStateUrl,
  signoutUrl,
  thirdCallbackUrl,
  thirdOauthCallbackUrl,
} from '@lib/shared/api/requrls/system/login';
import type { LoginParams } from '@lib/shared/models/system/login';
import type { UserInfo } from '@lib/shared/models/user';
import type { Result } from '@lib/shared/types/axios';
import type { AxiosResponse } from 'axios';

export default function useProductApi(CDR: CordysAxios) {
  // 登录
  function login(data: LoginParams) {
    return CDR.post<UserInfo>({ url: loginUrl, data });
  }

  // 登出
  function signout() {
    return CDR.get({ url: signoutUrl });
  }

  // 是否登录
  function isLogin(isDisabledErrorTip = false) {
    return CDR.get<UserInfo>({ url: isLoginUrl }, { ignoreCancelToken: true, noErrorTip: isDisabledErrorTip });
  }

  // 获取登录密钥
  function getKey() {
    return CDR.get<string>({ url: getKeyUrl });
  }

  // 三方二维码登录
  function getThirdCallback(code: string, type: string, state: string) {
    return CDR.get<UserInfo>({ url: `${thirdCallbackUrl}/${type}`, params: { code, state } });
  }

  // 三方oauth2登录
  function getThirdOauthCallback(code: string, type: string, state: string) {
    return CDR.get<AxiosResponse<Result<UserInfo>>>(
      { url: `${thirdOauthCallbackUrl}/${type}`, params: { code, state } },
      { ignoreCancelToken: true, isReturnNativeResponse: true, noErrorTip: true }
    );
  }

  function getOauthState(flow: string) {
    return CDR.get<string>({ url: `${oauthStateUrl}/${flow}` });
  }

  return {
    login,
    signout,
    isLogin,
    getKey,
    getThirdCallback,
    getThirdOauthCallback,
    getOauthState,
  };
}
