<template>
  <n-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-placement="left"
    label-width="auto"
    require-mark-placement="left"
  >
    <n-form-item path="head" :label="t('opportunity.receiver')">
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
    <n-form-item
      v-if="[ModuleConfigEnum.CLUE_MANAGEMENT, ModuleConfigEnum.CUSTOMER_MANAGEMENT].includes(props.moduleType)"
      path="belongToPublicPool"
      :label="t('clue.belongToPublicPool')"
    >
      <n-select v-model:value="form.belongToPublicPool" :options="[]" />
    </n-form-item>
  </n-form>
</template>

<script lang="ts" setup>
  import { FormInst, FormRules, NForm, NFormItem, NSelect } from 'naive-ui';

  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';

  import CrmUserSelect from '@/components/business/crm-user-select/index.vue';

  import { getUserOptions } from '@/api/modules/system/org';
  import { defaultTransferForm } from '@/config/opportunity';
  import { useI18n } from '@/hooks/useI18n';

  const props = defineProps<{
    moduleType: ModuleConfigEnum;
  }>();

  const { t } = useI18n();

  const form = defineModel<{ head: string | null; belongToPublicPool?: string | null }>('form', {
    required: true,
    default: {
      ...defaultTransferForm,
    },
  });

  const formRef = ref<FormInst | null>(null);

  const rules: FormRules = {
    head: [{ required: true, message: t('opportunity.selectReceiverPlaceholder') }],
    belongToPublicPool: [{ required: true, message: t('clue.belongToPublicPool') }],
  };

  defineExpose({
    formRef,
  });
</script>

<style lang="less" scoped></style>
