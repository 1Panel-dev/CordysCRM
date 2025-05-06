<template>
  <n-layout-sider
    v-model:collapsed="collapsed"
    collapse-mode="width"
    :collapsed-width="56"
    :width="180"
    :native-scrollbar="false"
    class="crm-layout-sider"
    @update-collapsed="appStore.setMenuCollapsed"
  >
    <div class="flex h-full flex-col justify-between">
      <n-scrollbar content-style="min-height: 500px;height: 100%;width: 100%">
        <n-menu
          v-model:value="menuValue"
          :root-indent="24"
          :indent="appStore.getMenuIconStatus ? 38 : 8"
          :collapsed-width="appStore.collapsedWidth"
          :icon-size="18"
          :collapsed-icon-size="28"
          :options="menuOptions"
          @update-value="menuChange"
        />
      </n-scrollbar>
      <div class="flex flex-col items-start p-[8px]">
        <n-popover trigger="hover" raw :show-arrow="false" placement="right-end" class="personal-popover">
          <template #trigger>
            <div class="flex w-full items-center gap-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)]">
              <CrmAvatar class="flex-shrink-0" />
              <div v-if="!collapsed" class="one-line-text">
                <div class="one-line-text">{{ userStore.userInfo.name }}</div>
                <n-tag
                  v-if="userStore.userInfo.id === 'admin' || (userStore.userInfo.roles[0] as any)?.name"
                  :bordered="false"
                  size="small"
                  :color="{
                    color: 'var(--primary-6)',
                    textColor: 'var(--primary-8)',
                  }"
                >
                  {{
                    userStore.userInfo.id === 'admin' ? t('common.admin') : (userStore.userInfo.roles[0] as any)?.name
                  }}
                </n-tag>
              </div>
            </div>
          </template>
          <template #header>
            <n-text strong depth="1">
              <n-tooltip trigger="hover" :delay="300">
                <template #trigger>
                  <div class="personal-name one-line-text max-w-[300px]">{{ userStore.userInfo.name }}</div>
                </template>
                {{ userStore.userInfo.name }}
              </n-tooltip>
            </n-text>
          </template>
          <div class="personal-menu flex h-full flex-col">
            <n-menu
              v-model:value="personalMenuValue"
              :icon-size="16"
              :collapsed-icon-size="16"
              :collapsed="false"
              :show="true"
              :duration="50000"
              keep-alive-on-hover
              :options="personalMenuOptions"
              @update-value="personalMenuChange"
            />
          </div>
        </n-popover>
        <n-divider />
        <div class="ml-[8px] w-full cursor-pointer px-[8px]" @click="() => appStore.setMenuCollapsed(!collapsed)">
          <CrmIcon :type="collapsed ? 'iconicon_menu_fold1' : 'iconicon_menu_unfold1'" :size="16" />
        </div>
      </div>
    </div>
  </n-layout-sider>
  <PersonalInfoDrawer v-model:visible="showPersonalInfo" :active-tab-value="personalTab" />
</template>

