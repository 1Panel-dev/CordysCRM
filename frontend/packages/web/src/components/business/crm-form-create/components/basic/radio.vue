<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="(props.fieldConfig.showLabel && !props.isSubTableRender) || props.isSubTableField"
    :path="props.path"
    :rule="props.fieldConfig.rules"
    :required="props.fieldConfig.rules.some((rule) => rule.key === 'required')"
    :label-placement="props.isSubTableField || props.isSubTableRender ? 'top' : props.formConfig?.labelPos"
  >
    <template #label>
      <div v-if="props.fieldConfig.showLabel" class="flex h-[22px] items-center gap-[4px] whitespace-nowrap">
        <div class="one-line-text">{{ props.fieldConfig.name }}</div>
        <CrmIcon v-if="props.fieldConfig.resourceFieldId" type="iconicon_correlation" />
      </div>
      <div v-else-if="props.isSubTableField || props.isSubTableRender" class="h-[22px]"></div>
    </template>
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-divider v-if="props.isSubTableField && !props.isSubTableRender" class="!my-0" />
    <n-radio-group
      v-model:value="value"
      :disabled="props.fieldConfig.editable === false || !!props.fieldConfig.resourceFieldId"
    >
      <n-space :item-class="props.fieldConfig.direction === 'horizontal' ? '' : 'w-full'">
        <n-radio v-for="item in props.fieldConfig.options" :key="item.value" :value="item.value">
          {{ item.label }}
        </n-radio>
      </n-space>
    </n-radio-group>
  </n-form-item>
</template>

<script setup lang="ts">
  import { NDivider, NFormItem, NRadio, NRadioGroup, NSpace } from 'naive-ui';

  import type { FormConfig } from '@lib/shared/models/system/module';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formConfig?: FormConfig;
    path: string;
    needInitDetail?: boolean; // 判断是否编辑情况
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
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
