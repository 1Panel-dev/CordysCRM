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
              {{ userStore.userInfo.id === 'admin' ? t('common.admin') : userStore.userInfo.roles[0] }}
            </n-tag>
          </div>
        </div>
        <n-divider />
        <div class="ml-[8px] w-full cursor-pointer px-[8px]" @click="() => appStore.setMenuCollapsed(!collapsed)">
          <CrmIconFont :type="collapsed ? 'iconicon_menu_fold1' : 'iconicon_menu_unfold1'" :size="16" />
        </div>
      </div>
    </div>
  </n-layout-sider>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { NDivider, NLayoutSider, NMenu, NTag } from 'naive-ui';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmIconFont from '@/components/pure/crm-icon-font/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';

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

  function menuChange(key: string) {
    router.push({ name: key });
  }

  onBeforeMount(() => {
    menuValue.value = router.currentRoute.value.name as (typeof AppRouteEnum)[keyof typeof AppRouteEnum];
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
</style>
