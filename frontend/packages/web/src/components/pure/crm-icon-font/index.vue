<template>
  <n-icon :size="props.size" :color="props.color">
    <svg>
      <use :xlink:href="`#${props.type}`"></use>
    </svg>
  </n-icon>
</template>

<script lang="ts" setup>
  import '@/assets/icon-font/iconfont';

  const props = defineProps<{
    size?: number;
    type: string;
    color?: string;
  }>();
</script>
