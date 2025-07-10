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
        <CrmButtonGroup
          v-permission="['MODULE_SETTING:UPDATE']"
          :list="item.groupList"
          @select="(key) => handleSelect(key, item)"
        >
          <template #more>
            <n-dropdown trigger="hover" :options="moreOptions" @select="handleMoreSelect">
              <n-button type="primary" text :keyboard="false">{{ t('common.more') }}</n-button>
            </n-dropdown>
          </template>
        </CrmButtonGroup>
        <n-divider v-if="item.groupList.length" v-permission="['MODULE_SETTING:UPDATE']" class="!mx-[4px]" vertical />
        <CrmPopConfirm
          v-if="item.disabled"
          :title="t('module.DataEaseTip')"
          icon-type="primary"
          :content="t('module.DataEaseTipContent')"
          :positive-text="t('module.goConfig')"
          trigger="hover"
          negative-text=""
          placement="right-end"
          @confirm="goDEConfig"
        >
          <NSwitch
            :disabled="!hasAnyPermission(['MODULE_SETTING:UPDATE']) || item.disabled"
            :value="item.enable"
            @update:value="(value:boolean)=>toggleModule(value,item)"
          />
        </CrmPopConfirm>
        <NSwitch
          v-else
          :disabled="!hasAnyPermission(['MODULE_SETTING:UPDATE'])"
          :value="item.enable"
          @update:value="(value:boolean)=>toggleModule(value,item)"
        />
      </div>
    </div>
  </div>
  <customManagementFormDrawer v-model:visible="customerManagementFormVisible" />
  <customManagementContactFormDrawer v-model:visible="customerManagementContactFormVisible" />
  <followRecordDrawer v-model:visible="customerManagementFollowRecordVisible" />
  <followPlanDrawer v-model:visible="customerManagementFollowPlanVisible" />
  <OpportunityCloseRulesDrawer v-model:visible="businessManagementBusinessParamsSetVisible" />
  <OpportunityFormDrawer v-model:visible="businessManagementFormVisible" />
  <CapacitySetDrawer
    v-model:visible="capacitySetVisible"
    :type="selectKey"
    :title="
      selectKey === ModuleConfigEnum.CUSTOMER_MANAGEMENT
        ? t('module.customer.capacitySet')
        : t('module.clue.capacitySet')
    "
  />
  <CluePoolDrawer v-model:visible="clueManagementCluePoolVisible" />
  <clueFormDrawer v-model:visible="clueManagementFormVisible" />
  <OpenSeaDrawer v-model:visible="customerManagementOpenSeaVisible" :type="selectKey" />
  <ProductFromDrawer v-model:visible="productManagementFormVisible" />
</template>

