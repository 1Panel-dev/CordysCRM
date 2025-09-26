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
            <CrmMoreAction
              :options="getMoreList(item.key)"
              trigger="hover"
              @select="(item) => handleMoreSelect(item.key as string)"
            >
              <n-button type="primary" text :keyboard="false">{{ t('common.more') }}</n-button>
            </CrmMoreAction>
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
          size="small"
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
  <MoveAccountReasonDrawer
    v-model:visible="showAccountReasonDrawer"
    v-model:config="isHasConfigAccountReason"
    v-model:enable="enableAccountMoveReason"
    @load-config="() => getGlobalReasonConfig()"
  />
  <MoveLeadReasonDrawer
    v-model:visible="showLeadReasonDrawer"
    v-model:enable="enableLeadMoveReason"
    v-model:config="isHasConfigLeadReason"
    @load-config="() => getGlobalReasonConfig()"
  />
  <OpportunityReasonDrawer
    v-model:visible="showOptReasonDrawer"
    v-model:config="isHasConfigOptReason"
    v-model:enable="enableOptMoveReason"
    @load-config="() => getGlobalReasonConfig()"
  />
</template>

<script setup lang="ts">
  import { RendererElement } from 'vue';
  import { NButton, NDivider, NSwitch, NTooltip, useMessage } from 'naive-ui';

  import { ModuleConfigEnum, ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ModuleNavItem } from '@lib/shared/models/system/module';

  import CrmButtonGroup from '@/components/pure/crm-button-group/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmPopConfirm from '@/components/pure/crm-pop-confirm/index.vue';
  import CapacitySetDrawer from './capacitySetDrawer.vue';
  import CluePoolDrawer from './clueManagement/cluePoolDrawer.vue';
  import clueFormDrawer from './clueManagement/formDrawer.vue';
  import MoveLeadReasonDrawer from './clueManagement/moveReasonDrawer.vue';
  import customManagementContactFormDrawer from './customManagement/contactFormDrawer.vue';
  import followPlanDrawer from './customManagement/followPlanDrawer.vue';
  import followRecordDrawer from './customManagement/followRecordDrawer.vue';
  import customManagementFormDrawer from './customManagement/formDrawer.vue';
  import MoveAccountReasonDrawer from './customManagement/moveReasonDrawer.vue';
  import OpenSeaDrawer from './customManagement/openSeaDrawer.vue';
  import OpportunityReasonDrawer from './opportunity/failReasonDrawer.vue';
  import OpportunityFormDrawer from './opportunity/formDrawer.vue';
  import OpportunityCloseRulesDrawer from './opportunity/opportunityCloseRulesDrawer.vue';
  import ProductFromDrawer from './productManagement/formDrawer.vue';

  import { getReasonConfig, toggleModuleNavStatus, updateReasonEnable } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import router from '@/router';
  // import useLicenseStore from '@/store/modules/setting/license';
  import { hasAnyPermission } from '@/utils/permission';

  import { SystemRouteEnum } from '@/enums/routeEnum';

  const { openModal } = useModal();
  const Message = useMessage();
  const { t } = useI18n();
  // const licenseStore = useLicenseStore();

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

  const renderAccountReasonConfig = ref<VNode<RendererElement, { [key: string]: any }> | null>(null);
  const renderLeadReasonConfig = ref<VNode<RendererElement, { [key: string]: any }> | null>(null);
  const renderOptReasonConfig = ref<VNode<RendererElement, { [key: string]: any }> | null>(null);
  // 是否已配置原因
  const isHasConfigAccountReason = ref<boolean>(false);
  const isHasConfigLeadReason = ref<boolean>(false);
  const isHasConfigOptReason = ref<boolean>(false);

  // 全局原因配置
  const enableAccountMoveReason = ref(false);
  const enableLeadMoveReason = ref(false);
  const enableOptMoveReason = ref(false);

  const showAccountReasonDrawer = ref(false);
  const showLeadReasonDrawer = ref(false);
  const showOptReasonDrawer = ref(false);

  // 配置原因
  function handleConfigReason(e: MouseEvent, type: ReasonTypeEnum) {
    switch (type) {
      case ReasonTypeEnum.CUSTOMER_POOL_RS:
        showAccountReasonDrawer.value = true;
        break;
      case ReasonTypeEnum.CLUE_POOL_RS:
        showLeadReasonDrawer.value = true;
        break;
      case ReasonTypeEnum.OPPORTUNITY_FAIL_RS:
        showOptReasonDrawer.value = true;
        break;
      default:
        break;
    }
  }

  const refreshReasonConfigKey = ref(0);
  // 切换原因全局开关
  async function toggleReason(e: MouseEvent, type: ReasonTypeEnum, enable: boolean) {
    try {
      await updateReasonEnable({
        module: type,
        enable: !enable,
      });

      Message.success(enable ? t('common.closeSuccess') : t('common.enableSuccess'));
      refreshReasonConfigKey.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function getLabel(type: ReasonTypeEnum) {
    switch (type) {
      case ReasonTypeEnum.CUSTOMER_POOL_RS:
        return t('module.movePublicPoolReasonConfig');
      case ReasonTypeEnum.CLUE_POOL_RS:
        return t('module.moveLeadPoolReasonConfig');
      case ReasonTypeEnum.OPPORTUNITY_FAIL_RS:
        return t('module.failReasonConfig');
      default:
        break;
    }
  }

  // 获取原因开关
  function renderReason(
    type: ReasonTypeEnum,
    switchValue: Ref<boolean>,
    isConfigAvailableReason: Ref<boolean>,
    onToggleCallBack: (e: MouseEvent, moduleType: ReasonTypeEnum, val: boolean) => void,
    onConfigReasonCallBack: (e: MouseEvent, moduleType: ReasonTypeEnum) => void
  ) {
    if (!hasAnyPermission(['MODULE_SETTING:UPDATE'])) {
      return null;
    }
    const label = getLabel(type);
    return h(
      'div',
      {
        class: 'flex items-center text-[var(--text-n1)]',
        onClick: (e: MouseEvent) => onConfigReasonCallBack(e, type),
      },
      [
        label,
        h(
          NTooltip,
          {
            delay: 300,
            flip: true,
            disabled: isConfigAvailableReason.value,
          },
          {
            trigger: () =>
              h(NSwitch, {
                value: switchValue.value,
                rubberBand: false,
                size: 'small',
                disabled: !hasAnyPermission(['MODULE_SETTING:UPDATE']) || !isConfigAvailableReason.value,
                class: 'ml-[8px] text-[var(--primary-8)]',
                onClick: (e: MouseEvent) => {
                  e.stopPropagation();
                  if (isConfigAvailableReason.value) {
                    onToggleCallBack(e, type, switchValue.value);
                  }
                },
              }),
            default: () => t('module.configReasonTooltip'),
          }
        ),
      ]
    );
  }

  const accountMoreOptions = computed<ActionsItem[]>(() => [
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
    {
      label: t('module.movePublicPoolReasonConfig'),
      key: 'move',
      render: renderAccountReasonConfig.value,
    },
  ]);

  const leadMoreOptions = computed<ActionsItem[]>(() => [
    {
      label: t('module.moveLeadPoolReasonConfig'),
      key: 'move',
      render: renderLeadReasonConfig.value,
    },
  ]);

  const opportunityMoreOptions = computed<ActionsItem[]>(() => [
    {
      label: t('module.failReasonConfig'),
      key: 'move',
      render: renderOptReasonConfig.value,
    },
  ]);

  function getMoreList(key: ModuleConfigEnum) {
    switch (key) {
      case ModuleConfigEnum.CUSTOMER_MANAGEMENT:
        return accountMoreOptions.value;
      case ModuleConfigEnum.CLUE_MANAGEMENT:
        return leadMoreOptions.value;
      case ModuleConfigEnum.BUSINESS_MANAGEMENT:
        return opportunityMoreOptions.value;
      default:
        return [];
    }
  }

  const staticConfigList = [
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
        {
          label: t('common.more'),
          slotName: 'more',
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
        {
          label: t('common.more'),
          slotName: 'more',
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
    {
      label: t('module.agent'),
      key: ModuleConfigEnum.AGENT,
      icon: 'iconicon_bot',
      groupList: [],
      enable: true,
    },
  ];
  const moduleConfigList = computed<ModuleConfigItem[]>(() =>
    staticConfigList.map((item) => {
      const findConfigItem = props.list.find((e) => e.key === item.key);
      return {
        ...item,
        enable: findConfigItem?.enable ?? false,
        id: findConfigItem?.id ?? '',
        disabled: findConfigItem?.disabled ?? false,
      };
    })
  );

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

  function initRenderReasonSwitch() {
    renderAccountReasonConfig.value = renderReason(
      ReasonTypeEnum.CUSTOMER_POOL_RS,
      enableAccountMoveReason,
      isHasConfigAccountReason,
      toggleReason,
      handleConfigReason
    );
    renderLeadReasonConfig.value = renderReason(
      ReasonTypeEnum.CLUE_POOL_RS,
      enableLeadMoveReason,
      isHasConfigLeadReason,
      toggleReason,
      handleConfigReason
    );
    renderOptReasonConfig.value = renderReason(
      ReasonTypeEnum.OPPORTUNITY_FAIL_RS,
      enableOptMoveReason,
      isHasConfigOptReason,
      toggleReason,
      handleConfigReason
    );
  }

  async function getGlobalReasonConfig() {
    try {
      const reasonTypes = [
        {
          type: ReasonTypeEnum.CUSTOMER_POOL_RS,
          enableRef: enableAccountMoveReason,
          hasConfigRef: isHasConfigAccountReason,
        },
        { type: ReasonTypeEnum.CLUE_POOL_RS, enableRef: enableLeadMoveReason, hasConfigRef: isHasConfigLeadReason },
        {
          type: ReasonTypeEnum.OPPORTUNITY_FAIL_RS,
          enableRef: enableOptMoveReason,
          hasConfigRef: isHasConfigOptReason,
        },
      ];

      const configs = await Promise.all(reasonTypes.map((item) => getReasonConfig(item.type)));

      configs.forEach((config, index) => {
        const { enable, dictList } = config;
        const { enableRef, hasConfigRef } = reasonTypes[index];
        enableRef.value = enable;
        hasConfigRef.value = dictList.filter((e) => e.id !== 'system').length > 0;
      });

      initRenderReasonSwitch();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  onBeforeMount(() => {
    getGlobalReasonConfig();
  });

  watch(
    () => refreshReasonConfigKey.value,
    (val) => {
      if (val) {
        getGlobalReasonConfig();
      }
    }
  );

  onMounted(() => {
    initRenderReasonSwitch();
  });
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
