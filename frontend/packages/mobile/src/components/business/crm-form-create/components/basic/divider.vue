<template>
  <div class="mx-[16px] mb-[16px] mt-[16px]">
    <div
      v-if="props.fieldConfig.showLabel"
      class="mb-[8px] font-semibold"
      :style="{
        color: fieldConfig.titleColor,
      }"
    >
      {{ props.fieldConfig.name }}
    </div>
    <van-divider
      class="!mb-[4px] !mt-0"
      :class="props.fieldConfig.dividerClass"
      :style="{ borderColor: dividerColor }"
      :dashed="props.fieldConfig.dividerClass === 'divider--dashed'"
    />
  </div>
</template>

<script setup lang="ts">
  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();

  const dividerColor = computed(() => {
    return props.fieldConfig.dividerColor || 'var(--text-n4)';
  });
</script>

<style lang="less" scoped>
  .divider--hidden {
    @apply hidden;
  }
  .divider--double {
    &::before {
      border-width: 2px 0 0;
    }
  }
</style>
