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
    <div class="flex-1 overflow-auto"></div>
  </div>
</template>

<script setup lang="ts">
  import contact from './components/contact.vue';
  import customer from './components/customer.vue';
  import openSea from './components/openSea.vue';

  import { useI18n } from '@cordys/web/src/hooks/useI18n';

  const { t } = useI18n();
  const activeName = ref('a');
  const tabList = [
    {
      name: 'customer',
      title: t('menu.customer'),
    },
    {
      name: 'contact',
      title: t('menu.contact'),
    },
    {
      name: 'openSea',
      title: t('menu.openSea'),
    },
  ];
</script>

<style lang="less" scoped>
  .customer-tabs {
    @apply h-full;
    :deep(.van-tabs__content) {
      @apply h-full;
      .van-tab__panel {
        @apply h-full;
      }
    }
  }
</style>
