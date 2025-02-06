import CDR from '@/api/http/index';

import { getKeyUrl, isLoginUrl, loginUrl, signoutUrl } from '@lib/shared/api/requrls/system/login';
import type { LoginParams } from '@lib/shared/models/system/login';

// 登录
export function login(data: LoginParams) {
  return CDR.post({ url: loginUrl, data });
}

// 登出
export function signout() {
  return CDR.get({ url: signoutUrl });
}

// 是否登录
export function isLogin() {
  return CDR.get({ url: isLoginUrl });
}

// 获取登录密钥
export function getKey() {
  return CDR.get<string>({ url: getKeyUrl });
}
