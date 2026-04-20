<template>
  <CrmDrawer
    v-model:show="visible"
    width="75%"
    :footer="false"
    :closable="false"
    :close-on-esc="false"
    :loading="loading"
    header-class="crm-process-drawer-header"
    body-content-class="!p-0"
    @cancel="handleCancel"
  >
    <template #header>
      <div class="crm-process-drawer-header-content">
        <div class="crm-process-drawer-header-item flex items-center">
          <n-button text class="mr-[4px] w-[32px]" @click="handleCancel">
            <n-icon size="16">
              <ChevronBackOutline />
            </n-icon>
          </n-button>
          <div class="one-line-text flex flex-1 items-center gap-[8px]">
            <n-tooltip trigger="hover" :delay="300" :disabled="!props.title">
              <template #trigger>
                <div class="one-line-text !leading-[20px]"> {{ props.title ?? '-' }}</div>
              </template>
              {{ props.title ?? '-' }}
            </n-tooltip>
          </div>
        </div>
        <div class="flex justify-center">
          <CrmTab
            v-model:active-tab="activeTab"
            class="-mb-[9px] flex"
            no-content
            :tab-list="props.tabList"
            type="line"
          />
        </div>
        <div class="crm-process-drawer-header-item flex justify-end gap-[12px]">
          <slot name="headerActions">
            <n-button type="primary" ghost class="n-btn-outline-primary" @click="handleCancel">
              {{ t('common.cancel') }}
            </n-button>
            <n-button
              type="primary"
              ghost
              class="n-btn-outline-primary"
              :disabled="activeTab === props.tabList[props.tabList.length - 1].name"
              @click="() => emit('nextStep')"
            >
              {{ t('common.nextStep') }}
            </n-button>
            <n-button type="primary" @click="() => emit('save')">
              {{ t('common.save') }}
            </n-button>
          </slot>
        </div>
      </div>
    </template>
    <div class="h-full">
      <slot></slot>
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { watchEffect } from 'vue';
  import { NButton, NIcon, NTooltip } from 'naive-ui';
  import { ChevronBackOutline } from '@vicons/ionicons5';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTab, { CrmTabListItem } from '@/components/pure/crm-tab/index.vue';

  const { t } = useI18n();

  const props = defineProps<{
    tabList: CrmTabListItem[];
    title?: string;
  }>();

  const emit = defineEmits<{
    (e: 'save'): void;
    (e: 'nextStep'): void;
    (e: 'cancel'): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const activeTab = defineModel<string | number>('activeTab', {
    default: '',
  });

  const loading = ref(false);

  function handleCancel() {
    emit('cancel');
    visible.value = false;
  }

  watchEffect(() => {
    if (!activeTab.value && props.tabList.length) {
      activeTab.value = props.tabList[0].name as string | number;
    }
  });
</script>

<style scoped lang="less">
  .crm-process-drawer-header-content {
    padding: 8px;
    box-sizing: border-box;
    @apply flex items-center;
    .crm-process-drawer-header-item {
      @apply flex flex-1;
    }
  }
</style>

<style lang="less">
  .crm-process-drawer-header {
    padding: 0 16px !important;
    .n-drawer-header__main {
      max-width: 100%;
    }
  }
</style>
