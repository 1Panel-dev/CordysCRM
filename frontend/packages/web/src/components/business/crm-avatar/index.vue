<template>
  <n-avatar v-if="userStore.userInfo.avatar" round :size="props.size" :src="userStore.userInfo.avatar" />
  <n-avatar v-else round :size="props.size">{{ userStore.userInfo.name.substring(0, 4) }}</n-avatar>
</template>

<script setup lang="ts">
  import { NAvatar } from 'naive-ui';

  import useUserStore from '@/store/modules/user';

  const userStore = useUserStore();
  const props = withDefaults(
    defineProps<{
      size?: number;
    }>(),
    {
      size: 40,
    }
  );
</script>

<style lang="less" scoped></style>
