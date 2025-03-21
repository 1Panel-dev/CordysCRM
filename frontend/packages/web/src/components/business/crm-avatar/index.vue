<template>
  <n-avatar v-if="innerAvatar" round :size="props.size" :src="innerAvatar" />
  <n-avatar v-else round :size="props.size">
    {{ props.word?.substring(0, 4) || userStore.userInfo.name.substring(0, 4) }}
  </n-avatar>
</template>

<script setup lang="ts">
  import { NAvatar } from 'naive-ui';

  import useUserStore from '@/store/modules/user';

  const userStore = useUserStore();
  const props = withDefaults(
    defineProps<{
      avatar?: 'default' | 'word' | string;
      size?: number;
      word?: string; // 用于显示文字头像
    }>(),
    {
      size: 40,
      isUser: true,
    }
  );

  const innerAvatar = computed(() => {
    return props.avatar || userStore.userInfo.avatar;
  });
</script>

<style lang="less" scoped></style>
