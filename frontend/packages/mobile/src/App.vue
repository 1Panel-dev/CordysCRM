<template>
  <Suspense v-if="appInitialized">
    <RouterView />
  </Suspense>
  <LoginLoading v-else class="h-screen" role="status" />
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { useRouter } from 'vue-router';

  import { hasToken } from '@lib/shared/method/auth';

  import LoginLoading from '@/views/base/login/loading.vue';

  import { LOGIN_LOADING } from '@/router/constants';
  import useLicenseStore from '@/store/modules/setting/license';
  import useUserStore from '@/store/modules/user';

  import { AppRouteEnum } from '@/enums/routeEnum';

  import useLogin from './hooks/useLogin';

  const router = useRouter();
  const userStore = useUserStore();
  const { oAuthLogin } = useLogin();
  const licenseStore = useLicenseStore();
  const appInitialized = ref(false);

  onBeforeMount(async () => {
    let redirecting = false;
    try {
      // main.ts 已初始化语言；回调期间切换语言可能整页重载并重放一次性授权参数。
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
        await router.replace({ name: LOGIN_LOADING, query: router.currentRoute.value.query });
        redirecting = !(await oAuthLogin());
        if (!redirecting && hasToken()) await licenseStore.getValidateLicense();
        return;
      }
      if (hasToken()) await licenseStore.getValidateLicense();
      await router.replace({ name: AppRouteEnum.WORKBENCH });
    } finally {
      appInitialized.value = !redirecting;
    }
  });
</script>

<style lang="less" scoped></style>
