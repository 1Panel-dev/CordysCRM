<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :path="props.path"
    :rule="mergedRules"
    :show-label="(props.fieldConfig.showLabel && !props.isSubTableRender) || props.isSubTableField"
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
    <n-input
      v-model:value="value"
      :maxlength="props.fieldConfig.format === '11' ? 16 : 30"
      :placeholder="
        props.fieldConfig.placeholder ||
        (props.fieldConfig.format === '11' ? '+8613800138000' : t('common.pleaseInput'))
      "
      :disabled="props.fieldConfig.editable === false || !!props.fieldConfig.resourceFieldId"
      :allow-input="allowInput"
      clearable
      @update-value="handlePhoneInput"
      @blur="handlePhoneBlur"
    >
      <template #prefix>
        <CrmIcon type="iconicon_phone" />
      </template>
    </n-input>
  </n-form-item>
</template>

<script setup lang="ts">
  import { NDivider, NFormItem, NInput } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { normalizeToE164, validateE164Phone } from '@lib/shared/method/validate';
  import type { FormConfig } from '@lib/shared/models/system/module';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

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
    (e: 'change', value: string): void;
  }>();

  const value = defineModel<string>('value', {
    default: '',
  });

  const { t } = useI18n();

  /**
   * 输入限制函数
   * format='11': E.164 严格模式，只允许 + 和数字
   * 其他: 宽松模式，允许数字、+、-、空格、括号
   */
  const allowInput = computed(() => {
    if (props.fieldConfig.format === '11') {
      // E.164 严格模式
      return (val: string) => {
        if (!val) return true;
        return /^\+?\d*$/.test(val);
      };
    }
    // 宽松模式
    return (val: string) => {
      if (!val) return true;
      return /^[0-9+\- ()（）]*$/.test(val);
    };
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
        return t('crmFormDesign.phone.formatValidator');
      }
    }
    return undefined;
  }

  /**
   * 处理手机号输入
   */
  function handlePhoneInput(val: string) {
    value.value = val;
    emit('change', val);
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

  const mergedRules = computed(() => {
    const rawRules = props.fieldConfig.rules || [];
    const formatRule = {
      key: 'phone-validator',
      trigger: ['input', 'blur'],
      validator: (_rule: any, val: string) => {
        const error = validatePhone(val);
        if (error) {
          return Promise.reject(new Error(error));
        }
        return Promise.resolve();
      },
    };
    return [...rawRules, formatRule];
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
