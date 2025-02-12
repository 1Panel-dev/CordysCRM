<template>
  <div class="flex flex-col">
    <div class="crm-form-design-field-title">{{ t('crmFormDesign.basicField') }}</div>
    <VueDraggable
      v-model="basicFields"
      :animation="150"
      ghost-class="crm-form-design--composition-item-ghost"
      :group="{ name: 'crmFormDesign', pull: 'clone', put: false }"
      :clone="clone"
      :sort="false"
      class="crm-form-design-field-wrapper mb-[24px]"
    >
      <div
        v-for="field of basicFields"
        :key="field.type"
        class="crm-form-design-field-item"
        draggable="true"
        @click="() => handleFieldClick(field)"
      >
        <CrmIcon :type="field.icon" />
        <div>{{ t(field.name) }}</div>
      </div>
    </VueDraggable>
    <div class="crm-form-design-field-title">{{ t('crmFormDesign.advancedField') }}</div>
    <div class="crm-form-design-field-wrapper">
      <div
        v-for="field of advancedFields"
        :key="field.type"
        class="crm-form-design-field-item"
        draggable="true"
        @click="() => handleFieldClick(field)"
      >
        <CrmIcon :type="field.icon" />
        <div>{{ t(field.name) }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { useI18n } from '@/hooks/useI18n';
  import { getGenerateId } from '@/utils';

  import { FieldTypeEnum } from '../enum';
  import { FieldItem } from '../types';
  import { VueDraggable } from 'vue-draggable-plus';

  const emit = defineEmits<{
    (e: 'select', type: FieldTypeEnum, name: string): void;
    (e: 'drag', type: FieldTypeEnum, name: string): void;
  }>();

  const { t } = useI18n();

  const basicFields: FieldItem[] = [
    {
      type: FieldTypeEnum.INPUT,
      icon: 'iconicon_single_line_text',
      name: 'crmFormDesign.input',
    },
    {
      type: FieldTypeEnum.TEXTAREA,
      icon: 'iconicon_multiline',
      name: 'crmFormDesign.textarea',
    },
    {
      type: FieldTypeEnum.INPUT_NUMBER,
      icon: 'iconicon_hashtag_key',
      name: 'crmFormDesign.inputNumber',
    },
    {
      type: FieldTypeEnum.DATE_TIME,
      icon: 'iconicon_calendar1',
      name: 'crmFormDesign.dateTime',
    },
    {
      type: FieldTypeEnum.RADIO,
      icon: 'iconicon_radio',
      name: 'crmFormDesign.radio',
    },
    {
      type: FieldTypeEnum.CHECKBOX,
      icon: 'icona-icon_collectbeifen12',
      name: 'crmFormDesign.checkbox',
    },
    {
      type: FieldTypeEnum.SELECT_SINGLE,
      icon: 'iconicon_pull_down_single_choice',
      name: 'crmFormDesign.selectSingle',
    },
    {
      type: FieldTypeEnum.SELECT_MULTIPLE,
      icon: 'iconicon_pull_down_multiple_selection',
      name: 'crmFormDesign.selectMultiple',
    },
    {
      type: FieldTypeEnum.MEMBER_SINGLE,
      icon: 'iconicon_member_single_choice',
      name: 'crmFormDesign.memberSingle',
    },
    {
      type: FieldTypeEnum.MEMBER_MULTIPLE,
      icon: 'iconicon_multiple_choice_of_members',
      name: 'crmFormDesign.memberMultiple',
    },
    {
      type: FieldTypeEnum.DEPARTMENT_SINGLE,
      icon: 'iconicon_department_single_choice',
      name: 'crmFormDesign.departmentSingle',
    },
    {
      type: FieldTypeEnum.DEPARTMENT_MULTIPLE,
      icon: 'icona-icon_multiple_selection_of_departments',
      name: 'crmFormDesign.departmentMultiple',
    },
    {
      type: FieldTypeEnum.DIVIDER,
      icon: 'iconicon_dividing_line',
      name: 'crmFormDesign.divider',
    },
  ];
  const advancedFields: FieldItem[] = [
    {
      type: FieldTypeEnum.PICTURE,
      icon: 'iconicon_picture',
      name: 'crmFormDesign.picture',
    },
    {
      type: FieldTypeEnum.LOCATION,
      icon: 'iconicon_map',
      name: 'crmFormDesign.location',
    },
    {
      type: FieldTypeEnum.PHONE,
      icon: 'iconicon_phone',
      name: 'crmFormDesign.phone',
    },
  ];

  function clone(e: FieldItem) {
    return {
      ...e,
      grid: 4,
      id: getGenerateId(),
    };
  }

  function handleFieldClick(field: FieldItem) {
    emit('select', field.type, field.name);
  }
</script>

<style lang="less" scoped>
  .crm-form-design-field-title {
    @apply font-semibold;

    margin-bottom: 16px;
    color: var(--text-n1);
  }
  .crm-form-design-field-wrapper {
    @apply grid grid-cols-2;

    gap: 12px;
    .crm-form-design-field-item {
      @apply flex cursor-move items-center;

      padding: 6px 12px;
      border: 1px solid transparent;
      border-radius: var(--border-radius-small);
      background-color: var(--text-n9);
      line-height: 22px;
      gap: 8px;
      &:hover {
        border: 1px solid var(--primary-1);
        color: var(--primary-1);
      }
    }
  }
</style>
