<template>
  <Suspense v-if="appInitialized">
    <RouterView />
  </Suspense>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { showLoadingToast } from 'vant';

  import useLocale from '@lib/shared/locale/useLocale';
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
  const appInitialized = ref(false);

  onBeforeMount(async () => {
    try {
      changeLocale(navigator.language as LocaleType);
      await router.isReady();
      const params = new URLSearchParams(window.location.search);
      const flow = params.get('state')?.split('.')[0];
      const hasCallback = ['code', 'authCode', 'state', 'error', 'error_description'].some((param) =>
        params.has(param)
      );
      // 回调优先于登录态检查，避免旧 Token 或旧会话跳过本次授权。
      const loginStatus = hasCallback ? false : await userStore.isLogin(true);

      const ua = navigator.userAgent.toLowerCase();
      const isWXWork = ua.includes('wxwork') || flow === 'wecom';

      const isDingTalk = ua.includes('dingtalk') || !!params.get('authCode') || flow === 'ding-talk';

      const isLark = ua.includes('feishu') || ua.includes('lark') || flow === 'lark-mobile';

      if (!loginStatus && (isWXWork || isDingTalk || isLark)) {
        await oAuthLogin();
        await licenseStore.getValidateLicense();
        return;
      }
      await licenseStore.getValidateLicense();
      await router.replace({ name: AppRouteEnum.WORKBENCH });
    } finally {
      appInitialized.value = true;
    }
  });
</script>

<style lang="less" scoped></style>
