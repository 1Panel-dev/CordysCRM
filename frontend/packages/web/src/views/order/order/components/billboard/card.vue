<template>
  <div class="flex items-center justify-between">
    <CrmTableButton v-if="props.item.name" @click="emit('openDetail', 'order', props.item)">
      <template #trigger>{{ props.item.name }}</template>
      {{ props.item.name }}
    </CrmTableButton>
    <div v-else>-</div>
  </div>
  <div class="crm-stage-board-item-desc">
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
  <div class="crm-stage-board-item-desc">
    <n-tooltip trigger="hover" :delay="300">
      <template #trigger>
        <div class="crm-stage-board-item-desc-label">
          {{ fieldLabelMap.contractId }}
        </div>
      </template>
      {{ fieldLabelMap.contractId }}
    </n-tooltip>
    <div class="crm-stage-board-item-desc-value">
      <CrmTableButton
        v-if="props.item.contractName"
        size="small"
        class="text-[14px]"
        @click="emit('openDetail', 'contract', props.item)"
      >
        <template #trigger>{{ props.item.contractName }}</template>
        {{ props.item.contractName }}
      </CrmTableButton>
      <div v-else>-</div>
    </div>
  </div>
  <div class="crm-stage-board-item-desc">
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
        v-if="props.item.customerName"
        size="small"
        class="text-[14px]"
        @click="emit('openDetail', 'customer', props.item)"
      >
        <template #trigger>{{ props.item.customerName }}</template>
        {{ props.item.customerName }}
      </CrmTableButton>
      <div v-else>-</div>
    </div>
  </div>
  <div class="crm-stage-board-item-desc">
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
  <div class="crm-stage-board-item-desc">
    <n-tooltip trigger="hover" :delay="300">
      <template #trigger>
        <div class="crm-stage-board-item-desc-label">
          {{ fieldLabelMap.number }}
        </div>
      </template>
      {{ fieldLabelMap.number }}
    </n-tooltip>
    <n-tooltip trigger="hover" :delay="300">
      <template #trigger>
        <div class="crm-stage-board-item-desc-value one-line-text">{{ props.item.number || '-' }}</div>
      </template>
      {{ props.item.number || '-' }}
    </n-tooltip>
  </div>
</template>

<script setup lang="ts">
  import { NTooltip } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatNumberValue } from '@lib/shared/method/formCreate';

  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import { FormCreateField } from '@/components/business/crm-form-create/types';

  const props = defineProps<{
    item: any;
    fieldList: FormCreateField[];
  }>();

  const emit = defineEmits<{
    (e: 'openDetail', type: 'contract' | 'customer' | 'order', item: any): void;
  }>();

  const { t } = useI18n();

  const fieldLabelMap = computed(() => {
    const map: Record<string, string> = {
      amount: t('opportunity.totalAmount'),
      contractId: t('invoice.contractName'),
      customerId: t('module.customerManagement'),
      owner: t('common.owner'),
      number: t('common.number'),
    };
    props.fieldList.forEach((field) => {
      if (field.businessKey) {
        map[field.businessKey] = field.name;
      }
    });
    return map;
  });
</script>
