<template>
  <roleTreeNodePrefix v-if="props.type === 'CUSTOM'" :text="t('contract.businessTitle.dataSourceSelf')" />
  <CrmIcon v-else class="text-[var(--primary-8)]" type="iconicon_enterprise" :size="16" />
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import roleTreeNodePrefix from '@/components/business/crm-select-user-drawer/roleTreeNodePrefix.vue';

  const { t } = useI18n();

  const props = defineProps<{
    type: 'THIRD_PARTY' | 'CUSTOM';
  }>();
</script>

<style scoped></style>
