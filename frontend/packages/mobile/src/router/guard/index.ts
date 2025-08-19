import { AxiosCanceler } from '@lib/shared/api/http/axiosCancel';
import { setRouteEmitter } from '@lib/shared/method/route-listener';

import setupPermissionGuard from './permission';
import setupUserLoginInfoGuard from './userLoginInfo';
import NProgress from 'nprogress';
import type { Router } from 'vue-router';

function setupPageGuard(router: Router) {
  const axiosCanceler = new AxiosCanceler();
  router.beforeEach((to, from, next) => {
    NProgress.start();
    // 监听路由变化
    setRouteEmitter(to);
    // 取消上个路由未完成的请求（不包含设置了ignoreCancelToken的请求）
    axiosCanceler.removeAllPending();
    // TODO 携带参数
    next();
  });
}

export default function createRouteGuard(router: Router) {
  // 设置路由监听守卫
  setupPageGuard(router);
  // 设置用户登录校验守卫
  // TODO test xinixnwu
  const isWecom = window.navigator.userAgent.toLowerCase().includes('wxwork');
  if (!isWecom) {
    setupUserLoginInfoGuard(router);
  }
  // 设置菜单权限守卫
  setupPermissionGuard(router);
}
