<template>
  <CrmPageWrapper :title="detailTitle">
    <van-form ref="formRef" required class="crm-form">
      <van-cell-group v-if="route.query.type === 'phone'" inset>
        <van-field
          v-model="form.phone"
          name="phone"
          :label="t('common.phoneNumber')"
          :placeholder="t('common.pleaseInput')"
          :rules="[
            { required: true, message: t('common.notNull', { value: `${t('common.phoneNumber')}` }) },
            { validator: validateUserPhone },
          ]"
          class="!text-[16px]"
        />
      </van-cell-group>
      <van-cell-group v-else-if="route.query.type === 'email'" inset>
        <van-field
          v-model="form.email"
          name="email"
          :label="t('mine.email')"
          :placeholder="t('common.pleaseInput')"
          :rules="[
            { required: true, message: t('common.notNull', { value: `${t('mine.email')}` }) },
            { validator: validateUserEmail },
          ]"
          class="!text-[16px]"
        />
      </van-cell-group>
      <div v-if="route.query.type === 'resetPassWord'">
        <van-cell-group inset>
          <van-field
            v-model="form.email"
            name="email"
            :label="t('mine.email')"
            :placeholder="t('common.pleaseInput')"
            :rules="[
              { required: true, message: t('common.notNull', { value: `${t('mine.email')}` }) },
              { validator: validateUserEmail },
            ]"
            class="!text-[16px]"
          />
        </van-cell-group>
        <van-cell-group inset>
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
                <CrmTextButton
                  :color="`${isCounting ? 'var(--primary-4)' : 'var(--primary-8)'}`"
                  :text="getCodeText"
                  @click="sendCode"
                />
              </div>
            </template>
          </van-field>
        </van-cell-group>
        <van-cell-group inset>
          <CrmPasswordInput
            v-model:value="form.password"
            name="password"
            :label="t('mine.newPassWord')"
            :placeholder="t('mine.pleaseInputPassWord')"
            :rules="[{ required: true, message: t('mine.pleaseInputPassWord') }]"
            class="!text-[16px]"
          />
        </van-cell-group>
        <van-cell-group inset>
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
          {{ t('common.update') }}
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
  import { PersonalPassword, SendEmailDTO } from '@lib/shared/models/system/business';
  import { OrgUserInfo } from '@lib/shared/models/system/org';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmPasswordInput from '@/components/pure/crm-password-input/index.vue';
  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';

  import { getPersonalUrl, sendEmailCode, updatePersonalInfo, updateUserPassword } from '@/api/modules/index';
  import { defaultUserInfo } from '@/config/mine';

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

  const form = ref<OrgUserInfo & PersonalPassword>({
    ...defaultUserInfo,
    code: '',
    confirmPassword: '',
  });

  const validateUserPhone = (value: string) => (!validatePhone(value) ? t('mine.userPhoneErrTip') : true);

  const validateUserEmail = (value: string) => (!validateEmail(value) ? t('mine.emailErrTip') : true);

  function validatePasswordStartWith(value: string): boolean {
    return !!form.value.password && form.value.password.startsWith(value) && form.value.password.length >= value.length;
  }

  function validatePasswordSame(value: string): boolean {
    return value === form.value.password;
  }

  const loading = ref(false);
  const formRef = ref<FormInstance>();

  const isEditPersonalInfo = computed(() => ['phone', 'email'].includes(route.query.type as string));

  // 更新个人信息
  async function handleUpdatePersonalInfo() {
    try {
      loading.value = true;
      const { email, phone } = form.value;
      await updatePersonalInfo({
        email,
        phone,
      });
      showToast(t('common.updateSuccess'));
      router.back();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  // 重置密码
  async function handleResetPassword() {
    try {
      loading.value = true;
      const { email, code, password, confirmPassword } = form.value;
      await updateUserPassword({
        email,
        code,
        password,
        confirmPassword,
      });
      showToast(t('common.updateSuccess'));
      router.back();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function save() {
    try {
      await formRef.value?.validate();
      if (isEditPersonalInfo.value) {
        await handleUpdatePersonalInfo();
      } else {
        await handleResetPassword();
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const personalInfo = ref<OrgUserInfo>({
    ...defaultUserInfo,
  });

  let timer: ReturnType<typeof setInterval> | null = null;
  const isCounting = ref(false);
  const count = ref(60);

  const getCodeText = computed(() =>
    isCounting.value ? t('mine.retryGetCode', { count: count.value }) : t('mine.getCode')
  );

  function GetCodeStartCountdown() {
    isCounting.value = true;
    count.value = 60;
    timer = setInterval(() => {
      if (count.value <= 1) {
        clearInterval(timer!);
        timer = null;
        isCounting.value = false;
      } else {
        count.value--;
      }
    }, 1000);
  }

  // 发送验证码
  async function sendCode() {
    if (isCounting.value) return;
    try {
      GetCodeStartCountdown();
      const emailData: SendEmailDTO = {
        email: form.value.email,
      };
      await sendEmailCode(emailData);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function initPersonInfo() {
    try {
      personalInfo.value = await getPersonalUrl();
      form.value = { ...form.value, ...personalInfo.value };
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  onBeforeMount(() => {
    initPersonInfo();
  });
</script>

<style lang="less" scoped></style>
