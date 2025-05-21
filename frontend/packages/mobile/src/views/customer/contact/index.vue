<template>
  <CrmContactList :source-id="props.customerId" :form-key="FormDesignKeyEnum.CONTACT" />
</template>

<script setup lang="ts">
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

  import CrmContactList from '@/components/business/crm-contact-list/index.vue';

  const props = defineProps<{
    customerId?: string;
  }>();
</script>

<style lang="less" scoped></style>
