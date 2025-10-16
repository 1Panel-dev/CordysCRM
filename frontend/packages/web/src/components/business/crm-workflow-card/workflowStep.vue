<template>
  <n-scrollbar x-scrollable>
    <div class="crm-workflow-step">
      <div class="flex flex-1 gap-[16px]">
        <div
          v-for="(item, index) of workflowData"
          :key="item.value"
          :class="`crm-workflow-item`"
          @click="changeStage(item.value as string)"
        >
          <div class="crm-workflow-item-status" :class="statusClass(index, item)">
            <CrmIcon
              v-if="index < currentStatusIndex || item.value === failureStage"
              :type="item.value === failureStage ? 'iconicon_close' : 'iconicon_check'"
              :size="16"
            />
            <div v-else class="flex items-center justify-center">{{ index + 1 }} </div>
          </div>
          <div class="crm-workflow-item-name" :class="statusClass(index, item)">
            {{
              item.value === failureStage && props.failureReason
                ? `${item.label}（${props.failureReason}）`
                : item.label
            }}
          </div>
          <div
            v-if="index !== workflowData.length - 1"
            class="crm-workflow-item-line"
            :class="{
              'in-progress': index < currentStatusIndex,
            }"
          >
          </div>
        </div>
      </div>
      <slot
        v-if="currentStatusIndex !== workflowData.length - 1"
        name="action"
        :current-status-index="currentStatusIndex"
      >
      </slot>
    </div>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { NScrollbar, SelectOption } from 'naive-ui';

  import { StageConfigItem } from '@lib/shared/models/opportunity';

  import { hasAllPermission, hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    workflowList: SelectOption[];
    stageConfigList: StageConfigItem[]; // 阶段列表
    operationPermission?: string[];
    readonly?: boolean;
    isLimitBack?: boolean; // 是否限制状态往返
    backStagePermission?: string[];
    failureReason?: string;
    afootRollBack?: boolean; // 是否允许从跟进中回退
    endRollBack?: boolean; // 是否允许从成功或失败回退
  }>();

  const emit = defineEmits<{
    (e: 'change', value: string): void;
  }>();

  const currentStatus = defineModel<string>('status', {
    required: true,
  });

  const workflowData = computed(() => props.workflowList || []);
  const currentStatusIndex = computed(() => workflowData.value.findIndex((e) => e.value === currentStatus.value));
  const readonly = computed(() => props.readonly || !hasAnyPermission(props.operationPermission));
  const successStage = computed(
    () => props.stageConfigList.find((e) => e.type === 'END' && e.rate === '100')?.id || ''
  );
  const failureStage = computed(() => props.stageConfigList.find((e) => e.type === 'END' && e.rate === '0')?.id || '');

  const isDisabledStage = (stage: string) => {
    const isSameStage = currentStatus.value === stage;
    const isFailureStage = stage === failureStage.value;
    const isCurrentEndStage = currentStatus.value === successStage.value || currentStatus.value === failureStage.value;
    const hasPermission = props.backStagePermission && hasAllPermission(props.backStagePermission);

    // 获取当前阶段和目标阶段在流程中的索引
    const currentIndex = workflowData.value.findIndex((item) => item.value === currentStatus.value);
    const targetIndex = workflowData.value.findIndex((item) => item.value === stage);
    // 限制回退状态
    if (props.isLimitBack) {
      // 当前为成功状态，且目标为失败状态，需要返签权限
      if (currentStatus.value === successStage.value && isFailureStage) {
        return isSameStage || readonly.value || !hasPermission;
      }
      // 当前为完结状态，且目标是进行中状态，需要开启完结阶段回退
      if (currentStatus.value === successStage.value || currentStatus.value === failureStage.value) {
        return isSameStage || readonly.value || !props.endRollBack;
      }

      // 当前处于进行中阶段时的处理逻辑
      if (!isCurrentEndStage) {
        // 开启则不限制
        if (props.afootRollBack) {
          return isSameStage || readonly.value;
        }
        // 允许前进到当前阶段的后边的任意阶段 无论是进行中、成功或失败）
        return isSameStage || readonly.value || targetIndex < currentIndex;
      }
      // 不限制回退状态
      return isSameStage || readonly.value;
    }
    return false;
  };

  function statusClass(index: number, item: SelectOption) {
    return {
      'done': index < currentStatusIndex.value && item.value !== failureStage.value,
      'current': index === currentStatusIndex.value && item.value !== failureStage.value,
      'error': currentStatus.value === failureStage.value && item.value === failureStage.value,
      'cursor-pointer': !isDisabledStage(item.value as string),
    };
  }

  function changeStage(stage: string) {
    if (isDisabledStage(stage)) return;
    emit('change', stage);
  }
</script>

<style scoped lang="less">
  .crm-workflow-step {
    padding: 24px;
    border-radius: var(--border-radius-medium);
    background: var(--text-n9);
    gap: 24px;
    @apply flex;
    .crm-workflow-item {
      gap: 16px;
      @apply flex flex-nowrap items-center;
      .crm-workflow-item-status {
        width: 24px;
        height: 24px;
        line-height: 24px;
        font-size: 16px;
        border: 1px solid var(--text-n4);
        border-radius: 50%;
        color: var(--text-n4);
        @apply flex flex-shrink-0 items-center justify-center font-semibold;
        &.current {
          border-color: var(--primary-8);
          color: var(--text-n10);
          background: var(--primary-8);
        }
        &.done {
          border-color: var(--primary-8);
          color: var(--primary-8);
        }
        &.error {
          border-color: var(--error-red);
          color: var(--error-red);
        }
      }
      .crm-workflow-item-name {
        font-size: 16px;
        color: var(--text-n4);
        @apply break-keep font-medium;
        &.current {
          color: var(--primary-8);
          @apply font-medium;
        }
        &.done {
          border-color: var(--primary-8);
          color: var(--text-n1);
          @apply font-normal;
        }
        &.error {
          color: var(--error-red);
          @apply font-medium;
        }
      }
      .crm-workflow-item-line {
        width: auto;
        min-width: 18px;
        height: 2px;
        background: var(--text-n7);

        @apply flex-1;
        &.in-progress {
          background: var(--primary-8);
        }
      }
      &:first-child {
        .crm-workflow-item-line {
          width: 50px;
        }
      }
    }
  }
</style>
