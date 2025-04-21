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
    <n-select
      v-model:value="value"
      filterable
      multiple
      tag
      :placeholder="props.fieldConfig.placeholder || t('common.tagsInputPlaceholder')"
      :show-arrow="false"
      :show="false"
      :default-value="props.fieldConfig.defaultValue"
      :disabled="props.fieldConfig.editable === false"
      :input-props="{
        maxlength: 64,
      }"
      :fallback-option="value?.length < 10 ? fallbackOption : false"
      :render-tag="renderTag"
      clearable
      @update-value="($event) => emit('change', $event)"
      @keydown.enter="handleInputEnter"
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem, NSelect, NTag, NTooltip, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { FormCreateField } from '../../types';
  import { SelectBaseOption } from 'naive-ui/es/select/src/interface';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: (string | number)[]): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();

  const value = defineModel<(string | number)[]>('value', {
    default: [],
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      value.value = val;
    },
    {
      immediate: true,
    }
  );

  function fallbackOption(val: string | number) {
    return {
      label: `${val}`,
      value: val,
    };
  }

  function renderTag({ option, handleClose }: { option: SelectBaseOption; handleClose: () => void }) {
    return h(
      NTooltip,
      {},
      {
        default: () => {
          return h('div', {}, { default: () => option.label });
        },
        trigger: () => {
          return h(NTag, { closable: true, onClose: handleClose }, { default: () => option.label });
        },
      }
    );
  }

  function handleInputEnter() {
    if (value.value?.length >= 10) {
      Message.warning(t('crmFormCreate.basic.tagInputLimitTip'));
    }
  }
</script>

<style lang="less" scoped></style>
