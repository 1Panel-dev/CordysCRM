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
        <n-dropdown
          class="personal-dropdown"
          trigger="hover"
          placement="right-end"
          :options="personalMenuOptions"
          @select="personalMenuChange"
          @update-show="personalMenuUpdateShow"
        >
          <div
            class="personal-info-menu relative flex w-full cursor-pointer items-center gap-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[8px] hover:bg-[var(--primary-7)]"
            :class="personalMenuShow ? 'bg-[var(--primary-6)]' : ''"
          >
            <CrmPopConfirm
              v-model:show="showPopModal"
              :title="t('system.personal.addNewExport')"
              icon-type="primary"
              :content="t('system.personal.addNewExportPopContent')"
              :positive-text="t('common.gotIt')"
              negative-text=""
              trigger="manual"
              placement="right-end"
              @confirm="confirmHandler"
            >
              <span class="personal-export-pop"></span>
            </CrmPopConfirm>
            <CrmAvatar :size="collapsed ? 25 : 40" class="flex-shrink-0 transition-all" />
            <div v-if="!collapsed" class="one-line-text">
              <div class="one-line-text max-w-[110px]">{{ userStore.userInfo.name }}</div>
              <!-- <n-tag
                v-if="userStore.userInfo.id === 'admin' || (userStore.userInfo.roles[0] as any)?.name"
                :bordered="false"
                size="small"
                :color="{
                  color: 'var(--primary-6)',
                  textColor: 'var(--primary-8)',
                }"
                class="max-w-[110px]"
              >
                {{ userStore.userInfo.id === 'admin' ? t('common.admin') : (userStore.userInfo.roles[0] as any)?.name }}
              </n-tag> -->
            </div>
          </div>
        </n-dropdown>
        <n-divider />
        <div class="ml-[8px] w-full cursor-pointer px-[8px]" @click="() => appStore.setMenuCollapsed(!collapsed)">
          <CrmIcon :type="collapsed ? 'iconicon_menu_fold1' : 'iconicon_menu_unfold1'" :size="16" />
        </div>
      </div>
    </div>
  </n-layout-sider>
  <PersonalInfoDrawer v-model:visible="showPersonalInfo" :active-tab-value="personalTab" />
  <personalExportDrawer v-model:visible="showPersonalExport" />
</template>

