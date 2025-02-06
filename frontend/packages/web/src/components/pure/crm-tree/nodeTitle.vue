<template>
  <div class="crm-tree-node-rename-container">
    <n-tooltip :delay="300" flip :placement="props.titleTooltipPosition">
      <template #trigger>
        <div
          :class="`one-line-text w-full ${props.titleClass || 'crm-tree-node-title'}`"
          @dblclick.stop="() => emit('click')"
        >
          <slot name="labelSlot">
            {{ form.name }}
          </slot>
        </div>
      </template>
      {{ props.fieldConfig?.name }}
    </n-tooltip>

    <div v-if="props.mode === 'rename'" class="crm-tree-node-rename-form">
      <n-form ref="formRef" :model="form" size="small" :rules="rules">
        <n-form-item label="" :show-label="false" :show-feedback="false" path="name">
          <n-input
            ref="inputInstRef"
            v-model:value="form.name"
            :maxlength="props.fieldConfig?.maxLength || 255"
            :placeholder="props.fieldConfig?.placeholder || t('common.pleaseInputToEnter')"
            :loading="props.loading"
            @keydown.enter="handleSave"
            @blur="handleSave"
            @input="validateValue"
          >
            <template #suffix>
              <CrmClearSuffix :status="validateNameError" @clear="clearHandler" />
            </template>
          </n-input>
        </n-form-item>
      </n-form>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { FormInst, FormItemRule, FormRules, InputInst, NForm, NFormItem, NInput, NTooltip } from 'naive-ui';
  import { debounce } from 'lodash-es';

  import type { ClearStatusType } from '@/components/pure/crm-clear-suffix/index.vue';
  import CrmClearSuffix from '@/components/pure/crm-clear-suffix/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  import type { FieldConfig } from './type';

  const { t } = useI18n();

  const props = defineProps<{
    mode: 'rename' | 'view';
    loading?: boolean;
    fieldConfig?: FieldConfig; // 表单配置项
    allNames?: string[]; // 添加或者重命名名称重复
    nodeId?: string; // 节点 id
    titleClass?: string;
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
    (e: 'save', name: string, notChange: boolean): void;
    (e: 'click'): void;
  }>();

  const form = ref({ name: props.fieldConfig?.name || '' });

  const validateNameError = ref<ClearStatusType>('default');
  const formRef = ref<FormInst | null>(null);
  const inputInstRef = ref<InputInst | null>(null);

  // 校验名称是否重复
  const validateName = (rule: FormItemRule, value: string) => {
    if ((props.allNames || []).includes(value)) {
      return new Error(t(props.fieldConfig?.nameExistTipText || 'common.nameExists'));
    }
  };

  const rules: FormRules = {
    name: [
      { required: true, message: t('common.nameNotNull'), trigger: ['input'] },
      { validator: validateName, trigger: ['input'] },
    ],
  };

  // 避免失失去焦点和回车事件重复
  const debouncedSave = debounce((name: string, isSame: boolean) => {
    emit('save', name, isSame);
  }, 100);

  function handleSave() {
    formRef.value?.validate((errors) => {
      if (!errors) {
        validateNameError.value = 'default';
        debouncedSave(form.value.name, form.value.name === props.fieldConfig?.name);
      } else {
        validateNameError.value = 'error';
      }
    });
  }

  function validateValue() {
    formRef.value?.validate((errors) => {
      validateNameError.value = errors ? 'error' : 'default';
    });
  }

  function clearHandler() {
    form.value.name = '';
    validateValue();
  }

  watch(
    () => props.mode,
    (newMode) => {
      if (newMode === 'rename') {
        nextTick(() => inputInstRef.value?.focus());
      }
    }
  );
</script>

<style scoped lang="less">
  .crm-tree-node-rename-container {
    height: 34px;
    @apply relative flex  w-full items-center;
  }

  // 通过绝对定位，避免整棵树重新布局渲染
  .crm-tree-node-rename-form {
    top: 50%;
    left: 0;
    transform: translateY(-50%);
    @apply absolute w-full;
  }
</style>
