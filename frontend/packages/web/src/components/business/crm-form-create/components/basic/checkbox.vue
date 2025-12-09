<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="props.fieldConfig.showLabel"
    :path="props.path"
    :rule="props.fieldConfig.rules"
    :required="props.fieldConfig.rules.some((rule) => rule.key === 'required')"
  >
    <template #label>
      <div class="flex items-center gap-[4px]">
        {{ props.fieldConfig.name }}
        <CrmIcon v-if="props.fieldConfig.resourceFieldId" type="iconicon_correlation" />
      </div>
    </template>
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-divider v-if="props.isSubTableField && !props.isSubTableRender" class="!my-0" />
    <n-checkbox-group
      v-model:value="value"
      :disabled="props.fieldConfig.editable === false || !!props.fieldConfig.resourceFieldId"
      @update-value="($event) => emit('change', $event)"
    >
      <n-space :item-class="props.fieldConfig.direction === 'horizontal' ? '' : 'w-full'">
        <n-checkbox
          v-for="item in props.fieldConfig.options"
          :key="item.value"
          :value="item.value"
          :label="item.label"
        />
      </n-space>
    </n-checkbox-group>
  </n-form-item>
</template>

<script setup lang="ts">
  import { NCheckbox, NCheckboxGroup, NDivider, NFormItem, NSpace } from 'naive-ui';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
    needInitDetail?: boolean; // 判断是否编辑情况
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
  }>();
  const emit = defineEmits<{
    (e: 'change', value: (string | number)[]): void;
  }>();

  const value = defineModel<(string | number)[]>('value', {
    default: [],
  });

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
