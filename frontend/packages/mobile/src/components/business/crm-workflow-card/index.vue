<template>
  <div class="crm-workflow-wrapper">
    <div class="flex items-center justify-between">
      <div class="font-semibold text-[var(--text-n1)]"> {{ props.title }}</div>
      <div
        v-if="
          currentStageIndex !== workflowList.length - 1 &&
          hasAnyPermission(props.operationPermission) &&
          !props.readonly
        "
        class="flex items-center gap-[8px]"
      >
        <van-button
          v-if="props.showErrorBtn"
          plain
          type="danger"
          size="small"
          @click="handleUpdateStage(StageResultEnum.FAIL)"
        >
          {{ t('common.followFailed') }}
        </van-button>
      </div>
    </div>
    <div class="crm-workflow-step overflow-x-auto">
      <div
        v-for="(item, index) of workflowList"
        :key="item.value"
        :class="`crm-workflow-item ${index === workflowList.length - 1 ? '' : 'flex-1'}`"
      >
        <div class="relative -left-[16px] flex flex-nowrap items-center justify-center">
          <div
            class="crm-workflow-item-line"
            :class="{
              'in-progress': index <= currentStageIndex,
              'invisible': index === 0,
              'visible': index !== 0,
            }"
          >
          </div>
          <div
            class="crm-workflow-item-status mx-[8px]"
            :class="statusClass(index, item)"
            @click="handleUpdateStage(item.value)"
          >
            <CrmIcon
              v-if="index < currentStageIndex || item.value === StageResultEnum.FAIL"
              :name="item.value === StageResultEnum.FAIL ? 'iconicon_close' : 'iconicon_check'"
              width="16px"
              height="16px"
              :color="item.value === StageResultEnum.FAIL ? 'var(--error-red)' : 'var(--primary-8)'"
            />
            <div v-else class="flex items-center justify-center">
              {{ index + 1 }}
            </div>
          </div>
          <div
            class="crm-workflow-item-line"
            :class="{
              'in-progress': index < currentStageIndex,
              'invisible': index === workflowList.length - 1,
              'visible': index !== workflowList.length - 1,
            }"
          >
          </div>
        </div>
        <div class="crm-workflow-item-name one-line-text relative -left-[16px]" :class="statusClass(index, item)">
          {{ item.label }}
        </div>
      </div>
    </div>
  </div>
  <van-popup v-model:show="showPopConfirm" destroy-on-close position="bottom">
    <van-picker
      v-model="formStage"
      :columns="resultColumns"
      @confirm="onConfirm"
      @cancel="() => (showPopConfirm = false)"
    />
  </van-popup>
</template>

