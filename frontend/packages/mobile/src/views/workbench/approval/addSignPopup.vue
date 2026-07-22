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
      <div class="text-[16px] font-semibold">{{ t('workbench.operation.SIGN') }}</div>
    </div>
    <van-form ref="fallbackFormRef" label-align="top" @submit="">
      <van-field name="radio" :label="t('workbench.addSignType')" label-align="left">
        <template #input>
          <van-radio-group v-model="addSignForm.type" direction="horizontal">
            <van-radio name="BEFORE">{{ t('workbench.beforeMe') }}</van-radio>
            <van-radio name="AFTER">{{ t('workbench.afterMe') }}</van-radio>
          </van-radio-group>
        </template>
      </van-field>
      <CrmMemberSelect
        v-model:value="addSignForm.reviewer"
        :field-config="{
          id: getGenerateId(),
          showLabel: true,
          editable: true,
          readable: true,
          description: '',
          type: FieldTypeEnum.MEMBER,
          name: t('workbench.addSignReviewer'),
          fieldWidth: 1,
          rules: [{ key: FieldRuleEnum.REQUIRED }],
          icon: '',
        }"
      />
      <van-field
        name="reason"
        :label="t('workbench.addSignAdvice')"
        :placeholder="t('common.pleaseInput')"
        v-model="addSignForm.reason"
        autosize
        show-word-limit
        rows="2"
        type="textarea"
        maxlength="300"
      />
      <CrmUploadFile
        v-model:value="addSignForm.fileList"
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
        {{ addSignForm.type === 'BEFORE' ? t('workbench.operation.SIGN') : t('workbench.addSignAndApproved') }}
      </van-button>
    </div>
  </van-popup>
</template>
<script setup lang="ts">
  import CrmUploadFile from '@/components/business/crm-form-create/components/advanced/file.vue';
  import CrmMemberSelect from '@/components/business/crm-form-create/components/basic/memberSelect.vue';
  import { getGenerateId } from '@lib/shared/method';
  import { FieldRuleEnum, FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { addSignApproval } from '@/api/modules';
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
  const addSignForm = ref({
    type: 'BEFORE',
    reviewer: '',
    reason: '',
    fileList: [] as string[],
  });
  const loading = ref(false);
  const fallbackFormRef = ref<FormInstance>();

  async function fallback() {
    try {
      if (!props.approvingItem) {
        return;
      }
      loading.value = true;
      await addSignApproval({
        id: props.approvingItem.approvalTaskId || '',
        nodeId: props.approvingItem.approvalNodeId || '',
        instanceId: props.approvingItem.approvalInstanceId || '',
        approverId: props.approvingItem.approvalId || '',
        comment: addSignForm.value.reason,
        attachmentIds: addSignForm.value.fileList,
        type: addSignForm.value.type,
        module: 'WORKBENCH',
        signApprover: addSignForm.value.reviewer,
      });
      showToast({
        type: 'success',
        message: t('workbench.addSignSuccess'),
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
        addSignForm.value.reason = '';
        addSignForm.value.fileList = [];
      }
    }
  );
</script>
<style lang="less" scoped></style>
