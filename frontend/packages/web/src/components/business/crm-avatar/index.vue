<template>
  <n-avatar v-if="userStore.userInfo.avatar" round :size="40" :src="userStore.userInfo.avatar" />
  <n-avatar v-else round :size="40">{{ userStore.userInfo.name.substring(0, 4) }}</n-avatar>
</template>

<script setup lang="ts">
  import { NAvatar } from 'naive-ui';

  import useUserStore from '@/store/modules/user';

  const userStore = useUserStore();
</script>

<style lang="less" scoped></style>
