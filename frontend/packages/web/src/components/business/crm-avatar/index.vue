<template>
  <n-avatar v-if="innerAvatar" round :size="props.size" :src="innerAvatar" />
  <n-avatar v-else round :size="props.size">
    <div class="flex items-center justify-center font-medium">
      <div
        v-if="avatarText?.length"
        :style="{
          fontSize: `${avatarTextSize}px`,
        }"
      >
        {{ avatarText?.substring(0, 1) }}
      </div>
      <CrmIcon v-else type="iconicon_user" :size="defaultUserAvatarSize" class="text-[var(--text-n10)]" />
    </div>
  </n-avatar>
</template>

<script setup lang="ts">
  import { NAvatar } from 'naive-ui';

  import useUserStore from '@/store/modules/user';

  const userStore = useUserStore();
  const props = withDefaults(
    defineProps<{
      avatar?: string;
      size?: number;
      word?: string; // 用于显示文字头像
      isUser?: boolean;
    }>(),
    {
      size: 40,
      isUser: true, // 当前登录用户还是其他用户
    }
  );

  const innerAvatar = computed(() => (props.isUser ? userStore.userInfo.avatar : props.avatar));

  const avatarText = computed(() => (props.isUser ? userStore.userInfo.name : props?.word));

  const avatarTextSize = computed(() => Math.floor(props.size * 0.52));

  const defaultUserAvatarSize = computed(() => Math.floor(props.size * 0.6));
</script>

<style lang="less" scoped></style>
