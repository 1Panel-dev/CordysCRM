<template>
  <CrmModal
    v-model:show="showModal"
    size="medium"
    :title="t('org.enterpriseWhatSync')"
    :ok-loading="loading"
    @cancel="cancel"
  >
    <div>
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" :label-width="80">
        <n-form-item
          require-mark-placement="left"
          label-placement="left"
          path="enterpriseId"
          :label="t('org.enterpriseID')"
        >
          <n-input v-model:value="form.enterpriseId" type="text" :placeholder="t('org.EncodingAESKeyTip')" />
        </n-form-item>
        <n-form-item require-mark-placement="left" label-placement="left" path="secret" :label="t('org.Secret')">
          <n-input v-model:value="form.secret" type="text" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
        <n-form-item
          require-mark-placement="left"
          label-placement="left"
          path="callbackAddress"
          :label="t('org.callbackAddress')"
        >
          <n-input v-model:value="form.callbackAddress" type="password" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
        <n-form-item require-mark-placement="left" label-placement="left" path="token" :label="t('org.token')">
          <n-input v-model:value="form.token" type="text" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
        <n-form-item
          require-mark-placement="left"
          label-placement="left"
          path="encodingAESKey"
          :label="t('org.EncodingAESKey')"
        >
          <n-input v-model:value="form.encodingAESKey" type="text" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
      </n-form>
    </div>
    <template #footer>
      <div class="flex w-full items-center justify-between">
        <div class="ml-[4px] flex items-center gap-[8px]">
          <n-switch v-model:value="form.status" /> {{ t('common.status') }}
        </div>
        <div>
          <n-button :disabled="loading" secondary @click="cancel">
            {{ t('common.cancel') }}
          </n-button>
          <n-button class="mx-[12px]" :loading="linkLoading" type="tertiary" @click="continueLink">
            {{ t('org.testConnection') }}
          </n-button>
          <n-button :loading="loading" type="primary" @click="confirmHandler">
            {{ t('common.confirm') }}
          </n-button>
        </div>
      </div>
    </template>
  </CrmModal>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { FormInst, FormRules, NButton, NForm, NFormItem, NInput, NSwitch, useMessage } from 'naive-ui';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const Message = useMessage();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const formRef = ref<FormInst | null>(null);

  const form = ref({
    enterpriseId: '',
    secret: '',
    callbackAddress: '',
    token: '',
    encodingAESKey: '',
    status: true,
  });

  const rules: FormRules = {
    enterpriseId: [{ required: true, message: t('common.pleaseInput') }],
    secret: [{ required: true, message: t('common.pleaseInput') }],
  };

  function cancel() {
    showModal.value = false;
  }

  /** *
   * 保存
   */
  const loading = ref(false);
  function confirmHandler() {
    formRef.value?.validate((error) => {
      if (!error) {
        try {
          loading.value = true;
          Message.success(t('common.updateSuccess'));
        } catch (e) {
          console.log(e);
        }
      }
    });
  }

  /** *
   * 测试连接
   */
  const linkLoading = ref<boolean>(false);
  function continueLink() {
    formRef.value?.validate((error) => {
      if (!error) {
        try {
          linkLoading.value = true;
          Message.success(t('org.testConnectionSuccess'));
        } catch (e) {
          console.log(e);
        }
      }
    });
  }
</script>

<style scoped></style>
