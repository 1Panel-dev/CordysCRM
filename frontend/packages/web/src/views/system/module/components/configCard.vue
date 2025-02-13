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
        <NSwitch v-model:value="item.enable" @update:value="(value:boolean)=>toggleModule(value,item)" />
      </div>
    </div>
  </div>
  <customManagementFormDrawer v-model:visible="customerManagementFormVisible" />
  <OpportunityCloseRulesDrawer v-model:visible="businessManagementBusinessParamsSetVisible" />
  <CapacitySetDrawer v-model:visible="customerManagementCapacitySetVisible" />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NSwitch } from 'naive-ui';

  import CrmButtonGroup from '@/components/pure/crm-button-group/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CapacitySetDrawer from './customManagement/capacitySetDrawer.vue';
  import customManagementFormDrawer from './customManagement/formDrawer.vue';
  import OpportunityCloseRulesDrawer from './opportunity/opportunityCloseRulesDrawer.vue';

  import { useI18n } from '@/hooks/useI18n';

  import { ModuleConfigEnum } from '@/enums/moduleEnum';

  const { t } = useI18n();

  type ModuleConfigItem = {
    label: string;
    key: ModuleConfigEnum;
    icon?: string;
    enable: boolean;
    groupList: ActionsItem[];
  };

  const moduleConfigList = ref<ModuleConfigItem[]>([
    {
      label: t('module.workbenchHome'),
      key: ModuleConfigEnum.HOME,
      icon: 'iconicon_home',
      groupList: [
        {
          label: t('module.newForm'),
          key: 'newForm',
        },
      ],
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
      icon: 'iconicon_clue',
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
      icon: 'iconicon_business_opportunity',
      enable: true,
      groupList: [
        {
          label: t('module.newForm'),
          key: 'newForm',
        },
        {
          label: t('module.businessManage.businessCloseRule'),
          key: 'businessParamsSet',
        },
      ],
    },
    {
      label: t('module.dataManagement'),
      key: ModuleConfigEnum.DATA_MANAGEMENT,
      icon: 'iconicon_data',
      enable: true,
      groupList: [],
    },
    {
      label: t('module.productManagement'),
      key: ModuleConfigEnum.PRODUCT_MANAGEMENT,
      icon: 'iconicon_product',
      groupList: [
        {
          label: t('module.newForm'),
          key: 'newForm',
        },
      ],
      enable: true,
    },
  ]);

  function toggleModule(value: boolean, item: ModuleConfigItem) {}

  const customerManagementFormVisible = ref(false);
  const customerManagementOpenSeaVisible = ref(false);
  const customerManagementCapacitySetVisible = ref(false);

  const clueManagementFormVisible = ref(false);
  const clueManagementCluePoolVisible = ref(false);
  const clueManagementCapacitySetVisible = ref(false);

  const businessManagementFormVisible = ref(false);
  const businessManagementBusinessParamsSetVisible = ref(false);

  const productManagementFormVisible = ref(false);

  function handleSelect(key: string, item: ModuleConfigItem) {
    switch (item.key) {
      case ModuleConfigEnum.CUSTOMER_MANAGEMENT:
        if (key === 'newForm') {
          customerManagementFormVisible.value = true;
        } else if (key === 'openSea') {
          customerManagementOpenSeaVisible.value = true;
        } else if (key === 'capacitySet') {
          customerManagementCapacitySetVisible.value = true;
        }
        break;
      case ModuleConfigEnum.CLUE_MANAGEMENT:
        if (key === 'newForm') {
          clueManagementFormVisible.value = true;
        } else if (key === 'cluePool') {
          clueManagementCluePoolVisible.value = true;
        } else if (key === 'capacitySet') {
          clueManagementCapacitySetVisible.value = true;
        }
        break;
      case ModuleConfigEnum.BUSINESS_MANAGEMENT:
        if (key === 'newForm') {
          businessManagementFormVisible.value = true;
        } else if (key === 'businessParamsSet') {
          businessManagementBusinessParamsSetVisible.value = true;
        }
        break;
      case ModuleConfigEnum.PRODUCT_MANAGEMENT:
        if (key === 'newForm') {
          productManagementFormVisible.value = true;
        }
        break;
      default:
        break;
    }
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
