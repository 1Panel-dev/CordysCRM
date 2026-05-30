<template>
  <n-select
    ref="selectRef"
    v-model:value="value"
    filterable
    multiple
    tag
    :placeholder="props.placeholder || t('common.tagsInputPlaceholder')"
    :show-arrow="false"
    :show="false"
    :disabled="props.disabled"
    :input-props="{
      maxlength: 64,
    }"
    max-tag-count="responsive"
    :fallback-option="value?.length <= 10 ? fallbackOption : false"
    :render-tag="renderTag"
    clearable
    @keydown.enter="handleInputEnter"
  />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NSelect, NTag, NTooltip, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { SelectBaseOption } from 'naive-ui/es/select/src/interface';

  const { t } = useI18n();

  const props = defineProps<{
    placeholder?: string;
    disabled: boolean;
  }>();

  const value = defineModel<(string | number)[]>('value', {
    default: [],
  });

  const Message = useMessage();

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

  function fallbackOption(val: string | number) {
    return {
      label: `${val}`,
      value: val,
    };
  }
</script>

<style scoped></style>
