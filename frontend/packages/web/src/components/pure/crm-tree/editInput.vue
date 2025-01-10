<template>
  <n-form v-if="props.mode !== 'view'" ref="formRef" class="w-full" :model="form" size="small" :rules="rules">
    <n-form-item label="" :show-label="false" :show-feedback="false" path="name">
      <n-input
        ref="inputInstRef"
        v-model:value="form.name"
        :maxlength="props.fieldConfig?.maxLength || 255"
        :placeholder="props.fieldConfig?.placeholder || t('common.pleaseInputToEnter')"
        :loading="props.loading"
        @keydown.enter="() => handleSave(false)"
        @blur="() => handleSave(false)"
        @focus="handleFocus"
        @input="inputHandler"
      >
        <template #suffix>
          <CrmClearSuffix :status="validateNameError" @clear="clearHandler" />
        </template>
      </n-input>
    </n-form-item>
  </n-form>
  <NTooltip v-else :delay="300" flip :placement="props.titleTooltipPosition">
    <template #trigger>
      <div class="one-line-text w-full" @dblclick.stop="() => emit('click')"> {{ form.name }}</div>
    </template>
    {{ props.fieldConfig?.name }}
  </NTooltip>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { FormInst, FormItemRule, FormRules, InputInst, NForm, NFormItem, NInput, NTooltip } from 'naive-ui';

  import type { ClearStatusType } from '@/components/pure/crm-clear-suffix/index.vue';
  import CrmClearSuffix from '@/components/pure/crm-clear-suffix/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  import type { FieldConfig } from './type';

  const { t } = useI18n();
  const props = defineProps<{
    mode: 'rename' | 'view' | 'create';
    loading?: boolean;
    fieldConfig?: FieldConfig; // 表单配置项
    allNames?: string[]; // 添加或者重命名名称重复
    nodeId?: string; // 节点 id
    titleTooltipPosition?:
      | 'top-start'
      | 'top'
      | 'top-end'
      | 'right-start'
      | 'right'
      | 'right-end'
      | 'bottom-start'
      | 'bottom'
      | 'bottom-end'
      | 'left-start'
      | 'left'
      | 'left-end'; // 标题 tooltip 的位置
  }>();

  const emit = defineEmits<{
    (e: 'save', name: string): void;
    (e: 'click'): void;
  }>();

  const form = ref<{
    name: string;
  }>({
    name: props.fieldConfig?.name || '',
  });

  // 校验名称是否重复
  const validateName = (rule: FormItemRule, value: string) => {
    if ((props.allNames || []).includes(value)) {
      if (props.fieldConfig && props.fieldConfig.nameExistTipText) {
        return new Error(t(props.fieldConfig.nameExistTipText));
      }
      return new Error(t('common.nameExists'));
    }
  };

  const rules: FormRules = {
    name: [
      { required: true, message: t('common.nameNotNull'), trigger: ['change', 'blur'] },
      { validator: validateName },
    ],
  };

  const validateNameError = ref<ClearStatusType>('default');
  const formRef = ref<FormInst | null>(null);

  const isFocusInput = ref(false);

  function handleSave(isFocus = false) {
    isFocusInput.value = false;
    formRef.value?.validate((errors) => {
      if (!errors) {
        validateNameError.value = 'default';
        if (!isFocus) {
          emit('save', form.value.name);
        }
      } else {
        validateNameError.value = 'error';
        if (!form.value.name && props.mode === 'create' && !isFocus) {
          emit('save', form.value.name);
        }
      }
    });
  }

  function inputHandler() {
    formRef.value?.validate();
  }

  function handleFocus() {
    if (props.mode === 'create') {
      isFocusInput.value = true;
      handleSave(true);
    }
  }

  function clearHandler() {
    form.value.name = '';
  }

  const inputInstRef = ref<InputInst | null>(null);

  watch(
    () => props.mode,
    () => {
      nextTick(() => {
        inputInstRef.value?.focus();
      });
    },
    {
      immediate: true,
    }
  );
</script>

<style scoped lang="less"></style>
