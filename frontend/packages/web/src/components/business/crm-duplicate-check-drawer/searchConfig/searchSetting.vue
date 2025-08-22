<template>
  <div class="bg-[var(--text-n9)] p-[16px]">
    <div class="flex flex-1 flex-col">
      <div class="flex flex-1 gap-[12px]">
        <div class="w-[200px]">{{ t('workbench.duplicateCheck.searchModule') }}</div>
        <div>{{ t('workbench.duplicateCheck.matchFields') }}</div>
      </div>
      <n-form ref="formRef" :model="formModel.list" label-placement="left" class="flex flex-1 flex-col gap-[12px]">
        <div v-for="element of lastScopedOptions" :key="element.value" class="flex flex-1 items-start gap-[12px]">
          <div class="w-[200px]">
            <div class="advanced-label">
              {{ element.label }}
            </div>
          </div>
          <div class="flex-1">
            <n-form-item
              :path="`formModel.list[element.value]`"
              :rule="[{ required: true, message: t('common.value.nameNotNull') }]"
              class="block flex-initial overflow-hidden"
            >
              <CrmTreeSelect
                v-model:value="formModel.list[element.value]"
                :placeholder="t('common.pleaseSelect')"
                :limit-select-tooltip="t('workbench.duplicateCheck.maxSelectCountTooltip')"
                :limit-select-count="5"
                label-field="label"
                key-field="value"
                v-bind="{
                  multiple: true,
                  checkable: true,
                  options: getOptions(element.value),
                }"
              />
            </n-form-item>
          </div>
        </div>
      </n-form>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { FormInst, NForm, NFormItem, TreeSelectOption } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { scrollIntoView } from '@lib/shared/method/dom';

  import { FilterFormItem } from '@/components/pure/crm-advance-filter/type';
  import CrmTreeSelect from '@/components/pure/crm-tree-select/index.vue';

  import { lastScopedOptions } from '../config';

  const { t } = useI18n();
  const props = defineProps<{
    searchFieldMap: Record<string, FilterFormItem[]>;
  }>();

  const formRef = ref<FormInst | null>(null);

  const formModel = defineModel<Record<string, any>>('formModel', {
    required: true,
  });

  function getOptions(value: string) {
    const selected = formModel.value.list[value] || [];
    return props.searchFieldMap[value]?.map((e) => ({
      label: e.title,
      value: e.dataIndex,
      disabled: selected.length >= 5 && !selected.includes(e.dataIndex),
    })) as TreeSelectOption[];
  }

  function validateForm(cb: (res?: Record<string, any>) => void) {
    formRef.value?.validate(async (errors) => {
      if (errors) {
        scrollIntoView(document.querySelector('.n-form-item-blank--error'), { block: 'center' });
        return;
      }
      if (typeof cb === 'function') {
        cb();
      }
    });
  }

  defineExpose({
    formRef,
    validateForm,
  });
</script>

<style scoped lang="less">
  .advanced-label {
    width: 200px;
    height: 32px;
    border: 1px solid var(--text-n7);
    border-radius: @border-radius-small;
    color: var(--text-n1);
    background: var(--text-n10);
    @apply flex items-center justify-center;
  }
  :deep(.n-form-item-feedback-wrapper) {
    min-height: 0;
  }
</style>
