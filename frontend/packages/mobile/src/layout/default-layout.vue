<template>
  <div class="page">
    <router-view v-slot="{ Component, route }">
      <transition name="fade" mode="out-in" appear>
        <!-- transition内必须有且只有一个根元素，不然会导致二级路由的组件无法渲染 -->
        <div class="page-content">
          <keep-alive :include="[]">
            <component :is="Component" :key="route.name" />
          </keep-alive>
        </div>
      </transition>
    </router-view>
    <van-tabbar v-model="active" :fixed="false" class="!py-[8px]">
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
            width="20px"
            height="20px"
            :color="active === menu.name ? 'var(--van-tabbar-item-active-color)' : ''"
          />
        </template>
        {{ t(menu.text) }}
      </van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { useI18n } from '@cordys/web/src/hooks/useI18n';

  const { t } = useI18n();

  const active = ref('workbench');
  const menuList = [
    {
      name: 'workbench',
      icon: 'iconicon_home',
      text: t('menu.workbench'),
    },
    {
      name: 'customer',
      icon: 'iconicon_customer',
      text: t('menu.customer'),
    },
    {
      name: 'clue',
      icon: 'iconicon_clue',
      text: t('menu.clue'),
    },
    {
      name: 'opportunity',
      icon: 'iconicon_business_opportunity',
      text: t('menu.opportunity'),
    },
    {
      name: 'mine',
      icon: 'iconicon_user_circle',
      text: t('menu.mine'),
    },
  ];
</script>

<style lang="less" scoped>
  .page {
    @apply flex flex-col;

    height: 100vh;
    background-color: var(--text-n9);
    .page-content {
      @apply flex-1 overflow-hidden;
    }
  }
</style>
