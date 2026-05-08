<template>
  <n-scrollbar class="p-[16px]">
    <n-data-table
      :columns="columns"
      :data="formFields"
      :pagination="false"
      :bordered="false"
      class="form-permission-table crm-data-table-compact"
    />
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { h, ref, watch } from 'vue';
  import { DataTableColumn, NDataTable, NRadio, NScrollbar } from 'naive-ui';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ApprovalActionNode, ApprovalFieldPermissionMode } from '@lib/shared/models/system/process';

  import { getFormConfigApiMap } from '@/components/business/crm-form-create/config';
  import type { FormCreateField } from '@/components/business/crm-form-create/types';

  defineOptions({
    name: 'FormPermissionTab',
  });

  const props = defineProps<{
    formType: string;
  }>();

  const nodeConfig = defineModel<ApprovalActionNode>('nodeConfig', {
    required: true,
  });

  const { t } = useI18n();
  const formFields = ref<FormCreateField[]>([]);

  const permissionOptions: Array<{ label: string; value: ApprovalFieldPermissionMode }> = [
    {
      label: t('process.process.flow.permission.hidden'),
      value: 'HIDDEN',
    },
    {
      label: t('process.process.flow.permission.view'),
      value: 'VIEW',
    },
    {
      label: t('process.process.flow.permission.edit'),
      value: 'EDIT',
    },
  ];

  // 仅单行文本、多行文本支持编辑， 其它类型的编辑按钮禁用
  function isEditableField(field: FormCreateField) {
    return (
      [FieldTypeEnum.INPUT, FieldTypeEnum.TEXTAREA].includes(field.type) &&
      field.editable !== false &&
      !field.resourceFieldId
    );
  }

  function getFieldPermission(fieldId: string) {
    return nodeConfig.value.fieldPermissions?.find((item) => item.fieldId === fieldId)?.permissionType ?? 'VIEW';
  }

  function setFieldPermission(fieldId: string, permissionType: ApprovalFieldPermissionMode) {
    const fieldPermissions = nodeConfig.value.fieldPermissions ?? [];
    const target = fieldPermissions.find((item) => item.fieldId === fieldId);

    if (target) {
      target.permissionType = permissionType;
    } else {
      fieldPermissions.push({
        fieldId,
        permissionType,
      });
    }

    nodeConfig.value.fieldPermissions = fieldPermissions;
  }

  function isPermissionDisabled(field: FormCreateField, permissionType: ApprovalFieldPermissionMode) {
    return permissionType === 'EDIT' && !isEditableField(field);
  }

  function getSelectableFields(permissionType: ApprovalFieldPermissionMode) {
    return formFields.value.filter((field) => !isPermissionDisabled(field, permissionType));
  }

  function isColumnChecked(permissionType: ApprovalFieldPermissionMode) {
    const selectableFields = getSelectableFields(permissionType);
    return (
      selectableFields.length > 0 && selectableFields.every((field) => getFieldPermission(field.id) === permissionType)
    );
  }

  function setColumnPermission(permissionType: ApprovalFieldPermissionMode) {
    getSelectableFields(permissionType).forEach((field) => {
      setFieldPermission(field.id, permissionType);
    });
  }

  const columns: DataTableColumn<FormCreateField>[] = [
    {
      title: t('process.process.flow.field'),
      key: 'name',
      ellipsis: {
        tooltip: true,
      },
      width: 120,
      render: (row) => row.name,
    },
    ...permissionOptions.map(
      (item) =>
        ({
          title: () =>
            h('div', { class: 'flex items-center justify-center gap-[8px]' }, [
              h(NRadio, {
                checked: isColumnChecked(item.value),
                disabled: getSelectableFields(item.value).length === 0,
                onUpdateChecked: (checked: boolean) => {
                  if (checked) {
                    setColumnPermission(item.value);
                  }
                },
              }),
              h('span', item.label),
            ]),
          key: item.value,
          render: (row) =>
            h(NRadio, {
              checked: getFieldPermission(row.id) === item.value,
              disabled: isPermissionDisabled(row, item.value),
              onUpdateChecked: (checked: boolean) => {
                if (checked) {
                  setFieldPermission(row.id, item.value);
                }
              },
            }),
        } as DataTableColumn<FormCreateField>)
    ),
  ];

  function normalizeFieldPermissions(fields: FormCreateField[]) {
    const existingPermissionMap = new Map(
      (nodeConfig.value.fieldPermissions ?? []).map((item) => [item.fieldId, item.permissionType])
    );

    nodeConfig.value.fieldPermissions = fields.map((field) => {
      const permissionType = existingPermissionMap.get(field.id) ?? 'VIEW';
      return {
        fieldId: field.id,
        permissionType: permissionType === 'EDIT' && !isEditableField(field) ? 'VIEW' : permissionType,
      };
    });
  }

  async function loadFormFields() {
    try {
      const api = getFormConfigApiMap[props.formType as FormDesignKeyEnum];
      const res = await api();
      // 仅使用顶层字段；子表格保留父字段名，不展开 subFields 明细。
      formFields.value = res.fields;
      normalizeFieldPermissions(formFields.value);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => props.formType,
    () => {
      loadFormFields();
    },
    {
      immediate: true,
    }
  );
</script>

<style scoped lang="less">
  .form-permission-table {
    :deep(.n-data-table-th) {
      padding-top: 6px;
      padding-bottom: 6px;
      font-weight: 400;
      color: var(--text-n4);
      background-color: var(--text-n9);
    }
  }
</style>
