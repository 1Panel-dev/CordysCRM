<template>
  <div class="p-[16px]">
    <n-form
      :rules="rules"
      class="process-setting-form"
      require-mark-placement="right"
      :model="node"
      label-placement="top"
    >
      <n-form-item path="approvalType" :label="t('process.process.flow.approvalType')">
        <n-select
          :value="approvalType"
          :options="approvalTypeOptions"
          :placeholder="t('common.pleaseSelect')"
          @update:value="setApprovalType"
        />
      </n-form-item>
      <n-form-item path="name" :label="t('process.process.flow.nodeName')">
        <n-input v-model:value="node.name" :maxlength="255" type="text" :placeholder="t('common.pleaseInput')" />
      </n-form-item>
    </n-form>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import { FormRules, NForm, NFormItem, NInput, NSelect } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import type { ActionNode } from '@/components/business/crm-flow/types';

  import { type ApprovalType, approvalTypeOptions, resolveApprovalActionNodeDefaults } from '@/config/process';

  defineOptions({
    name: 'ApprovalActionNodeForm',
  });

  const props = defineProps<{
    node: ActionNode;
  }>();

  const { t } = useI18n();

  const rules: FormRules = {
    name: [
      {
        required: true,
        message: t('common.notNull', { value: `${t('process.process.flow.nodeName')}` }),
        trigger: ['blur'],
      },
    ],
  };

  const approvalType = computed<ApprovalType>(() => {
    const value = props.node.config?.approvalType;
    if (value === 'manual' || value === 'auto-approve' || value === 'auto-reject') {
      return value;
    }
    return 'manual';
  });

  function setApprovalType(value: ApprovalType) {
    props.node.config = {
      ...props.node.config,
      approvalType: value,
    };

    const defaults = resolveApprovalActionNodeDefaults(value);
    props.node.description = defaults.description;
  }
</script>
