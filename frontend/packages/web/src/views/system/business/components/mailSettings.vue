<template>
  <CrmCard auto-height no-content-padding hide-footer>
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
              item.value === 'false'
                ? t('system.business.mailSettings.closed')
                : t('system.business.mailSettings.opened')
            }}
          </div>
        </div>
      </template>
    </CrmDescription>
    <n-divider class="!m-0" />
    <div class="my-[12px] mr-[24px] flex justify-end">
      <n-button type="primary" ghost> {{ t('system.business.mailSettings.testLink') }} </n-button>
      <n-button type="primary" class="ml-[8px]" @click="showDrawer = true">{{ t('common.edit') }}</n-button>
    </div>
  </CrmCard>

  <CrmDrawer
    v-model:show="showDrawer"
    :width="680"
    :title="t('system.business.mailSettings.updateEmailSettings')"
    :ok-text="t('common.update')"
    @confirm="confirm"
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
      <n-form-item :label="t('system.business.mailSettings.smtpHost')" path="smtpHost">
        <n-input v-model:value="form.smtpHost" :maxlength="255" :placeholder="t('common.pleaseInput')" clearable />
      </n-form-item>
      <n-form-item :label="t('system.business.mailSettings.smtpPort')" path="smtpPort">
        <n-input
          v-model:value="form.smtpPort"
          class="!w-[240px]"
          :maxlength="255"
          :placeholder="t('common.pleaseInput')"
          clearable
        />
      </n-form-item>
      <n-form-item :label="t('system.business.mailSettings.smtpAccount')" path="smtpAccount">
        <n-input v-model:value="form.smtpAccount" :maxlength="255" :placeholder="t('common.pleaseInput')" clearable />
      </n-form-item>
      <n-form-item :label="t('system.business.mailSettings.smtpPassword')" path="smtpPassword">
        <n-input
          v-model:value="form.smtpPassword"
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
        <n-button type="primary" ghost> {{ t('system.business.mailSettings.testLink') }} </n-button>
      </n-form-item>
    </n-form>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { FormInst, FormItemRule, NButton, NDivider, NForm, NFormItem, NInput, NSwitch, useMessage } from 'naive-ui';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDescription, { Description } from '@/components/pure/crm-description/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';

  import { useI18n } from '@/hooks/useI18n';
  import { desensitize } from '@/utils';
  import { validateEmail } from '@/utils/validate';

  const { t } = useI18n();
  const Message = useMessage();

  const showPassword = ref(false);
  function changeShowVisible() {
    showPassword.value = !showPassword.value;
  }

  const descriptions = ref<Description[]>([
    {
      label: t('system.business.mailSettings.smtpHost'),
      value: 'xxxxxxxx',
    },
    { label: t('system.business.mailSettings.smtpPort'), value: 'xxx' },
    { label: t('system.business.mailSettings.smtpAccount'), value: 'xxx' },
    { label: t('system.business.mailSettings.smtpPassword'), value: 'xxx', slotName: 'password' },
    { label: t('system.business.mailSettings.from'), value: 'xxx' },
    { label: t('system.business.mailSettings.recipient'), value: 'xxx' },
    { label: t('system.business.mailSettings.ssl'), value: 'true', slotName: 'ssl' },
    { label: t('system.business.mailSettings.tsl'), value: 'false', slotName: 'ssl' },
  ]);

  const showDrawer = ref(false);
  const formRef = ref<FormInst | null>(null);
  const form = ref({
    smtpHost: '',
    smtpPort: '',
    smtpAccount: '',
    smtpPassword: '',
    from: '',
    recipient: '',
    ssl: 'true',
    tsl: 'false',
  });

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
    smtpHost: [
      {
        required: true,
        message: t('common.notNull', { value: t('system.business.mailSettings.smtpHost') }),
        trigger: ['blur'],
      },
    ],
    smtpPort: [
      {
        required: true,
        message: t('common.notNull', { value: t('system.business.mailSettings.smtpPort') }),
        trigger: ['blur'],
      },
    ],
    smtpAccount: [
      {
        required: true,
        message: t('common.notNull', { value: t('system.business.mailSettings.smtpAccount') }),
        trigger: ['blur'],
      },
    ],
    from: [emailRule],
    recipient: [emailRule],
  });

  function confirm() {
    formRef.value?.validate((errors) => {
      if (!errors) {
        Message.success(t('common.updateSuccess'));
        showDrawer.value = false;
      }
    });
  }
</script>
