<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="props.fieldConfig.showLabel"
    :path="props.path"
    :rule="mergedRules"
    :required="props.fieldConfig.rules.some((rule) => rule.key === 'required')"
  >
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-input
      v-model:value="value"
      :maxlength="Number(props.fieldConfig.format) || 255"
      :placeholder="props.fieldConfig.placeholder"
      :disabled="props.fieldConfig.editable === false"
      :allow-input="onlyAllowNumber"
      clearable
      @update-value="($event) => emit('change', $event)"
    >
      <template #prefix>
        <CrmIcon type="iconicon_phone" />
      </template>
    </n-input>
  </n-form-item>
</template>

<script setup lang="ts">
  import { watch } from 'vue';
  import { NFormItem, NInput } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
    needInitDetail?: boolean; // 判断是否编辑情况
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string): void;
  }>();

  const value = defineModel<string>('value', {
    default: '',
  });

  const { t } = useI18n();

  function onlyAllowNumber(val: string) {
    if (!val) return true;
    return /^[0-9+\- ()（）]*$/.test(val);
  }

  const mergedRules = computed(() => {
    const rawRules = props.fieldConfig.rules || [];
    const formatRule = {
      key: 'phone-length-validator',
      trigger: ['input', 'blur'],
      validator: (_rule: any, val: string) => {
        if (!val) return Promise.resolve();
        if (props.fieldConfig.format === '11' && value.value.length !== 11) {
          return Promise.reject(new Error(t('crmFormDesign.phone.lengthValidator', { count: 11 })));
        }
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
