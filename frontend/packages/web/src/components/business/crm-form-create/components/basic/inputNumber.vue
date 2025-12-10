<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="(props.fieldConfig.showLabel && !props.isSubTableRender) || props.isSubTableField"
    :path="props.path"
    :rule="props.fieldConfig.rules"
    :required="props.fieldConfig.rules.some((rule) => rule.key === 'required')"
    :label-placement="props.isSubTableField || props.isSubTableRender ? 'top' : 'left'"
  >
    <template #label>
      <div v-if="props.fieldConfig.showLabel" class="flex h-[22px] items-center gap-[4px] whitespace-nowrap">
        <div class="one-line-text">{{ props.fieldConfig.name }}</div>
        <CrmIcon v-if="props.fieldConfig.resourceFieldId" type="iconicon_correlation" />
      </div>
      <div v-else-if="props.isSubTableField || props.isSubTableRender" class="h-[22px]"></div>
    </template>
    <div
      v-if="props.fieldConfig.description && !props.isSubTableRender"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-divider v-if="props.isSubTableField && !props.isSubTableRender" class="!my-0" />
    <CrmInputNumber
      v-model:value="value"
      :max="1000000000"
      :min="props.fieldConfig.min"
      :placeholder="props.fieldConfig.placeholder"
      :disabled="props.fieldConfig.editable === false || !!props.fieldConfig.resourceFieldId"
      :parse="parse"
      :format="format"
      :precision="props.fieldConfig.precision"
      clearable
      class="w-full"
      @update-value="($event:number | null) => emit('change', $event)"
    >
      <template v-if="props.fieldConfig.numberFormat === 'percent'" #suffix> % </template>
    </CrmInputNumber>
  </n-form-item>
</template>

<script setup lang="ts">
  import { NDivider, NFormItem } from 'naive-ui';

  import CrmInputNumber from '@/components/pure/crm-input-number/index.vue';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
    needInitDetail?: boolean; // 判断是否编辑情况
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
  }>();
  const emit = defineEmits<{
    (e: 'change', value: number | null): void;
  }>();

  const value = defineModel<number | null>('value', {
    default: null,
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      if (!props.needInitDetail) {
        value.value = val !== undefined ? val : value.value;
        emit('change', value.value);
      }
    },
    {
      immediate: true,
    }
  );

  watch(
    () => [props.fieldConfig.numberFormat, props.fieldConfig.showThousandsSeparator],
    () => {
      const temp = value.value;
      value.value = null;
      nextTick(() => {
        value.value = temp;
      });
    }
  );

  function parse(val: string) {
    const nums = val.replace(/,/g, '').trim();
    if ((!props.fieldConfig.showThousandsSeparator || /^\d+(\.(\d+)?)?$/.test(nums)) && nums !== '') {
      return Number(nums);
    }
    return nums === '' ? null : Number.NaN;
  }

  function format(val: number | null) {
    if (val === null) return '';
    if (props.fieldConfig.numberFormat === 'number' && props.fieldConfig.showThousandsSeparator) {
      return val.toLocaleString('en-US');
    }
    return val.toString();
  }
</script>

<style lang="less" scoped></style>
