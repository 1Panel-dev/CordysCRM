<template>
  <div class="bg-[var(--text-n10)] p-[16px]">
    <n-spin :show="updateStageLoading">
      <WorkflowStep
        v-model:status="currentStage"
        :readonly="props.readonly"
        :operation-permission="props.operationPermission"
        :workflow-list="workflowList"
        :is-limit-back="props.isLimitBack"
        :back-stage-permission="props.backStagePermission"
        @change="handleUpdateStatus"
      >
        <template v-if="!props.readonly" #action>
          <div v-permission="props.operationPermission" class="flex items-center">
            <n-button
              v-if="props.showErrorBtn"
              type="error"
              ghost
              class="n-btn-outline-error"
              @click="handleUpdateStatus(StageResultEnum.FAIL)"
            >
              {{ t('common.followFailed') }}
            </n-button>
          </div>
        </template>
      </WorkflowStep>

      <CrmModal
        v-model:show="updateStatusModal"
        :title="t('common.complete')"
        :ok-loading="updateStageLoading"
        size="small"
        @confirm="handleConfirm"
        @cancel="handleCancel"
      >
        <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" require-mark-placement="left">
          <n-form-item
            require-mark-placement="left"
            label-placement="left"
            path="stage"
            :show-feedback="false"
            :label="t('common.result')"
          >
            <n-radio-group v-model:value="form.stage" name="radiogroup">
              <n-space>
                <n-radio
                  key="success"
                  :value="StageResultEnum.SUCCESS"
                  :disabled="props.backStagePermission && hasAllPermission(props.backStagePermission)"
                >
                  {{ t('common.success') }}
                </n-radio>
                <n-radio key="fail" :value="StageResultEnum.FAIL">
                  {{ t('common.fail') }}
                </n-radio>
              </n-space>
            </n-radio-group>
          </n-form-item>
        </n-form>
      </CrmModal>
    </n-spin>
  </div>
</template>

<script lang="ts" setup>
  import {
    FormInst,
    FormRules,
    NButton,
    NForm,
    NFormItem,
    NRadio,
    NRadioGroup,
    NSpace,
    NSpin,
    SelectOption,
    useMessage,
  } from 'naive-ui';

  import { StageResultEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import WorkflowStep from './workflowStep.vue';

  import { hasAllPermission } from '@/utils/permission';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    baseSteps: SelectOption[]; // 基础步骤
    sourceId: string; // 资源id
    showErrorBtn?: boolean;
    showConfirmStatus?: boolean; // 是否二次确认更新成功 | 成败
    updateApi?: (params: { id: string; stage: string }) => Promise<any>;
    operationPermission?: string[];
    readonly?: boolean;
    isLimitBack?: boolean; // 是否限制状态往返
    backStagePermission?: string[];
  }>();

  const emit = defineEmits<{
    (e: 'loadDetail'): void;
  }>();

  const currentStage = defineModel<string>('stage', {
    required: true,
  });

  const lastStage = defineModel<string>('lastStage', {
    required: true,
  });

  const updateStatusModal = ref<boolean>(false);

  const rules: FormRules = {
    status: [{ required: true, message: t('common.pleaseSelect') }],
  };

  const form = ref<{
    stage: string;
  }>({
    stage:
      props.backStagePermission && hasAllPermission(props.backStagePermission)
        ? StageResultEnum.FAIL
        : StageResultEnum.SUCCESS,
  });

  const endStage = computed<SelectOption[]>(() => {
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

  const workflowList = computed<SelectOption[]>(() => {
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

  function handleCancel() {
    updateStatusModal.value = false;
    form.value.stage = StageResultEnum.SUCCESS;
  }

  const formRef = ref<FormInst | null>(null);
  const updateStageLoading = ref(false);

  async function handleSave(stage: string) {
    try {
      updateStageLoading.value = true;
      if (props.updateApi) {
        await props.updateApi({
          id: props.sourceId,
          stage,
        });
        Message.success(t('common.updateSuccess'));
        emit('loadDetail');
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      updateStageLoading.value = false;
    }
  }

  // 更新状态
  async function handleUpdateStatus(stage: string) {
    if (props.showConfirmStatus && stage === workflowList.value[workflowList.value.length - 1].value) {
      updateStatusModal.value = true;
      return;
    }
    await handleSave(stage);
  }

  // 确认更新
  async function handleConfirm() {
    formRef.value?.validate(async (errors) => {
      if (!errors) {
        await handleSave(form.value.stage);
        handleCancel();
      }
    });
  }
</script>

<style lang="less" scoped></style>
