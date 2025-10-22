<template>
  <Suspense>
    <RouterView />
  </Suspense>
</template>

<script lang="ts" setup>
  import { useRouter } from 'vue-router';
  import { showLoadingToast } from 'vant';

  import useLocale from '@lib/shared/locale/useLocale';
  import { getQueryVariable } from '@lib/shared/method';
  import { hasToken } from '@lib/shared/method/auth';
  import { LocaleType } from '@lib/shared/types/global';

  import useLicenseStore from '@/store/modules/setting/license';
  import useUserStore from '@/store/modules/user';

  import { AppRouteEnum } from '@/enums/routeEnum';

  import useLogin from './hooks/useLogin';

  const router = useRouter();
  const userStore = useUserStore();
  const { oAuthLogin } = useLogin();
  const licenseStore = useLicenseStore();
  const { changeLocale } = useLocale(showLoadingToast);

  onBeforeMount(async () => {
    changeLocale(navigator.language as LocaleType);
    const loginStatus = await userStore.isLogin(true);

    const ua = navigator.userAgent.toLowerCase();
    const isWXWork = ua.includes('wxwork');

    const isDingTalk = ua.includes('dingtalk') || ua.includes('aliapp(dingtalk') || (getQueryVariable('authCode') !== '' && getQueryVariable('authCode') !== undefined && getQueryVariable('authCode') !== null);

    const isLark = ua.includes('feishu') || ua.includes('lark') || getQueryVariable('state') === 'LARK';

    if (!loginStatus && !hasToken() && (isWXWork || isDingTalk || isLark)) {
      await oAuthLogin();
      return;
    }
    router.replace({ name: AppRouteEnum.WORKBENCH });
    licenseStore.getValidateLicense();
  });
</script>

<style lang="less" scoped></style>
