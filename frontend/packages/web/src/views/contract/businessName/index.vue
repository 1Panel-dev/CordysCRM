<template>
  <div ref="businessCardRef" class="h-full">
    <CrmCard no-content-padding hide-footer>
      <div class="h-full px-[16px] pt-[16px]">
        <businessNameTable />
      </div>
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import businessNameTable from './components/businessNameTable.vue';

  const { t } = useI18n();
  const Message = useMessage();
</script>

<style scoped></style>
