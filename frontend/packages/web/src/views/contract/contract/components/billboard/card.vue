<template>
  <div class="flex items-center justify-between">
    <CrmTableButton v-if="props.item.name" @click="emit('openDetail', 'contract', props.item)">
      <template #trigger>{{ props.item.name }}</template>
      {{ props.item.name }}
    </CrmTableButton>
    <div v-else>-</div>
  </div>
  <div class="crm-stage-board-item-desc crm-stage-board-item-desc--wide">
    <n-tooltip trigger="hover" :delay="300">
      <template #trigger>
        <div class="crm-stage-board-item-desc-label">
          {{ fieldLabelMap.amount }}
        </div>
      </template>
      {{ fieldLabelMap.amount }}
    </n-tooltip>
    <div class="crm-stage-board-item-desc-value">
      {{
        formatNumberValue(
          props.item.amount,
          (props.fieldList.find((field) => field.businessKey === 'amount') as FormCreateField) || {}
        )
      }}
    </div>
  </div>
  <div class="crm-stage-board-item-desc crm-stage-board-item-desc--wide">
    <n-tooltip trigger="hover" :delay="300">
      <template #trigger>
        <div class="crm-stage-board-item-desc-label">
          {{ fieldLabelMap.customerId }}
        </div>
      </template>
      {{ fieldLabelMap.customerId }}
    </n-tooltip>
    <div class="crm-stage-board-item-desc-value">
      <CrmTableButton
        v-if="props.item.customerName && hasAnyPermission(['CUSTOMER_MANAGEMENT:READ'])"
        size="small"
        class="text-[14px]"
        @click="emit('openDetail', 'customer', props.item)"
      >
        <template #trigger>{{ props.item.customerName }}</template>
        {{ props.item.customerName }}
      </CrmTableButton>
      <CrmNameTooltip v-else :text="props.item.customerName ?? '-'" />
    </div>
  </div>
  <div class="crm-stage-board-item-desc crm-stage-board-item-desc--wide">
    <n-tooltip trigger="hover" :delay="300">
      <template #trigger>
        <div class="crm-stage-board-item-desc-label">
          {{ fieldLabelMap.owner }}
        </div>
      </template>
      {{ fieldLabelMap.owner }}
    </n-tooltip>
    <div class="crm-stage-board-item-desc-value">{{ props.item.ownerName || '-' }}</div>
  </div>
  <div class="crm-stage-board-item-desc crm-stage-board-item-desc--wide">
    <n-tooltip trigger="hover" :delay="300">
      <template #trigger>
        <div class="crm-stage-board-item-desc-label">
          {{ fieldLabelMap.alreadyPayAmount }}
        </div>
      </template>
      {{ fieldLabelMap.alreadyPayAmount }}
    </n-tooltip>
    <div class="crm-stage-board-item-desc-value">
      {{ formatThousands(props.item.alreadyPayAmount) || '-' }}
    </div>
  </div>
  <div class="crm-stage-board-item-desc crm-stage-board-item-desc--wide">
    <n-tooltip trigger="hover" :delay="300">
      <template #trigger>
        <div class="crm-stage-board-item-desc-label">
          {{ fieldLabelMap.startTime }}
        </div>
      </template>
      {{ fieldLabelMap.startTime }}
    </n-tooltip>
    <div class="crm-stage-board-item-desc-value">
      {{ props.item.startTime ? dayjs(props.item.startTime).format('YYYY-MM-DD') : '-' }}
    </div>
  </div>
  <div class="crm-stage-board-item-desc crm-stage-board-item-desc--wide">
    <n-tooltip trigger="hover" :delay="300">
      <template #trigger>
        <div class="crm-stage-board-item-desc-label">
          {{ fieldLabelMap.endTime }}
        </div>
      </template>
      {{ fieldLabelMap.endTime }}
    </n-tooltip>
    <div
      class="crm-stage-board-item-desc-value"
      :class="{ '!text-[var(--error-red)]': dayjs(props.item.endTime).isSame(dayjs(), 'M') }"
    >
      {{ props.item.endTime ? dayjs(props.item.endTime).format('YYYY-MM-DD') : '-' }}
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NTooltip } from 'naive-ui';
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatThousands } from '@lib/shared/method';
  import { formatNumberValue } from '@lib/shared/method/formCreate';

  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import { FormCreateField } from '@/components/business/crm-form-create/types';

  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    item: any;
    fieldList: FormCreateField[];
  }>();

  const emit = defineEmits<{
    (e: 'openDetail', type: 'contract' | 'customer', item: any): void;
  }>();

  const { t } = useI18n();

  const fieldLabelMap = computed(() => {
    const map: Record<string, string> = {
      amount: t('contract.contractAmount'),
      customerId: t('module.customerManagement'),
      owner: t('common.owner'),
      alreadyPayAmount: t('contract.alreadyPayAmount'),
      createTime: t('common.createTime'),
    };
    props.fieldList.forEach((field) => {
      if (field.businessKey) {
        map[field.businessKey] = field.name;
      }
    });
    return map;
  });
</script>
