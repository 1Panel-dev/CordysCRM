<template>
  <div class="page">
    <router-view v-slot="{ Component, route }">
      <!-- transition内必须有且只有一个根元素，不然会导致二级路由的组件无法渲染 -->
      <div class="page-content">
        <transition :name="transitionName">
          <keep-alive :include="Array.from(appStore.getCacheRoutes)">
            <component :is="Component" :key="route.name" />
          </keep-alive>
        </transition>
      </div>
    </router-view>
    <van-tabbar
      v-if="isModuleRouteIndex"
      v-model="active"
      :fixed="false"
      safe-area-inset-bottom
      class="page-bottom-tabbar !py-[8px]"
      @change="handleTabbarChange"
    >
      <van-tabbar-item
        v-for="menu of menuList"
        :key="menu.name"
        :name="menu.name"
        class="rounded-full"
        :class="active === menu.name ? '!bg-[var(--primary-7)]' : ''"
      >
        <template #icon>
          <CrmIcon
            :name="menu.icon"
            width="18px"
            height="16px"
            :color="active === menu.name ? 'var(--van-tabbar-item-active-color)' : ''"
          />
        </template>
        <div class="text-[10px]">{{ t(menu.text) }}</div>
      </van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { listenerRouteChange } from '@lib/shared/method/route-listener';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import useAppStore from '@/store/modules/app';

  import { AppRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();
  const router = useRouter();
  const appStore = useAppStore();

  function isIOS(): boolean {
    return /iPhone|iPad|iPod/i.test(navigator.userAgent);
  }
  const transitionName = ref('slide-left');
  // 路由深度判断，决定是前进还是返回
  let isForward = true;
  router.beforeEach((to, from, next) => {
    const toDepth = to.meta.depth || 0;
    const fromDepth = from.meta.depth || 0;
    isForward = toDepth >= fromDepth;
    transitionName.value = isForward ? 'slide-left' : 'slide-right';
    // 如果是IOS手势后退，禁用动画
    if (!isForward && isIOS() && !appStore.getManualBack) {
      transitionName.value = '';
    } else if (toDepth === 1 && fromDepth === 1) {
      // 如果是一级页面之间切换，则无需动画
      transitionName.value = '';
    }
    // 处理路由缓存
    if ((toDepth === 1 && fromDepth === 1) || (from.meta.isCache && toDepth < fromDepth)) {
      // 一级页面切换，移除页面缓存；从更深的页面返回上一层级页面，清理当前页面缓存
      appStore.removeCacheRoute(from.name as string);
    } else if (from.meta.isCache && isForward) {
      // 离开页面时，如果当前页面配置了缓存，且是前进，则将当前路由添加到缓存列表
      appStore.addCacheRoute(from.name as string);
    }
    next();
  });
  router.afterEach(() => {
    appStore.setManualBack(false);
  });
  const active = ref<string>(AppRouteEnum.WORKBENCH_INDEX);
  const menuList = [
    {
      name: AppRouteEnum.WORKBENCH,
      icon: 'iconicon_home',
      text: t('menu.workbench'),
    },
    {
      name: AppRouteEnum.CUSTOMER,
      icon: 'iconicon_customer',
      text: t('menu.customer'),
    },
    {
      name: AppRouteEnum.CLUE,
      icon: 'iconicon_clue',
      text: t('menu.clue'),
    },
    {
      name: AppRouteEnum.OPPORTUNITY,
      icon: 'iconicon_business_opportunity',
      text: t('menu.opportunity'),
    },
    {
      name: AppRouteEnum.MINE,
      icon: 'iconicon_user_circle',
      text: t('menu.mine'),
    },
  ];

  function handleTabbarChange(name: string) {
    router.replace({ name });
  }

  const isModuleRouteIndex = computed(() => router.currentRoute.value.name?.toString().includes('Index'));

  /**
   * 监听路由变化，切换菜单选中
   */
  listenerRouteChange((newRoute) => {
    const { name } = newRoute;
    menuList.forEach((item) => {
      if (name?.toString().includes(item.name)) {
        active.value = item.name;
      }
    });
  }, true);
</script>

<style lang="less">
  .slide-left-enter-active,
  .slide-left-leave-active,
  .slide-right-enter-active,
  .slide-right-leave-active {
    position: absolute !important;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 1;
    background-color: white;
    transition: all 0.3s;
  }
  .slide-left-enter-from,
  .slide-right-leave-to {
    opacity: 1;
    transform: translateX(100%);
  }
  .slide-right-enter-from,
  .slide-left-leave-to {
    opacity: 1;
    transform: translateX(-100%);
  }
  .slide-left-leave-to,
  .slide-right-leave-to {
    opacity: 0.3;
  }

  /* 禁用动画时 */
  .transition-none-enter-active,
  .transition-none-leave-active {
    transition: none;
  }
</style>

<style lang="less" scoped>
  .page {
    @apply flex flex-col;

    height: 100vh;
    background-color: var(--text-n9);
    .page-content {
      @apply relative flex-1 overflow-hidden;
    }
    .page-bottom-tabbar {
      gap: 8px;
      :deep(.van-tabbar-item__icon) {
        @apply mb-0;
      }
    }
  }
</style>
