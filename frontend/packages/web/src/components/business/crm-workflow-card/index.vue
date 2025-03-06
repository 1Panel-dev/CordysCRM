<template>
  <div class="bg-[var(--text-n10)] p-[16px]">
    <WorkflowStep v-model:status="currentStatus" :workflow-list="workflowList">
      <template #action="{ currentStatusIndex }">
        <n-button v-if="props.showErrorBtn" type="error" ghost class="n-btn-outline-error">
          {{ t('common.followFailed') }}
        </n-button>
        <n-button
          class="mr-[12px]"
          type="primary"
          :loading="updateStageLoading"
          @click="handleUpdateStatus(currentStatusIndex)"
        >
          {{ t('common.updateToCurrentProgress') }}
        </n-button>
      </template>
    </WorkflowStep>

    <CrmModal
      v-model:show="updateStatusModal"
      :title="t('common.complete')"
      :ok-loading="loading"
      size="small"
      @confirm="handleConfirm"
      @cancel="handleCancel"
    >
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" require-mark-placement="left">
        <n-form-item
          require-mark-placement="left"
          label-placement="left"
          path="status"
          :show-feedback="false"
          :label="t('common.result')"
        >
          <n-radio-group v-model:value="form.status" name="radiogroup">
            <n-space>
              <n-radio key="success" :value="true">
                {{ t('common.success') }}
              </n-radio>
              <n-radio key="fail" :value="false">
                {{ t('common.fail') }}
              </n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>
      </n-form>
    </CrmModal>
  </div>
</template>

<script lang="ts" setup>
  import { FormInst, FormRules, NButton, NForm, NFormItem, NRadio, NRadioGroup, NSpace } from 'naive-ui';

  import type { WorkflowStepItem } from '@lib/shared/models/opportunity';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import WorkflowStep from './workflowStep.vue';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const props = defineProps<{
    workflowList: WorkflowStepItem[];
    sourceId: string; // 资源id
    showErrorBtn?: boolean;
    showConfirmStatus?: boolean; // 是否二次确认更新成功 | 成败
    saveApi?: (status: boolean) => Promise<any>; // TODO 类型
  }>();

  const emit = defineEmits<{
    (e: 'loadList'): void;
  }>();

  const currentStatus = defineModel<string>('status', {
    required: true,
  });

  const updateStatusModal = ref<boolean>(false);

  const rules: FormRules = {
    status: [{ required: true, message: t('common.pleaseSelect') }],
  };

  const form = ref<{
    status: boolean;
  }>({
    status: true,
  });

  function handleCancel() {
    updateStatusModal.value = false;
    form.value.status = true;
  }

  const formRef = ref<FormInst | null>(null);

  async function executeWithLoading(cb: () => Promise<void>, loadingRef: Ref<boolean>) {
    try {
      loadingRef.value = true;
      await cb();
      emit('loadList');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loadingRef.value = false;
    }
  }

  async function handleSave(status: boolean) {
    try {
      if (props.saveApi) {
        // TODO 等待联调
        await props.saveApi(status);
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  // 更新状态
  const updateStageLoading = ref(false);
  async function handleUpdateStatus(currentStatusIndex: number) {
    if (props.showConfirmStatus && currentStatusIndex === props.workflowList.length - 2) {
      updateStatusModal.value = true;
      return;
    }
    await executeWithLoading(() => handleSave(true), updateStageLoading);
  }

  // 确认更新
  const loading = ref(false);
  async function handleConfirm() {
    formRef.value?.validate(async (errors) => {
      if (!errors) {
        await executeWithLoading(() => handleSave(form.value.status), loading);
        handleCancel();
      }
    });
  }
</script>

<style lang="less" scoped></style>
