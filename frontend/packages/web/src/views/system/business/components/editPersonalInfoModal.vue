<template>
  <CrmModal v-model:show="showModal" size="small" :title="t('common.update')">
    <n-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-placement="left"
      require-mark-placement="left"
      :label-width="80"
    >
      <n-form-item path="phone" :label="t('system.personal.phone')">
        <n-input v-model:value="form.phone" :placeholder="t('common.pleaseInput')" />
      </n-form-item>
      <n-form-item path="email" :label="t('system.personal.email')">
        <n-input v-model:value="form.email" :placeholder="t('common.pleaseInput')" />
      </n-form-item>
    </n-form>
    <template #footer>
      <div class="flex w-full items-center justify-end">
        <n-button :disabled="loading" secondary class="mx-[8px]" @click="cancel">
          {{ t('common.cancel') }}
        </n-button>
        <n-button :loading="loading" type="primary" @click="confirmHandler">
          {{ t('common.update') }}
        </n-button>
      </div>
    </template>
  </CrmModal>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { FormInst, FormRules, NButton, NForm, NFormItem, NInput, useMessage } from 'naive-ui';

  import { PersonalInfoRequest } from '@lib/shared/models/system/business';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import { updatePersonalUrl } from '@/api/modules/system/business';
  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    integration?: PersonalInfoRequest;
  }>();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const emit = defineEmits<{
    (e: 'initSync'): void;
  }>();

  const form = ref<PersonalInfoRequest>({
    phone: '',
    email: '',
  });

  watch(
    () => props.integration,
    (val) => {
      form.value = { ...(val as PersonalInfoRequest) };
    },
    { deep: true }
  );

  const rules: FormRules = {
    phone: [{ required: true, message: t('common.notNull', { value: `${t('system.personal.phone')} ` }) }],
    email: [{ required: true, message: t('common.notNull', { value: `${t('system.personal.email')} ` }) }],
  };

  const formRef = ref<FormInst | null>(null);
  function cancel() {
    showModal.value = false;
  }
  /** *
   * 保存
   */
  const loading = ref(false);
  function confirmHandler() {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          loading.value = true;
          await updatePersonalUrl(form.value);
          Message.success(t('common.updateSuccess'));
          showModal.value = false;
          emit('initSync');
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
