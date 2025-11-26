<template>
  <VueDraggable
    v-if="fieldConfig.subFields"
    v-model="fieldConfig.subFields"
    :animation="150"
    handle=".handle"
    class="flex flex-col gap-[8px]"
  >
    <div v-for="(item, i) in fieldConfig.subFields" :key="item.id" class="flex flex-wrap items-center gap-[8px]">
      <n-tooltip :delay="300" :show-arrow="false" class="crm-form-design--composition-item-tools-tip">
        <template #trigger>
          <CrmIcon type="iconicon_move" class="handle cursor-move" />
        </template>
        {{ t('common.sort') }}
      </n-tooltip>
      <n-input
        v-model:value="item.name"
        :maxlength="50"
        :disabled="props.disabled"
        :status="fieldConfig.subFields.some((e) => e.id !== item.id && e.name === item.name) ? 'error' : undefined"
        class="flex-1"
        clearable
      >
        <template #prefix>
          <CrmIcon :type="item.icon" />
        </template>
      </n-input>
      <template v-if="!item.businessKey">
        <n-tooltip :delay="300" :show-arrow="false" class="crm-form-design--composition-item-tools-tip">
          <template #trigger>
            <n-button
              quaternary
              type="primary"
              size="small"
              class="text-btn-error p-[4px] text-[var(--text-n1)]"
              @click="handleFieldCopy(i)"
            >
              <CrmIcon type="iconicon_file_copy" :size="14" />
            </n-button>
          </template>
          {{ t('common.copy') }}
        </n-tooltip>
        <n-tooltip :delay="300" :show-arrow="false" class="crm-form-design--composition-item-tools-tip">
          <template #trigger>
            <n-button
              quaternary
              type="error"
              size="small"
              class="text-btn-error p-[4px] text-[var(--text-n1)]"
              @click="handleFieldDelete(i)"
            >
              <CrmIcon type="iconicon_delete" :size="14" />
            </n-button>
          </template>
          {{ t('common.delete') }}
        </n-tooltip>
      </template>
      <template v-else>
        <div class="w-[52px]"></div>
      </template>
      <div
        v-if="fieldConfig.subFields.some((e) => e.id !== item.id && e.name === item.name)"
        class="ml-[48px] w-full text-[12px] text-[var(--error-red)]"
      >
        {{ t('crmFormDesign.repeatOptionName') }}
      </div>
    </div>
    <n-dropdown
      trigger="click"
      :options="fieldOptions"
      class="crm-form-design-fields-dropdown"
      @select="handleFieldSelect"
    >
      <n-button>{{ t('crmFormDesign.addSubField') }}</n-button>
    </n-dropdown>
  </VueDraggable>
</template>

<script setup lang="ts">
  import { NButton, NDropdown, NInput, NTooltip } from 'naive-ui';
  import { VueDraggable } from 'vue-draggable-plus';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getGenerateId } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import {
    inputDefaultFieldConfig,
    inputNumberDefaultFieldConfig,
    selectDefaultFieldConfig,
    selectMultipleDefaultFieldConfig,
  } from '@/components/business/crm-form-create/config';
  import { FormCreateField } from '@/components/business/crm-form-create/types';

  const props = defineProps<{
    disabled?: boolean;
  }>();

  const { t } = useI18n();

  const fieldConfig = defineModel<FormCreateField>('field', {
    default: null,
  });

  const fieldOptions = [
    inputDefaultFieldConfig,
    inputNumberDefaultFieldConfig,
    selectDefaultFieldConfig,
    selectMultipleDefaultFieldConfig,
  ].map((field) => ({
    ...field,
    label: t(field.name),
    key: field.type,
    _icon: field.icon,
    icon: () =>
      h(CrmIcon, {
        type: field.icon,
        size: 16,
      }),
  }));

  function handleFieldSelect(key: FieldTypeEnum) {
    const option = fieldOptions.find((e) => e.key === key);
    if (!option) return;
    const newField: FormCreateField = {
      ...option,
      id: getGenerateId(),
      name: option.label,
      icon: option._icon,
    };
    if (!fieldConfig.value.subFields) {
      fieldConfig.value.subFields = [];
    }
    fieldConfig.value.subFields.push(newField);
  }

  function handleFieldDelete(i: number) {
    fieldConfig.value.subFields?.splice(i, 1);
  }

  function handleFieldCopy(i: number) {
    if (fieldConfig.value.subFields) {
      const copyItem = JSON.parse(JSON.stringify(fieldConfig.value.subFields[i]));
      copyItem.id = getGenerateId();
      copyItem.name += `_${t('common.copy')}`;
      fieldConfig.value.subFields.splice(i + 1, 0, copyItem);
    }
  }
</script>

<style lang="less">
  .crm-form-design-fields-dropdown {
    width: 248px;
  }
</style>
