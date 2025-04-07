<template>
  <n-menu
    v-model:value="activeMenu"
    class="crm-top-menu"
    mode="horizontal"
    :options="topMenuList"
    :node-props="getNodeProps"
    responsive
    @update:value="handleSelected"
  />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { RouteRecordName, RouteRecordRaw, useRouter } from 'vue-router';
  import { MenuGroupOption, MenuOption, NMenu } from 'naive-ui';
  import { cloneDeep, debounce } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { listenerRouteChange } from '@lib/shared/method/route-listener';

  import usePermission from '@/hooks/usePermission';
  import appClientMenus from '@/router/app-menus';
  import useAppStore from '@/store/modules/app';

  const { t } = useI18n();
  const permission = usePermission();

  const copyRouters = cloneDeep(appClientMenus) as RouteRecordRaw[];

  const appStore = useAppStore();

  const router = useRouter();

  const topMenuList = computed<MenuOption[]>(() => {
    return appStore.getTopMenus.map((e: any) => {
      return {
        key: e.name,
        label: t(e?.meta?.locale ?? ''),
      };
    });
  });

  const activeMenu: Ref<string | null> = ref('');

  function getNodeProps(option: MenuOption | MenuGroupOption) {
    return {
      class: `${option.key === activeMenu.value ? 'crm-top-menu-selected-item' : ''}`,
    };
  }

  function checkAuthMenu() {
    const topMenus = appStore.getTopMenus;
    appStore.setTopMenus(topMenus);
  }

  watch(
    () => appStore.getCurrentTopMenu?.name,
    (val) => {
      checkAuthMenu();
      activeMenu.value = val as string;
    },
    {
      immediate: true,
    }
  );

  function setCurrentTopMenu(key: string) {
    // 先判断全等，避免同级路由出现命名包含情况
    const secParentFullSame = appStore.topMenus.find((route: RouteRecordRaw) => {
      return key === route?.name;
    });

    // 非全等的情况下，一定是父子路由包含关系
    const secParentLike = appStore.topMenus.find((route: RouteRecordRaw) => {
      return key.includes(route?.name as string);
    });

    if (secParentFullSame) {
      appStore.setCurrentTopMenu(secParentFullSame);
    } else if (secParentLike) {
      appStore.setCurrentTopMenu(secParentLike);
    }
  }

  const handleSelected = debounce((route: RouteRecordName | undefined) => {
    router.push({ name: route });
  }, 150);

  /**
   * 监听路由变化，存储打开的顶部菜单
   */
  listenerRouteChange((newRoute) => {
    const { name } = newRoute;
    for (let i = 0; i < copyRouters.length; i++) {
      const firstRoute = copyRouters[i];
      // 权限校验通过
      if (permission.accessRouter(firstRoute)) {
        if (name && firstRoute?.name && (name as string).includes(firstRoute.name as string)) {
          let currentParent = firstRoute?.children?.some((item) => item.meta?.isTopMenu)
            ? (firstRoute as RouteRecordRaw)
            : undefined;

          if (!currentParent) {
            // 二级菜单非顶部菜单，则判断三级菜单是否有顶部菜单
            currentParent = firstRoute?.children?.find(
              (item) => name && item?.name && (name as string).includes(item.name as string)
            );
          }
          const filterMenuTopRouter = currentParent?.children?.filter((item: any) => item.meta?.isTopMenu) || [];
          appStore.setTopMenus(filterMenuTopRouter);
          setCurrentTopMenu(name as string);
          return;
        }
      }
    }
    appStore.setTopMenus([]);
    setCurrentTopMenu('');
  }, true);
</script>

<style lang="less">
  .crm-top-menu {
    &.n-menu {
      .n-menu-item {
        height: 32px !important;
        &.crm-top-menu-selected-item {
          border-radius: --border-radius-small;
          background: var(--primary-7);
        }
        .n-menu-item-content {
          padding: 0 16px;
        }
      }
    }
  }
</style>
