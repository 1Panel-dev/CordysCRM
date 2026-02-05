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
          <CrmIcon :type="getFieldIcon(item.type) || ''" />
        </template>
      </n-input>
      <template v-if="!item.businessKey || item.resourceFieldId">
        <n-tooltip
          v-if="!item.resourceFieldId"
          :delay="300"
          :show-arrow="false"
          class="crm-form-design--composition-item-tools-tip"
        >
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
              @click="handleFieldDelete(item, i)"
            >
              <CrmIcon type="iconicon_delete" :size="14" />
            </n-button>
          </template>
          {{ t('common.delete') }}
        </n-tooltip>
        <div v-if="item.resourceFieldId" class="w-[22px]"></div>
      </template>
      <template v-else>
        <div class="w-[52px]"></div>
      </template>
      <div
        v-if="fieldConfig.subFields.some((e) => e.id !== item.id && e.name === item.name)"
        class="ml-[24px] w-full text-[12px] text-[var(--error-red)]"
      >
        {{ t('crmFormDesign.repeatFieldName') }}
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
    dataSourceDefaultFieldConfig,
    formulaDefaultFieldConfig,
    getFieldIcon,
    inputDefaultFieldConfig,
    inputNumberDefaultFieldConfig,
    pictureDefaultFieldConfig,
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
    dataSourceDefaultFieldConfig,
    formulaDefaultFieldConfig,
    pictureDefaultFieldConfig,
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
    if (
      [FieldTypeEnum.CHECKBOX, FieldTypeEnum.RADIO, FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE].includes(
        newField.type
      ) &&
      newField.options?.length === 0
    ) {
      newField.options = [
        {
          label: t('crmFormDesign.option', { i: 1 }),
          value: getGenerateId(),
        },
        {
          label: t('crmFormDesign.option', { i: 2 }),
          value: getGenerateId(),
        },
        {
          label: t('crmFormDesign.option', { i: 3 }),
          value: getGenerateId(),
        },
      ];
      newField.customOptions = [...newField.options];
    }
    if (!fieldConfig.value.subFields) {
      fieldConfig.value.subFields = [];
    }
    fieldConfig.value.subFields.push(newField);
  }

  function handleFieldDelete(item: FormCreateField, i: number) {
    fieldConfig.value.subFields?.splice(i, 1);
    if (item.resourceFieldId) {
      const resourceField = fieldConfig.value.subFields?.find((field) => field.id === item.resourceFieldId);
      if (resourceField) {
        resourceField.showFields = resourceField.showFields?.filter((id) => id !== item.id.split('_ref_')[1]); // 数据源显示字段 id 是拼接_ref_的
      }
    }
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
