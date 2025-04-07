<template>
  <div class="config-container h-full w-full">
    <div class="left-box">
      <CrmCard hide-footer>
        <div class="h-full">
          <div class="mb-[16px] flex items-center justify-between">
            <div class="font-medium text-[var(--text-n1)]">{{ t('module.businessManage.mainNavConfig') }}</div>
            <div class="text-[var(--text-n4)]">
              <n-switch v-model:value="enable" @update:value="changeIcon" />
              <span class="ml-[8px]">icon </span>
            </div>
          </div>
          <div class="nav-list">
            <VueDraggable
              v-model="moduleNavList"
              ghost-class="ghost"
              handle=".nav-item"
              :disabled="!hasAnyPermission(['MODULE_SETTING:UPDATE'])"
              @end="onDragEnd"
            >
              <div v-for="item in moduleNavList" :key="item.key" class="nav-item">
                <CrmIcon type="iconicon_move" :size="16" class="mt-[1px] cursor-move text-[var(--text-n4)]" />
                <CrmIcon v-if="enable" :type="item.icon ?? ''" :size="18" class="text-[var(--text-n1)]" />
                {{ item.label }}
              </div>
            </VueDraggable>
          </div>
        </div>
      </CrmCard>
    </div>
    <div class="right-box">
      <CrmCard hide-footer>
        <div class="h-full">
          <ConfigCard :list="moduleNavList" @load-module-list="initModuleNavList()" />
        </div>
      </CrmCard>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NSwitch, useMessage } from 'naive-ui';
  import { VueDraggable } from 'vue-draggable-plus';

  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ModuleNavItem } from '@lib/shared/models/system/module';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import ConfigCard from './components/configCard.vue';

  import { moduleNavListSort } from '@/api/modules';
  import useAppStore from '@/store/modules/app';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();
  const appStore = useAppStore();
  const Message = useMessage();

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
    // TODO 不上 xxw
    // {
    //   label: t('module.dataManagement'),
    //   key: ModuleConfigEnum.DATA_MANAGEMENT,
    //   icon: 'iconicon_data',
    // },
    {
      label: t('module.productManagement'),
      key: ModuleConfigEnum.PRODUCT_MANAGEMENT,
      icon: 'iconicon_product',
    },
    {
      label: t('menu.settings'),
      key: ModuleConfigEnum.SYSTEM_SETTINGS,
      icon: 'iconicon_set_up',
    },
  ]);

  const moduleNavList = ref<ModuleNavItem[]>([]);
  async function initModuleNavList() {
    await appStore.initModuleConfig();
    moduleNavList.value = appStore.moduleConfigList.map((item) => {
      const navItem = navList.value.find((e) => e.key === item.moduleKey);
      return {
        ...item,
        key: item.moduleKey,
        label: navItem?.label ?? '',
        icon: navItem?.icon ?? '',
      };
    });
  }

  // 模块排序
  const onDragEnd = async (event: any) => {
    const { newIndex, oldIndex } = event;
    try {
      await moduleNavListSort({
        dragModuleId: moduleNavList.value[newIndex].id,
        end: newIndex + 1,
        start: oldIndex + 1,
      });
      Message.success(t('common.operationSuccess'));
      initModuleNavList();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  };
  // 改变icon
  function changeIcon(val: boolean) {
    appStore.setMenuIconStatus(val);
  }

  onMounted(() => {
    enable.value = appStore.getMenuIconStatus;
  });

  watch(
    () => appStore.orgId,
    (val) => {
      if (val) {
        initModuleNavList();
      }
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped>
  .config-container {
    width: 100%;
    background: var(--text-n9);
    @apply flex h-full w-full gap-4;
    .left-box {
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
    // input 和 input-number
    &.n-input--resizable,
    .n-input--resizable {
      margin-right: 8px;
      width: 96px;
    }
    // input-number
    .n-input__suffix .n-button {
      display: none;
    }
  }
</style>