<script setup lang="ts">
  import { NButton, NDivider, NDropdown, NSwitch, useMessage } from 'naive-ui';

  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ModuleNavItem } from '@lib/shared/models/system/module';

  import CrmButtonGroup from '@/components/pure/crm-button-group/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmPopConfirm from '@/components/pure/crm-pop-confirm/index.vue';
  import CapacitySetDrawer from './capacitySetDrawer.vue';
  import CluePoolDrawer from './clueManagement/cluePoolDrawer.vue';
  import clueFormDrawer from './clueManagement/formDrawer.vue';
  import customManagementContactFormDrawer from './customManagement/contactFormDrawer.vue';
  import followPlanDrawer from './customManagement/followPlanDrawer.vue';
  import followRecordDrawer from './customManagement/followRecordDrawer.vue';
  import customManagementFormDrawer from './customManagement/formDrawer.vue';
  import OpenSeaDrawer from './customManagement/openSeaDrawer.vue';
  import OpportunityFormDrawer from './opportunity/formDrawer.vue';
  import OpportunityCloseRulesDrawer from './opportunity/opportunityCloseRulesDrawer.vue';
  import ProductFromDrawer from './productManagement/formDrawer.vue';

  import { toggleModuleNavStatus } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import router from '@/router';
  import { hasAnyPermission } from '@/utils/permission';

  import { SystemRouteEnum } from '@/enums/routeEnum';

  const { openModal } = useModal();
  const Message = useMessage();
  const { t } = useI18n();

  const props = defineProps<{
    list: ModuleNavItem[];
  }>();

  const emit = defineEmits<{
    (e: 'loadModuleList'): void;
  }>();

  type ModuleConfigItem = {
    id?: string;
    label: string;
    key: ModuleConfigEnum;
    icon?: string;
    enable: boolean;
    groupList: ActionsItem[];
    disabled?: boolean; // 是否禁用
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
      label: t('module.clueManagement'),
      key: ModuleConfigEnum.CLUE_MANAGEMENT,
      icon: 'iconicon_clue',
      groupList: [
        {
          label: t('module.clueFormSetting'),
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
      label: t('module.customerManagement'),
      key: ModuleConfigEnum.CUSTOMER_MANAGEMENT,
      icon: 'iconicon_customer',
      groupList: [
        {
          label: t('module.customerFormSetting'),
          key: 'newForm',
        },
        {
          label: t('module.newContactForm'),
          key: 'newContactForm',
        },
        {
          label: t('module.customer.openSea'),
          key: 'openSea',
        },
        {
          label: t('common.more'),
          slotName: 'more',
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
          label: t('module.opportunityFormSetting'),
          key: 'newForm',
        },
        {
          label: t('module.businessManage.businessCloseRule'),
          key: 'businessParamsSet',
        },
      ],
    },
    // TODO 不上 xxw
    // {
    //   label: t('module.dataManagement'),
    //   key: ModuleConfigEnum.DATA_MANAGEMENT,
    //   icon: 'iconicon_data',
    //   enable: true,
    //   groupList: [],
    // },
    {
      label: t('module.productManagement'),
      key: ModuleConfigEnum.PRODUCT_MANAGEMENT,
      icon: 'iconicon_product',
      groupList: [
        {
          label: t('module.productFormSetting'),
          key: 'newForm',
        },
      ],
      enable: true,
    },
    {
      label: t('common.dashboard'),
      key: ModuleConfigEnum.DASHBOARD,
      icon: 'iconicon_dashboard1',
      groupList: [],
      enable: true,
    },
  ]);

  const moreOptions = [
    {
      key: 'followRecord',
      label: t('module.followRecordFormSetting'),
    },
    {
      key: 'followPlan',
      label: t('module.followPlanFormSetting'),
    },
    {
      key: 'capacitySet',
      label: t('module.customer.capacitySet'),
    },
  ];

  // 切换模块状态
  async function toggleModule(enable: boolean, item: ModuleConfigItem) {
    const title = enable
      ? t('module.openModuleTip', { name: item.label })
      : t('module.closeModuleTip', { name: item.label });
    const content = enable ? t('module.openModuleTipContent') : t('module.closeModuleTipContent');
    const positiveText = t(enable ? 'common.confirmStart' : 'common.confirmClose');

    openModal({
      type: enable ? 'default' : 'warning',
      title,
      content,
      positiveText,
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          if (item.id) {
            await toggleModuleNavStatus(item.id ?? '');
            Message.success(t(enable ? 'common.opened' : 'common.closed'));
            emit('loadModuleList');
          }
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function goDEConfig() {
    router.push({
      name: SystemRouteEnum.SYSTEM_BUSINESS,
    });
  }

  const selectKey = ref<ModuleConfigEnum>(ModuleConfigEnum.CUSTOMER_MANAGEMENT);
  const customerManagementFormVisible = ref(false);
  const customerManagementContactFormVisible = ref(false);
  const customerManagementOpenSeaVisible = ref(false);
  const customerManagementFollowRecordVisible = ref(false);
  const customerManagementFollowPlanVisible = ref(false);
  const capacitySetVisible = ref(false);

  const clueManagementFormVisible = ref(false);
  const clueManagementCluePoolVisible = ref(false);

  const businessManagementFormVisible = ref(false);
  const businessManagementBusinessParamsSetVisible = ref(false);

  const productManagementFormVisible = ref(false);

  function handleSelect(key: string, item: ModuleConfigItem) {
    selectKey.value = item.key;
    switch (item.key) {
      case ModuleConfigEnum.CUSTOMER_MANAGEMENT:
        if (key === 'newForm') {
          customerManagementFormVisible.value = true;
        } else if (key === 'newContactForm') {
          customerManagementContactFormVisible.value = true;
        } else if (key === 'openSea') {
          customerManagementOpenSeaVisible.value = true;
        } else if (key === 'capacitySet') {
          capacitySetVisible.value = true;
        }
        break;
      case ModuleConfigEnum.CLUE_MANAGEMENT:
        if (key === 'newForm') {
          clueManagementFormVisible.value = true;
        } else if (key === 'cluePool') {
          clueManagementCluePoolVisible.value = true;
        } else if (key === 'capacitySet') {
          capacitySetVisible.value = true;
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

  function handleMoreSelect(key: string) {
    switch (key) {
      case 'followRecord':
        customerManagementFollowRecordVisible.value = true;
        break;
      case 'followPlan':
        customerManagementFollowPlanVisible.value = true;
        break;
      case 'capacitySet':
        selectKey.value = ModuleConfigEnum.CUSTOMER_MANAGEMENT;
        capacitySetVisible.value = true;
        break;
      default:
        break;
    }
  }

  watch(
    () => props.list,
    () => {
      moduleConfigList.value = moduleConfigList.value.map((item) => {
        const findConfigItem = props.list.find((e) => e.key === item.key);
        return {
          ...item,
          enable: findConfigItem?.enable ?? false,
          id: findConfigItem?.id ?? '',
          disabled: findConfigItem?.disabled ?? false,
        };
      });
    }
  );
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
