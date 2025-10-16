<template>
  <div class="crm-list-common-item" @click="emit('click', item)">
    <div class="crm-list-common-item-header">
      <div class="one-line-text text-[14px] font-semibold text-[var(--text-n1)]">{{ props.item.name }}</div>
      <CrmTag
        v-if="item.stageName && !props.hiddenStage"
        class="flex-shrink-0"
        :bg-color="getStage(item.stage)?.bgColor"
        :tag="item.stageName ?? ''"
        :text-color="getStage(item.stage)?.color"
      />
    </div>
    <div class="crm-list-common-item-content">
      <div
        v-for="desc of item.description"
        :key="desc.label"
        class="flex items-start gap-[8px]"
        :class="desc.fullLine ? 'basis-full' : ''"
      >
        <div class="whitespace-nowrap text-[12px] text-[var(--text-n4)]">{{ desc.label }}</div>
        <div class="break-words break-all text-[12px] text-[var(--text-n1)]">{{ desc.value }}</div>
      </div>
    </div>
    <van-divider v-if="actionList?.length" class="!m-0" />
    <div v-if="actionList?.length" class="crm-list-common-item-actions">
      <CrmTextButton
        v-for="btn of actionList"
        :key="btn.label"
        :text="btn.label"
        :icon="btn.icon"
        @click="btn.action(item)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';

  import useAppStore from '@/store/modules/app';
  import { hasAllPermission, hasAnyPermission } from '@/utils/permission';

  const appStore = useAppStore();

  export interface CrmListCommonItemActionsItem {
    label: string;
    icon: string;
    permission: string[];
    allPermission?: boolean;
    action: (data: any) => void;
  }
  const props = defineProps<{
    item: Record<string, any>;
    hiddenStage?: boolean;
    actions?: CrmListCommonItemActionsItem[];
  }>();

  const emit = defineEmits<{
    (e: 'click', item: Record<string, any>): void;
  }>();

  function stageStyle(type: 'success' | 'error' | 'info') {
    const map = {
      success: { bgColor: 'var(--success-5)', color: 'var(--success-green)' },
      error: { bgColor: 'var(--error-5)', color: 'var(--error-red)' },
      info: { bgColor: 'var(--info-5)', color: 'var(--info-blue)' },
    };
    return map[type];
  }

  function defaultStageStyle() {
    return stageStyle('info');
  }

  function getStage(stageId: string): Record<string, string> {
    const stage = appStore.originStageConfigList.find((item) => item.id === stageId);
    if (!stage) {
      return defaultStageStyle();
    }

    const { type, rate } = stage;
    if (type === 'END') {
      if (rate === '100') return stageStyle('success');
      if (rate === '0') return stageStyle('error');
    }

    return stageStyle('info');
  }

  const actionList = computed(() => {
    return props.actions?.filter((e) =>
      e.allPermission ? hasAllPermission(e.permission) : hasAnyPermission(e.permission)
    );
  });
</script>

<style lang="less" scoped>
  .crm-list-common-item {
    @apply flex flex-col;

    padding: 16px 20px;
    border-radius: var(--border-radius-small);
    background-color: var(--text-n10);
    gap: 8px;
    .crm-list-common-item-header,
    .crm-list-common-item-content {
      @apply flex items-center justify-between;
    }
    .crm-list-common-item-content {
      @apply flex-wrap;

      gap: 8px;
    }
    .crm-list-common-item-actions {
      @apply flex w-full items-center justify-between;

      gap: 8px;
    }
  }
</style>
