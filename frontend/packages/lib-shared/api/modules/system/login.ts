import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  getKeyUrl,
  isLoginUrl,
  loginUrl,
  signoutUrl,
  weComCallbackUrl,
  weComOauthCallbackUrl,
} from '@lib/shared/api/requrls/system/login';
import type { LoginParams } from '@lib/shared/models/system/login';
import type { UserInfo } from '@lib/shared/models/user';

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
  function isLogin() {
    return CDR.get<UserInfo>({ url: isLoginUrl }, { ignoreCancelToken: true });
  }

  // 获取登录密钥
  function getKey() {
    return CDR.get<string>({ url: getKeyUrl });
  }

  // 企业微信二维码登录
  function getWeComCallback(code: string) {
    return CDR.get<UserInfo>({ url: weComCallbackUrl, params: { code } });
  }

  // 企业微信oauth2登录
  function getWeComOauthCallback(code: string) {
    return CDR.get<UserInfo>({ url: weComOauthCallbackUrl, params: { code } }, { ignoreCancelToken: true });
  }

  return {
    login,
    signout,
    isLogin,
    getKey,
    getWeComCallback,
    getWeComOauthCallback,
  };
}
