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
    <n-select
      ref="selectRef"
      v-model:value="value"
      filterable
      multiple
      tag
      :placeholder="props.fieldConfig.placeholder || t('common.tagsInputPlaceholder')"
      :show-arrow="false"
      :show="false"
      :disabled="props.fieldConfig.editable === false || !!props.fieldConfig.resourceFieldId"
      :input-props="{
        maxlength: 64,
      }"
      :fallback-option="value?.length <= 10 ? fallbackOption : false"
      :render-tag="renderTag"
      clearable
      @update-value="($event) => emit('change', $event)"
      @keydown.enter="handleInputEnter"
    />
  </n-form-item>
</template>

<script setup lang="ts">
  import { NDivider, NFormItem, NSelect, NTag, NTooltip, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FormConfig } from '@lib/shared/models/system/module';

  import { FormCreateField } from '../../types';
  import { SelectBaseOption } from 'naive-ui/es/select/src/interface';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formConfig?: FormConfig;
    path: string;
    needInitDetail?: boolean; // 判断是否编辑情况
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
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
      if (!props.needInitDetail) {
        value.value = val || value.value;
        emit('change', value.value);
      }
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

  const selectRef = ref<InstanceType<typeof NSelect>>();

  function handleInputEnter() {
    if (value.value?.length > 10) {
      value.value = value.value.slice(0, 10);
      Message.warning(t('crmFormCreate.basic.tagInputLimitTip'));
    } else if (value.value?.includes(selectRef.value?.$el.querySelector('.n-base-selection-input-tag__input').value)) {
      Message.warning(t('crmFormCreate.basic.tagInputRepeatTip'));
    }
  }
</script>

<style lang="less" scoped></style>
