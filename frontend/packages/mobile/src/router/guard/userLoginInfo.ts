import { isWeComBrowser } from '@lib/shared/method';
import { clearToken, hasToken, isLoginExpires } from '@lib/shared/method/auth';

import useUser from '@/hooks/useUser';

import { AppRouteEnum } from '@/enums/routeEnum';

import { LOGIN_LOADING } from '../constants';
import NProgress from 'nprogress';
import type { Router } from 'vue-router';

export default function setupUserLoginInfoGuard(router: Router) {
  router.beforeEach(async (to, from, next) => {
    NProgress.start();

    const { isWhiteListPage } = useUser();
    // 登录过期清除token
    if (isLoginExpires()) {
      clearToken();
    }

    const tokenExists = hasToken(to.name as string);

    // 未登录访问受限页面重定向登录页
    if (!tokenExists) {
      if (to.name === 'login') {
        // 允许主动退出后访问 login
        next();
        NProgress.done();
        return;
      }
      // 企业微信进入，未登录则进去loading页面
      if (isWeComBrowser()) {
        if (to.name !== LOGIN_LOADING) {
          next({ name: LOGIN_LOADING });
          NProgress.done();
          return;
        }
        // 其他浏览器进入则到登录页面
      } else if (to.name !== 'login' && !isWhiteListPage()) {
        next({ name: 'login' });
        NProgress.done();
        return;
      }
    }
    // 已登录访问登录页面和loading页面则都去首页
    if ((to.name === 'login' || to.name === LOGIN_LOADING) && tokenExists) {
      next({ name: AppRouteEnum.WORKBENCH });
      NProgress.done();
      return;
    }

    // 其他情况（放行：已登录访问正常页面\未登录访问白名单页面）
    next();
    NProgress.done();
  });
}
