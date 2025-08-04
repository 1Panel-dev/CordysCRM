<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="props.fieldConfig.showLabel"
    :path="props.path"
    :rule="mergedRules"
    :required="required"
  >
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-input-group>
      <n-select
        v-model:value="areaCode"
        :options="areaCodeOptions"
        class="w-[180px]"
        filterable
        @update-value="(val) => updateValue(val, phoneNumber)"
      />
      <n-input
        v-model:value="phoneNumber"
        :maxlength="maxLength"
        :placeholder="props.fieldConfig.placeholder"
        :disabled="props.fieldConfig.editable === false"
        :allow-input="onlyAllowNumber"
        clearable
        @update-value="(val) => updateValue(areaCode, val)"
      >
        <template #prefix>
          <CrmIcon type="iconicon_phone" />
        </template>
      </n-input>
    </n-input-group>
  </n-form-item>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { NFormItem, NInput, NInputGroup, NSelect } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string): void;
  }>();

  const value = defineModel<string>('value', {
    default: '',
  });

  const { t } = useI18n();

  function onlyAllowNumber(val: string) {
    return !val || /^\d+$/.test(val);
  }

  // 区号选项
  const areaCodeOptions = [
    { label: `${t('crmFormDesign.phone.area.china')} +86`, value: '+86', length: 11 },
    { label: `${t('crmFormDesign.phone.area.hongKong')} +852`, value: '+852', length: 8 },
    { label: `${t('crmFormDesign.phone.area.macau')} +853`, value: '+853', length: 8 },
    { label: `${t('crmFormDesign.phone.area.taiwan')} +886`, value: '+886', length: 10 },
  ];

  const areaCode = ref('+86');
  const phoneNumber = ref('');

  const maxLength = computed(() => {
    return areaCodeOptions.find((item) => item.value === areaCode.value)?.length || 11;
  });
  const required = computed(() => props.fieldConfig.rules.some((rule) => rule.key === 'required'));

  const mergedRules = computed(() => {
    const rawRules = props.fieldConfig.rules || [];
    const lengthRule = {
      key: 'phone-length-validator',
      trigger: ['input', 'blur'],
      validator: (_rule: any, val: string) => {
        if ((!required.value && !phoneNumber.value) || !val) return Promise.resolve();
        return phoneNumber.value.length === maxLength.value
          ? Promise.resolve()
          : Promise.reject(new Error(t('crmFormDesign.phone.lengthValidator', { count: maxLength.value })));
      },
    };
    return [...rawRules, lengthRule];
  });

  function updateValue(area: string, phone: string) {
    const fullNumber = `(${area})${phone}`;
    emit('change', fullNumber);
    value.value = fullNumber;
  }

  watch(
    () => value.value,
    (val) => {
      if (!val) {
        phoneNumber.value = '';
        return;
      }
      // 处理 (+86)xxxx 格式
      if (val.startsWith('(+') && val.includes(')')) {
        const endBracket = val.indexOf(')');
        const code = val.substring(1, endBracket);
        const number = val.substring(endBracket + 1);

        if (areaCodeOptions.some((opt) => opt.value === code)) {
          areaCode.value = code;
          phoneNumber.value = number || '';
        }
      }
      // 处理纯数字格式 (默认为中国+86)
      else {
        phoneNumber.value = val;
        areaCode.value = '+86';
        updateValue(areaCode.value, phoneNumber.value);
      }
    },
    { immediate: true }
  );
</script>

<style lang="less" scoped></style>
