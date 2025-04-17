<template>
  <van-field
    v-model="fieldValue"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    :rules="props.fieldConfig.rules as FieldRule[]"
    is-link
    readonly
    :placeholder="props.fieldConfig.placeholder || t('common.pleaseSelect')"
    :disabled="props.fieldConfig.editable === false"
    clearable
    @click="showPicker = true"
  >
  </van-field>
  <van-popup v-model:show="showPicker" destroy-on-close round position="bottom">
    <van-picker
      :model-value="[value]"
      :columns="
        props.fieldConfig.options?.map((item) => ({
          text: item.label,
          value: item.value,
        }))
      "
      @cancel="showPicker = false"
      @confirm="onConfirm"
    />
  </van-popup>
</template>

<script setup lang="ts">
  import { FieldRule } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string): void;
  }>();

  const { t } = useI18n();

  const value = defineModel<string>('value', {
    default: '',
  });

  const showPicker = ref(false);
  const fieldValue = ref('');

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      value.value = val;
      if (props.fieldConfig.options) {
        const opt = props.fieldConfig.options.find((item) => item.value === val);
        if (opt) {
          fieldValue.value = opt.label;
        }
      }
    },
    {
      immediate: true,
    }
  );

  function onConfirm({ selectedValues, selectedOptions }: any) {
    showPicker.value = false;
    [value.value] = selectedValues;
    fieldValue.value = selectedOptions[0].text;
    emit('change', selectedValues[0]);
  }
</script>

<style lang="less" scoped></style>
