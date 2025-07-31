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
        :failure-reason="getFailureReason"
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
            class="mb-[16px]"
            :label="t('common.result')"
          >
            <n-radio-group v-model:value="form.stage" name="radiogroup">
              <n-space>
                <n-radio key="success" :value="StageResultEnum.SUCCESS" :disabled="isHasBackPermission">
                  {{ t('common.success') }}
                </n-radio>
                <n-radio key="fail" :value="StageResultEnum.FAIL">
                  {{ t('common.fail') }}
                </n-radio>
              </n-space>
            </n-radio-group>
          </n-form-item>
          <!-- TODO 先不要了 -->
          <!-- <n-form-item
            require-mark-placement="left"
            label-placement="left"
            path="expectedEndTime"
            :label="t('opportunity.endTime')"
            :rule="[{ required: true, message: t('common.notNull', { value: t('opportunity.endTime') }) }]"
          >
            <n-date-picker
              v-model:value="form.expectedEndTime"
              :default-value="Date.now()"
              class="w-full"
              type="date"
              clearable
            >
              <template #date-icon>
                <CrmIcon class="text-[var(--text-n4)]" type="iconicon_time" :size="16" />
              </template>
            </n-date-picker>
          </n-form-item> -->
          <n-form-item
            v-if="form.stage === StageResultEnum.FAIL && enableReason"
            require-mark-placement="left"
            label-placement="left"
            path="failureReason"
            :label="t('opportunity.failureReason')"
            :rule="[{ required: true, message: t('common.notNull', { value: t('opportunity.failureReason') }) }]"
          >
            <n-select
              v-model:value="form.failureReason"
              :options="reasonList"
              :placeholder="t('common.pleaseSelect')"
            />
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
    NSelect,
    NSpace,
    NSpin,
    SelectOption,
    useMessage,
  } from 'naive-ui';

  import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { StageResultEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { UpdateStageParams } from '@lib/shared/models/opportunity';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import type { Option } from '@/components/business/crm-select-user-drawer/type';
  import WorkflowStep from './workflowStep.vue';

  import { getReasonConfig } from '@/api/modules';
  import { hasAllPermission } from '@/utils/permission';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    baseSteps: SelectOption[]; // 基础步骤
    sourceId: string; // 资源id
    showErrorBtn?: boolean;
    showConfirmStatus?: boolean; // 是否二次确认更新成功 | 成败
    updateApi?: (params: UpdateStageParams) => Promise<any>;
    operationPermission?: string[];
    readonly?: boolean;
    isLimitBack?: boolean; // 是否限制状态往返
    backStagePermission?: string[];
    failureReason?: string;
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

  const reasonList = ref<Option[]>([]);
  const updateStatusModal = ref<boolean>(false);

  const rules: FormRules = {
    status: [{ required: true, message: t('common.pleaseSelect') }],
  };

  const isHasBackPermission = computed(
    () =>
      props.backStagePermission &&
      hasAllPermission(props.backStagePermission) &&
      currentStage.value === StageResultEnum.SUCCESS
  );

  const getInitForm = (): UpdateStageParams => ({
    id: '',
    stage: isHasBackPermission.value ? StageResultEnum.FAIL : StageResultEnum.SUCCESS,
    // expectedEndTime: Date.now(),
    failureReason: null,
  });

  const form = ref<UpdateStageParams>({
    ...getInitForm(),
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

  const getFailureReason = computed(() => reasonList.value.find((e) => e.value === props.failureReason)?.label);
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
    form.value = { ...getInitForm() };
    form.value.stage = isHasBackPermission.value ? StageResultEnum.FAIL : StageResultEnum.SUCCESS;
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
          // expectedEndTime:
          //   props.showConfirmStatus &&
          //   [StageResultEnum.FAIL, StageResultEnum.SUCCESS].includes(stage as StageResultEnum)
          //     ? form.value.expectedEndTime
          //     : undefined,
          failureReason:
            props.showConfirmStatus && stage === StageResultEnum.FAIL ? form.value.failureReason : undefined,
        });
        handleCancel();
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
      form.value.stage = isHasBackPermission.value ? StageResultEnum.FAIL : StageResultEnum.SUCCESS;
      return;
    }
    handleSave(stage);
  }

  // 确认更新
  const enableReason = ref(false);
  async function handleConfirm() {
    if (enableReason.value) {
      formRef.value?.validate(async (errors) => {
        if (!errors) {
          handleSave(form.value.stage);
        }
      });
    } else {
      handleSave(form.value.stage);
    }
  }

  async function initReason() {
    try {
      const { dictList, enable } = await getReasonConfig(ReasonTypeEnum.OPPORTUNITY_FAIL_RS);
      enableReason.value = enable;
      reasonList.value = dictList.map((e) => ({ label: e.name, value: e.id }));
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  onBeforeMount(() => {
    if (props.showConfirmStatus) {
      initReason();
    }
  });
</script>

<style lang="less" scoped></style>
