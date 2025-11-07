<template>
  <van-field
    v-model="fieldLabel"
    is-link
    readonly
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    :rules="props.fieldConfig.rules as FieldRule[]"
    :placeholder="props.fieldConfig.placeholder || t('formCreate.advanced.selectIndustry')"
    :disabled="props.fieldConfig.editable === false"
    clearable
    @click="show = true"
  />
  <van-popup v-model:show="show" round position="bottom">
    <van-cascader
      v-model="cascaderValue"
      :title="props.fieldConfig.placeholder || t('formCreate.advanced.selectIndustry')"
      :options="industryOptions"
      :field-names="{ text: 'label', value: 'value', children: 'children' }"
      @close="show = false"
      @finish="onConfirm"
    />
  </van-popup>
</template>

<script setup lang="ts">
  import { FieldRule } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getIndustryPath } from '@lib/shared/method';

  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';
  import { industryOptions } from '@cordys/web/src/components/pure/crm-industry-select/config';
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

  const fieldLabel = computed(() => getIndustryPath(value.value));

  function onConfirm({ selectedOptions }: { selectedOptions: CascaderOption[] }) {
    show.value = false;
    if (!selectedOptions?.length) return;
    value.value = cascaderValue.value as string;
    emit('change', value.value);
  }

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
