<template>
  <div class="flex items-center gap-[8px]">
    <CrmIcon :type="statusMap[props.status]?.icon" :size="16" :class="`text-[${statusMap[props.status]?.color}]`" />
    {{ statusMap[props.status]?.label }}
  </div>
</template>

<script setup lang="ts">
  import { ContractPaymentPlanEnum, ContractStatusEnum } from '@lib/shared/enums/contractEnum';

  import { contractPaymentPlanStatus, contractStatusMap } from '@/config/contract';

  const props = defineProps<{
    status: ContractPaymentPlanEnum | ContractStatusEnum;
  }>();

  const statusMap = {
    ...contractPaymentPlanStatus,
    ...contractStatusMap,
  };
</script>
