<template>
  <div class="flex items-center gap-[8px]">
    <CrmIcon
      :type="contractBusinessNameStatusMap[props.status]?.icon"
      :size="16"
      :class="`text-[${contractBusinessNameStatusMap[props.status]?.color}]`"
    />
    {{ contractBusinessNameStatusMap[props.status]?.label }}
  </div>
</template>

<script setup lang="ts">
  import { ContractBusinessNameStatusEnum } from '@lib/shared/enums/contractEnum';

  import { contractBusinessNameStatusMap } from '@/config/contract';

  const props = defineProps<{
    status: ContractBusinessNameStatusEnum;
  }>();
</script>

<style scoped></style>
