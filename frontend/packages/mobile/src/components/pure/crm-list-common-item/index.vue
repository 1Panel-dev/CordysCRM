<template>
  <div class="crm-list-common-item" @click="emit('click', item)">
    <div class="crm-list-common-item-header">
      <div class="one-line-text min-w-0 flex-1 text-[14px] font-semibold text-[var(--text-n1)]">
        {{ props.item.name }}
      </div>
      <div class="flex shrink-0 items-center gap-[8px]">
        <CrmTag
          v-if="item.stageName && !props.hiddenStage"
          class="flex-shrink-0"
          :bg-color="getStage(item.stage)?.bgColor"
          :tag="item.stageName ?? ''"
          :text-color="getStage(item.stage)?.color"
        />
        <ApprovalStatus v-if="item.approvalStatus" :status="item.approvalStatus" />
        <CrmTag
          v-if="item.frozen"
          bg-color="var(--warning-5)"
          text-color="var(--warning-yellow)"
          :tag="t('common.frozen')"
          @click.stop="frozenPopupShow = true"
        />
      </div>
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
        v-for="btn of visibleActionList"
        :key="btn.label"
        :text="btn.label"
        :icon="btn.icon"
        @click="btn.action(item)"
      />
      <van-popover
        v-if="moreActionList.length"
        v-model:show="showActionPopover"
        placement="top-end"
        :actions="morePopoverActions"
        @select="handleMoreActionSelect"
      >
        <template #reference>
          <CrmTextButton icon="iconicon_ellipsis" :text="t('common.more')" @click="showActionPopover = true" />
        </template>
      </van-popover>
    </div>
  </div>
  <van-popup v-model:show="frozenPopupShow" position="bottom" class="rounded-[12px_12px_0_0]" closeable>
    <div class="mb-[16px] flex w-full flex-col items-center gap-[8px] px-[16px]">
      <div class="flex items-center gap-[4px] p-[16px]">
        <CrmIcon name="iconicon_info_circle_filled" color="var(--warning-yellow)" width="16px" height="16px" />
        <div class="font-semibold">
          {{ `${props.resourceType === 'customer' ? t('menu.customer') : t('menu.clue')}${t('common.frozen')}` }}
        </div>
      </div>
      <div class="w-full break-words text-left">{{ item.freezeReason }}</div>
      <div class="flex w-full items-center justify-between text-[12px]">
        <div class="text-[var(--text-n4)]">
          {{ t('common.unfreezeTime') }}
        </div>
        {{ item.unfreezeTime ? dayjs(item.unfreezeTime).format('YYYY-MM-DD HH:mm:ss') : t('common.freezeForever') }}
      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';

  import ApprovalStatus from '@/components/business/crm-approval/crm-approval-status.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import useAppStore from '@/store/modules/app';
  import { hasAllPermission, hasAnyPermission } from '@/utils/permission';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import dayjs from 'dayjs';

  const appStore = useAppStore();
  const { t } = useI18n();

  export interface CrmListCommonItemActionsItem {
    key?: string;
    label: string;
    icon: string;
    permission?: string[];
    allPermission?: boolean;
    action: (data: any) => void;
  }
  const props = defineProps<{
    item: Record<string, any>;
    hiddenStage?: boolean;
    actions?: CrmListCommonItemActionsItem[];
    resourceType?: 'customer' | 'lead';
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
    return (props.actions || []).filter((e) => {
      if (props.item.frozen && ['pick', 'distribute'].includes(e.key || '')) {
        return false;
      }

      if (!e.permission?.length) {
        return true;
      }

      return e.allPermission ? hasAllPermission(e.permission) : hasAnyPermission(e.permission);
    });
  });

  const visibleActionList = computed(() => {
    if (actionList.value.length <= 4) {
      return actionList.value;
    }
    return actionList.value.slice(0, 3);
  });

  const moreActionList = computed(() => {
    if (actionList.value.length <= 4) {
      return [];
    }
    return actionList.value.slice(3);
  });

  const morePopoverActions = computed(() => moreActionList.value.map((action) => ({ ...action, text: action.label })));

  const showActionPopover = ref(false);
  const frozenPopupShow = ref(false);

  function handleMoreActionSelect(action: CrmListCommonItemActionsItem) {
    showActionPopover.value = false;
    action.action(props.item);
  }
</script>

<style lang="less" scoped>
  .crm-list-common-item {
    @apply flex flex-col;

    padding: 16px 20px;
    border-radius: var(--border-radius-small);
    background-color: var(--text-n10);
    gap: 8px;
    .crm-list-common-item-header {
      @apply flex items-center;

      gap: 8px;
    }
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
