<template>
  <div class="flex h-full flex-col overflow-hidden">
    <van-tabs v-model:active="activeName" border class="customer-tabs">
      <van-tab v-for="tab of tabList" :key="tab.name" :name="tab.name">
        <template #title>
          <div class="text-[16px]" :class="activeName === tab.name ? 'text-[var(--primary-8)]' : ''">
            {{ tab.title }}
          </div>
        </template>
        <Clue v-if="tab.name === 'clue'" />
        <Pool v-else-if="tab.name === 'cluePool'" />
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import Clue from './clue/index.vue';
  import Pool from './pool/index.vue';

  import { hasAnyPermission } from '@/utils/permission';

  import { ClueRouteEnum } from '@/enums/routeEnum';

  defineOptions({
    name: ClueRouteEnum.CLUE_INDEX,
  });
  const { t } = useI18n();
  const activeName = ref('clue');
  const tabList = computed(() =>
    [
      {
        name: 'clue',
        title: t('menu.clue'),
        permission: ['CLUE_MANAGEMENT:READ'],
      },
      {
        name: 'cluePool',
        title: t('menu.cluePool'),
        permission: ['CLUE_MANAGEMENT_POOL:READ'],
      },
    ].filter((item) => hasAnyPermission(item.permission))
  );
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
