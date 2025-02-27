import {getKeyUrl, isLoginUrl, loginUrl, signoutUrl, weComCallbackUrl} from '@lib/shared/api/requrls/system/login';
import type { LoginParams } from '@lib/shared/models/system/login';
import type { UserInfo } from '@lib/shared/models/user';

import CDR from '@/api/http/index';

// 登录
export function login(data: LoginParams) {
  return CDR.post<UserInfo>({ url: loginUrl, data });
}

// 登出
export function signout() {
  return CDR.get({ url: signoutUrl });
}

// 是否登录
export function isLogin() {
  return CDR.get<UserInfo>({ url: isLoginUrl }, { ignoreCancelToken: true });
}

// 获取登录密钥
export function getKey() {
  return CDR.get<string>({ url: getKeyUrl });
}

// 企业微信二维码登录
export function getWeComCallback(code: string) {
  return CDR.get<UserInfo>({ url: weComCallbackUrl,  params: { code } });
}
