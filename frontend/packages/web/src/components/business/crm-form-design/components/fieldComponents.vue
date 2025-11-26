<template>
  <n-scrollbar class="flex flex-col p-[16px]">
    <div class="crm-form-design-field-title">{{ t('crmFormDesign.basicField') }}</div>
    <VueDraggable
      v-model="basicFields"
      :animation="150"
      ghost-class="crm-form-design--composition-item-ghost"
      :group="{ name: 'crmFormDesign', pull: 'clone', put: false }"
      :clone="clone"
      :sort="false"
      class="crm-form-design-field-wrapper mb-[24px]"
      @move="handleMove"
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
    <VueDraggable
      v-model="realAdvancedFields"
      :animation="150"
      ghost-class="crm-form-design--composition-item-ghost"
      :group="{ name: 'crmFormDesign', pull: 'clone', put: false }"
      :clone="clone"
      :sort="false"
      class="crm-form-design-field-wrapper"
      @move="handleMove"
    >
      <div
        v-for="field of realAdvancedFields"
        :key="field.type"
        class="crm-form-design-field-item"
        :class="getFieldDisable(field) ? 'crm-form-design-field-item--disabled' : ''"
        :draggable="!getFieldDisable(field)"
        @click="() => handleFieldClick(field)"
      >
        <CrmIcon :type="field.icon" />
        <div>{{ t(field.name) }}</div>
      </div>
    </VueDraggable>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { NScrollbar } from 'naive-ui';
  import { VueDraggable } from 'vue-draggable-plus';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getGenerateId } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import { advancedFields, basicFields } from '@/components/business/crm-form-create/config';
  import { FormCreateField } from '@/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldList: FormCreateField[];
    formKey: FormDesignKeyEnum;
  }>();
  const emit = defineEmits<{
    (e: 'select', field: FormCreateField): void;
  }>();

  const { t } = useI18n();

  const realAdvancedFields: FormCreateField[] = [];
  if (props.formKey === FormDesignKeyEnum.PRICE) {
    advancedFields.forEach((field) => {
      if (field.type !== FieldTypeEnum.SUB_PRICE) {
        realAdvancedFields.push(field);
      }
    });
  } else if (props.formKey === FormDesignKeyEnum.OPPORTUNITY_QUOTATION) {
    advancedFields.forEach((field) => {
      if (field.type !== FieldTypeEnum.SUB_PRODUCT) {
        realAdvancedFields.push(field);
      }
    });
  } else {
    advancedFields.forEach((field) => {
      if (![FieldTypeEnum.SUB_PRODUCT, FieldTypeEnum.SUB_PRICE].includes(field.type)) {
        realAdvancedFields.push(field);
      }
    });
  }

  function getFieldDisable(item: FormCreateField) {
    if (item.type === FieldTypeEnum.SERIAL_NUMBER) {
      return props.fieldList.some((e) => e.type === FieldTypeEnum.SERIAL_NUMBER);
    }
    if (item.type === FieldTypeEnum.SUB_PRICE) {
      return props.fieldList.some((e) => e.type === FieldTypeEnum.SUB_PRICE);
    }
    if (item.type === FieldTypeEnum.SUB_PRODUCT) {
      return props.fieldList.some((e) => e.type === FieldTypeEnum.SUB_PRODUCT);
    }
    if (item.type === FieldTypeEnum.FORMULA) {
      return props.fieldList.some((e) => e.type === FieldTypeEnum.FORMULA);
    }
    return false;
  }

  function handleMove(e: any) {
    return !getFieldDisable(e.data) && e.to.className.includes('crm-form-design-subtable-wrapper')
      ? [
          FieldTypeEnum.INPUT,
          FieldTypeEnum.INPUT_NUMBER,
          FieldTypeEnum.SELECT,
          FieldTypeEnum.SELECT_MULTIPLE,
          FieldTypeEnum.DATA_SOURCE,
        ].includes(e.data.type)
      : true;
  }

  function clone(e: FormCreateField) {
    const res: FormCreateField = {
      ...e,
      id: getGenerateId(),
      name: t(e.name),
    };
    if (
      [FieldTypeEnum.CHECKBOX, FieldTypeEnum.RADIO, FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE].includes(
        e.type
      ) &&
      e.options?.length === 0
    ) {
      res.options = [
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
    }
    return res;
  }

  function handleFieldClick(field: FormCreateField) {
    if (!getFieldDisable(field)) {
      emit('select', field);
    }
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
    .crm-form-design-field-item--disabled {
      @apply cursor-not-allowed;

      color: var(--text-n6);
      &:hover {
        border: 1px solid transparent;
        color: var(--text-n6);
      }
    }
  }
</style>
