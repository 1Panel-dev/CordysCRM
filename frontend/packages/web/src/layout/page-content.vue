<template>
  <router-view v-slot="{ Component, route }">
    <transition name="fade" mode="out-in" appear>
      <!-- transition内必须有且只有一个根元素，不然会导致二级路由的组件无法渲染 -->
      <div class="h-full w-full overflow-hidden">
        <n-scrollbar content-style="min-height: 500px;height: 100%;width: 100%">
          <div
            class="page-content"
            :class="
              route.name?.toString().includes(DashboardRouteEnum.DASHBOARD) ||
              route.name?.toString().includes(TenderRouteEnum.TENDER)
                ? '!pt-0'
                : ''
            "
          >
            <CrmExpireAlert v-if="route.path.includes(SystemRouteEnum.SYSTEM)" />
            <n-breadcrumb v-if="route.meta?.breadcrumbs" class="mb-[8px]">
              <n-breadcrumb-item
                v-for="item in route.meta?.breadcrumbs"
                :key="(item as BreadcrumbItem).name"
                @click="jumpTo((item as BreadcrumbItem).name)"
              >
                {{ t((item as BreadcrumbItem).locale) }}
              </n-breadcrumb-item>
            </n-breadcrumb>
            <Suspense>
              <keep-alive :include="[]">
                <component :is="Component" :key="route.name" />
              </keep-alive>
            </Suspense>
          </div>
        </n-scrollbar>
      </div>
    </transition>
  </router-view>
</template>

<script lang="ts" setup>
  import { RouteRecordNameGeneric } from 'vue-router';
  import { NBreadcrumb, NBreadcrumbItem, NScrollbar } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmExpireAlert from '@/components/business/crm-expire-alert/index.vue';

  import router from '@/router';
  import { BreadcrumbItem } from '@/router/routes/types';

  import { DashboardRouteEnum, SystemRouteEnum, TenderRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();

  function jumpTo(name: RouteRecordNameGeneric) {
    router.replace({ name });
  }
</script>

<style lang="less">
  .page-content {
    @apply absolute h-full w-full;

    padding: 16px;
    background-color: var(--text-n9);
    .n-breadcrumb-item__link {
      padding: 0 !important;
    }
  }
</style>
