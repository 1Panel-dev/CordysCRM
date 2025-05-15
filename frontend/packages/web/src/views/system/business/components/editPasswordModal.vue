<template>
  <CrmModal v-model:show="showModal" size="small" :title="t('system.personal.changePassword')" @cancel="cancel">
    <n-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-placement="left"
      require-mark-placement="left"
      :label-width="70"
    >
      <n-form-item path="email" :label="t('system.personal.email')">
        <n-input v-model:value="form.email" :placeholder="t('common.pleaseInput')" @keydown.enter.prevent />
      </n-form-item>

      <n-form-item path="code" :label="t('system.personal.code')">
        <div class="space-between flex w-full">
          <n-input
            v-model:value="form.code"
            :placeholder="t('common.pleaseInput')"
            :maxlength="6"
            class="codeInput"
            @keydown.enter.prevent
          />
          <n-button v-if="showRetryCode === 2" :loading="loading" type="primary" @click="sendCodeTip">
            {{ t('system.personal.retry') }}
            (<n-countdown
              :render="renderCountdown"
              :duration="60 * 1000"
              :active="true"
              :on-finish="finishCountdown"
            />s)
          </n-button>
          <n-button v-if="showRetryCode === 1" :disabled="loading" secondary @click="sendCode">
            {{ t('system.personal.getCode') }}
          </n-button>
          <n-button v-if="showRetryCode === 3" :disabled="loading" secondary @click="sendCode">
            {{ t('system.personal.retry') }}
          </n-button>
        </div>
      </n-form-item>

      <n-form-item path="password" :label="t('system.personal.password')">
        <n-input
          v-model:value="form.password"
          type="password"
          show-password-on="click"
          :input-props="{ autocomplete: 'new-password' }"
          :placeholder="t('login.form.password.placeholder')"
          @keydown.enter.prevent
        />
      </n-form-item>
      <n-form-item ref="rPasswordFormItemRef" first path="confirmPassword" class="confirm-password">
        <n-input
          v-model:value="form.confirmPassword"
          type="password"
          show-password-on="click"
          :input-props="{ autocomplete: 'new-password' }"
          :placeholder="t('login.form.password.placeholder')"
          @keydown.enter.prevent
        />
      </n-form-item>
    </n-form>
    <template #footer>
      <div class="flex w-full items-center justify-end">
        <n-button :disabled="loading" secondary class="mx-[8px]" @click="cancel">
          {{ t('common.cancel') }}
        </n-button>
        <n-button
          :loading="loading"
          :disabled="form.code === '' || form.password === '' || form.email === '' || form.confirmPassword === ''"
          type="primary"
          @click="confirmHandler"
        >
          {{ t('common.save') }}
        </n-button>
      </div>
    </template>
  </CrmModal>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import {
    FormInst,
    FormItemInst,
    FormItemRule,
    FormRules,
    NButton,
    NCountdown,
    NForm,
    NFormItem,
    NInput,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { validateEmail } from '@lib/shared/method/validate';
  import { PersonalPassword, SendEmailDTO } from '@lib/shared/models/system/business';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import { sendEmailCode, updateUserPassword } from '@/api/modules';
  import useUserStore from '@/store/modules/user';

  import type { CountdownProps } from 'naive-ui';

  const { t } = useI18n();
  const Message = useMessage();
  const userStore = useUserStore();

  const props = defineProps<{
    integration?: PersonalPassword;
  }>();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const form = ref<PersonalPassword>({
    email: '',
    code: '',
    password: '',
    confirmPassword: '',
  });

  watch(
    () => props.integration,
    (val) => {
      if (val) {
        form.value = { ...val };
      }
    },
    { deep: true }
  );

  function validatePasswordStartWith(rule: FormItemRule, value: string): boolean {
    return !!form.value.password && form.value.password.startsWith(value) && form.value.password.length >= value.length;
  }

  function validatePasswordSame(rule: FormItemRule, value: string): boolean {
    return value === form.value.password;
  }
  function validatePasswordStyle(rule: FormItemRule, value: string): boolean {
    const reg = /^(?=.*\d)(?=.*[A-Za-z]).*$/;
    return reg.test(value);
  }
  function validatePasswordLength(rule: FormItemRule, value: string): boolean {
    const reg = /^1[3-9]\d{9}$/;
    return reg.test(value);
  }
  const formRef = ref<FormInst | null>(null);
  const rPasswordFormItemRef = ref<FormItemInst | null>(null);
  const rules: FormRules = {
    email: [{ required: true, message: t('common.notNull', { value: `${t('system.personal.email')} ` }) }],
    code: [{ required: true, message: t('common.notNull', { value: `${t('system.personal.code')} ` }) }],
    password: [{ required: true, message: t('common.notNull', { value: `${t('system.personal.password')} ` }) }],
    confirmPassword: [
      { required: true, message: t('common.notNull') },
      {
        validator: validatePasswordStartWith,
        message: t('system.personal.password.diff'),
        trigger: 'input',
      },
      {
        validator: validatePasswordSame,
        message: t('system.personal.password.diff'),
        trigger: ['blur', 'password-input'],
      },
      {
        validator: validatePasswordStyle,
        message: t('system.personal.password.style'),
        trigger: ['blur', 'password-input'],
      },
      {
        validator: validatePasswordLength,
        message: t('system.personal.password.length'),
        trigger: ['blur', 'password-input'],
      },
    ],
  };

  const showRetryCode = ref<number>(1); // 已配置

  const renderCountdown: CountdownProps['render'] = ({ hours, minutes, seconds }) => {
    return hours * 60 * 60000 + minutes * 60 + seconds;
  };

  const finishCountdown: CountdownProps['onFinish'] = () => {
    showRetryCode.value = 3;
  };

  function cancel() {
    showRetryCode.value = 1;
    form.value = {
      email: props.integration?.email || '',
      code: '',
      password: '',
      confirmPassword: '',
    };
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
          await updateUserPassword(form.value);
          Message.success(t('common.updateSuccess'));
          showModal.value = false;
          userStore.logout();
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          loading.value = false;
        }
      }
    });
  }
  async function sendCode() {
    if (form.value.email === '') {
      Message.error(t('system.personal.email.empty'));
      return;
    }
    if (!validateEmail(form.value.email)){
      Message.error(t('system.personal.email.style'));
      return;
    }
    showRetryCode.value = 2;
    const emailData: SendEmailDTO = {
      email: form.value.email,
    };
    await sendEmailCode(emailData);
  }
  function sendCodeTip() {
    Message.warning(t('system.personal.countdown'));
  }
</script>

<style scoped lang="less">
  .confirm-password {
    padding-left: 70px !important;
  }
  .codeInput {
    margin-right: 16px !important;
  }
</style>
