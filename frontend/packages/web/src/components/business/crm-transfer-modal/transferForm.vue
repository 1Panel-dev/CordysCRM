<template>
  <n-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-placement="left"
    label-width="auto"
    require-mark-placement="left"
    class="min-w-[350px]"
  >
    <n-form-item path="owner" :label="t('opportunity.receiver')">
      <CrmUserSelect
        v-model:value="form.owner"
        :placeholder="t('opportunity.selectReceiverPlaceholder')"
        value-field="id"
        label-field="name"
        mode="remote"
        :fetch-api="getUserOptions"
        max-tag-count="responsive"
      />
    </n-form-item>
  </n-form>
</template>

<script lang="ts" setup>
  import { FormInst, FormRules, NForm, NFormItem } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { TransferParams } from '@lib/shared/models/customer/index';

  import CrmUserSelect from '@/components/business/crm-user-select/index.vue';

  import { getUserOptions } from '@/api/modules';
  import { defaultTransferForm } from '@/config/opportunity';

  const { t } = useI18n();

  const form = defineModel<TransferParams>('form', {
    required: true,
    default: {
      ...defaultTransferForm,
    },
  });

  const formRef = ref<FormInst | null>(null);

  const rules: FormRules = {
    owner: [{ required: true, message: t('opportunity.selectReceiverPlaceholder') }],
  };

  defineExpose({
    formRef,
  });
</script>

<style lang="less" scoped></style>
