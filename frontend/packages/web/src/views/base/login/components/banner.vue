<template>
  <div class="banner-wrap">
    <img class="img" :style="props.isPreview ? 'height: 100%;' : 'height: 100vh'" :src="innerBanner" />
    <a href="https://cordys.cn/" target="_blank" class="logo-link"></a>
  </div>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';

  // import { GetLoginImageUrl } from '@/api/requrls/setting/config';

  const props = defineProps<{
    isPreview?: boolean;
    banner?: string;
  }>();

  const innerBanner = computed(() => {
    return props.banner || '/images/login-banner.png';
  });
</script>

<style lang="less" scoped>
  .banner-wrap {
    .img {
      width: 100%;
      object-fit: cover;
    }
    .logo-link {
      @apply absolute;

      top: 40px;
      left: 30px;
      width: 400px;
      height: 100px;
    }
  }
</style>
