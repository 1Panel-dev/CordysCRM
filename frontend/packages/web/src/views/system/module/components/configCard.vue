<template>
  <div class="nav-config-list">
    <div v-for="item of moduleConfigList" :key="item.key" class="nav-config-item">
      <div class="nav-config-item-title">
        <div class="nav-config-item-icon">
          <CrmIcon :type="item.icon ?? ''" :size="20" class="text-[var(--text-n10)]" />
        </div>
        <div>{{ item.label }}</div>
      </div>
      <div class="nav-config-item-action">
        <CrmButtonGroup :list="item.groupList" @select="(key) => handleSelect(key, item)" />
        <NSwitch v-model:value="item.enable" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NSwitch } from 'naive-ui';

  import CrmButtonGroup from '@/components/pure/crm-button-group/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';

  import { useI18n } from '@/hooks/useI18n';

  import { ModuleConfigEnum } from '@/enums/moduleEnum';

  const { t } = useI18n();

  type ModuleConfigItem = {
    label: string;
    key: string;
    icon?: string;
    enable: boolean;
    groupList: ActionsItem[];
  };

  const moduleConfigList = ref<ModuleConfigItem[]>([
    {
      label: t('module.workbenchHome'),
      key: ModuleConfigEnum.HOME,
      icon: 'iconicon_home',
      groupList: [],
      enable: true,
    },
    {
      label: t('module.customerManagement'),
      key: ModuleConfigEnum.CUSTOMER_MANAGEMENT,
      icon: 'iconicon_customer',
      groupList: [
        {
          label: t('module.newForm'),
          key: 'newForm',
        },
        {
          label: t('module.customer.openSea'),
          key: 'openSea',
        },
        {
          label: t('module.customer.capacitySet'),
          key: 'capacitySet',
        },
      ],
      enable: true,
    },
    {
      label: t('module.clueManagement'),
      key: ModuleConfigEnum.CLUE_MANAGEMENT,
      icon: 'iconicon_customer',
      groupList: [
        {
          label: t('module.newForm'),
          key: 'newForm',
        },
        {
          label: t('module.clue.cluePool'),
          key: 'cluePool',
        },
        {
          label: t('module.clue.capacitySet'),
          key: 'capacitySet',
        },
      ],
      enable: true,
    },
    {
      label: t('module.businessManagement'),
      key: ModuleConfigEnum.BUSINESS_MANAGEMENT,
      icon: 'iconicon_customer',
      enable: true,
      groupList: [
        {
          label: t('module.newForm'),
          key: 'newForm',
        },
        {
          label: t('module.businessManage.businessParamsSet'),
          key: 'newForm',
        },
      ],
    },
    {
      label: t('module.dataManagement'),
      key: ModuleConfigEnum.DATA_MANAGEMENT,
      icon: 'iconicon_customer',
      enable: true,
      groupList: [],
    },
    {
      label: t('module.productManagement'),
      key: ModuleConfigEnum.PRODUCT_MANAGEMENT,
      icon: 'iconicon_customer',
      groupList: [
        {
          label: t('module.newForm'),
          key: 'newForm',
        },
      ],
      enable: true,
    },
  ]);

  function handleSelect(key: string, item: any) {
    console.log(key, item);
  }
</script>

<style scoped lang="less">
  .nav-config-item {
    padding: 24px;
    height: 80px;
    border-radius: var(--border-radius-medium);
    background: var(--text-n9);
    @apply mb-4 flex items-center justify-between;
    .nav-config-item-title {
      gap: 8px;
      color: var(--text-n1);
      @apply flex items-center font-medium;
      .nav-config-item-icon {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        background: var(--primary-8);
        @apply flex items-center  justify-center;
      }
    }
    .nav-config-item-action {
      gap: 8px;
      @apply flex items-center;
    }
  }
</style>
