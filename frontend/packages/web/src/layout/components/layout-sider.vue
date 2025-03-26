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
      <n-menu
        v-model:value="menuValue"
        :root-indent="24"
        :indent="28"
        :collapsed-width="appStore.collapsedWidth"
        :icon-size="18"
        :collapsed-icon-size="18"
        :options="menuOptions"
        @update-value="menuChange"
      />
      <div class="flex flex-col items-start p-[8px]">
        <n-popover trigger="hover" raw :show-arrow="false" placement="right-end" class="personal-popover">
          <template #trigger>
            <CrmAvatar v-if="collapsed"> </CrmAvatar>
            <div
              v-else
              class="flex w-full items-center gap-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[8px]"
            >
              <CrmAvatar />
              <div>
                <div>{{ userStore.userInfo.name }}</div>
                <n-tag
                  :bordered="false"
                  size="small"
                  :color="{
                    color: 'var(--primary-6)',
                    textColor: 'var(--primary-8)',
                  }"
                >
                  {{ userStore.userInfo.id === 'admin' ? t('common.admin') : (userStore.userInfo.roles[0] as any)?.name }}
                </n-tag>
              </div>
            </div>
          </template>
          <template #header>
            <n-text strong depth="1">
              <div class="personal-name">{{ userStore.userInfo.name }}</div>
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
  import { useRouter } from 'vue-router';
  import { NDivider, NLayoutSider, NMenu, NPopover, NTag, NText } from 'naive-ui';

  import { PersonalEnum } from '@lib/shared/enums/systemEnum';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';
  import PersonalInfoDrawer from '@/views/system/business/components/personalInfoDrawer.vue';

  import { useI18n } from '@/hooks/useI18n';
  import useAppStore from '@/store/modules/app';
  import useUserStore from '@/store/modules/user';

  import { AppRouteEnum } from '@/enums/routeEnum';

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

  const menuOptions = [
    {
      label: t('menu.workbench'),
      key: AppRouteEnum.WORKBENCH_INDEX,
      icon: renderIcon('iconicon_home1'),
    },
    {
      label: t('module.customerManagement'),
      key: AppRouteEnum.CUSTOMER,
      icon: renderIcon('iconicon_multiple_choice_of_members'),
    },
    {
      label: t('module.clueManagement'),
      key: AppRouteEnum.CLUE_MANAGEMENT_CLUE,
      icon: renderIcon('iconicon_clue'),
    },
    {
      label: t('menu.opportunity'),
      key: AppRouteEnum.OPPORTUNITY_OPT,
      icon: renderIcon('iconicon_business_opportunity'),
    },
    {
      label: t('module.productManagement'),
      key: AppRouteEnum.PRODUCT_PRO,
      icon: renderIcon('iconicon_product'),
    },
    {
      label: t('menu.settings'),
      key: 'a-wild-sheep-chase',
      icon: renderIcon('iconicon_set_up'),
      children: [
        {
          label: t('menu.settings.org'),
          key: AppRouteEnum.SYSTEM_ORG,
        },
        {
          label: t('menu.settings.permission'),
          key: AppRouteEnum.SYSTEM_ROLE,
        },
        {
          label: t('menu.settings.moduleSetting'),
          key: AppRouteEnum.SYSTEM_MODULE,
        },
        {
          label: t('menu.settings.messageSetting'),
          key: AppRouteEnum.SYSTEM_MESSAGE,
        },
        {
          label: t('menu.settings.businessSetting'),
          key: AppRouteEnum.SYSTEM_BUSINESS,
        },
        {
          label: t('menu.settings.log'),
          key: AppRouteEnum.SYSTEM_LOG,
        },
      ],
    },
  ];

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

  function menuChange(key: string) {
    router.push({ name: key });
  }

  function personalMenuChange(key: string) {
    personalMenuValue.value = key;
    if (key === AppRouteEnum.PERSONAL_INFO || key === AppRouteEnum.PERSONAL_PLAN) {
      if (key === AppRouteEnum.PERSONAL_INFO) {
        personalTab.value = PersonalEnum.INFO;
      } else {
        personalTab.value = PersonalEnum.MY_PLAN;
      }
      showPersonalInfo.value = true;
    } else {
      userStore.logout();
      router.push({ name: 'login' });
    }
  }

  onBeforeMount(() => {
    if (router.currentRoute.value.meta.isTopMenu) {
      menuValue.value = router.currentRoute.value.matched[0].name as (typeof AppRouteEnum)[keyof typeof AppRouteEnum];
    } else {
      menuValue.value = router.currentRoute.value.name as (typeof AppRouteEnum)[keyof typeof AppRouteEnum];
    }
  });
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
