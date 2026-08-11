<template>
  <CrmDrawer
    v-model:show="show"
    no-padding
    :footer="false"
    :show-mask="false"
    :title="detail?.name || '-'"
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
            <pre class="detail-section-content">{{ detail?.trace || '-' }}</pre>
          </div>
        </div>
      </CrmCard>
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import dayjs from 'dayjs';

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

  const statusLabelMap = computed(() => ({
    success: t('common.success'),
    failed: t('common.fail'),
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
      label: t('log.operationIp'),
      value: props.detail?.callIp || '-',
    },
    {
      label: t('log.executionTime'),
      value: props.detail?.callTime ? dayjs(props.detail.callTime).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      label: t('log.tokenCost'),
      value: props.detail?.totalTokens?.toLocaleString() || '-',
    },
    {
      label: t('common.status'),
      value: props.detail?.status ? statusLabelMap.value[props.detail.status] : '-',
      valueSlotName: 'status',
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
    white-space: pre-wrap;
    background: var(--text-n9);
  }
</style>
