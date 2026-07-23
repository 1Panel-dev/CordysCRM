<template>
  <van-popup
    v-model:show="show"
    position="bottom"
    :style="{ height: '100vh' }"
    safe-area-inset-top
    safe-area-inset-bottom
  >
    <div class="relative p-[16px] text-center">
      <CrmTextButton
        icon="iconicon_chevron_left"
        icon-size="24px"
        color="var(--text-n1)"
        class="absolute left-[12px] top-[16px]"
        @click="show = false"
      />
      <div class="text-[16px] font-semibold">{{ t('workbench.operation.BACK') }}</div>
    </div>
    <van-form ref="fallbackFormRef" label-align="top" @submit="">
      <van-field
        v-model="fieldValue"
        is-link
        readonly
        :label="t('workbench.fallbackTo')"
        :placeholder="t('common.pleaseSelect')"
        label-align="left"
        required
        :rules="[
          {
            required: approvalConfig?.requireComment,
            message: t('common.notNull', {
              value: t('workbench.fallbackTo'),
            }),
          },
        ]"
        @click="showPicker = true"
      />
      <van-popup v-model:show="showPicker" destroy-on-close round position="bottom">
        <van-picker
          v-model:model-value="pickerValue"
          :columns="props.fallbackOptions"
          @cancel="showPicker = false"
          @confirm="
            () => {
              showPicker = false;
              fieldValue = props.fallbackOptions.find((e) => e.value === pickerValue[0])?.text?.toString() || '';
              fallbackForm.node = pickerValue[0];
            }
          "
        />
      </van-popup>
      <van-field
        name="reason"
        :label="t('workbench.fallbackReason')"
        :placeholder="t('common.pleaseInput')"
        v-model="fallbackForm.reason"
        autosize
        show-word-limit
        rows="2"
        type="textarea"
        maxlength="300"
      />
      <CrmUploadFile
        v-model:value="fallbackForm.fileList"
        :field-config="{
          id: getGenerateId(),
          name: t('crm.fileListPop.title'),
          type: FieldTypeEnum.ATTACHMENT,
          showLabel: true,
          readable: true,
          editable: true,
          fieldWidth: 1,
          description: '',
          icon: '',
          rules: [],
          limitSize: '50',
        }"
      />
    </van-form>
    <div class="fixed bottom-0 left-0 right-0 flex items-center justify-between gap-[16px] p-[16px]">
      <van-button
        type="default"
        class="flex-1 !border-none !bg-[var(--text-n8)] !text-[var(--text-n1)]"
        :disabled="loading"
        @click="show = false"
      >
        {{ t('common.cancel') }}
      </van-button>
      <van-button type="primary" class="flex-1" :loading="loading" @click="handleFallback">
        {{ t('workbench.operation.BACK') }}
      </van-button>
    </div>
  </van-popup>
</template>
<script setup lang="ts">
  import CrmUploadFile from '@/components/business/crm-form-create/components/advanced/file.vue';
  import { getGenerateId } from '@lib/shared/method';
  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { backApproval } from '@/api/modules';
  import { showToast, type FormInstance, type PickerOption } from 'vant';
  import type { ApprovalProcessDetail, ApprovalTodoItem } from '@lib/shared/models/system/process';

  const props = defineProps<{
    approvingItem?: Partial<ApprovalTodoItem>;
    fallbackOptions: PickerOption[];
    approvalConfig?: ApprovalProcessDetail;
  }>();
  const emit = defineEmits<{
    (e: 'refresh'): void;
  }>();

  const { t } = useI18n();

  const show = defineModel<boolean>('show', {
    default: false,
  });
  const fallbackForm = ref({
    node: '',
    reason: '',
    fileList: [] as string[],
  });
  const loading = ref(false);
  const fallbackFormRef = ref<FormInstance>();
  const pickerValue = ref([]);
  const fieldValue = ref('');

  async function fallback() {
    try {
      if (!props.approvingItem) {
        return;
      }
      loading.value = true;
      await backApproval({
        id: props.approvingItem.approvalTaskId || '',
        nodeId: props.approvingItem.approvalNodeId || '',
        instanceId: props.approvingItem.approvalInstanceId || '',
        approverId: props.approvingItem.approvalId || '',
        comment: fallbackForm.value.reason,
        attachmentIds: fallbackForm.value.fileList,
        module: 'WORKBENCH',
        returnToNodeId: fallbackForm.value.node || '',
      });
      showToast({
        type: 'success',
        message: t('workbench.fallbackSuccess'),
      });
      emit('refresh');
      setTimeout(() => {
        show.value = false;
      }, 2000);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function handleFallback() {
    if (fallbackFormRef.value?.validate()) {
      fallback();
    }
  }

  const showPicker = ref(false);

  watch(
    () => show.value,
    (val) => {
      if (!val) {
        fallbackForm.value.reason = '';
        fallbackForm.value.fileList = [];
      }
    }
  );
</script>
<style lang="less" scoped></style>
