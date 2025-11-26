<template>
  <n-data-table
    :columns="realColumns"
    :data="data"
    :paging="false"
    :pagination="false"
    :scroll-x="scrollXWidth"
    class="crm-sub-table"
  />
</template>

<script setup lang="ts">
  import { NDataTable } from 'naive-ui';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { SpecialColumnEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatNumberValue } from '@lib/shared/method/formCreate';

  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import dataSource from '@/components/business/crm-form-create/components/advanced/dataSource.vue';
  import formula from '@/components/business/crm-form-create/components/advanced/formula.vue';
  import inputNumber from '@/components/business/crm-form-create/components/basic/inputNumber.vue';
  import singleText from '@/components/business/crm-form-create/components/basic/singleText.vue';

  import { FormCreateField } from '../crm-form-create/types';
  import { TableColumns } from 'naive-ui/es/data-table/src/interface';

  const props = defineProps<{
    parentId: string;
    subFields: FormCreateField[];
    fixedColumn?: number;
    formDetail?: Record<string, any>;
    needInitDetail?: boolean; // 判断是否编辑情况
    readonly?: boolean;
  }>();

  const { t } = useI18n();

  const data = defineModel<Record<string, any>[]>('value', {
    default: [],
  });

  const renderColumns = computed<CrmDataTableColumn[]>(() => {
    if (props.readonly) {
      return props.subFields.map((field, index) => {
        if (field.type === FieldTypeEnum.INPUT_NUMBER) {
          return {
            title: field.name,
            width: 150,
            key: field.id,
            fieldId: field.id,
            render: (row: any) => formatNumberValue(row[field.id], field),
            filedType: field.type,
            fixed: props.fixedColumn && props.fixedColumn >= index + 1 ? 'left' : undefined,
          };
        }
        return {
          title: field.name,
          width: 150,
          key: field.id,
          fieldId: field.id,
          render: (row: any) => row[field.id],
          filedType: field.type,
          fixed: props.fixedColumn && props.fixedColumn >= index + 1 ? 'left' : undefined,
        };
      });
    }
    return props.subFields.map((field, index) => {
      if (field.type === FieldTypeEnum.DATA_SOURCE) {
        return {
          title: field.name,
          width: 150,
          key: field.id,
          fieldId: field.id,
          render: (row: any, rowIndex: number) =>
            h(dataSource, {
              value: row[field.id],
              fieldConfig: field,
              path: `${props.parentId}[${rowIndex}].${field.id}`,
              isSubTableRender: true,
              needInitDetail: props.needInitDetail,
              formDetail: props.formDetail,
              onChange: (val: any) => {
                row[field.id] = val;
              },
            }),
          fixed: props.fixedColumn && props.fixedColumn >= index + 1 ? 'left' : undefined,
        };
      }
      if (field.type === FieldTypeEnum.FORMULA) {
        return {
          title: field.name,
          width: 150,
          key: field.id,
          fieldId: field.id,
          render: (row: any, rowIndex: number) =>
            h(formula, {
              value: row[field.id],
              fieldConfig: field,
              path: `${props.parentId}[${rowIndex}].${field.id}`,
              isSubTableRender: true,
              needInitDetail: props.needInitDetail,
              formDetail: props.formDetail,
              onChange: (val: any) => {
                row[field.id] = val;
              },
            }),
          fixed: props.fixedColumn && props.fixedColumn >= index + 1 ? 'left' : undefined,
        };
      }
      if (field.type === FieldTypeEnum.INPUT_NUMBER) {
        return {
          title: field.name,
          width: 150,
          key: field.id,
          fieldId: field.id,
          render: (row: any, rowIndex: number) =>
            h(inputNumber, {
              value: row[field.id],
              fieldConfig: field,
              path: `${props.parentId}[${rowIndex}].${field.id}`,
              isSubTableRender: true,
              needInitDetail: props.needInitDetail,
              onChange: (val: any) => {
                row[field.id] = val;
              },
            }),
          fixed: props.fixedColumn && props.fixedColumn >= index + 1 ? 'left' : undefined,
        };
      }
      return {
        title: field.name,
        width: 150,
        key: field.id,
        fieldId: field.id,
        render: (row: any, rowIndex: number) =>
          h(singleText, {
            value: row[field.id],
            fieldConfig: field,
            path: `${props.parentId}[${rowIndex}].${field.id}`,
            isSubTableRender: true,
            needInitDetail: props.needInitDetail,
            onChange: (val: any) => {
              row[field.id] = val;
            },
          }),
        fixed: props.fixedColumn && props.fixedColumn >= index + 1 ? 'left' : undefined,
        filedType: field.type,
      };
    });
  });

  const realColumns = computed(() => {
    const cols: CrmDataTableColumn[] = [
      {
        fixed: 'left',
        key: SpecialColumnEnum.ORDER,
        title: '',
        width: 38,
        resizable: false,
        render: (row: any, rowIndex: number) =>
          h('div', { class: 'flex items-center justify-center' }, { default: () => rowIndex + 1 }),
      },
      ...renderColumns.value,
    ];
    return cols as TableColumns;
  });
  const scrollXWidth = computed(() =>
    realColumns.value.reduce((prev, curr) => {
      const width = typeof curr.width === 'number' ? curr.width : 0;
      return prev + width;
    }, 0)
  );
</script>

<style lang="less">
  .crm-sub-table {
    .n-data-table-td {
      padding: 8px 4px;
    }
    .n-form-item-blank--error + .n-form-item-feedback-wrapper {
      @apply block;
    }
    .n-form-item-feedback-wrapper {
      @apply hidden;

      height: 16px;
      min-height: 0;
    }
  }
</style>
