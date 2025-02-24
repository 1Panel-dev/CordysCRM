<template>
  <div>
    <div v-if="props.fieldConfig.showLabel" class="mb-[8px]" :class="fieldConfig.titleColor">
      {{ props.fieldConfig.name }}
    </div>
    <n-divider
      class="!mb-[4px] !mt-0"
      :class="props.fieldConfig.dividerClass"
      :dashed="props.fieldConfig.dividerClass === 'divider--dashed'"
    />
    <div v-if="props.fieldConfig.description" class="text-[12px] leading-[20px] text-[var(--text-n4)]">
      {{ props.fieldConfig.description }}
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NDivider } from 'naive-ui';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();
</script>

<style lang="less" scoped>
  .divider--hidden {
    @apply hidden;
  }
  .divider--double {
    :deep(.n-divider__line) {
      height: 2px;
    }
  }
</style>
