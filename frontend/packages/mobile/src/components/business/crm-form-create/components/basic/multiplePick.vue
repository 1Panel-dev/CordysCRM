<template>
  <van-field
    v-model="value"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    :rules="props.fieldConfig.rules as FieldRule[]"
    is-link
    readonly
    :placeholder="props.fieldConfig.placeholder"
    :disabled="props.fieldConfig.editable === false"
    clearable
    @click="showPicker = true"
    @update:model-value="($event) => emit('change', $event)"
  >
  </van-field>
  <van-popup v-model:show="showPicker" destroy-on-close round position="bottom">
    <!-- TODO: -->
  </van-popup>
</template>

<script setup lang="ts">
  import { FieldRule } from 'vant';

  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string): void;
  }>();

  const value = defineModel<string>('value', {
    default: '',
  });

  const showPicker = ref(false);

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      value.value = val;
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped></style>
