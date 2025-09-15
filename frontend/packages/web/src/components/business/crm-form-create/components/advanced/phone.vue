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
        @update:value="onAreaCodeChange"
      />
      <n-input
        v-model:value="phoneNumber"
        :maxlength="maxLength"
        :placeholder="props.fieldConfig.placeholder"
        :disabled="props.fieldConfig.editable === false"
        :allow-input="onlyAllowNumber"
        clearable
        @update:value="updateValue"
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
  import { getPatternByAreaCode } from '@lib/shared/method/validate';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
    id: string;
    originFormDetail?: Record<string, any>;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string): void;
  }>();

  const value = defineModel<string>('value', {
    default: '',
  });

  const { t } = useI18n();

  // 区号选项
  const areaCodeOptions = [
    { label: `${t('crmFormDesign.phone.area.china')} +86`, value: '+86', length: 11 },
    { label: `${t('crmFormDesign.phone.area.hongKong')} +852`, value: '+852', length: 8 },
    { label: `${t('crmFormDesign.phone.area.macau')} +853`, value: '+853', length: 8 },
    { label: `${t('crmFormDesign.phone.area.taiwan')} +886`, value: '+886', length: 10 },
    { label: t('crmFormDesign.phone.area.other'), value: '', length: undefined },
  ];

  const areaCode = ref('+86');
  const phoneNumber = ref('');

  function onlyAllowNumber(val: string) {
    if (!val) return true;
    // 如果没选区号
    if (!areaCode.value?.length) {
      return /^[0-9+\- ()（）]*$/.test(val);
    }
    // 有区号，只允许数字
    return /^\d+$/.test(val);
  }

  const maxLength = computed(() => {
    return areaCodeOptions.find((item) => item.value === areaCode.value)?.length || undefined;
  });
  const required = computed(() => props.fieldConfig.rules.some((rule) => rule.key === 'required'));

  const mergedRules = computed(() => {
    const rawRules = props.fieldConfig.rules || [];
    const formatRule = {
      key: 'phone-length-validator',
      trigger: ['input', 'blur'],
      validator: (_rule: any, val: string) => {
        if ((!required.value && !phoneNumber.value) || !val || !areaCode.value.length) return Promise.resolve();
        const message = [];
        if (phoneNumber.value.length !== maxLength.value) {
          message.push(t('crmFormDesign.phone.lengthValidator', { count: maxLength.value }));
        }
        const pattern = getPatternByAreaCode(areaCode.value);
        if (!pattern?.test(phoneNumber.value)) {
          message.push(t('crmFormDesign.phone.formatValidator'));
        }

        return !message.length ? Promise.resolve() : Promise.reject(new Error(message.join('；')));
      },
    };
    return [...rawRules, formatRule];
  });

  function updateValue() {
    const fullNumber = !areaCode.value.length ? phoneNumber.value : `(${areaCode.value})${phoneNumber.value}`;
    emit('change', fullNumber);
    value.value = fullNumber;
  }

  // 区号变化处理
  function onAreaCodeChange(newCode: string) {
    areaCode.value = newCode;
    // 清洗数字
    if (newCode) {
      phoneNumber.value = phoneNumber.value.replace(/\D+/g, '');
    }
    updateValue();
  }

  watch(
    () => props.originFormDetail?.[props.id],
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
          return;
        }
      }
      phoneNumber.value = val;
      areaCode.value = '';
      updateValue();
    },
    { immediate: true }
  );
</script>

<style lang="less" scoped></style>
