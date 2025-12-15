<template>
  <van-field
    v-model="value"
    type="tel"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    :rules="mergedRules"
    :placeholder="props.fieldConfig.placeholder || '+8613800138000'"
    :disabled="props.fieldConfig.editable === false"
    :maxlength="16"
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
   * 验证手机号（使用内置方法，并处理国际化消息）
   */
  function validatePhone(val: string): string | undefined {
    // 先转换为 E.164 格式再验证
    const normalized = normalizeToE164(val, props.fieldConfig.format);
    const errorKey = validateE164Phone(normalized);
    if (!errorKey) return undefined;
    
    return t('formCreate.phone.formatValidator');
  }

  /**
   * 处理手机号输入
   * 过滤掉不符合 E.164 格式的字符（只允许 + 和数字，且 + 只能在开头）
   */
  function handlePhoneInput(val: string) {
    if (!val) {
      value.value = '';
      emit('change', '');
      return;
    }
    
    // 过滤：只保留 + 和数字，且 + 只能在开头
    let filtered = val.replace(/[^+\d]/g, '');
    // 确保 + 只在开头
    if (filtered.includes('+') && !filtered.startsWith('+')) {
      filtered = '+' + filtered.replace(/\+/g, '');
    }
    // 限制长度（E.164 最多16个字符：+ + 15位数字）
    if (filtered.length > 16) {
      filtered = filtered.substring(0, 16);
    }
    
    value.value = filtered;
    emit('change', filtered);
  }

  /**
   * 处理手机号失焦，自动转换为 E.164 格式
   */
  function handlePhoneBlur() {
    if (value.value) {
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
        return Promise.resolve();
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
