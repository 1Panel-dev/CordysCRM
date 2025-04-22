<template>
  <Suspense>
    <RouterView />
  </Suspense>
</template>

<script lang="ts" setup>
  import { useRouter } from 'vue-router';

  import useUserStore from '@/store/modules/user';

  import { AppRouteEnum } from '@/enums/routeEnum';

  import useLogin from './hooks/useLogin';

  const router = useRouter();
  const userStore = useUserStore();
  const { oAuthLogin } = useLogin();

  onBeforeMount(async () => {
    const loginStatus = await userStore.checkIsLogin();
    if (loginStatus) {
      router.push({
        name: AppRouteEnum.WORKBENCH,
      });
    } else if (import.meta.env.DEV) {
      router.push({
        name: AppRouteEnum.WORKBENCH,
      });
    } else {
      await oAuthLogin();
    }
  });
</script>

<style lang="less" scoped></style>
