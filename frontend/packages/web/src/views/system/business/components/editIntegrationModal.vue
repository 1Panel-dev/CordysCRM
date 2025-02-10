<template>
  <CrmModal v-model:show="showModal" :title="currentIntegration?.title">
    <n-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-placement="left"
      require-mark-placement="left"
      :label-width="80"
    >
      <!-- 企业 ID，应用 ID -->
      <template v-if="['WE_COM', 'DING_TALK'].includes(currentIntegration?.key)">
        <n-form-item path="corpId" :label="t('system.business.corpId')">
          <n-input v-model:value="form.corpId" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
        <n-form-item path="agentId" :label="t('system.business.agentId')">
          <n-input v-model:value="form.agentId" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
      </template>

      <!-- 应用 key -->
      <n-form-item
        v-if="['LARK', 'LARK_SUITE', 'DING_TALK'].includes(currentIntegration?.key)"
        path="appKey"
        :label="t('system.business.appKey')"
      >
        <n-input
          v-model:value="form.appKey"
          type="password"
          show-password-on="click"
          :input-props="{ autocomplete: 'new-password' }"
          :placeholder="t('common.pleaseInput')"
        />
      </n-form-item>

      <!-- 应用密钥，回调域名 -->
      <n-form-item path="appSecret" :label="t('system.business.appSecret')">
        <n-input
          v-model:value="form.appSecret"
          type="password"
          show-password-on="click"
          :input-props="{ autocomplete: 'new-password' }"
          :placeholder="t('common.pleaseInput')"
        />
      </n-form-item>
      <n-form-item path="callBack" :label="t('system.business.callBack')">
        <n-input v-model:value="form.callBack" :placeholder="t('common.pleaseInput')" />
      </n-form-item>
    </n-form>
    <template #footer>
      <div class="flex w-full items-center justify-between">
        <div class="ml-[4px] flex items-center gap-[8px]">
          <n-switch v-model:value="form.enable" /> {{ t('common.status') }}
        </div>
        <div>
          <n-button :disabled="loading" secondary @click="cancel">
            {{ t('common.cancel') }}
          </n-button>
          <n-button
            :loading="linkLoading"
            type="primary"
            ghost
            class="n-btn-outline-primary mx-[12px]"
            @click="continueLink"
          >
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

  const props = defineProps<{
    integration: any; // TODO: 定义具体的类型
  }>();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const currentIntegration = ref();
  watch(
    () => props.integration,
    (val) => {
      currentIntegration.value = val;
    },
    { deep: true }
  );

  const form = ref({
    corpId: '',
    agentId: '',
    appKey: '',
    appSecret: '',
    callBack: '',
    enable: true,
  });

  const rules: FormRules = {
    corpId: [{ required: true, message: t('common.notNull', { value: `${t('system.business.corpId')} ` }) }],
    agentId: [{ required: true, message: t('common.notNull', { value: `${t('system.business.agentId')} ` }) }],
    appKey: [{ required: true, message: t('common.notNull', { value: `${t('system.business.appKey')} ` }) }],
    appSecret: [{ required: true, message: t('common.notNull', { value: t('system.business.appSecret') }) }],
    callBack: [{ required: true, message: t('common.notNull', { value: t('system.business.callBack') }) }],
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
    formRef.value?.validate((error) => {
      if (!error) {
        try {
          loading.value = true;
          Message.success(t('common.updateSuccess'));
        } catch (e) {
          // eslint-disable-next-line no-console
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
          // eslint-disable-next-line no-console
          console.log(e);
        }
      }
    });
  }
</script>
