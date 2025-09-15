<template>
  <van-field
    v-model="fieldLabel"
    is-link
    readonly
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    :rules="props.fieldConfig.rules as FieldRule[]"
    :placeholder="props.fieldConfig.placeholder || t('formCreate.advanced.selectLocation')"
    :disabled="props.fieldConfig.editable === false"
    clearable
    @click="show = true"
  />
  <van-popup v-model:show="show" round position="bottom">
    <van-cascader
      v-model="cascaderValue"
      :title="props.fieldConfig.placeholder || t('formCreate.advanced.selectLocation')"
      :options="options"
      :field-names="{ text: 'label', value: 'value', children: 'children' }"
      @close="show = false"
      @finish="onConfirm"
    />
  </van-popup>
  <van-field
    v-if="props.fieldConfig.locationType === 'detail'"
    v-model="detail"
    type="textarea"
    label=" "
    :name="props.fieldConfig.id"
    :maxlength="200"
    :placeholder="t('formCreate.advanced.inputLocationDetail')"
    :disabled="props.fieldConfig.editable === false"
    clearable
    @update:model-value="handleCityAndDetailChange"
  />
</template>

<script setup lang="ts">
  import { FieldRule } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getCityPath } from '@lib/shared/method';

  import { CHINA_PC, CHINA_PCD, COUNTRIES_TREE } from '@cordys/web/src/components/business/crm-city-select/config';
  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';
  import type { CascaderOption } from 'vant';

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();

  const value = defineModel<string>('value', {
    default: '',
  });

  const emit = defineEmits<{
    (e: 'change', v: string): void;
  }>();

  const { t } = useI18n();

  const show = ref(false);
  const cascaderValue = ref<string | number>('');
  const city = ref('');
  const detail = ref(''); // 详细地址

  const fieldLabel = computed(() => getCityPath(city.value));

  const options = computed<CascaderOption[]>(() => {
    return props.fieldConfig.locationType === 'PC' ? [CHINA_PC, ...COUNTRIES_TREE] : [CHINA_PCD, ...COUNTRIES_TREE];
  });

  function handleCityAndDetailChange() {
    value.value = city.value || detail.value ? `${city.value || ''}-${detail.value || ''}` : '';
    emit('change', value.value);
  }

  function onConfirm({ selectedOptions }: { selectedOptions: CascaderOption[] }) {
    show.value = false;
    if (!selectedOptions?.length) return;
    city.value = cascaderValue.value as string;
    handleCityAndDetailChange();
  }

  watch(
    () => value.value,
    (val) => {
      if (!val) {
        city.value = '';
        detail.value = '';
        cascaderValue.value = '';
        return;
      }
      const localArr = val.split('-');
      city.value = localArr[0] || '';
      cascaderValue.value = localArr[0] || '';
      detail.value = localArr.slice(1).join('-') || '';
    },
    { immediate: true }
  );
</script>
