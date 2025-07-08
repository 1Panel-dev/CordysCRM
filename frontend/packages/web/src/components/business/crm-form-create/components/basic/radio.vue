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
    <n-radio-group v-model:value="value" :disabled="props.fieldConfig.editable === false">
      <n-space :item-class="props.fieldConfig.direction === 'horizontal' ? '' : 'w-full'">
        <n-radio v-for="item in props.fieldConfig.options" :key="item.value" :value="item.value">
          {{ item.label }}
        </n-radio>
      </n-space>
    </n-radio-group>
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem, NRadio, NRadioGroup, NSpace } from 'naive-ui';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
    needInitDetail?: boolean; // 判断是否编辑情况
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string | number): void;
  }>();

  const value = defineModel<string>('value', {
    default: '',
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      if (!props.needInitDetail) {
        value.value = val;
        emit('change', value.value);
      }
    }
  );

  watch(
    () => value.value,
    (val) => {
      emit('change', val);
    }
  );

  onBeforeMount(() => {
    if (!props.needInitDetail) {
      value.value = props.fieldConfig.defaultValue || value.value;
      emit('change', value.value);
    }
  });
</script>

<style lang="less" scoped></style>
