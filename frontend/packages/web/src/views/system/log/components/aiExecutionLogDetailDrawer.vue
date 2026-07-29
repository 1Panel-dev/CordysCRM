<template>
  <CrmDrawer
    v-model:show="show"
    no-padding
    :footer="false"
    :show-mask="false"
    :title="detail?.taskName || '-'"
    :width="680"
  >
    <div class="h-full bg-[var(--text-n9)] p-[16px]">
      <CrmCard hide-footer>
        <div class="flex w-full flex-col gap-[24px]">
          <div>
            <div class="mb-[16px] font-[600]">{{ t('common.baseInfo') }}</div>
            <CrmDescription
              :descriptions="descriptions"
              :column="2"
              labelWidth="95px"
              label-align="start"
              value-align="start"
            />
          </div>

          <div class="detail-section">
            <div class="detail-section-title">{{ t('log.rawPrompt') }}</div>
            <div class="detail-section-content">{{ detail?.prompt || '-' }}</div>
          </div>

          <div class="detail-section">
            <div class="detail-section-title">{{ t('log.aiResult') }}</div>
            <div class="detail-section-content">{{ detail?.result || '-' }}</div>
          </div>

          <div class="detail-section">
            <div class="detail-section-title">{{ t('common.executionResult') }}</div>
            <div class="detail-section-content text-[var(--success)]">{{ detail?.executionResult || '-' }}</div>
          </div>
        </div>
      </CrmCard>
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiExecutionLogItem } from '@lib/shared/models/system/log';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDescription, { type Description } from '@/components/pure/crm-description/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';

  const { t } = useI18n();

  const props = defineProps<{
    detail?: AiExecutionLogItem;
  }>();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  const operationTypeLabelMap = computed(() => ({
    data_write: t('log.operationType.dataWrite'),
    data_read: t('log.operationType.dataRead'),
    task_execution: t('log.operationType.taskExecution'),
    model_call: t('log.operationType.modelCall'),
  }));

  const descriptions = computed<Description[]>(() => [
    {
      label: t('log.logId'),
      value: props.detail?.id || '-',
    },
    {
      label: t('common.operator'),
      value: props.detail?.operatorName || '-',
    },
    {
      label: 'IP',
      value: props.detail?.operatorIp || '-',
    },
    {
      label: t('log.usedModel'),
      value: props.detail?.modelName || '-',
    },
    {
      label: t('log.tokenCost'),
      value: props.detail?.tokenCost?.toLocaleString() || '-',
    },
    {
      label: t('log.operationType'),
      value: props.detail?.operationType ? operationTypeLabelMap.value[props.detail.operationType] : '-',
    },
  ]);
</script>

<style scoped lang="less">
  .detail-section {
    display: flex;
    flex-direction: column;
    gap: 5px;
  }
  .detail-section-title {
    color: var(--text-n2);
  }
  .detail-section-content {
    padding: 16px;
    border-radius: var(--border-radius-small);
    background: var(--text-n9);
  }
</style>
