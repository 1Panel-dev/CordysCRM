<template>
  <svg class="svg-icon" aria-hidden="true">
    <use :xlink:href="`#icon-${props.type}`" :fill="color" />
  </svg>
</template>

<script setup lang="ts">
  interface Props {
    type: string;
    color?: string;
    width?: string;
    height?: string;
  }
  const props = withDefaults(defineProps<Props>(), {
    type: '',
    width: '16px',
    height: '16px',
  });
</script>

<style scoped lang="less">
  .svg-icon {
    @apply relative;

    width: v-bind(width);
    height: v-bind(height);
  }
</style>
