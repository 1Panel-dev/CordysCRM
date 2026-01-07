<template>
  <div class="flex items-center gap-[8px]">
    <CrmIcon
      :type="contractBusinessTitleStatusMap[props.status]?.icon"
      :size="16"
      :class="`text-[${contractBusinessTitleStatusMap[props.status]?.color}]`"
    />
    {{ contractBusinessTitleStatusMap[props.status]?.label }}
  </div>
</template>

<script setup lang="ts">
  import { ContractBusinessTitleStatusEnum } from '@lib/shared/enums/contractEnum';

  import { contractBusinessTitleStatusMap } from '@/config/contract';

  const props = defineProps<{
    status: ContractBusinessTitleStatusEnum;
  }>();
</script>

<style scoped></style>
