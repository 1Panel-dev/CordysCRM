<template>
  <van-field
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    :rules="props.fieldConfig.rules as FieldRule[]"
  >
    <template #input>
      <van-radio-group
        v-model="value"
        direction="horizontal"
        class="gap-y-[8px]"
        :disabled="props.fieldConfig.editable === false"
        @change="($event) => emit('change', $event)"
      >
        <van-radio v-for="item in props.fieldConfig.options" :key="item.value" :name="item.value">
          {{ item.label }}
        </van-radio>
      </van-radio-group>
    </template>
  </van-field>
</template>

<script setup lang="ts">
  import { FieldRule } from 'vant';

  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: number): void;
  }>();

  const value = defineModel<any>('value', {
    default: null,
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      value.value = val || value.value;
    },
    {
      immediate: true,
    }
  );
</script>
