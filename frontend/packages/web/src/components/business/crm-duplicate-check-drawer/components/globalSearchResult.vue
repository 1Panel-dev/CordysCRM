<template>
  <div class="flex items-center font-semibold">
    {{ t('workbench.duplicateCheck.searchInCordysResult', { name: props.title }) }}
    <div class="text-[var(--text-n4)]"> （{{ props.total }}） </div>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  const { t } = useI18n();

  const props = defineProps<{
    total: number;
    title: string;
  }>();

  const emit = defineEmits<{
    (e: 'initTotal', val: number): void;
  }>();

  watch(
    () => props.total,
    () => {
      emit('initTotal', props.total);
    },
    {
      immediate: true,
    }
  );
</script>

<style scoped></style>
