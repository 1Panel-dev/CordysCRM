<template>
  <van-nav-bar :title="props.title" class="crm-page-header" fixed>
    <template #left>
      <CrmIcon v-if="!props.hideBack" name="iconicon_chevron_left" width="24px" height="24px" @click="handleBack" />
    </template>
  </van-nav-bar>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import useAppStore from '@/store/modules/app';

  const props = defineProps<{
    title: string;
    hideBack?: boolean;
    backRouteName?: string;
  }>();

  const router = useRouter();
  const appStore = useAppStore();

  function handleBack() {
    appStore.setManualBack(true);
    if (props.backRouteName) {
      router.replace({ name: props.backRouteName });
    } else {
      router.back();
    }
  }
</script>

<style lang="less" scoped>
  .crm-page-header {
    height: 48px;
    :deep(.van-nav-bar__left) {
      padding: 0 12px;
    }
  }
</style>
