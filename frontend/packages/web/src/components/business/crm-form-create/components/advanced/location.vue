<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="props.fieldConfig.showLabel"
    :path="props.path"
    :rule="props.fieldConfig.rules"
  >
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <CrmCitySelect
      v-model:value="city"
      :placeholder="t('crmFormCreate.advanced.selectLocation')"
      :default-value="props.fieldConfig.defaultValue"
      :disabled="props.fieldConfig.editable === false"
      clearable
      @change="handleCityAndDetailChange"
    />
    <n-input
      v-if="props.fieldConfig.locationType === 'detail'"
      v-model:value="detail"
      :maxlength="1000"
      :placeholder="t('crmFormCreate.advanced.inputLocationDetail')"
      :default-value="props.fieldConfig.defaultValue"
      :disabled="props.fieldConfig.editable === false"
      type="textarea"
      clearable
      class="mt-[4px]"
      @update-value="handleCityAndDetailChange"
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem, NInput } from 'naive-ui';

  import CrmCitySelect from '@/components/business/crm-city-select/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
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
    value.value = `${city.value}-${detail.value}`;
    emit('change', value.value);
  }

  onBeforeMount(() => {
    const [cityValue, detailValue] = value.value.split('-');
    city.value = cityValue || null;
    detail.value = detailValue;
  });
</script>

<style lang="less" scoped></style>
