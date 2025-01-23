<template>
  <router-view v-slot="{ Component, route }">
    <transition name="fade" mode="out-in" appear>
      <!-- transition内必须有且只有一个根元素，不然会导致二级路由的组件无法渲染 -->
      <n-scrollbar content-style="min-height: 500px;height: 100%;width: 100%">
        <div class="page-content">
          <keep-alive :include="[]">
            <component :is="Component" :key="route.name" />
          </keep-alive>
        </div>
      </n-scrollbar>
    </transition>
  </router-view>
</template>

<script lang="ts" setup>
  import { NScrollbar } from 'naive-ui';
</script>

<style lang="less">
  .page-content {
    @apply h-full w-full;

    padding: 16px;
    background-color: var(--text-n9);
    .n-scrollbar-content {
      @apply h-full;
    }
  }
</style>
