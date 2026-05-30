<template>
  <NScrollbar
    :style="{
      overflow: 'auto',
      width: props.isPreview ? '100%' : '100vw',
      height: props.isPreview ? '100%' : '100vh',
    }"
  >
    <div class="login-page" :style="props.isPreview ? '' : 'min-width: 1200px;'">
      <banner />
      <n-spin :show="appStore.getLoginLoadingStatus" class="flex-1">
        <loginForm :is-preview="props.isPreview" />
      </n-spin>
    </div>
  </NScrollbar>
</template>

<script lang="ts" setup>
  import { NScrollbar, NSpin } from 'naive-ui';

  import banner from './components/banner.vue';
  import loginForm from './components/login-form.vue';

  import useAppStore from '@/store/modules/app';

  const appStore = useAppStore();

  const props = defineProps<{
    isPreview?: boolean;
  }>();
</script>

<style lang="less" scoped>
  .login-page {
    @apply flex items-center;
  }
</style>
