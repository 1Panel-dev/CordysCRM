<template>
  <div class="page">
    <router-view v-slot="{ Component, route }">
      <transition name="fade" mode="out-in" appear>
        <!-- transition内必须有且只有一个根元素，不然会导致二级路由的组件无法渲染 -->
        <div class="page-content">
          <Suspense>
            <keep-alive :include="[]">
              <component :is="Component" :key="route.name" />
            </keep-alive>
          </Suspense>
        </div>
      </transition>
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

  import { AppRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();
  const router = useRouter();

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

<style lang="less" scoped>
  .page {
    @apply flex flex-col;

    height: 100vh;
    background-color: var(--text-n9);
    .page-content {
      @apply flex-1 overflow-hidden;
    }
    .page-bottom-tabbar {
      gap: 8px;
      :deep(.van-tabbar-item__icon) {
        @apply mb-0;
      }
    }
  }
</style>
