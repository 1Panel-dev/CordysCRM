<template>
  <CrmModal
    v-model:show="visible"
    :title="t('crmFormCreate.submitValidationProgressTitle')"
    :width="352"
    :footer="false"
    :mask="false"
    :mask-closable="false"
    class="submit-validation-progress-modal"
  >
    <div class="flex flex-col gap-[12px] py-[4px]">
      <div
        v-for="(rule, index) in props.rules"
        :key="index"
        class="flex items-center gap-[12px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] px-[16px] py-[12px]"
      >
        <div class="w-[12px] shrink-0 text-[14px] leading-[22px] text-[var(--text-n1)]">{{ index + 1 }}</div>
        <n-tooltip trigger="hover">
          <template #trigger>
            <div class="one-line-text flex-1 text-[14px] leading-[22px] text-[var(--text-n1)]">{{ rule.message }}</div>
          </template>
          {{ rule.message }}
        </n-tooltip>

        <div class="flex shrink-0 items-center gap-[8px]">
          <CrmIcon
            :type="statusMap[props.statusList[index] || 'pending'].icon"
            :size="16"
            :color="statusMap[props.statusList[index] || 'pending'].color"
          />
          <span class="text-[14px] leading-[22px] text-[var(--text-n1)]">
            {{ t(statusMap[props.statusList[index] || 'pending'].label) }}
          </span>
          <n-button
            v-if="props.ignoreRuleIndices.includes(index)"
            text
            type="primary"
            size="small"
            @click="emit('ignore', index)"
          >
            {{ t('crmFormCreate.submitValidationIgnore') }}
          </n-button>
        </div>
      </div>
    </div>
  </CrmModal>
</template>

<script setup lang="ts">
  import { NButton, NTooltip } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FormSubmitValidationRule, FormSubmitValidationRuleStatus } from '@lib/shared/models/system/module';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';

  const props = defineProps<{
    rules: FormSubmitValidationRule[];
    statusList: FormSubmitValidationRuleStatus[];
    ignoreRuleIndices: number[];
  }>();
  const emit = defineEmits<{
    (e: 'ignore', index: number): void;
  }>();

  const visible = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const { t } = useI18n();

  const statusMap: Record<FormSubmitValidationRuleStatus, { icon: string; color: string; label: string }> = {
    pending: {
      icon: 'iconicon_minus_circle_filled1',
      color: 'var(--text-n4)',
      label: 'crmFormCreate.submitValidationNotExecuted',
    },
    pass: {
      icon: 'iconicon_check_circle_filled',
      color: 'var(--success-green)',
      label: 'crmFormCreate.submitValidationPassed',
    },
    fail: {
      icon: 'iconicon_close_circle_filled',
      color: 'var(--error-red)',
      label: 'crmFormCreate.submitValidationFailed',
    },
    ignored: {
      icon: 'iconicon_check_circle_filled',
      color: 'var(--text-n4)',
      label: 'crmFormCreate.submitValidationIgnored',
    },
    skipped: {
      icon: 'iconicon_minus_circle_filled1',
      color: 'var(--text-n4)',
      label: 'crmFormCreate.submitValidationNotExecuted',
    },
  };
</script>

<style lang="less">
  // 无蒙层进度弹窗，固定在页面顶部展示
  .submit-validation-progress-modal {
    align-self: flex-start;
    margin-top: 80px;
  }
</style>
