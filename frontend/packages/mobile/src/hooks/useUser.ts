import { showToast } from 'vant';

import { useI18n } from '@lib/shared/hooks/useI18n';

import router from '@/router';
import { WHITE_LIST } from '@/router/constants';
import useUserStore from '@/store/modules/user';

export default function useUser() {
  const { t } = useI18n();

  const logout = async (logoutTo?: string, _noRedirect?: boolean, silence = false) => {
    try {
      const userStore = useUserStore();
      await userStore.logout();
      if (!silence) {
        showToast(t('message.logoutSuccess'));
        router.replace({
          name: logoutTo && typeof logoutTo === 'string' ? logoutTo : 'login',
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
