<template>
  <n-layout class="default-layout">
    <LayoutHeader v-if="!route.name?.toString().includes(DashboardRouteEnum.DASHBOARD)" />
    <n-layout class="flex-1" has-sider>
      <LayoutSider />
      <PageContent />
    </n-layout>
  </n-layout>
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import { NLayout } from 'naive-ui';

  import LayoutHeader from './components/layout-header.vue';
  import LayoutSider from './components/layout-sider.vue';
  import PageContent from './page-content.vue';

  import { DashboardRouteEnum } from '@/enums/routeEnum';

  const route = useRoute();
</script>

<style lang="less">
  .default-layout {
    @apply flex;

    height: 100vh;
    .n-layout-scroll-container {
      @apply flex w-full flex-col overflow-hidden;
    }
  }
</style>