<script setup lang="ts">
  import { closeToast, PickerOption, showLoadingToast, showSuccessToast } from 'vant';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { StageResultEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { updateClueStatus, updateOptStage } from '@/api/modules';
  import { hasAllPermission, hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();

  export interface Options {
    value: string;
    label: string;
  }

  export type WorkStageTypeKey = FormDesignKeyEnum.BUSINESS | FormDesignKeyEnum.CLUE;

  const props = defineProps<{
    title: string;
    baseSteps: Options[]; // 基础步骤
    sourceId: string; // 资源id
    showConfirmStatus?: boolean; // 是否二次确认更新成功 | 成败
    formStageKey: WorkStageTypeKey;
    operationPermission?: string[];
    showErrorBtn?: boolean;
    readonly?: boolean;
    isLimitBack?: boolean; // 是否限制状态往返
    backStagePermission?: string[];
  }>();

  const emit = defineEmits<{
    (e: 'loadDetail'): void;
  }>();

  const currentStage = defineModel<string>('stage');

  const lastStage = defineModel<string>('lastStage');

  const updateStageApi = {
    [FormDesignKeyEnum.BUSINESS]: updateOptStage,
    [FormDesignKeyEnum.CLUE]: updateClueStatus,
  };

  const endStage = computed<Options[]>(() => {
    if (currentStage.value === StageResultEnum.SUCCESS) {
      return [
        {
          value: StageResultEnum.SUCCESS,
          label: t('common.success'),
        },
      ];
    }

    if (currentStage.value === StageResultEnum.FAIL) {
      return [
        {
          value: StageResultEnum.FAIL,
          label: t('common.fail'),
        },
      ];
    }
    return [];
  });

  const workflowList = computed<Options[]>(() => {
    // 失败返回基础阶段截止当前 + 失败阶段
    if (currentStage.value === StageResultEnum.FAIL) {
      const lastStageIndex = props.baseSteps.findIndex((e) => e.value === lastStage.value);
      return [...props.baseSteps.slice(0, lastStageIndex + 1), ...endStage.value];
    }
    // 成功返回全部阶段
    if (currentStage.value === StageResultEnum.SUCCESS) {
      return [...props.baseSteps.slice(0, props.baseSteps.length - 1), ...endStage.value];
    }
    // 其他返回基础阶段
    return [...props.baseSteps];
  });

  const currentStageIndex = computed(() => workflowList.value.findIndex((e) => e.value === currentStage.value));

  function statusClass(index: number, item: Options) {
    return {
      done: index < currentStageIndex.value && item.value !== StageResultEnum.FAIL,
      current: index === currentStageIndex.value && item.value !== StageResultEnum.FAIL,
      error: item.value === StageResultEnum.FAIL,
    };
  }

  const resultColumns: PickerOption[] = [
    { text: t('common.success'), value: StageResultEnum.SUCCESS },
    { text: t('common.fail'), value: StageResultEnum.FAIL },
  ];

  const formStage = ref<StageResultEnum[]>([StageResultEnum.SUCCESS]);

  const showPopConfirm = ref(false);

  async function handleSave(stage: string) {
    try {
      showLoadingToast(t('common.updating'));
      await updateStageApi[props.formStageKey]({
        id: props.sourceId,
        stage,
      });
      showSuccessToast(t('common.operationSuccess'));
      emit('loadDetail');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      closeToast();
    }
  }

  async function onConfirm() {
    const [nextStage] = formStage.value;
    await handleSave(nextStage);
    showPopConfirm.value = false;
  }

  const readonly = computed(() => props.readonly || !hasAnyPermission(props.operationPermission));

  const isDisabledStage = (stage: string) => {
    const isLastStage =
      currentStageIndex.value === workflowList.value.length - 1 &&
      stage === workflowList.value[workflowList.value.length - 1].value;

    const isSameStage = currentStage.value === stage;

    const isSuccessOrFail = [StageResultEnum.SUCCESS, StageResultEnum.FAIL].includes(
      currentStage.value as StageResultEnum
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

  // 更新状态
  async function handleUpdateStage(stage: string) {
    if (isDisabledStage(stage)) return;

    if (props.showConfirmStatus && stage === workflowList.value[workflowList.value.length - 1].value) {
      showPopConfirm.value = true;
      return;
    }
    await handleSave(stage);
  }
</script>

<style scoped lang="less">
  .crm-workflow-wrapper {
    padding: 16px;
    border-radius: @border-radius-large;
    background: var(--text-n10);
    .crm-workflow-step {
      padding: 16px 0;
      border-radius: var(--border-radius-medium);

      @apply flex flex-nowrap justify-start;
      .crm-workflow-item {
        gap: 8px;
        @apply flex flex-col;
        .crm-workflow-item-status {
          width: 22px;
          height: 22px;
          line-height: 22px;
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
            border-color: var(--primary-7);
            color: var(--primary-8);
            background: var(--primary-7);
          }
          &.error {
            border-color: var(--error-red);
            color: var(--error-red);
          }
        }
        .crm-workflow-item-name {
          min-width: 78px;
          color: var(--text-n4);
          @apply flex w-auto items-center justify-center font-medium;
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
          min-width: 20px;
          height: 2px;
          background: var(--text-n7);

          @apply flex-1;
          &.in-progress {
            background: var(--primary-8);
          }
        }
      }
    }
  }
</style>
