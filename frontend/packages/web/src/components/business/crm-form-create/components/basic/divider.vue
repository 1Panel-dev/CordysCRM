<template>
  <div>
    <div v-if="props.fieldConfig.showLabel" class="mb-[8px]">{{ props.fieldConfig.name }}</div>
    <n-divider class="!mb-[4px] !mt-0" :class="props.fieldConfig.dividerClass" />
    <div v-if="props.fieldConfig.description" class="text-[12px] leading-[20px]">
      {{ props.fieldConfig.description }}
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NDivider } from 'naive-ui';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();
</script>

<style lang="less" scoped></style>
