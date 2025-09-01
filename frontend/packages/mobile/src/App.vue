<template>
  <Suspense>
    <RouterView />
  </Suspense>
</template>

<script lang="ts" setup>
  import { useRouter } from 'vue-router';

  import useLicenseStore from '@/store/modules/setting/license';
  import useUserStore from '@/store/modules/user';

  import { AppRouteEnum } from '@/enums/routeEnum';

  import useLogin from './hooks/useLogin';
  import useAppStore from './store/modules/app';

  const router = useRouter();
  const userStore = useUserStore();
  const appStore = useAppStore();
  const { oAuthLogin } = useLogin();
  const licenseStore = useLicenseStore();

  onBeforeMount(async () => {
    const loginStatus = await userStore.isLogin();
    const isWXWork = navigator.userAgent.includes('wxwork');
    if (!loginStatus && isWXWork) {
      await oAuthLogin();
      router.replace({
        name: AppRouteEnum.WORKBENCH,
      });
      return;
    }
    router.replace({ name: AppRouteEnum.WORKBENCH });
    licenseStore.getValidateLicense();
    appStore.initModuleConfig();
  });
</script>

<style lang="less" scoped></style>
