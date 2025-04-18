import { useI18n } from '@lib/shared/hooks/useI18n';

import router from '@/router';
import { NO_RESOURCE_ROUTE_NAME, WHITE_LIST } from '@/router/constants';
import useUserStore from '@/store/modules/user';
import { getFirstRouteNameByPermission, routerNameHasPermission } from '@/utils/permission';

import useDiscreteApi from './useDiscreteApi';

export default function useUser() {
  const { t } = useI18n();

  const logout = async (logoutTo?: string, noRedirect?: boolean, silence = false) => {
    try {
      const userStore = useUserStore();
      await userStore.logout();
      const { message } = useDiscreteApi();
      const currentRoute = router.currentRoute.value;
      if (!silence) {
        message.success(t('message.logoutSuccess'));
        router.push({
          name: logoutTo && typeof logoutTo === 'string' ? logoutTo : 'login',
          query: noRedirect
            ? {}
            : {
                ...router.currentRoute.value.query,
                redirect: currentRoute.name as string,
              },
        });
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  };

  const isLoginPage = () => {
    return window.location.hash.indexOf('login') > -1;
  };

  const isWhiteListPage = () => {
    const currentRoute = router.currentRoute.value;
    return WHITE_LIST.some((e) => e.path.includes(currentRoute.path));
  };

  const goUserHasPermissionPage = () => {
    const { redirect, ...othersQuery } = router.currentRoute.value.query;
    const currentRouteName = getFirstRouteNameByPermission(router.getRoutes());
    const redirectHasPermission =
      redirect &&
      ![NO_RESOURCE_ROUTE_NAME].includes(redirect as string) &&
      routerNameHasPermission(redirect as string, router.getRoutes());
    router.push({
      name: redirectHasPermission ? (redirect as string) : currentRouteName,
      query: {
        ...othersQuery,
      },
    });
  };

  return {
    logout,
    isLoginPage,
    isWhiteListPage,
    goUserHasPermissionPage,
  };
}
