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
        :collapsed-width="56"
        :icon-size="18"
        :collapsed-icon-size="18"
        :options="menuOptions"
        @update-value="menuChange"
      />
      <div class="flex flex-col items-start p-[8px]">
        <n-avatar v-if="collapsed" round :size="40" src="https://07akioni.oss-cn-beijing.aliyuncs.com/07akioni.jpeg" />
        <div
          v-else
          class="flex w-full items-center gap-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[8px]"
        >
          <n-avatar round :size="40" src="https://07akioni.oss-cn-beijing.aliyuncs.com/07akioni.jpeg" />
          <div>
            <div>迪丽热巴</div>
            <n-tag
              :bordered="false"
              size="small"
              :color="{
                color: 'var(--primary-6)',
                textColor: 'var(--primary-8)',
              }"
            >
              超级管理员
            </n-tag>
          </div>
        </div>
        <n-divider />
        <div class="ml-[8px] w-full cursor-pointer px-[8px]" @click="() => appStore.setMenuCollapsed(!collapsed)">
          <CrmIconFont :type="collapsed ? 'iconicon_menu_fold' : 'iconicon_menu_unfold'" :size="16" />
        </div>
      </div>
    </div>
  </n-layout-sider>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { NAvatar, NDivider, NIcon, NLayoutSider, NMenu, NTag } from 'naive-ui';

  import CrmIconFont from '@/components/pure/crm-icon-font/index.vue';

  import { useI18n } from '@/hooks/useI18n';
  import useAppStore from '@/store/modules/app';

  import { SystemRouteEnum } from '@/enums/routeEnum';

  import { BookOutline as BookIcon } from '@vicons/ionicons5';

  const { t } = useI18n();
  const appStore = useAppStore();
  const router = useRouter();
  const collapsed = ref(appStore.getMenuCollapsed);
  const menuValue = ref<string>('setting');

  watch(
    () => appStore.getMenuCollapsed,
    (value) => {
      collapsed.value = value;
    }
  );

  function renderIcon(icon: Component) {
    return () => h(NIcon, null, { default: () => h(icon) });
  }

  const menuOptions = [
    {
      label: t('menu.settings'),
      key: 'a-wild-sheep-chase',
      icon: renderIcon(BookIcon),
      children: [
        {
          label: t('menu.settings.org'),
          key: SystemRouteEnum.SYSTEM_ORG,
        },
        {
          label: t('menu.settings.permission'),
          key: SystemRouteEnum.SYSTEM_ROLE,
        },
        {
          label: t('menu.settings.moduleSetting'),
          key: SystemRouteEnum.SYSTEM_MODULE,
        },
        {
          label: t('menu.settings.businessSetting'),
          key: SystemRouteEnum.SYSTEM_BUSINESS,
        },
        {
          label: t('menu.settings.log'),
          key: SystemRouteEnum.SYSTEM_LOG,
        },
      ],
    },
  ];

  function menuChange(key: string) {
    router.push({ name: key });
  }
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
