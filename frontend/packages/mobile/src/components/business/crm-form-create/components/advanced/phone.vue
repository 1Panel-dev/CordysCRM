<template>
  <van-field
    v-model="value"
    type="tel"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    :rules="mergedRules"
    :placeholder="props.fieldConfig.placeholder || t('common.pleaseInput')"
    :disabled="props.fieldConfig.editable === false"
    :maxlength="255"
    clearable
    @update:model-value="($event) => emit('change', $event)"
  >
  </van-field>
</template>

<script setup lang="ts">
  import { FieldRule, FieldRuleValidator } from 'vant';

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

  const mergedRules = computed<FieldRule[]>(() => {
    const rawRules = (props.fieldConfig.rules as FieldRule[]) || [];
    const formatRule: FieldRule = {
      trigger: ['onBlur', 'onChange'],
      validator: ((val: string) => {
        if (!val) return Promise.resolve();
        if (props.fieldConfig.format === '11' && val.replace(/[\s\uFEFF\xA0]+/g, '').length !== 11) {
          return t('formCreate.phone.lengthValidator', { count: 11 });
        }
      }) as FieldRuleValidator,
    };
    return [...rawRules, formatRule];
  });

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
