<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="props.fieldConfig.showLabel"
    :path="props.path"
    :rule="props.fieldConfig.rules"
  >
    <div v-if="props.fieldConfig.description" class="n-form-item-desc" v-html="props.fieldConfig.description"></div>
    <n-checkbox-group
      v-model:value="value"
      :default-value="props.fieldConfig.defaultValue"
      :disabled="props.fieldConfig.editable === false"
    >
      <n-space :item-class="props.fieldConfig.direction === 'horizontal' ? '' : 'w-full'">
        <n-checkbox
          v-for="(item, i) in props.fieldConfig.options"
          :key="item.value"
          :value="item.value"
          :label="t(item.label, { i: i + 1 })"
        />
      </n-space>
    </n-checkbox-group>
  </n-form-item>
</template>

<script setup lang="ts">
  import { NCheckbox, NCheckboxGroup, NFormItem, NSpace } from 'naive-ui';

  import { useI18n } from '@/hooks/useI18n';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();

  const { t } = useI18n();

  const value = defineModel<(string | number)[]>('value', {
    default: [],
  });
</script>

<style lang="less" scoped></style>
