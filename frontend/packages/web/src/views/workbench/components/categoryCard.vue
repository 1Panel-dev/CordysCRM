<template>
  <div class="flex bg-[var(--text-n9)]">
    <div
      :class="`category-card-block ${
        currentLocale === 'en-US' ? ' flex-col justify-center' : 'items-center justify-start'
      }`"
      :style="cardStyle"
    >
      <CrmIcon :type="props.icon" :size="24" />
      <div class="text-[16px] font-semibold">{{ props.title }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useMessage } from 'naive-ui';

  import useLocale from '@lib/shared/locale/useLocale';

  const Message = useMessage();
  const { currentLocale } = useLocale(Message.loading);
  const props = defineProps<{
    width: string;
    bgColor: string;
    icon: string;
    title: string;
  }>();

  const cardStyle = computed(() => ({
    width: props.width,
    background: props.bgColor,
  }));
</script>

<style scoped lang="less">
  .category-card-block {
    padding: 16px;
    height: 102px;
    border-radius: 4px 0 0 4px;
    color: var(--text-n10);
    clip-path: polygon(0 0, 100% 0, 87% 99%, 0% 100%);
    @apply flex gap-2;
  }
</style>
