<template>
  <n-scrollbar class="h-full" @scroll="handleReachBottom">
    <div class="min-h-full px-[24px] pt-[24px]">
      <div class="mb-[16px] flex items-center justify-between">
        <div class="font-semibold">{{ props.title || t('common.changeRecord') }}</div>
        <n-button type="default" class="outline--secondary px-[8px]" :loading="loading" @click="loadRecords(true)">
          <CrmIcon type="iconicon_refresh" :size="16" />
        </n-button>
      </div>
      <n-spin :show="loading">
        <div v-if="records.length" class="change-record-list">
          <div v-for="record in records" :key="record.id" class="flex gap-[16px]">
            <div class="change-record-timeline">
              <div class="change-record-timeline-dot"></div>
              <div class="change-record-timeline-line"></div>
            </div>
            <div class="mb-[16px] flex min-w-0 flex-1 flex-col gap-[8px]">
              <div class="flex items-center">
                <span>{{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}</span>
                <CrmAvatar :size="24" :word="record.operatorName" :is-user="false" class="ml-[16px] mr-[8px]" />
                <n-tooltip :delay="300">
                  <template #trigger>
                    <span class="one-line-text max-w-[300px]">{{ record.operatorName }}</span>
                  </template>
                  {{ record.operatorName }}
                </n-tooltip>
              </div>
              <div
                v-if="[OperationTypeEnum.UPDATE, OperationTypeEnum.MERGE].includes(record.type as OperationTypeEnum)"
                class="flex flex-col gap-[8px]"
              >
                <div
                  v-for="field in record.diffs || []"
                  :key="field.column"
                  class="flex flex-col gap-[8px] rounded-[var(--border-radius-small)] border border-solid border-[var(--text-n8)] p-[12px] text-[12px]"
                >
                  <div class="font-semibold">{{ field.columnName }}:</div>
                  <div
                    v-if="getDiffValue(field.oldValueName, field.oldValue)"
                    class="change-record-value bg-[var(--error-5)]"
                  >
                    <span class="line-through">{{ getDiffValue(field.oldValueName, field.oldValue) }}</span>
                  </div>
                  <div
                    v-if="getDiffValue(field.newValueName, field.newValue)"
                    class="change-record-value bg-[var(--success-5)]"
                  >
                    {{ getDiffValue(field.newValueName, field.newValue) }}
                  </div>
                </div>
              </div>
              <div v-else class="rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[12px] text-[12px]">
                <div>{{ getTypeLabel(record.type) }}</div>
                <span>{{ record.detail }}</span>
              </div>
            </div>
          </div>
        </div>
        <n-empty v-else :description="t('common.noData')" class="pt-[80px]" />
      </n-spin>
    </div>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { NButton, NEmpty, NScrollbar, NSpin, NTooltip } from 'naive-ui';
  import dayjs from 'dayjs';

  import { OperationTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ChangeRecordQueryParams } from '@lib/shared/models/changeRecord';
  import type { CommonList } from '@lib/shared/models/common';
  import type { OperationLogDetail } from '@lib/shared/models/system/log';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';

  import { logTypeOption } from '@/config/system';

  export type ChangeRecordFetcher = (params: ChangeRecordQueryParams) => Promise<CommonList<OperationLogDetail>>;

  const props = defineProps<{
    sourceId: string;
    fetcher: ChangeRecordFetcher;
    title?: string;
  }>();

  const { t } = useI18n();
  const loading = ref(false);
  const records = ref<OperationLogDetail[]>([]);

  const pageNation = ref({
    current: 1,
    total: 0,
  });
  const pageSize = 20;

  async function loadRecords(refresh = false) {
    if (!props.sourceId || loading.value || (!refresh && records.value.length >= pageNation.value.total)) return;
    const current = refresh ? 1 : pageNation.value.current + 1;
    loading.value = true;
    try {
      const page = await props.fetcher({ sourceId: props.sourceId, current, pageSize });
      records.value = refresh ? page.list : [...records.value, ...page.list];
      pageNation.value = {
        current: page.current,
        total: page.total,
      };
    } finally {
      loading.value = false;
    }
  }

  function handleReachBottom(event: Event) {
    const element = event.target as HTMLElement;
    if (element.scrollHeight - element.scrollTop <= element.clientHeight + 24) {
      loadRecords();
    }
  }

  function getDiffValue(displayValue?: unknown, value?: unknown) {
    const targetValue = displayValue ?? value;
    if (targetValue === null || targetValue === undefined || targetValue === '') {
      return '';
    }
    return typeof targetValue === 'string' ? targetValue : JSON.stringify(targetValue);
  }

  function getTypeLabel(type: string) {
    return t(logTypeOption.find((item) => item.value === type)?.label ?? '');
  }

  watch(
    () => props.sourceId,
    () => {
      records.value = [];
      pageNation.value = { current: 1, total: 0 };
      loadRecords(true);
    },
    { immediate: true }
  );
</script>

<style scoped lang="less">
  .change-record-timeline {
    padding-top: 8px;
    width: 8px;
    gap: 8px;

    @apply flex flex-col items-center;
    &-dot {
      width: 8px;
      height: 8px;
      border: 2px solid var(--text-n7);
      border-radius: 50%;
      flex-shrink: 0;
    }
    &-line {
      width: 2px;
      height: 100%;
      background: var(--text-n8);
    }
  }
  .change-record-value {
    padding: 2px 8px;
    width: fit-content;
    border-radius: var(--border-radius-small);
    color: var(--text-n1);

    @apply whitespace-pre-line;
  }
</style>
