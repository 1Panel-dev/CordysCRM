<template>
  <CrmPageWrapper :title="detailTitle">
    <van-form ref="formRef" required>
      <van-cell-group v-if="route.query.type === 'phone'" inset class="mine-van-cell">
        <van-field
          v-model="form.phoneNumber"
          name="phoneNumber"
          :label="t('common.phoneNumber')"
          :placeholder="t('common.pleaseInput')"
          :rules="[{ required: true, validator: validateUserPhone }]"
          class="!text-[16px]"
        />
      </van-cell-group>
      <van-cell-group v-else inset class="mine-van-cell">
        <van-field
          v-model="form.email"
          name="email"
          :label="t('mine.email')"
          :placeholder="t('common.pleaseInput')"
          :rules="[{ required: true, validator: validateUserEmail }]"
          class="!text-[16px]"
        />
      </van-cell-group>
      <div v-if="route.query.type === 'resetPassWord'">
        <van-cell-group inset class="mine-van-cell">
          <van-field
            v-model="form.code"
            name="code"
            :label="t('mine.verificationCode')"
            :placeholder="t('mine.pleaseInputVerificationCode')"
            :rules="[{ required: true, message: t('common.notNull', { value: `${t('mine.verificationCode')}` }) }]"
            class="!text-[16px]"
            center
          >
            <template #button>
              <div class="flex items-center gap-[8px]">
                <van-divider class="!m-0 !h-[24px]" vertical :style="{ borderColor: 'var(--text-n7)' }" />
                <!-- TODO xxw -->
                <CrmTextButton :loading="loading" color="var(--primary-8)" :text="getCodeText" />
              </div>
            </template>
          </van-field>
        </van-cell-group>
        <van-cell-group inset class="mine-van-cell">
          <CrmPasswordInput
            v-model:value="form.password"
            name="password"
            :label="t('mine.newPassWord')"
            :placeholder="t('mine.pleaseInputPassWord')"
            :rules="[{ required: true, message: t('mine.pleaseInputPassWord') }]"
            class="!text-[16px]"
          />
        </van-cell-group>
        <van-cell-group inset class="mine-van-cell">
          <CrmPasswordInput
            v-model:value="form.confirmPassword"
            name="confirmPassword"
            :label="t('mine.repeatPassWord')"
            :placeholder="t('mine.pleaseInputConfirmPassWord')"
            :rules="[
              { required: true, message: t('mine.pleaseInputConfirmPassWord') },
              {
                validator: validatePasswordStartWith,
                message: t('mine.passwordDiff'),
                trigger: 'input',
              },
              {
                validator: validatePasswordSame,
                message: t('mine.passwordDiff'),
                trigger: ['blur', 'password-input'],
              },
            ]"
            class="!text-[16px]"
          />
        </van-cell-group>
      </div>
    </van-form>

    <template #footer>
      <div class="flex items-center gap-[16px]">
        <van-button
          type="default"
          class="crm-button-primary--secondary !rounded-[var(--border-radius-small)] !text-[16px]"
          block
          :disabled="loading"
          @click="router.back"
        >
          {{ t('common.cancel') }}
        </van-button>
        <van-button
          type="primary"
          class="!rounded-[var(--border-radius-small)] !text-[16px]"
          :loading="loading"
          block
          @click="save"
        >
          {{ route.query.id ? t('common.update') : t('common.create') }}
        </van-button>
      </div>
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';
  import { FormInstance, showToast } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { validateEmail, validatePhone } from '@lib/shared/method/validate';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmPasswordInput from '@/components/pure/crm-password-input/index.vue';
  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';

  const { t } = useI18n();

  const route = useRoute();
  const router = useRouter();

  const detailTitle = computed(() => {
    switch (route.query.type) {
      case 'phone':
        return t('common.phoneNumber');
      case 'email':
        return t('mine.email');
      default:
        return t('mine.resetPassWord');
    }
  });

  const showRetryCode = ref<number>(1); // 已配置

  const getCodeText = computed(() => (showRetryCode.value !== 1 ? t('mine.retryGetCode') : t('mine.getCode')));

  // TODO
  const form = ref<any>({
    phoneNumber: '132141234314',
    email: '132141234314',
    code: '',
    password: '',
    confirmPassword: '',
  });

  function validateUserPhone(value: string) {
    if (!value) {
      return t('common.notNull', { value: `${t('common.phoneNumber')}` });
    }
    if (!validatePhone(value)) {
      return t('mine.userPhoneErrTip');
    }
    return true;
  }

  function validateUserEmail(value: string) {
    if (!value) {
      return t('common.notNull', { value: `${t('mine.email')}` });
    }
    if (!validateEmail(value)) {
      return t('mine.emailErrTip');
    }
    return true;
  }

  function validatePasswordStartWith(value: string): boolean {
    return !!form.value.password && form.value.password.startsWith(value) && form.value.password.length >= value.length;
  }

  function validatePasswordSame(value: string): boolean {
    return value === form.value.password;
  }

  const loading = ref(false);
  const formRef = ref<FormInstance>();

  async function save() {
    try {
      await formRef.value?.validate();
      if (route.query.id) {
        // update
        showToast(t('common.updateSuccess'));
      } else {
        // create
        showToast(t('common.createSuccess'));
      }
      router.back();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
</script>

<style lang="less" scoped>
  .mine-van-cell {
    border-bottom: 1px solid var(--text-n8);
  }
</style>
