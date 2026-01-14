<template>
  <div class="flex items-center gap-[8px]">
    <CrmIcon :type="statusMap[props.status]?.icon" :size="16" :class="`text-[${statusMap[props.status]?.color}]`" />
    {{ statusMap[props.status]?.label }}
  </div>
</template>

<script setup lang="ts">
  import { ContractInvoiceEnum } from '@lib/shared/enums/contractEnum';

  import { contractInvoiceStatus } from '@/config/contract';

  const props = defineProps<{
    status: ContractInvoiceEnum;
  }>();

  const statusMap = {
    ...contractInvoiceStatus,
  };
</script>
