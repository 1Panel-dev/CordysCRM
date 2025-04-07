import { useI18n } from '@lib/shared/hooks/useI18n';

import router from '@/router';
import { WHITE_LIST } from '@/router/constants';
import useUserStore from '@/store/modules/user';

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

  return {
    logout,
    isLoginPage,
    isWhiteListPage,
  };
}
