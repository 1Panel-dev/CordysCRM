<template>
  <div class="bg-[var(--text-n10)] p-[16px]">
    <n-spin :show="updateStageLoading">
      <WorkflowStep v-model:status="currentStatus" :workflow-list="workflowList">
        <template #action="{ currentStatusIndex }">
          <n-button
            v-if="props.showErrorBtn"
            type="error"
            ghost
            class="n-btn-outline-error mr-[12px]"
            @click="handleUpdateStatus(currentStatusIndex, true)"
          >
            {{ t('common.followFailed') }}
          </n-button>
          <n-button type="primary" @click="handleUpdateStatus(currentStatusIndex)">
            {{ t('common.updateToCurrentProgress') }}
          </n-button>
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
                <n-radio key="success" :value="StageResultEnum.SUCCESS">
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
  } from 'naive-ui';

  import { StageResultEnum } from '@lib/shared/enums/opportunityEnum';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import WorkflowStep from './workflowStep.vue';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const props = defineProps<{
    workflowList: SelectOption[];
    sourceId: string; // 资源id
    showErrorBtn?: boolean;
    showConfirmStatus?: boolean; // 是否二次确认更新成功 | 成败
    updateApi?: (params: { id: string; stage: string }) => Promise<any>;
  }>();

  const emit = defineEmits<{
    (e: 'loadDetail'): void;
  }>();

  const currentStatus = defineModel<string>('status', {
    required: true,
  });

  const updateStatusModal = ref<boolean>(false);

  const rules: FormRules = {
    status: [{ required: true, message: t('common.pleaseSelect') }],
  };

  const form = ref<{
    stage: string;
  }>({
    stage: 'SUCCESS',
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
  async function handleUpdateStatus(currentStatusIndex: number, isError = false) {
    if (props.showConfirmStatus && currentStatusIndex === props.workflowList.length - 2) {
      updateStatusModal.value = true;
      return;
    }

    const nextStage = isError ? StageResultEnum.FAIL : props.workflowList[currentStatusIndex + 1]?.value;
    await handleSave(nextStage as string);
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
