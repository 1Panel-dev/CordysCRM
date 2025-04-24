<template>
  <van-image
    round
    :width="props.size"
    :height="props.size"
    :src="props.isWord ? 'no-res' : userStore.userInfo?.avatar || 'no-res'"
  >
    <template #error>
      <div
        v-if="avatarText?.length"
        :style="{
          fontSize: `${fontSize}px`,
        }"
      >
        {{ avatarText?.substring(0, 1) }}
      </div>
      <CrmIcon
        v-else
        name="iconicon_user"
        :width="`${defaultUserAvatarSize}px`"
        :height="`${defaultUserAvatarSize}px`"
        color="var(--text-n2)"
      />
    </template>
  </van-image>
</template>

<script setup lang="ts">
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import useUserStore from '@/store/modules/user';

  const props = withDefaults(
    defineProps<{
      isWord?: boolean;
      size?: number;
      text?: string;
    }>(),
    {
      isWord: true,
      size: 40,
    }
  );

  const userStore = useUserStore();

  const fontSize = computed(() => Math.floor(props.size * 0.42)); // 比例调节
  const defaultUserAvatarSize = computed(() => Math.floor(props.size * 0.5)); // 比例调节

  const avatarText = computed(() => (props.isWord ? props.text : userStore.userInfo?.name));
</script>

<style scoped></style>
