<template>
  <div class="crm-workflow-step">
    <div class="flex flex-1 gap-[16px]">
      <div
        v-for="(item, index) of workflowData"
        :key="item.value"
        :class="`crm-workflow-item ${index === workflowData.length - 1 ? '' : 'flex-1'}`"
        @click="changeStage(item.value as string)"
      >
        <div class="crm-workflow-item-status" :class="statusClass(index, item)">
          <CrmIcon
            v-if="index < currentStatusIndex || item.value === StageResultEnum.FAIL"
            :type="item.value === StageResultEnum.FAIL ? 'iconicon_close' : 'iconicon_check'"
            :size="16"
          />
          <div v-else class="flex items-center justify-center">{{ index + 1 }} </div>
        </div>
        <div class="crm-workflow-item-name" :class="statusClass(index, item)">
          {{ item.label }}
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
</template>

<script setup lang="ts">
  import { SelectOption } from 'naive-ui';

  import { StageResultEnum } from '@lib/shared/enums/opportunityEnum';

  import { hasAllPermission, hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    workflowList: SelectOption[];
    operationPermission?: string[];
    readonly?: boolean;
    isLimitBack?: boolean; // 是否限制状态往返
    backStagePermission?: string[];
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

  const isDisabledStage = (stage: string) => {
    const isLastStage =
      currentStatusIndex.value === workflowData.value.length - 1 &&
      stage === workflowData.value[workflowData.value.length - 1].value;

    const isSameStage = currentStatus.value === stage;

    const isSuccessOrFail = [StageResultEnum.SUCCESS, StageResultEnum.FAIL].includes(
      currentStatus.value as StageResultEnum
    );
    const isSuccessStage = [StageResultEnum.SUCCESS].includes(stage as StageResultEnum);
    // 限制回退状态
    if (props.isLimitBack) {
      // 当前状态为成功和失败判断是否有高阶权限且不能操作非成功失败阶段的状态
      if (isSuccessOrFail) {
        const hasPermission = props.backStagePermission && hasAllPermission(props.backStagePermission);
        if (!hasPermission) return true;
        if (hasPermission && !isSuccessStage) return true;
        // 非当前状态和仅读状态
      } else {
        return isSameStage || readonly.value;
      }
      // 不限制回退状态
    } else {
      return isSameStage || readonly.value || isLastStage;
    }
    return false;
  };

  function statusClass(index: number, item: SelectOption) {
    return {
      'done': index < currentStatusIndex.value && item.value !== StageResultEnum.FAIL,
      'current': index === currentStatusIndex.value && item.value !== StageResultEnum.FAIL,
      'error': item.value === StageResultEnum.FAIL,
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
        @apply font-medium;
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
        height: 2px;
        background: var(--text-n7);

        @apply flex-1;
        &.in-progress {
          background: var(--primary-8);
        }
      }
    }
  }
</style>
