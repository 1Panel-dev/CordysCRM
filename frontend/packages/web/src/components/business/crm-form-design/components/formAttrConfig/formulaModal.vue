<template>
  <CrmModal
    v-model:show="visible"
    :title="t('crmFormDesign.formulaSetting')"
    :positive-text="t('common.confirm')"
    :maskClosable="false"
    footer
    :width="800"
    @confirm="saveCalculateFormula"
    @cancel="handleCancel"
  >
    <CrmFormulaEditor
      ref="crmFormulaEditorRef"
      :field-config="fieldConfig"
      :form-fields="formFields"
      :is-sub-table-field="isSubTableField"
    />
  </CrmModal>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import { FormCreateField } from '@/components/business/crm-form-create/types';
  import CrmFormulaEditor from '@/components/business/crm-formula-editor/index.vue';

  const { t } = useI18n();
  const visible = defineModel<boolean>('visible', { required: true });

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formFields: FormCreateField[];
    isSubTableField?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'save', result?: string): void;
  }>();

  const crmFormulaEditorRef = ref<InstanceType<typeof CrmFormulaEditor>>();

  function saveCalculateFormula() {
    const result = crmFormulaEditorRef.value?.getCalculateFormula();
    emit('save', result);
  }

  function handleCancel() {
    visible.value = false;
  }
</script>

<style scoped lang="less"></style>
