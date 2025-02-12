<template>
  <CrmCard no-content-padding hide-footer>
    <div class="config-container">
      <div class="left-box">
        <div class="mb-[16px] flex items-center justify-between">
          <div class="font-medium text-[var(--text-n1)]">{{ t('module.businessManage.mainNavConfig') }}</div>
          <div class="text-[var(--text-n4)]">
            <n-switch v-model:value="enable" @update:value="changeIcon" />
            <span class="ml-[8px]">icon </span>
          </div>
        </div>
        <div class="nav-list">
          <VueDraggable v-model="navList" ghost-class="ghost" handle=".nav-item">
            <div v-for="item in navList" :key="item.key" class="nav-item" @click="handleClick(item)">
              <CrmIcon type="iconicon_move" :size="16" class="mt-[1px] text-[var(--text-n4)]" />
              <CrmIcon v-if="enable" :type="item.icon ?? ''" :size="18" class="text-[var(--text-n1)]" />
              {{ item.label }}
            </div>
          </VueDraggable>
        </div>
      </div>
      <div class="right-box">
        <ConfigCard />
      </div>
    </div>
  </CrmCard>
</template>

<script setup lang="ts">
  import { NSwitch } from 'naive-ui';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import ConfigCard from './components/configCard.vue';

  import { useI18n } from '@/hooks/useI18n';

  import { ModuleConfigEnum } from '@/enums/moduleEnum';

  import { VueDraggable } from 'vue-draggable-plus';

  const { t } = useI18n();

  const enable = ref(false);

  const navList = ref([
    {
      label: t('module.workbenchHome'),
      key: ModuleConfigEnum.HOME,
      icon: 'iconicon_home',
    },
    {
      label: t('module.customerManagement'),
      key: ModuleConfigEnum.CUSTOMER_MANAGEMENT,
      icon: 'iconicon_customer',
    },
    {
      label: t('module.clueManagement'),
      key: ModuleConfigEnum.CLUE_MANAGEMENT,
      icon: 'iconicon_clue',
    },
    {
      label: t('module.businessManagement'),
      key: ModuleConfigEnum.BUSINESS_MANAGEMENT,
      icon: 'iconicon_business_opportunity',
    },
    {
      label: t('module.dataManagement'),
      key: ModuleConfigEnum.DATA_MANAGEMENT,
      icon: 'iconicon_data',
    },
    {
      label: t('module.productManagement'),
      key: ModuleConfigEnum.PRODUCT_MANAGEMENT,
      icon: 'iconicon_product',
    },
    {
      label: t('menu.settings'),
      key: ModuleConfigEnum.PRODUCT_MANAGEMENT,
      icon: 'iconicon_set_up',
    },
  ]);

  function handleClick(item: Record<string, any>) {}

  // 改变icon
  function changeIcon() {}
</script>

<style lang="less" scoped>
  .config-container {
    width: 100%;
    background: var(--text-n9);
    @apply flex h-full w-full gap-4;
    .left-box {
      padding: 24px;
      width: 24%;
      border-radius: var(--border-radius-medium);
      background: var(--text-n10);
      .nav-list {
        .nav-item {
          padding: 8px;
          height: 36px;
          border: 1px solid transparent;
          border-radius: var(--border-radius-small);
          background: var(--text-n9);
          gap: 8px;
          @apply mb-2 flex cursor-pointer items-center;
          &:hover {
            border-color: var(--primary-8);
          }
        }
      }
    }
    .right-box {
      padding: 24px;
      width: 76%;
      border-radius: var(--border-radius-medium);
      background: var(--text-n10);
      @apply flex-1;
    }
  }
</style>

<style lang="less">
  .crm-module-form-title {
    color: var(--text-n1);
    @apply mb-4 font-medium;
  }

  // 到期提醒表单
  .crm-reminder-advance-input {
    &.n-input--resizable {
      margin-right: 8px;
      width: 96px;
    }
  }
</style>
