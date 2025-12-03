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
        <van-button v-if="props.showErrorBtn" plain type="danger" size="small" @click="handleUpdateStage(failureStage)">
          {{ t('common.followFailed') }}
        </van-button>
      </div>
    </div>
    <div class="crm-workflow-step overflow-x-auto">
      <div
        v-for="(item, index) of workflowList"
        :key="item.value"
        :class="`crm-workflow-item ${index === workflowList.length - 1 ? '' : 'flex-1'} ${
          workflowList.length === 1 ? 'pl-[16px]' : ''
        }`"
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
              v-if="index < currentStageIndex || item.value === failureStage"
              :name="item.value === failureStage ? 'iconicon_close' : 'iconicon_check'"
              width="16px"
              height="16px"
              :color="
                item.value === failureStage && currentStage === failureStage ? 'var(--error-red)' : 'var(--primary-8)'
              "
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
          {{
            item.value === failureStage && props.failureReason ? `${item.label}（${props.failureReason}）` : item.label
          }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { closeToast, showLoadingToast, showSuccessToast } from 'vant';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { StageConfigItem } from '@lib/shared/models/opportunity';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { getReasonConfig, updateClueStatus, updateOptStage } from '@/api/modules';
  import { hasAllPermission, hasAnyPermission } from '@/utils/permission';

  import { CommonRouteEnum } from '@/enums/routeEnum';
  import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';

  const { t } = useI18n();
  const router = useRouter();
  export interface Options {
    value: string;
    label: string;
  }

  export type WorkStageTypeKey = FormDesignKeyEnum.BUSINESS | FormDesignKeyEnum.CLUE;

  const props = defineProps<{
    title: string;
    stageConfigList: StageConfigItem[]; // 阶段列表
    sourceId: string; // 资源id
    showConfirmStatus?: boolean; // 是否二次确认更新成功 | 成败
    formStageKey: WorkStageTypeKey;
    operationPermission?: string[];
    showErrorBtn?: boolean;
    readonly?: boolean;
    isLimitBack?: boolean; // 是否限制状态往返
    backStagePermission?: string[];
    failureReason?: string; // 失败原因
    afootRollBack?: boolean; // 是否允许从跟进中回退
    endRollBack?: boolean; // 是否允许从成功或失败回退
  }>();

  const emit = defineEmits<{
    (e: 'loadDetail'): void;
  }>();

  const currentStage = defineModel<string>('stage');

  const updateStageApi = {
    [FormDesignKeyEnum.BUSINESS]: updateOptStage,
    [FormDesignKeyEnum.CLUE]: updateClueStatus,
  };

  const workflowList = computed<Options[]>(() => {
    return props.stageConfigList.map((item) => ({
      label: item.name,
      value: item.id,
    }));
  });
  const enableReason = ref(false);
  const currentStageIndex = computed(() => workflowList.value.findIndex((e) => e.value === currentStage.value));
  const failureStage = computed(() => props.stageConfigList.find((e) => e.type === 'END' && e.rate === '0')?.id || '');
  const successStage = computed(
    () => props.stageConfigList.find((e) => e.type === 'END' && e.rate === '100')?.id || ''
  );

  function statusClass(index: number, item: Options) {
    return {
      done: index < currentStageIndex.value && item.value !== failureStage.value,
      current: index === currentStageIndex.value && item.value !== failureStage.value,
      error: currentStage.value === failureStage.value && item.value === failureStage.value,
    };
  }

  const isHasBackPermission = computed(
    () =>
      props.backStagePermission &&
      hasAllPermission(props.backStagePermission) &&
      currentStage.value === successStage.value
  );

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

  const readonly = computed(() => props.readonly || !hasAnyPermission(props.operationPermission));

  const isDisabledStage = (stage: string) => {
    const isSameStage = currentStage.value === stage;
    const isFailureStage = stage === failureStage.value;
    const isCurrentEndStage = currentStage.value === successStage.value || currentStage.value === failureStage.value;

    const hasPermission = props.backStagePermission && hasAllPermission(props.backStagePermission);

    // 获取当前阶段和目标阶段在流程中的索引
    const currentIndex = workflowList.value.findIndex((item) => item.value === currentStage.value);
    const targetIndex = workflowList.value.findIndex((item) => item.value === stage);
    // 限制回退状态
    if (props.isLimitBack) {
      // 当前为成功状态，且目标为失败状态，需要返签权限
      if (currentStage.value === successStage.value && isFailureStage) {
        return isSameStage || readonly.value || !hasPermission;
      }
      // 当前为完结状态，且目标是进行中状态，需要开启完结阶段回退
      if (currentStage.value === successStage.value || currentStage.value === failureStage.value) {
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
    } else {
      // 不限制回退状态
      return isSameStage || readonly.value;
    }
    return false;
  };

  // 更新状态
  async function handleUpdateStage(stage: string) {
    if (isDisabledStage(stage)) return;

    if (
      props.showConfirmStatus &&
      stage === workflowList.value[workflowList.value.length - 1].value &&
      enableReason.value
    ) {
      router.push({
        name: CommonRouteEnum.WORKFLOW_STAGE,
        query: {
          id: props.sourceId,
          type: props.formStageKey,
          lastName: workflowList.value[workflowList.value.length - 1].label,
          isHasBack: isHasBackPermission.value ? 'Y' : 'N',
        },
      });
      return;
    }
    await handleSave(stage);
  }

  async function initReason() {
    try {
      const res = await getReasonConfig(ReasonTypeEnum.OPPORTUNITY_FAIL_RS);
      enableReason.value = res.enable;
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  onBeforeMount(() => {
    initReason();
  });
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
