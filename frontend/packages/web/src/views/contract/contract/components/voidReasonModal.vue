<template>
  <CrmModal
    v-model:show="visible"
    size="small"
    :ok-loading="loading"
    :positive-text="t('common.voided')"
    @confirm="handleConfirm"
  >
    <template #title>
      <div class="flex gap-[4px] overflow-hidden">
        <div class="text-[var(--text-n1)]">{{ t('contract.voidReason') }}</div>
        <div class="flex text-[var(--text-n4)]">
          (
          <div class="one-line-text max-w-[300px]">{{ props.name }}</div>
          )
        </div>
      </div>
    </template>
    <n-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-placement="left"
      label-width="auto"
      require-mark-placement="right"
    >
      <n-form-item path="reason" :label="t('contract.voidReason')">
        <n-input
          v-model:value="form.reason"
          type="textarea"
          :placeholder="t('common.pleaseInput')"
          allow-clear
          maxlength="200"
          show-count
        />
      </n-form-item>
    </n-form>
  </CrmModal>
</template>

<script setup lang="ts">
  import { FormInst, FormRules, NForm, NFormItem, NInput, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import { voidedContract } from '@/api/modules';

  const props = defineProps<{
    sourceId: string;
    name: string;
  }>();

  const emit = defineEmits<{
    (e: 'refresh'): void;
  }>();

  const Message = useMessage();
  const { t } = useI18n();

  const visible = defineModel<boolean>('visible', { required: true });

  const loading = ref(false);
  const formRef = ref<FormInst | null>(null);
  const form = ref<{ reason: string }>({ reason: '' });
  const rules: FormRules = {
    reason: [
      {
        trigger: ['input', 'blur'],
        required: true,
        message: t('common.notNull', { value: t('contract.voidReason') }),
      },
    ],
  };

  async function handleConfirm() {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          loading.value = true;
          await voidedContract(props.sourceId, form.value.reason);
          Message.success(t('common.voidSuccess'));
          visible.value = false;
          emit('refresh');
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          loading.value = false;
        }
      }
    });
  }
</script>
