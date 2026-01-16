<template>
  <div class="flex items-center gap-[8px]">
    <CrmIcon :type="statusMap[props.status]?.icon" :size="16" :class="`text-[${statusMap[props.status]?.color}]`" />
    {{ statusMap[props.status]?.label }}
  </div>
</template>

<script setup lang="ts">
  import { ContractInvoiceStatusEnum } from '@lib/shared/enums/contractEnum';

  import { contractInvoiceStatus } from '@/config/contract';

  const props = defineProps<{
    status: ContractInvoiceStatusEnum;
  }>();

  const statusMap = {
    ...contractInvoiceStatus,
  };
</script>
