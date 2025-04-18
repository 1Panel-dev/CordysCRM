<template>
  <Suspense>
    <RouterView />
  </Suspense>
</template>

<script lang="ts" setup>
  import { useRouter } from 'vue-router';

  import { AppRouteEnum } from './enums/routeEnum';
  import useUserStore from './store/modules/user';

  const router = useRouter();
  const userStore = useUserStore();

  onBeforeMount(async () => {
    const loginStatus = await userStore.checkIsLogin();
    if (loginStatus) {
      router.push({
        name: AppRouteEnum.WORKBENCH,
      });
    } else {
      router.push({
        name: 'login',
      });
    }
  });
</script>

<style lang="less" scoped></style>
