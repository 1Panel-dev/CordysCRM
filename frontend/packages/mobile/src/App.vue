<template>
  <Suspense v-if="appInitialized">
    <RouterView />
  </Suspense>
  <LoginLoading v-else class="h-screen" role="status" />
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { useRouter } from 'vue-router';

  import { clearToken, hasToken } from '@lib/shared/method/auth';

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
      // main.ts 已初始化语言，授权期间再次切换语言可能触发整页刷新。
      await router.isReady();
      const params = new URLSearchParams(window.location.search);
      const flow = params.get('state')?.split('.')[0];
      const hasCallback = ['code', 'authCode', 'state', 'error', 'error_description'].some((param) =>
        params.has(param)
      );
      // 回调优先于登录态检查，避免旧 Token 或旧会话跳过本次授权。
      const loginStatus = hasCallback ? false : await userStore.isLogin(true);

      const ua = navigator.userAgent.toLowerCase();
      const callbackFlow = ['wecom', 'ding-talk', 'lark', 'lark-mobile'].find((type) => type === flow);
      const isWXWork = callbackFlow ? callbackFlow === 'wecom' : ua.includes('wxwork');
      const isDingTalk = callbackFlow
        ? callbackFlow === 'ding-talk'
        : ua.includes('dingtalk') || !!params.get('authCode');
      const isLark = callbackFlow
        ? callbackFlow === 'lark' || callbackFlow === 'lark-mobile'
        : ua.includes('feishu') || ua.includes('lark');

      if (!loginStatus && (isWXWork || isDingTalk || isLark)) {
        clearToken();
        // 先完成登录路由，避免旧会话的 401 路由跳转取消授权请求。
        await router.replace({ name: LOGIN_LOADING, query: router.currentRoute.value.query });
        redirecting = !(await oAuthLogin());
        if (redirecting) return;
        if (hasToken()) await licenseStore.getValidateLicense();
        return;
      }
      if (hasToken()) await licenseStore.getValidateLicense();
      await router.replace({ name: AppRouteEnum.WORKBENCH });
    } finally {
      // 整页授权期间保持加载页，不能提前挂载页面或请求需要认证的接口。
      appInitialized.value = !redirecting;
    }
  });
</script>

<style lang="less" scoped></style>
