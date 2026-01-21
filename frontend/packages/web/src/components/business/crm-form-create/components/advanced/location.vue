<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :path="props.path"
    :rule="props.fieldConfig.rules"
    :required="props.fieldConfig.rules.some((rule) => rule.key === 'required')"
    :label-placement="props.isSubTableField || props.isSubTableRender ? 'top' : props.formConfig?.labelPos"
  >
    <template #label>
      <div v-if="props.fieldConfig.showLabel" class="flex h-[22px] items-center gap-[4px] whitespace-nowrap">
        <div class="one-line-text">{{ props.fieldConfig.name }}</div>
        <CrmIcon v-if="props.fieldConfig.resourceFieldId" type="iconicon_correlation" />
      </div>
      <div v-else class="h-[22px]"></div>
    </template>
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-divider v-if="props.isSubTableField && !props.isSubTableRender" class="!my-0" />
    <CrmCitySelect
      v-model:value="city"
      :placeholder="props.fieldConfig.placeholder || t('crmFormCreate.advanced.selectLocation')"
      :disabled="props.fieldConfig.editable === false || !!props.fieldConfig.resourceFieldId"
      :range="props.fieldConfig.locationType"
      clearable
      @change="handleCityAndDetailChange"
    />
    <n-input
      v-if="props.fieldConfig.locationType === 'detail'"
      v-model:value="detail"
      :maxlength="200"
      :placeholder="t('crmFormCreate.advanced.inputLocationDetail')"
      :disabled="props.fieldConfig.editable === false || !!props.fieldConfig.resourceFieldId"
      type="textarea"
      clearable
      class="mt-[4px]"
      @change="handleCityAndDetailChange"
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NDivider, NFormItem, NInput } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FormConfig } from '@lib/shared/models/system/module';

  import CrmCitySelect from '@/components/business/crm-city-select/index.vue';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formConfig?: FormConfig;
    path: string;
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string): void;
  }>();

  const { t } = useI18n();

  const value = defineModel<string>('value', {
    default: '',
  });

  const city = ref<string | null>('');
  const detail = ref('');

  function handleCityAndDetailChange() {
    value.value = city.value || detail.value ? `${city.value || ''}-${detail.value || ''}` : '';
    emit('change', value.value);
  }

  watch(
    () => value.value,
    () => {
      const localArr = value.value.split('-');
      city.value = localArr[0] || null;
      detail.value = localArr.filter((e, i) => i > 0).join('-') || ''; // 避免输入了-
    },
    { immediate: true }
  );
</script>

<style lang="less" scoped></style>
