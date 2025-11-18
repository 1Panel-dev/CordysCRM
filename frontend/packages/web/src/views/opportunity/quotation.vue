<template>
  <CrmCard hide-footer no-content-padding>
    <div class="h-full px-[16px] pt-[16px]">
      <quotationTable :formKey="FormDesignKeyEnum.BUSINESS" />
    </div>
  </CrmCard>
</template>
<script setup lang="ts">
  import { ref } from 'vue';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { useMessage } from 'naive-ui';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import quotationTable from './components/quotation/quotationTable.vue';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

  const Message = useMessage();
</script>
<style scoped></style>
