<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="props.fieldConfig.showLabel"
    :path="props.path"
    :rule="props.fieldConfig.rules"
    :required="props.fieldConfig.rules.some((rule) => rule.key === 'required')"
  >
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-input
      v-model:value="value"
      :maxlength="Number(props.fieldConfig.format)"
      :placeholder="props.fieldConfig.placeholder"
      :disabled="props.fieldConfig.editable === false"
      :allow-input="onlyAllowNumber"
      clearable
      @update-value="($event) => emit('change', $event)"
    >
      <template #prefix>
        <CrmIcon type="iconicon_phone" />
      </template>
    </n-input>
  </n-form-item>
</template>

<script setup lang="ts">
  import { watch } from 'vue';
  import { NFormItem, NInput } from 'naive-ui';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
    needInitDetail?: boolean; // 判断是否编辑情况
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string): void;
  }>();

  const value = defineModel<string>('value', {
    default: '',
  });

  function onlyAllowNumber(val: string) {
    if (!val) return true;
    return /^[0-9+\- ()（）]*$/.test(val);
  }

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      if (!props.needInitDetail) {
        value.value = val || value.value;
        emit('change', value.value);
      }
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped></style>
