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

  const router = useRouter();
  const userStore = useUserStore();
  const { oAuthLogin } = useLogin();
  const licenseStore = useLicenseStore();

  onBeforeMount(async () => {
    const loginStatus = await userStore.isLogin();
    if (loginStatus) {
      router.replace({
        name: AppRouteEnum.WORKBENCH,
      });
    } else {
      await oAuthLogin();
    }
    licenseStore.getValidateLicense();
  });
</script>

<style lang="less" scoped></style>
