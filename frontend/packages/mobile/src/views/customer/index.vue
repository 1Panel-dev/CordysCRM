<template>
  <div class="flex h-full flex-col overflow-hidden">
    <van-tabs v-model:active="activeName" border class="customer-tabs">
      <van-tab v-for="tab of tabList" :key="tab.name" :name="tab.name">
        <template #title>
          <div class="text-[16px]" :class="activeName === tab.name ? 'text-[var(--primary-8)]' : ''">
            {{ tab.title }}
          </div>
        </template>
        <customer v-if="tab.name === 'customer'" />
        <contact v-else-if="tab.name === 'contact'" />
        <open-sea v-else-if="tab.name === 'openSea'" />
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import customer from './components/customer.vue';
  import contact from './contact/index.vue';
  import openSea from './openSea/index.vue';

  import { hasAnyPermission } from '@/utils/permission';

  import { CustomerRouteEnum } from '@/enums/routeEnum';

  defineOptions({
    name: CustomerRouteEnum.CUSTOMER_INDEX,
  });

  const { t } = useI18n();
  const tabList = computed(() =>
    [
      {
        name: 'customer',
        title: t('menu.customer'),
        permission: ['CUSTOMER_MANAGEMENT:READ'],
      },
      {
        name: 'contact',
        title: t('menu.contact'),
        permission: ['CUSTOMER_MANAGEMENT_CONTACT:READ'],
      },
      {
        name: 'openSea',
        title: t('menu.openSea'),
        permission: ['CUSTOMER_MANAGEMENT_POOL:READ'],
      },
    ].filter((item) => hasAnyPermission(item.permission))
  );
  const activeName = ref(tabList.value[0]?.name);
</script>

<style lang="less" scoped>
  .customer-tabs {
    @apply h-full;
    :deep(.van-tabs__content) {
      height: calc(100% - var(--van-tabs-line-height));
      .van-tab__panel {
        @apply h-full;
      }
    }
  }
</style>
