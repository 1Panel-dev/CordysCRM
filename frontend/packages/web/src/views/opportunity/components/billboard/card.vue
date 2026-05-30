<template>
  <div class="flex items-center justify-between">
    <CrmTableButton v-if="props.item.name" @click="emit('openDetail', 'opportunity', props.item)">
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
          {{ fieldLabelMap.products }}
        </div>
      </template>
      {{ fieldLabelMap.products }}
    </n-tooltip>
    <div class="crm-stage-board-item-desc-value">
      <CrmTagGroup v-if="props.item.products.length > 0" :tags="productNames" />
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
  <div class="crm-stage-board-item-desc">
    <n-tooltip trigger="hover" :delay="300">
      <template #trigger>
        <div class="crm-stage-board-item-desc-label">
          {{ fieldLabelMap.owner }}
        </div>
      </template>
      {{ fieldLabelMap.owner }}
    </n-tooltip>
    <div class="crm-stage-board-item-desc-value">{{ props.item.ownerName }}</div>
  </div>
  <div class="crm-stage-board-item-desc">
    <n-tooltip trigger="hover" :delay="300">
      <template #trigger>
        <div class="crm-stage-board-item-desc-label">
          {{ fieldLabelMap.expectedEndTime }}
        </div>
      </template>
      {{ fieldLabelMap.expectedEndTime }}
    </n-tooltip>
    <div
      class="crm-stage-board-item-desc-value"
      :class="{ '!text-[var(--error-red)]': dayjs(props.item.expectedEndTime).isSame(dayjs(), 'M') }"
    >
      {{ props.item.expectedEndTime ? dayjs(props.item.expectedEndTime).format('YYYY-MM-DD') : '-' }}
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NTooltip } from 'naive-ui';
  import dayjs from 'dayjs';

  import { formatNumberValue } from '@lib/shared/method/formCreate';

  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmTagGroup from '@/components/pure/crm-tag-group/index.vue';
  import { FormCreateField } from '@/components/business/crm-form-create/types';

  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    item: any;
    fieldList: FormCreateField[];
    optionMap?: Record<string, any>;
  }>();

  const emit = defineEmits<{
    (e: 'openDetail', type: 'customer' | 'opportunity', item: any): void;
  }>();

  const fieldLabelMap = computed(() => {
    const map: Record<string, string> = {};
    props.fieldList.forEach((field) => {
      if (field.businessKey) {
        map[field.businessKey] = field.name;
      }
    });
    return map;
  });

  const productNames = computed(() => {
    const products = props.optionMap?.products || [];
    return products
      .filter((product: any) => props.item.products.includes(product.id))
      .map((product: any) => product.name);
  });
</script>
