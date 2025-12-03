<template>
  <div class="bg-[var(--text-n10)] p-[16px]">
    <n-spin :show="updateStageLoading">
      <WorkflowStep
        v-model:status="currentStage"
        :readonly="props.readonly"
        :operation-permission="props.operationPermission"
        :workflow-list="workflowList"
        :stage-config-list="stageConfigList"
        :is-limit-back="props.isLimitBack"
        :back-stage-permission="props.backStagePermission"
        :failure-reason="getFailureReason"
        :afoot-roll-back="props.afootRollBack"
        :end-roll-back="props.endRollBack"
        @change="handleUpdateStatus"
      >
        <template v-if="!props.readonly" #action>
          <div v-permission="props.operationPermission" class="flex items-center">
            <n-button
              v-if="props.showErrorBtn"
              type="error"
              ghost
              class="n-btn-outline-error"
              @click="handleUpdateStatus(failureStage)"
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
  import { FormInst, FormRules, NButton, NForm, NFormItem, NSelect, NSpin, SelectOption, useMessage } from 'naive-ui';

  import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { StageConfigItem, UpdateStageParams } from '@lib/shared/models/opportunity';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import type { Option } from '@/components/business/crm-select-user-drawer/type';
  import WorkflowStep from './workflowStep.vue';

  import { getReasonConfig } from '@/api/modules';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    stageConfigList: StageConfigItem[]; // 阶段列表
    sourceId: string; // 资源id
    showErrorBtn?: boolean;
    showConfirmStatus?: boolean; // 是否二次确认更新成功 | 成败
    updateApi?: (params: UpdateStageParams) => Promise<any>;
    operationPermission?: string[];
    readonly?: boolean;
    isLimitBack?: boolean; // 是否限制状态往返
    backStagePermission?: string[];
    failureReason?: string;
    afootRollBack?: boolean; // 是否允许从跟进中回退
    endRollBack?: boolean; // 是否允许从成功或失败回退
  }>();

  const emit = defineEmits<{
    (e: 'loadDetail'): void;
  }>();

  const currentStage = defineModel<string>('stage', {
    required: true,
  });

  const reasonList = ref<Option[]>([]);
  const updateStatusModal = ref<boolean>(false);

  const rules: FormRules = {
    status: [{ required: true, message: t('common.pleaseSelect') }],
  };
  const failureStage = computed(() => props.stageConfigList.find((e) => e.type === 'END' && e.rate === '0')?.id || '');

  const form = ref<UpdateStageParams>({
    id: '',
    stage: failureStage.value,
    failureReason: null,
  });

  const getFailureReason = computed(() => reasonList.value.find((e) => e.value === props.failureReason)?.label);
  const workflowList = computed<SelectOption[]>(() => {
    return props.stageConfigList.map((item) => ({
      label: item.name,
      value: item.id,
    }));
  });

  function handleCancel() {
    updateStatusModal.value = false;
    form.value = { id: '', stage: failureStage.value, failureReason: null };
    form.value.stage = failureStage.value;
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
          failureReason: props.showConfirmStatus && stage === failureStage.value ? form.value.failureReason : undefined,
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

  const enableReason = ref(false);
  // 更新状态
  async function handleUpdateStatus(stage: string) {
    if (
      props.showConfirmStatus &&
      stage === workflowList.value[workflowList.value.length - 1].value &&
      enableReason.value
    ) {
      updateStatusModal.value = true;
      form.value.stage = failureStage.value;
      return;
    }
    handleSave(stage);
  }
  // 确认更新
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
