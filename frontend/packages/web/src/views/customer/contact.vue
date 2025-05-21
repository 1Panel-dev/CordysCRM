<template>
  <ContactTable :form-key="FormDesignKeyEnum.CONTACT" />
</template>

<script setup lang="ts">
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

  import ContactTable from '@/components/business/crm-form-create-table/contactTable.vue';
</script>
