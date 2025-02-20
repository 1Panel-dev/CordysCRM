<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="props.fieldConfig.showLabel"
    :path="props.path"
    :rule="props.fieldConfig.rules"
  >
    <div v-if="props.fieldConfig.description" class="n-form-item-desc" v-html="props.fieldConfig.description"></div>
    <n-input
      v-model:value="value"
      :maxlength="11"
      :placeholder="props.fieldConfig.placeholder"
      :default-value="props.fieldConfig.defaultValue"
      :disabled="props.fieldConfig.editable === false"
      :allow-input="onlyAllowNumber"
      clearable
    >
      <template #prefix>
        <CrmIcon type="iconicon_phone" />
      </template>
    </n-input>
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem, NInput } from 'naive-ui';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();

  const value = defineModel<string>('value', {
    default: '',
  });

  function onlyAllowNumber(val: string) {
    return !val || /^\d+$/.test(val);
  }
</script>

<style lang="less" scoped></style>
