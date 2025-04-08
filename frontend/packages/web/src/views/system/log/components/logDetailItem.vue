<template>
  <div class="flex gap-[16px]">
    <div class="crm-follow-time-line">
      <div class="crm-follow-time-dot"></div>
      <div class="crm-follow-time-line"></div>
    </div>
    <div class="mb-4 flex w-full flex-col gap-[8px]">
      <div>{{ dayjs(props.detail?.createTime).format('YYYY-MM-DD HH:mm:ss') }}</div>
      <div class="flex items-center gap-[8px]">
        <CrmAvatar :size="24" :word="props.detail?.operatorName" />
        <n-tooltip :delay="300">
          <template #trigger>
            <div class="one-line-text max-w-[300px]">{{ props.detail?.operatorName }} </div>
          </template>
          {{ props.detail?.operatorName }}
        </n-tooltip>
      </div>
      <div v-if="detail?.type === OperationTypeEnum.UPDATE" class="flex flex-col gap-[8px]">
        <div
          v-for="item in props.detail?.diffs"
          :key="item.column"
          class="flex flex-col gap-[8px] rounded-[var(--border-radius-small)] border border-solid border-[var(--text-n8)] p-[12px] text-[12px]"
        >
          <div> {{ item.columnName }}: </div>
          <CrmTag
            v-if="item.oldValueName?.length"
            theme="light"
            type="error"
            :color="{
              color: 'var(--error-5)',
              textColor: 'var(--text-n1)',
            }"
            class="w-fit"
          >
            <span class="line-through">{{ item.oldValueName }}</span>
          </CrmTag>
          <CrmTag
            v-if="item.newValueName?.length"
            theme="light"
            type="success"
            :color="{
              color: 'var(--success-5)',
              textColor: 'var(--text-n1)',
            }"
            class="w-fit"
          >
            {{ item.newValueName }}
          </CrmTag>
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
  import type { OperationLogDetail } from '@lib/shared/models/system/log';

  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';

  import { logTypeOption } from '@/config/system';

  const props = defineProps<{
    detail?: OperationLogDetail;
  }>();

  const typeLabel = computed(() => logTypeOption.find((e) => e.value === props.detail?.type)?.label);
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
</style>
