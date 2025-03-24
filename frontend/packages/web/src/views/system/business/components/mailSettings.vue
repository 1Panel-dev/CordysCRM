<template>
  <CrmCard auto-height no-content-padding hide-footer :loading="emailLoading">
    <CrmDescription class="p-[24px]" :descriptions="descriptions" :column="2">
      <template #password="{ item }">
        <div class="flex items-center gap-[8px]">
          <div v-show="showPassword">{{ item.value }}</div>
          <div v-show="!showPassword">{{ desensitize(item.value as string) }}</div>
          <CrmIcon
            :type="showPassword ? 'iconicon_browse' : 'iconicon_browse_off'"
            :size="16"
            class="cursor-pointer text-[var(--text-n4)]"
            @click="changeShowVisible"
          />
        </div>
      </template>
      <template #ssl="{ item }">
        <div class="flex items-center gap-[8px]">
          <CrmIcon
            v-if="item.value === 'true'"
            type="iconicon_check_circle_filled"
            :size="16"
            class="text-[var(--success-green)]"
          />
          <CrmIcon v-else type="iconicon_disable" :size="16" class="text-[var(--text-n4)]" />
          <div>
            {{
              item.value === 'true'
                ? t('system.business.mailSettings.opened')
                : t('system.business.mailSettings.closed')
            }}
          </div>
        </div>
      </template>
    </CrmDescription>
    <n-divider class="!m-0" />
    <div class="my-[12px] mr-[24px] flex justify-end">
      <n-button type="primary" ghost @click="testLink(false)">
        {{ t('system.business.mailSettings.testLink') }}
      </n-button>
      <n-button v-permission="['SYSTEM_SETTING:UPDATE']" type="primary" class="ml-[8px]" @click="showDrawer = true">
        {{ t('common.edit') }}
      </n-button>
    </div>
  </CrmCard>

  <CrmDrawer
    v-model:show="showDrawer"
    :width="680"
    :title="t('system.business.mailSettings.updateEmailSettings')"
    :ok-text="t('common.update')"
    :loading="emailDrawerLoading"
    @confirm="confirm"
    @cancel="cancel"
  >
    <n-form
      ref="formRef"
      :rules="rules"
      label-placement="left"
      :model="form"
      class="!w-[80%]"
      require-mark-placement="left"
      :label-width="100"
    >
      <n-form-item :label="t('system.business.mailSettings.smtpHost')" path="host">
        <n-input v-model:value="form.host" :maxlength="255" :placeholder="t('common.pleaseInput')" clearable />
      </n-form-item>
      <n-form-item :label="t('system.business.mailSettings.smtpPort')" path="port">
        <n-input
          v-model:value="form.port"
          class="!w-[240px]"
          :maxlength="255"
          :placeholder="t('common.pleaseInput')"
          clearable
        />
      </n-form-item>
      <n-form-item :label="t('system.business.mailSettings.smtpAccount')" path="account">
        <n-input v-model:value="form.account" :maxlength="255" :placeholder="t('common.pleaseInput')" clearable />
      </n-form-item>
      <n-form-item :label="t('system.business.mailSettings.smtpPassword')" path="password">
        <n-input
          v-model:value="form.password"
          type="password"
          show-password-on="click"
          :input-props="{ autocomplete: 'new-password' }"
          :maxlength="255"
          :placeholder="t('common.pleaseInput')"
          clearable
        />
      </n-form-item>
      <n-form-item :label="t('system.business.mailSettings.from')" path="from">
        <n-input v-model:value="form.from" :maxlength="255" :placeholder="t('common.pleaseInput')" clearable />
      </n-form-item>
      <n-form-item :label="t('system.business.mailSettings.recipient')" path="recipient">
        <n-input v-model:value="form.recipient" :maxlength="255" :placeholder="t('common.pleaseInput')" clearable />
      </n-form-item>
      <n-form-item label=" " path="ssl">
        <div>
          <div class="flex items-center">
            <n-switch v-model:value="form.ssl" class="mr-[8px]" />
            {{ t('system.business.mailSettings.ssl') }}
          </div>
          <div class="text-[12px] text-[var(--text-n4)]">{{ t('system.business.mailSettings.sslTip') }}</div>
        </div>
      </n-form-item>
      <n-form-item label=" " path="tsl">
        <div>
          <div class="flex items-center">
            <n-switch v-model:value="form.tsl" class="mr-[8px]" />
            {{ t('system.business.mailSettings.tsl') }}
          </div>
          <div class="text-[12px] text-[var(--text-n4)]">{{ t('system.business.mailSettings.tslTip') }}</div>
        </div>
      </n-form-item>
      <n-form-item label=" ">
        <n-button type="primary" ghost @click="testLink(true)">
          {{ t('system.business.mailSettings.testLink') }}
        </n-button>
      </n-form-item>
    </n-form>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { FormInst, FormItemRule, NButton, NDivider, NForm, NFormItem, NInput, NSwitch, useMessage } from 'naive-ui';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDescription, { Description } from '@/components/pure/crm-description/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';

  import { getConfigEmail, testConfigEmail, updateConfigEmail } from '@/api/modules/system/business';
  import { useI18n } from '@/hooks/useI18n';
  import { desensitize } from '@/utils';
  import { validateEmail } from '@/utils/validate';

  const { t } = useI18n();
  const Message = useMessage();

  const showPassword = ref(false);
  function changeShowVisible() {
    showPassword.value = !showPassword.value;
  }

  const descriptions = ref<Description[]>([]);

  const showDrawer = ref(false);
  const formRef = ref<FormInst | null>(null);
  const originForm = ref({
    host: '',
    port: '',
    account: '',
    password: '',
    from: '',
    recipient: '',
    ssl: false,
    tsl: false,
  });
  const form = ref({ ...originForm.value });

  const emailRule = {
    validator: (rule: FormItemRule, value: string) => {
      if (value && !validateEmail(value)) {
        return new Error(t('common.emailErrTip'));
      }
      return true;
    },
    trigger: ['blur', 'input'],
  };

  const rules = ref({
    host: [
      {
        required: true,
        message: t('common.notNull', { value: t('system.business.mailSettings.smtpHost') }),
        trigger: ['blur'],
      },
    ],
    port: [
      {
        required: true,
        message: t('common.notNull', { value: t('system.business.mailSettings.smtpPort') }),
        trigger: ['blur'],
      },
    ],
    account: [
      {
        required: true,
        message: t('common.notNull', { value: t('system.business.mailSettings.smtpAccount') }),
        trigger: ['blur'],
      },
    ],
    from: [emailRule],
    recipient: [emailRule],
  });

  const emailLoading = ref(false);
  async function initEmailInfo() {
    try {
      emailLoading.value = true;
      const res = await getConfigEmail();
      originForm.value = { ...res, ssl: res.ssl === 'true', tsl: res.tsl === 'true' };
      form.value = { ...res, ssl: res.ssl === 'true', tsl: res.tsl === 'true' };
      descriptions.value = [
        { label: t('system.business.mailSettings.smtpHost'), value: res.host },
        { label: t('system.business.mailSettings.smtpPort'), value: res.port },
        { label: t('system.business.mailSettings.smtpAccount'), value: res.account },
        { label: t('system.business.mailSettings.smtpPassword'), value: res.password, valueSlotName: 'password' },
        { label: t('system.business.mailSettings.from'), value: res.from },
        { label: t('system.business.mailSettings.recipient'), value: res.recipient },
        { label: t('system.business.mailSettings.ssl'), value: res.ssl, valueSlotName: 'ssl' },
        { label: t('system.business.mailSettings.tsl'), value: res.tsl, valueSlotName: 'ssl' },
      ];
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      emailLoading.value = false;
    }
  }

  const emailDrawerLoading = ref(false);
  async function confirm() {
    formRef.value?.validate(async (errors) => {
      if (!errors) {
        try {
          emailDrawerLoading.value = true;
          await updateConfigEmail({ ...form.value, ssl: String(form.value.ssl), tsl: String(form.value.tsl) });
          Message.success(t('common.updateSuccess'));
          showDrawer.value = false;
          initEmailInfo();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        } finally {
          emailDrawerLoading.value = false;
        }
      }
    });
  }

  function cancel() {
    form.value = { ...originForm.value };
  }

  async function testLink(isDrawer: boolean) {
    try {
      if (isDrawer) {
        await formRef.value?.validate();
      }
      const params = {
        ...(isDrawer ? form.value : originForm.value),
        ssl: String(form.value.ssl),
        tsl: String(form.value.tsl),
      };
      await testConfigEmail(params);
      Message.success(t('common.connectionSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  onBeforeMount(() => {
    initEmailInfo();
  });
</script>
