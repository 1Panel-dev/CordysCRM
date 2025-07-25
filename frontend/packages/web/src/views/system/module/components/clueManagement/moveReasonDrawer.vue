<template>
  <CrmReasonDrawer
    v-model:visible="showReasonDrawer"
    v-model:enable="enable"
    v-model:config="hasConfig"
    :title="t('crmReasonDrawer.clue')"
    :reason-key="ReasonTypeEnum.CLUE_POOL_RS"
    @load-config="() => emit('loadConfig')"
  />
</template>

<script setup lang="ts">
  import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmReasonDrawer from '@/components/business/crm-reason-drawer/index.vue';

  const { t } = useI18n();

  const emit = defineEmits<{
    (e: 'loadConfig'): void;
  }>();

  const showReasonDrawer = defineModel<boolean>('visible', {
    required: true,
  });

  const enable = defineModel<boolean>('enable', {
    required: true,
  });

  const hasConfig = defineModel<boolean>('config', {
    required: true,
  });
</script>

<style scoped></style>
