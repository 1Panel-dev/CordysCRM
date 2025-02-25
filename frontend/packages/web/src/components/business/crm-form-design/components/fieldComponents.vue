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
    <VueDraggable
      v-model="advancedFields"
      :animation="150"
      ghost-class="crm-form-design--composition-item-ghost"
      :group="{ name: 'crmFormDesign', pull: 'clone', put: false }"
      :clone="clone"
      :sort="false"
      class="crm-form-design-field-wrapper"
    >
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
    </VueDraggable>
  </div>
</template>

<script setup lang="ts">
  import { VueDraggable } from 'vue-draggable-plus';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import { advancedFields, basicFields } from '@/components/business/crm-form-create/config';
  import { FieldTypeEnum } from '@/components/business/crm-form-create/enum';
  import { FormCreateField } from '@/components/business/crm-form-create/types';

  import { useI18n } from '@/hooks/useI18n';
  import { getGenerateId } from '@/utils';

  const emit = defineEmits<{
    (e: 'select', field: FormCreateField): void;
  }>();

  const { t } = useI18n();

  function clone(e: FormCreateField) {
    const res: FormCreateField = {
      ...e,
      id: getGenerateId(),
      name: t(e.name),
    };
    if (
      [
        FieldTypeEnum.DEPARTMENT,
        FieldTypeEnum.MEMBER,
        FieldTypeEnum.CHECKBOX,
        FieldTypeEnum.RADIO,
        FieldTypeEnum.SELECT,
      ].includes(e.type) &&
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
    emit('select', field);
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
