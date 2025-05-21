<template>
  <ColorPicker v-model:pure-color="innerPureColor" :z-index="1" picker-type="chrome" is-widget round-history />
</template>

<script setup lang="ts">
  import 'vue3-colorpicker/style.css';
  import { ColorPicker } from 'vue3-colorpicker';

  const innerPureColor = defineModel<string>('pureColor', {
    default: '#CF00FF',
  });
</script>

<style lang="less">
  .color-cube {
    overflow: hidden;
    border-radius: var(--border-radius-small);
  }
  .vc-transparent {
    background-image: none !important;
  }
  .color-list {
    width: 100% !important;
    .color-item {
      width: 24px !important;
      height: 24px !important;
    }
  }
  .color-item:not(:last-child) {
    margin-right: 2px !important;
  }
</style>
