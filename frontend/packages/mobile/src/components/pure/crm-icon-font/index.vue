<script setup lang="ts">
  import { computed } from 'vue';

  interface Props {
    name: string;
    color?: string;
    width?: string;
    height?: string;
    content?: number | string;
    dot?: boolean;
  }
  const props = withDefaults(defineProps<Props>(), {
    name: '',
    color: '#000',
    width: '1rem',
    height: '1rem',
    dot: false,
  });
  const iconName = computed(() => `#${props.name}`);
</script>

<template>
  <van-badge :content="props.content" :dot="props.dot">
    <svg class="c-icon" aria-hidden="true">
      <use :xlink:href="iconName" :fill="color" />
    </svg>
  </van-badge>
</template>

<style scoped lang="less">
  .c-icon {
    @apply relative;

    width: v-bind(width);
    height: v-bind(height);
    color: transparent; // 解决部分图标线条填充色问题
  }
</style>
