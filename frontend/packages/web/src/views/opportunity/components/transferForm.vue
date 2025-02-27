<template>
  <n-form ref="formRef" :model="form" :rules="rules">
    <n-form-item require-mark-placement="left" label-placement="left" path="head" :label="t('opportunity.receiver')">
      <CrmUserSelect
        v-model:value="form.head"
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

  import CrmUserSelect from '@/components/business/crm-user-select/index.vue';

  import { getUserOptions } from '@/api/modules/system/org';
  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const form = defineModel<{ head: string | null }>('form', {
    required: true,
    default: {
      head: null,
    },
  });

  const formRef = ref<FormInst | null>(null);

  const rules: FormRules = {
    head: [{ required: true, message: t('opportunity.selectReceiverPlaceholder') }],
  };

  defineExpose({
    formRef,
  });
</script>

<style lang="less" scoped></style>
