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
    <n-select
      v-model:value="value"
      :disabled="props.fieldConfig.editable === false"
      :options="options"
      :multiple="props.fieldConfig.type === FieldTypeEnum.SELECT_MULTIPLE"
      :placeholder="props.fieldConfig.placeholder"
      :fallback-option="value !== null && value !== undefined && value !== '' ? fallbackOption : false"
      clearable
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem, NSelect } from 'naive-ui';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
    needInitDetail?: boolean; // 判断是否编辑情况
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string | number | (string | number)[]): void;
  }>();

  const { t } = useI18n();

  const value = defineModel<string | number | (string | number)[]>('value', {
    default: '',
  });

  const options = computed(() => {
    if (props.fieldConfig.linkRange) {
      return props.fieldConfig.options?.filter((option) => props.fieldConfig.linkRange?.includes(option.value)) || [];
    }
    return props.fieldConfig.options;
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      if (!props.needInitDetail) {
        value.value = val || (props.fieldConfig.type === FieldTypeEnum.SELECT_MULTIPLE ? [] : '');
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

  function fallbackOption(val: string | number) {
    return {
      label: t('common.optionNotExist'),
      value: val,
    };
  }

  onBeforeMount(() => {
    if (!props.needInitDetail) {
      value.value =
        props.fieldConfig.defaultValue ||
        value.value ||
        (props.fieldConfig.type === FieldTypeEnum.SELECT_MULTIPLE ? [] : '');
      emit('change', value.value);
    }
  });
</script>

<style lang="less" scoped></style>
