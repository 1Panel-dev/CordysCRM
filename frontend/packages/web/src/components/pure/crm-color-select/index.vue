<template>
  <n-color-picker
    v-model:value="innerPureColor"
    v-bind="$attrs"
    default-show
    show-preview
    @update-value="handleUpdate"
  />
</template>

<script setup lang="ts">
  import { useDebounceFn } from '@vueuse/core';

  const innerPureColor = defineModel<string>('pureColor', {
    required: true,
  });

  const emits = defineEmits<{
    (e: 'change', color: string): void;
  }>();

  const handleUpdate = useDebounceFn((val: string) => {
    emits('change', val);
  }, 200);
</script>
