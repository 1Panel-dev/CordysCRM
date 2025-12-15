<template>
  <van-field
    v-model="value"
    type="tel"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    :rules="mergedRules"
    :placeholder="
      props.fieldConfig.placeholder || (props.fieldConfig.format === '11' ? '+8613800138000' : t('common.pleaseInput'))
    "
    :disabled="props.fieldConfig.editable === false"
    :maxlength="props.fieldConfig.format === '11' ? 16 : 30"
    clearable
    @update:model-value="handlePhoneInput"
    @blur="handlePhoneBlur"
  >
  </van-field>
</template>

<script setup lang="ts">
  import { FieldRule, FieldRuleValidator } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { normalizeToE164, validateE164Phone } from '@lib/shared/method/validate';

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

  /**
   * 验证手机号
   * 只有 format='11' 时才使用 E.164 严格验证
   */
  function validatePhone(val: string): string | undefined {
    // 只有 format='11' 时才启用 E.164 严格验证
    if (props.fieldConfig.format === '11') {
      const normalized = normalizeToE164(val, props.fieldConfig.format);
      const errorKey = validateE164Phone(normalized);
      if (errorKey) {
        return t('formCreate.phone.formatValidator');
      }
    }
    return undefined;
  }

  /**
   * 处理手机号输入
   * format='11': E.164 严格模式，只允许 + 和数字
   * 其他: 宽松模式，允许更多字符
   */
  function handlePhoneInput(val: string) {
    if (!val) {
      value.value = '';
      emit('change', '');
      return;
    }

    let filtered = val;

    if (props.fieldConfig.format === '11') {
      // E.164 严格模式：只保留 + 和数字
      filtered = val.replace(/[^+\d]/g, '');
      // 确保 + 只在开头
      if (filtered.includes('+')) {
        const digits = filtered.replace(/\+/g, '');
        filtered = '+' + digits;
      }
      // 限制长度（E.164 最多16个字符）
      if (filtered.length > 16) {
        filtered = filtered.substring(0, 16);
      }
    } else {
      // 宽松模式：允许数字、+、-、空格、括号
      filtered = val.replace(/[^0-9+\- ()（）]/g, '');
      // 限制长度
      if (filtered.length > 30) {
        filtered = filtered.substring(0, 30);
      }
    }

    value.value = filtered;
    emit('change', filtered);
  }

  /**
   * 处理手机号失焦
   * 只有 format='11' 时才自动转换为 E.164 格式
   */
  function handlePhoneBlur() {
    if (value.value && props.fieldConfig.format === '11') {
      const normalized = normalizeToE164(value.value, props.fieldConfig.format);
      if (normalized !== value.value) {
        value.value = normalized;
        emit('change', normalized);
      }
    }
  }

  const mergedRules = computed<FieldRule[]>(() => {
    const rawRules = (props.fieldConfig.rules as FieldRule[]) || [];
    const formatRule: FieldRule = {
      trigger: ['onBlur', 'onChange'],
      validator: ((val: string) => {
        const error = validatePhone(val);
        if (error) {
          return error;
        }
        return true;
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
