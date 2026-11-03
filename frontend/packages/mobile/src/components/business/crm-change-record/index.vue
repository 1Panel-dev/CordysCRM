<template>
  <div class="flex h-full flex-col bg-[var(--text-n10)] p-[16px]">
    <div class="mb-[16px] font-semibold text-[var(--text-n1)]">
      {{ props.title || t('common.changeRecord') }}
    </div>
    <CrmList
      ref="changeRecordListRef"
      v-model="records"
      class="min-h-0 flex-1"
      :load-list-api="loadRecords"
      :close-init-load="!props.sourceId || !props.formKey"
      :item-gap="0"
    >
      <template #item="{ item }">
        <div class="flex gap-[12px]">
          <div class="change-record-timeline">
            <div class="change-record-timeline-dot"></div>
            <div class="change-record-timeline-line"></div>
          </div>
          <div class="mb-[16px] min-w-0 flex-1">
            <div class="mb-[8px] flex items-center gap-[8px] text-[var(--text-n2)]">
              <span class="shrink-0 text-[var(--text-n1)]">
                {{ dayjs(item.createTime).format('YYYY-MM-DD HH:mm:ss') }}
              </span>
              <CrmAvatar :size="24" :text="item.operatorName" :is-user="false" />
              <span class="one-line-text">{{ item.operatorName }}</span>
            </div>
            <div
              v-if="[OperationTypeEnum.UPDATE, OperationTypeEnum.MERGE].includes(item.type)"
              class="flex flex-col gap-[8px]"
            >
              <div
                v-for="field in item.diffs || []"
                :key="field.column"
                class="rounded-[var(--border-radius-small)] border border-[var(--text-n8)] bg-[var(--text-n10)] p-[12px]"
              >
                <div class="mb-[8px] font-semibold text-[var(--text-n1)]">{{ field.columnName }}：</div>
                <div
                  v-if="getDiffValue(field.oldValueName, field.oldValue)"
                  class="change-record-value bg-[var(--error-5)]"
                >
                  <span class="line-through">{{ getDiffValue(field.oldValueName, field.oldValue) }}</span>
                </div>
                <div
                  v-if="getDiffValue(field.newValueName, field.newValue)"
                  class="change-record-value mt-[8px] bg-[var(--success-5)]"
                >
                  {{ getDiffValue(field.newValueName, field.newValue) }}
                </div>
              </div>
            </div>
            <div
              v-else
              class="rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[12px] text-[14px] text-[var(--text-n2)]"
            >
              <div>{{ getTypeLabel(item.type) }}</div>
              <div v-if="item.detail" class="mt-[4px] break-words">{{ item.detail }}</div>
            </div>
          </div>
        </div>
      </template>
    </CrmList>
  </div>
</template>

<script setup lang="ts">
  import dayjs from 'dayjs';

  import { OperationTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ChangeRecordQueryParams } from '@lib/shared/models/changeRecord';
  import type { CommonList } from '@lib/shared/models/common';
  import type { OperationLogDetail } from '@lib/shared/models/system/log';

  import CrmAvatar from '@/components/business/crm-avatar/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import { getChangeLogPage } from '@/api/modules';

  const props = defineProps<{
    sourceId: string;
    formKey: string;
    title?: string;
  }>();

  const { t } = useI18n();
  const records = ref<OperationLogDetail[]>([]);
  const changeRecordListRef = ref<InstanceType<typeof CrmList>>();

  function loadRecords(params: ChangeRecordQueryParams): Promise<CommonList<OperationLogDetail>> {
    return getChangeLogPage(props.formKey, {
      ...params,
      sourceId: props.sourceId,
    });
  }

  function getDiffValue(displayValue?: unknown, value?: unknown) {
    const targetValue = displayValue ?? value;
    if (targetValue === null || targetValue === undefined || targetValue === '') return '';
    return typeof targetValue === 'string' ? targetValue : JSON.stringify(targetValue);
  }

  function getTypeLabel(type: string) {
    const typeLabelMap: Partial<Record<OperationTypeEnum, string>> = {
      [OperationTypeEnum.ADD]: 'common.new',
      [OperationTypeEnum.DELETE]: 'common.delete',
      [OperationTypeEnum.IMPORT]: 'common.import',
      [OperationTypeEnum.EXPORT]: 'common.export',
      [OperationTypeEnum.SYNC]: 'common.sync',
      [OperationTypeEnum.MOVE_TO_CUSTOMER_POOL]: 'customer.moveToSeaOrPool',
      [OperationTypeEnum.PICK]: 'common.claim',
      [OperationTypeEnum.ASSIGN]: 'common.distribute',
      [OperationTypeEnum.CANCEL]: 'common.cancel',
      [OperationTypeEnum.ADD_USER]: 'common.addUser',
      [OperationTypeEnum.REMOVE_USER]: 'common.removeUser',
      [OperationTypeEnum.APPROVAL]: 'common.approval',
      [OperationTypeEnum.VOIDED]: 'common.voided',
      [OperationTypeEnum.CANCEL_VOID]: 'common.cancelVoid',
      [OperationTypeEnum.DOWNLOAD]: 'common.download',
      [OperationTypeEnum.FREEZE]: 'common.freeze',
      [OperationTypeEnum.UNFREEZE]: 'common.unfreeze',
    };
    return t(typeLabelMap[type as OperationTypeEnum] || '');
  }

  watch(
    () => [props.sourceId, props.formKey],
    () => {
      records.value = [];
      nextTick(() => changeRecordListRef.value?.loadList(true));
    }
  );
</script>

<style scoped lang="less">
  .change-record-timeline {
    display: flex;
    align-items: center;
    padding-top: 8px;
    width: 8px;
    flex-direction: column;
    gap: 8px;
  }
  .change-record-timeline-dot {
    width: 8px;
    height: 8px;
    flex-shrink: 0;
    border: 2px solid var(--text-n7);
    border-radius: 50%;
  }
  .change-record-timeline-line {
    width: 2px;
    height: 100%;
    background: var(--text-n8);
  }
  .change-record-value {
    padding: 2px 8px;
    width: fit-content;
    border-radius: var(--border-radius-small);
    white-space: pre-line;
    color: var(--text-n1);
  }
</style>
