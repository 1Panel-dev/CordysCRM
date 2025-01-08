const SESSION_ID = 'sessionId';
const CSRF_TOKEN = 'csrfToken';
const LOGIN_TYPE = 'loginType';

const isLogin = async () => {
  try {
    return Promise.resolve(false); // TODO:
  } catch (err) {
    return false;
  }
};

// 获取token
const getToken = () => {
  return { [SESSION_ID]: localStorage.getItem(SESSION_ID), [CSRF_TOKEN]: localStorage.getItem(CSRF_TOKEN) || '' };
};

const setToken = (sessionId: string, csrfToken: string) => {
  localStorage.setItem(SESSION_ID, sessionId);
  localStorage.setItem(CSRF_TOKEN, csrfToken);
};

const setLongType = (loginType: string) => {
  localStorage.setItem(LOGIN_TYPE, loginType);
};

const getLongType = () => {
  return localStorage.getItem(LOGIN_TYPE);
};

const clearToken = () => {
  localStorage.removeItem(SESSION_ID);
  localStorage.removeItem(CSRF_TOKEN);
};

const hasToken = (name: string) => {
  return !!localStorage.getItem(SESSION_ID) && !!localStorage.getItem(CSRF_TOKEN);
};

const setLoginExpires = () => {
  localStorage.setItem('loginExpires', Date.now().toString());
};

const isLoginExpires = () => {
  const lastLoginTime = Number(localStorage.getItem('loginExpires'));
  const now = Date.now();
  const diff = now - lastLoginTime;
  const thirtyDay = 24 * 60 * 60 * 1000 * 30;
  return diff > thirtyDay;
};

export { clearToken, getLongType, getToken, hasToken, isLogin, isLoginExpires, setLoginExpires, setLongType, setToken };
