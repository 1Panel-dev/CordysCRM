<template>
  <div class="flex gap-[16px]">
    <div class="crm-follow-time-line">
      <div class="crm-follow-time-dot"></div>
      <div class="crm-follow-time-line"></div>
    </div>
    <div class="mb-4 flex w-full flex-col gap-[8px]">
      <div>{{ dayjs(props.detail?.createTime).format('YYYY-MM-DD HH:mm:ss') }}</div>
      <div class="flex items-center gap-[8px]">
        <CrmAvatar :size="24" :word="props.detail?.operatorName" :is-user="false" />
        <n-tooltip :delay="300">
          <template #trigger>
            <div class="one-line-text max-w-[300px]">{{ props.detail?.operatorName }}</div>
          </template>
          {{ props.detail?.operatorName }}
        </n-tooltip>
      </div>
      <div
        v-if="[OperationTypeEnum.UPDATE, OperationTypeEnum.MERGE,OperationTypeEnum.APPROVAL].includes(props.detail?.type as OperationTypeEnum)"
        class="flex flex-col gap-[8px]"
      >
        <div
          v-for="item in props.detail?.diffs"
          :key="item.column"
          class="flex flex-col gap-[8px] rounded-[var(--border-radius-small)] border border-solid border-[var(--text-n8)] p-[12px] text-[12px]"
        >
          <div> {{ item.columnName }}:</div>
          <div
            v-if="(typeof item.oldValueName === 'number' ? String(item.oldValueName) : item.oldValueName)?.length"
            class="value-name bg-[var(--error-5)]"
          >
            <span class="line-through">
              {{ Array.isArray(item.oldValueName) ? item.oldValueName.join('；') : item.oldValueName }}
            </span>
          </div>
          <div
            v-if="(typeof item.newValueName === 'number' ? String(item.newValueName) : item.newValueName)?.length"
            class="value-name bg-[var(--success-5)]"
          >
            {{ Array.isArray(item.newValueName) ? item.newValueName.join('；') : item.newValueName }}
          </div>
        </div>
      </div>
      <div v-else class="rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[12px]">
        <span>
          {{ typeLabel }}
        </span>
        <span>
          {{ props.detail?.detail }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NTooltip } from 'naive-ui';
  import dayjs from 'dayjs';

  import { OperationTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { OperationLogDetail } from '@lib/shared/models/system/log';

  import CrmAvatar from '@/components/business/crm-avatar/index.vue';

  import { logTypeOption } from '@/config/system';

  const { t } = useI18n();

  const props = defineProps<{
    detail?: OperationLogDetail;
  }>();

  const typeLabel = computed(() => t(logTypeOption.find((e) => e.value === props.detail?.type)?.label ?? ''));
</script>

<style scoped lang="less">
  .crm-follow-time-line {
    padding-top: 8px;
    width: 8px;

    @apply flex flex-col items-center justify-center gap-2;
    .crm-follow-time-dot {
      width: 8px;
      height: 8px;
      border: 2px solid var(--text-n7);
      border-radius: 50%;
      flex-shrink: 0;
    }
    .crm-follow-time-line {
      width: 2px;
      background: var(--text-n8);
      @apply h-full;
    }
  }
  .value-name {
    padding: 2px 8px;
    border-radius: var(--border-radius-small);
    color: var(--text-n1);
    @apply w-fit whitespace-pre-line;
  }
</style>
