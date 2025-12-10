<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :path="props.path"
    :rule="mergedRules"
    :show-label="(props.fieldConfig.showLabel && !props.isSubTableRender) || props.isSubTableField"
    :required="props.fieldConfig.rules.some((rule) => rule.key === 'required')"
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
      :maxlength="30"
      :placeholder="props.fieldConfig.placeholder"
      :disabled="props.fieldConfig.editable === false || !!props.fieldConfig.resourceFieldId"
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
  import { NDivider, NFormItem, NInput } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
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
        if (props.fieldConfig.format === '11' && val.replace(/[\s\uFEFF\xA0]+/g, '').length !== 11) {
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