<script setup lang="ts">
  import { RouteLocationNormalizedGeneric, useRouter } from 'vue-router';
  import { NDivider, NLayoutSider, NMenu, NPopover, NScrollbar, NTag, NText, NTooltip } from 'naive-ui';

  import { PersonalEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { mapTree } from '@lib/shared/method';
  import { listenerRouteChange } from '@lib/shared/method/route-listener';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';
  import PersonalInfoDrawer from '@/views/system/business/components/personalInfoDrawer.vue';

  import useMenuTree from '@/hooks/useMenuTree';
  import useUser from '@/hooks/useUser';
  import type { AppRouteRecordRaw } from '@/router/routes/types';
  import useAppStore from '@/store/modules/app';
  import useUserStore from '@/store/modules/user';
  import { getFirstRouterNameByCurrentRoute } from '@/utils/permission';

  import { AppRouteEnum } from '@/enums/routeEnum';

  import { MenuOption } from 'naive-ui/es/menu/src/interface';

  const { logout } = useUser();

  const { t } = useI18n();
  const appStore = useAppStore();
  const userStore = useUserStore();
  const router = useRouter();
  const collapsed = ref(appStore.getMenuCollapsed);
  const menuValue = ref<string>(AppRouteEnum.SYSTEM_ORG);
  const personalMenuValue = ref<string>('');
  const showPersonalInfo = ref<boolean>(false);
  const personalTab = ref(PersonalEnum.INFO);

  watch(
    () => appStore.getMenuCollapsed,
    (value) => {
      collapsed.value = value;
    }
  );

  function renderIcon(type: string) {
    return () =>
      h(CrmIcon, {
        size: 20,
        type,
      });
  }

  const personalMenuOptions = [
    {
      label: t('module.personal.info'),
      key: AppRouteEnum.PERSONAL_INFO,
      icon: renderIcon('iconicon_set_up'),
    },
    {
      label: t('module.personal.plan'),
      key: AppRouteEnum.PERSONAL_PLAN,
      icon: renderIcon('iconicon_calendar1'),
    },
    {
      label: t('module.logout'),
      key: AppRouteEnum.LOGOUT,
      icon: renderIcon('iconicon_logout'),
    },
  ];

  function menuChange(key: string, item: MenuOption) {
    const routeItem = item as unknown as AppRouteRecordRaw;
    const name = routeItem.meta?.hideChildrenInMenu ? getFirstRouterNameByCurrentRoute(routeItem.name as string) : key;
    router.push({ name });
  }

  async function personalMenuChange(key: string) {
    personalMenuValue.value = key;
    if (key === AppRouteEnum.PERSONAL_INFO || key === AppRouteEnum.PERSONAL_PLAN) {
      if (key === AppRouteEnum.PERSONAL_INFO) {
        personalTab.value = PersonalEnum.INFO;
      } else {
        personalTab.value = PersonalEnum.MY_PLAN;
      }
      showPersonalInfo.value = true;
    } else {
      await userStore.logout();
      logout();
    }
  }

  const { menuTree } = useMenuTree();

  function getMenuIcon(e: AppRouteRecordRaw) {
    if (appStore.getMenuIconStatus) {
      return e?.meta?.icon ? renderIcon(e.meta.icon) : null;
    }

    return collapsed.value && e?.meta?.collapsedLocale
      ? () => h('div', { class: `flex flex-nowrap text-[14px]` }, t(e?.meta?.collapsedLocale ?? ''))
      : null;
  }

  const menuOptions = computed<MenuOption[]>(() => {
    return mapTree(menuTree.value, (e: any) => {
      const menuChildren = mapTree(e.children);
      return e.meta.isTopMenu
        ? null
        : {
            ...e,
            label: t(e?.meta?.locale ?? ''),
            key: e.name,
            children: menuChildren.length ? menuChildren : undefined,
            icon: getMenuIcon(e),
          };
    }) as unknown as MenuOption[];
  });

  function setMenuValue(route: RouteLocationNormalizedGeneric) {
    if (route.meta.isTopMenu || route.meta.hideChildrenInMenu) {
      menuValue.value = route.matched[0].name as (typeof AppRouteEnum)[keyof typeof AppRouteEnum];
    } else {
      menuValue.value = route.name as (typeof AppRouteEnum)[keyof typeof AppRouteEnum];
    }
  }

  onBeforeMount(() => {
    setMenuValue(router.currentRoute.value);
  });

  /**
   * 监听路由变化，切换菜单选中
   */
  listenerRouteChange((newRoute) => {
    setMenuValue(newRoute);
  }, true);

  watch(
    () => appStore.getRestoreMenuTimeStamp,
    (value) => {
      if (value) {
        setMenuValue(router.currentRoute.value);
      }
    }
  );

  watch(
    () => appStore.orgId,
    (orgId) => {
      if (orgId) {
        appStore.initModuleConfig();
      }
    },
    { immediate: true }
  );
</script>

<style lang="less">
  .crm-layout-sider {
    .n-scrollbar-content {
      @apply h-full;
    }
    .n-divider:not(.n-divider--vertical) {
      margin: 12px 0;
    }
  }
  .personal-menu {
    min-width: 120px;
    .n-menu .n-menu-item {
      align-items: flex-start;
      margin-top: 0;
      padding: 4px 12px;
      height: 30px;
      border-radius: 4px;
    }
    .n-menu .n-menu-item:hover {
      transition: 0.7s;

      --n-item-color-hover: var(--primary-7);
    }
    .n-menu-item-content {
      padding-left: 0 !important;
      .n-menu-item-content-header {
        color: var(--text-n2);
      }
      .n-menu-item-content__icon {
        width: 16px;
        height: 16px;
        color: var(--text-n2);
      }
    }
    .n-menu-item-content::before {
      top: -4px;
      right: -8px;
      bottom: -4px;
      left: -8px;
    }
  }
  .personal-popover {
    min-width: 120px;
    background-color: var(--text-n10);
    .n-popover__content {
      padding: 0 !important;
    }
  }
  .personal-name {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-n1);
    line-height: 22px;
  }
</style>
