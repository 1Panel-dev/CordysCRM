<template>
  <div class="banner-wrap">
    <img class="img" :style="props.isPreview ? 'height: 100%;' : 'height: 100vh'" :src="innerBanner" />
    <a href="https://cordys.cn/" target="_blank" class="logo-link">
      <CrmSvg name="login-logo" class="logo" />
    </a>
  </div>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';

  import CrmSvg from '@/components/pure/crm-svg/index.vue';

  import { defaultLoginImage } from '@/config/business';
  import { useAppStore } from '@/store';

  const props = defineProps<{
    isPreview?: boolean;
    banner?: string;
  }>();

  const appStore = useAppStore();

  const innerBanner = computed(() =>
    props.banner ? props.banner : appStore.pageConfig.loginImage[0]?.url ?? defaultLoginImage
  );
</script>

<style lang="less" scoped>
  .banner-wrap {
    @apply relative;
    .img {
      width: 100%;
      object-fit: cover;
    }
    .logo-link {
      @apply absolute;

      top: 5%;
      left: 5%;
      width: 37%;
      height: 7%;
      .logo {
        width: 100%;
        height: 100%;
      }
    }
  }
</style>
