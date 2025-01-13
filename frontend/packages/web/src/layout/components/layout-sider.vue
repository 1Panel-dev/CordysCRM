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
      <n-menu :collapsed-width="56" :collapsed-icon-size="18" :options="menuOptions" />
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
  import { NAvatar, NDivider, NIcon, NLayoutSider, NMenu, NTag } from 'naive-ui';

  import CrmIconFont from '@/components/pure/crm-icon-font/index.vue';

  import useAppStore from '@/store/modules/app';

  import { BookOutline as BookIcon } from '@vicons/ionicons5';

  const appStore = useAppStore();
  const collapsed = ref(appStore.getMenuCollapsed);

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
      label: '收藏',
      key: 'hear-the-wind-sing',
      icon: renderIcon(BookIcon),
    },
    {
      label: '首页',
      key: 'pinball-1973',
      icon: renderIcon(BookIcon),
    },
    {
      label: '客户管理',
      key: 'a-wild-sheep-chase',
      icon: renderIcon(BookIcon),
      children: [
        {
          label: '客户信息',
          key: 'people',
        },
        {
          label: '客户公海',
          key: 'beverage',
        },
      ],
    },
    {
      label: '线索管理',
      key: 'dance-dance-dance',
      icon: renderIcon(BookIcon),
    },
    {
      label: '商机管理',
      key: 'dance-dance-dance',
      icon: renderIcon(BookIcon),
    },
    {
      label: '数据',
      key: 'dance-dance-dance',
      icon: renderIcon(BookIcon),
    },
    {
      label: '设置',
      key: 'dance-dance-dance',
      icon: renderIcon(BookIcon),
    },
  ];
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
