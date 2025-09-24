<template>
  <CrmModal
    v-model:show="showModal"
    class="crm-form-modal"
    size="small"
    :title="t('system.personal.changePassword')"
    @cancel="cancel"
  >
    <n-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-placement="left"
      require-mark-placement="left"
      :label-width="currentLocale === 'en-US' ? 100 : 80"
    >
      <n-form-item path="originPassword" :label="t('system.personal.currentPassword')">
        <n-input
          v-model:value="form.originPassword"
          type="password"
          show-password-on="click"
          :placeholder="t('login.form.password.placeholder')"
          @keydown.enter.prevent
        />
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
      <n-form-item
        ref="rPasswordFormItemRef"
        first
        path="confirmPassword"
        :class="currentLocale === 'en-US' ? 'pl-[100px]' : 'confirm-password'"
      >
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
          :disabled="form.originPassword === '' || form.password === '' || form.confirmPassword === ''"
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
    NForm,
    NFormItem,
    NInput,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import useLocale from '@lib/shared/locale/useLocale';
  import { PersonalPassword } from '@lib/shared/models/system/business';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import { updateUserPassword } from '@/api/modules';
  import useUserStore from '@/store/modules/user';

  const { t } = useI18n();
  const Message = useMessage();
  const userStore = useUserStore();
  const { currentLocale } = useLocale(Message.loading);

  const props = defineProps<{
    integration?: PersonalPassword;
  }>();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const form = ref<PersonalPassword>({
    originPassword: '',
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
    const reg = /^.{1,64}$/;
    return reg.test(value);
  }
  const formRef = ref<FormInst | null>(null);
  const rPasswordFormItemRef = ref<FormItemInst | null>(null);
  const rules: FormRules = {
    originPassword: [
      { required: true, message: t('common.notNull', { value: `${t('system.personal.currentPassword')} ` }) },
    ],
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

  function cancel() {
    form.value = {
      originPassword: '',
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
</script>

<style scoped lang="less">
  .confirm-password {
    padding-left: 80px !important;
  }
</style>
