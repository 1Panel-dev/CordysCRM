<template>
  <van-field :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''" :required="required">
    <template #input>
      <div class="flex items-baseline">
        <div class="mr-[8px] flex items-center text-[var(--primary-8)]" @click="clickAreaCode">
          <span class="break-keep">{{ areaCode?.length ? areaCode : t('formCreate.phone.area.other') }}</span>
          <van-icon name="arrow-down" class="ml-[4px]" />
        </div>
        <van-field
          v-model="phoneNumber"
          class="phone-number !p-0"
          type="tel"
          :disabled="props.fieldConfig.editable === false"
          :maxlength="maxLength"
          :path="props.fieldConfig.id"
          :rules="mergedRules"
          :placeholder="props.fieldConfig.placeholder || t('common.pleaseInput')"
          @update:model-value="($event) => updateValue(areaCode, $event)"
        />
      </div>
    </template>
  </van-field>

  <van-popup v-model:show="showPicker" destroy-on-close round position="bottom">
    <van-picker
      :model-value="pickerValue"
      :columns="areaCodeOptions"
      @cancel="showPicker = false"
      @confirm="onConfirm"
    />
  </van-popup>
</template>

<script setup lang="ts">
  import { FieldRule, FieldRuleValidator } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getPatternByAreaCode } from '@lib/shared/method/validate';

  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    id: string;
    originFormDetail?: Record<string, any>;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string): void;
  }>();

  const { t } = useI18n();

  const value = defineModel<string>('value', {
    default: '',
  });

  const areaCode = ref('+86');
  const phoneNumber = ref('');

  function updateValue(area: string, phone: string) {
    const fullNumber = !area.length ? phone : `(${area})${phone}`;
    emit('change', fullNumber);
    value.value = fullNumber;
  }

  // 区号选项
  const areaCodeOptions = [
    { text: `${t('formCreate.phone.area.china')} +86`, value: '+86', length: 11 },
    { text: `${t('formCreate.phone.area.hongKong')} +852`, value: '+852', length: 8 },
    { text: `${t('formCreate.phone.area.macau')} +853`, value: '+853', length: 8 },
    { text: `${t('formCreate.phone.area.taiwan')} +886`, value: '+886', length: 10 },
    { text: t('formCreate.phone.area.other'), value: '', length: undefined },
  ];

  const showPicker = ref(false);
  const pickerValue = ref<any[]>([]);

  function clickAreaCode() {
    if (props.fieldConfig.editable === false) return;
    showPicker.value = true;
  }

  const onConfirm = ({ selectedValues }: any) => {
    showPicker.value = false;
    pickerValue.value = selectedValues;
    areaCode.value = selectedValues[0] as string;
    if (areaCode.value?.length) {
      phoneNumber.value = phoneNumber.value.replace(/\D+/g, ''); // 清洗数字
    }
    updateValue(selectedValues[0], phoneNumber.value);
  };

  const maxLength = computed(() => {
    return areaCodeOptions.find((item) => item.value === areaCode.value)?.length || undefined;
  });
  const required = computed(() => props.fieldConfig.rules.some((rule) => rule.key === 'required'));

  const mergedRules = computed<FieldRule[]>(() => {
    const rawRules = (props.fieldConfig.rules as FieldRule[]) || [];
    const formatRule: FieldRule = {
      trigger: ['onBlur', 'onChange'],
      validator: ((val: string) => {
        if ((!required.value && !phoneNumber.value) || !val || !areaCode.value.length) return Promise.resolve();
        const message = [];
        if (phoneNumber.value.length !== maxLength.value) {
          message.push(t('formCreate.phone.lengthValidator', { count: maxLength.value }));
        }
        const pattern = getPatternByAreaCode(areaCode.value);
        if (!pattern?.test(phoneNumber.value)) {
          message.push(t('formCreate.phone.formatValidator'));
        }
        return !message.length ? true : message.join('；');
      }) as FieldRuleValidator,
    };
    return [...rawRules, formatRule];
  });

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
        }
      }
      // 处理纯数字格式 符合中国的就回显中国，否则回显其他
      else {
        phoneNumber.value = val;
        if (getPatternByAreaCode('+86')?.test(val)) {
          areaCode.value = '+86';
        } else {
          areaCode.value = '';
        }
        updateValue(areaCode.value, phoneNumber.value);
      }
    },
    { immediate: true }
  );
</script>

<style lang="less" scoped>
  .phone-number::before {
    border-bottom: none !important;
  }
</style>