<script setup lang="ts">
  import { RouteLocationNormalizedGeneric, useRouter } from 'vue-router';
  import { NDivider, NDropdown, NLayoutSider, NMenu, NScrollbar, NTooltip } from 'naive-ui';

  import { PersonalEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { mapTree } from '@lib/shared/method';
  import { listenerRouteChange } from '@lib/shared/method/route-listener';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmPopConfirm from '@/components/pure/crm-pop-confirm/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';
  import personalExportDrawer from '@/views/system/business/components/personalExportDrawer.vue';
  import PersonalInfoDrawer from '@/views/system/business/components/personalInfoDrawer.vue';

  import useMenuTree from '@/hooks/useMenuTree';
  import useUser from '@/hooks/useUser';
  import useVisit from '@/hooks/useVisit';
  import type { AppRouteRecordRaw } from '@/router/routes/types';
  import useAppStore from '@/store/modules/app';
  import useUserStore from '@/store/modules/user';
  import { getFirstRouterNameByCurrentRoute, hasAnyPermission } from '@/utils/permission';

  import { AppRouteEnum, ClueRouteEnum, CustomerRouteEnum, OpportunityRouteEnum } from '@/enums/routeEnum';

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
  const visitedKey = 'doNotShowPersonalExportAgain';
  const { addVisited, getIsVisited } = useVisit(visitedKey);

  watch(
    () => appStore.getMenuCollapsed,
    (value) => {
      collapsed.value = value;
    }
  );

  function renderIcon(type: string) {
    return () =>
      h(CrmIcon, {
        size: 18,
        type,
        class: 'text-[var(--text-n2)]',
      });
  }

  const hasExportPermission = computed(() =>
    hasAnyPermission(['CUSTOMER_MANAGEMENT:EXPORT', 'OPPORTUNITY_MANAGEMENT:EXPORT', 'CLUE_MANAGEMENT:EXPORT'])
  );

  const personalMenuOptions = computed(() => [
    {
      key: 'header',
      type: 'render',
      render: () =>
        h(
          NTooltip,
          {
            delay: 300,
          },
          {
            trigger: () =>
              h(
                'div',
                { class: 'personal-name one-line-text max-w-[110px]' },
                { default: () => userStore.userInfo.name }
              ),
            default: () => h('div', {}, { default: () => userStore.userInfo.name }),
          }
        ),
    },
    {
      key: 'header-divider',
      type: 'divider',
    },
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
    ...(hasExportPermission.value
      ? [
          {
            label: t('module.personal.myExport'),
            key: AppRouteEnum.PERSONAL_EXPORT,
            icon: renderIcon('iconicon_export'),
          },
        ]
      : []),
    {
      label: t('module.logout'),
      key: AppRouteEnum.LOGOUT,
      icon: renderIcon('iconicon_logout'),
    },
  ]);

  const showPopModal = ref(false);
  function confirmHandler() {
    addVisited();
    showPopModal.value = false;
  }

  let timer: any = null;
  function initExportPop() {
    if (!getIsVisited() && hasExportPermission.value) {
      showPopModal.value = true;

      if (timer) {
        clearTimeout(timer);
        timer = null;
      }
      timer = setTimeout(() => {
        confirmHandler();
        timer = null;
      }, 5000);
    }
  }

  const isRequiredExportRoute = (key: OpportunityRouteEnum | ClueRouteEnum | CustomerRouteEnum) =>
    [AppRouteEnum.CLUE_MANAGEMENT, AppRouteEnum.OPPORTUNITY, AppRouteEnum.CUSTOMER].includes(
      key as OpportunityRouteEnum | ClueRouteEnum | CustomerRouteEnum
    );

  async function menuChange(key: string, item: MenuOption) {
    const routeItem = item as unknown as AppRouteRecordRaw;
    const name = routeItem.meta?.hideChildrenInMenu ? getFirstRouterNameByCurrentRoute(routeItem.name as string) : key;
    await router.push({ name });
    if (isRequiredExportRoute(key as OpportunityRouteEnum | ClueRouteEnum | CustomerRouteEnum)) {
      initExportPop();
    }
  }

  const personalMenuShow = ref(false);
  function personalMenuUpdateShow(value: boolean) {
    personalMenuShow.value = value;
  }

  const showPersonalExport = ref(false);

  async function personalMenuChange(key: string) {
    personalMenuValue.value = key;
    if (key === AppRouteEnum.PERSONAL_INFO || key === AppRouteEnum.PERSONAL_PLAN) {
      if (key === AppRouteEnum.PERSONAL_INFO) {
        personalTab.value = PersonalEnum.INFO;
      } else {
        personalTab.value = PersonalEnum.MY_PLAN;
      }
      showPersonalInfo.value = true;
    } else if (key === AppRouteEnum.PERSONAL_EXPORT) {
      showPersonalExport.value = true;
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

    const routeName = router.currentRoute.value.matched[0]?.name as
      | OpportunityRouteEnum
      | ClueRouteEnum
      | CustomerRouteEnum;
    if (isRequiredExportRoute(routeName)) {
      initExportPop();
    }
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
    font-weight: 500;
    .n-scrollbar-content {
      @apply h-full;
    }
    .n-divider:not(.n-divider--vertical) {
      margin: 12px 0;
    }
  }
  .n-menu-item-content--selected,
  .n-menu-item-content--child-active {
    .n-icon {
      color: var(--n-item-text-color-active) !important;
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
    padding: 4px 8px;
    font-size: 14px;
    font-weight: 500;
    color: var(--text-n1);
    line-height: 22px;
  }
  .personal-dropdown {
    .n-dropdown-option-body {
      color: var(--text-n2) !important;
    }
  }
</style>

<style lang="less" scoped>
  .personal-export-pop {
    position: absolute;
    right: 0;
    bottom: 0;
  }
</style>
