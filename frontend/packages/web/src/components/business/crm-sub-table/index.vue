<template>
  <n-data-table
    :columns="realColumns"
    :data="data"
    :paging="false"
    :pagination="false"
    :scroll-x="scrollXWidth"
    :summary="props.sumColumns?.length ? summary : undefined"
    class="crm-sub-table"
  />
  <n-button v-if="!props.readonly" type="primary" text class="mt-[8px]" @click="addLine">
    <CrmIcon type="iconicon_add" class="mr-[8px]" />
    {{ t('crm.subTable.addLine') }}
  </n-button>
</template>

<script setup lang="ts">
  import { DataTableCreateSummary, NButton, NDataTable } from 'naive-ui';

  import { FieldRuleEnum, FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { SpecialColumnEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatNumberValue } from '@lib/shared/method/formCreate';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import dataSource from '@/components/business/crm-form-create/components/advanced/dataSource.vue';
  import formula from '@/components/business/crm-form-create/components/advanced/formula.vue';
  import inputNumber from '@/components/business/crm-form-create/components/basic/inputNumber.vue';
  import select from '@/components/business/crm-form-create/components/basic/select.vue';
  import singleText from '@/components/business/crm-form-create/components/basic/singleText.vue';

  import { FormCreateField } from '../crm-form-create/types';
  import { RowData, TableColumns } from 'naive-ui/es/data-table/src/interface';

  const props = defineProps<{
    parentId: string;
    subFields: FormCreateField[];
    fixedColumn?: number;
    sumColumns?: string[];
    formDetail?: Record<string, any>;
    needInitDetail?: boolean; // 判断是否编辑情况
    readonly?: boolean;
    optionMap?: Record<string, any[]>;
  }>();

  const { t } = useI18n();

  const data = defineModel<Record<string, any>[]>('value', {
    required: true,
  });

  function makeRequiredTitle(title: string) {
    return h('div', { class: 'flex items-center' }, [
      h('span', {}, title),
      h('span', { class: 'text-[var(--error-red)] ml-[4px]' }, '*'),
    ]);
  }

  const renderColumns = computed<CrmDataTableColumn[]>(() => {
    if (props.readonly) {
      return props.subFields.map((field, index) => {
        const key = field.businessKey || field.id;
        if (field.type === FieldTypeEnum.INPUT_NUMBER) {
          return {
            title: field.name,
            width: 150,
            key,
            fieldId: key,
            filedType: field.type,
            fieldConfig: field,
            render: (row: any) => formatNumberValue(row[key], field),
            fixed: props.fixedColumn && props.fixedColumn >= index + 1 ? 'left' : undefined,
          };
        }
        if (field.type === FieldTypeEnum.DATA_SOURCE) {
          return {
            title: field.name,
            width: 200,
            key,
            fieldId: key,
            render: (row: any) => props.optionMap?.[key]?.find((e) => e.id === row[key])?.name || '',
            filedType: field.type,
            fixed: props.fixedColumn && props.fixedColumn >= index + 1 ? 'left' : undefined,
          };
        }
        if ([FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE].includes(field.type)) {
          return {
            title: field.name,
            width: 150,
            key,
            fieldId: key,
            render: (row: any) =>
              field.options
                ?.filter((option) =>
                  field.type === FieldTypeEnum.SELECT
                    ? option.value === row[key]
                    : (row[key] || []).includes(option.value)
                )
                .map((option) => option.label)
                .join(', '),
            filedType: field.type,
            fixed: props.fixedColumn && props.fixedColumn >= index + 1 ? 'left' : undefined,
          };
        }
        return {
          title: field.name,
          width: 150,
          key,
          fieldId: key,
          render: (row: any) => row[key],
          filedType: field.type,
          fixed: props.fixedColumn && props.fixedColumn >= index + 1 ? 'left' : undefined,
        };
      });
    }
    return props.subFields.map((field, index) => {
      const key = field.businessKey || field.id;
      if (field.type === FieldTypeEnum.DATA_SOURCE) {
        return {
          title: field.rules.some((rule) => rule.key === FieldRuleEnum.REQUIRED)
            ? () => makeRequiredTitle(field.name)
            : field.name,
          width: 250,
          key,
          fieldId: key,
          render: (row: any, rowIndex: number) =>
            h(dataSource, {
              value: row[key] || [],
              fieldConfig: field,
              path: `${props.parentId}[${rowIndex}].${key}`,
              isSubTableRender: true,
              needInitDetail: props.needInitDetail,
              formDetail: props.formDetail,
              onChange: (val: any) => {
                row[key] = val;
              },
            }),
          fixed: props.fixedColumn && props.fixedColumn >= index + 1 ? 'left' : undefined,
        };
      }
      if (field.type === FieldTypeEnum.FORMULA) {
        return {
          title: field.rules.some((rule) => rule.key === FieldRuleEnum.REQUIRED)
            ? () => makeRequiredTitle(field.name)
            : field.name,
          width: 200,
          key,
          fieldId: key,
          render: (row: any, rowIndex: number) =>
            h(formula, {
              value: row[key],
              fieldConfig: field,
              path: `${props.parentId}[${rowIndex}].${key}`,
              isSubTableRender: true,
              needInitDetail: props.needInitDetail,
              formDetail: props.formDetail,
              onChange: (val: any) => {
                row[key] = val;
              },
            }),
          fixed: props.fixedColumn && props.fixedColumn >= index + 1 ? 'left' : undefined,
        };
      }
      if (field.type === FieldTypeEnum.INPUT_NUMBER) {
        return {
          title: field.rules.some((rule) => rule.key === FieldRuleEnum.REQUIRED)
            ? () => makeRequiredTitle(field.name)
            : field.name,
          width: 200,
          key,
          fieldId: key,
          render: (row: any, rowIndex: number) =>
            h(inputNumber, {
              value: row[key],
              fieldConfig: field,
              path: `${props.parentId}[${rowIndex}].${key}`,
              isSubTableRender: true,
              needInitDetail: props.needInitDetail,
              onChange: (val: any) => {
                row[key] = val;
              },
            }),
          fixed: props.fixedColumn && props.fixedColumn >= index + 1 ? 'left' : undefined,
        };
      }
      if ([FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE].includes(field.type)) {
        return {
          title: field.rules.some((rule) => rule.key === FieldRuleEnum.REQUIRED)
            ? () => makeRequiredTitle(field.name)
            : field.name,
          width: 200,
          key,
          fieldId: key,
          render: (row: any, rowIndex: number) =>
            h(select, {
              value: row[key],
              fieldConfig: field,
              path: `${props.parentId}[${rowIndex}].${key}`,
              isSubTableRender: true,
              needInitDetail: props.needInitDetail,
              onChange: (val: any) => {
                row[key] = val;
              },
            }),
          fixed: props.fixedColumn && props.fixedColumn >= index + 1 ? 'left' : undefined,
        };
      }
      return {
        title: field.rules.some((rule) => rule.key === FieldRuleEnum.REQUIRED)
          ? () => makeRequiredTitle(field.name)
          : field.name,
        width: 200,
        key,
        fieldId: key,
        render: (row: any, rowIndex: number) =>
          h(singleText, {
            value: row[key],
            fieldConfig: field,
            path: `${props.parentId}[${rowIndex}].${key}`,
            isSubTableRender: true,
            needInitDetail: props.needInitDetail,
            onChange: (val: any) => {
              row[key] = val;
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
    if (!props.readonly) {
      cols.push({
        title: '',
        key: 'operation',
        fixed: 'right',
        width: 40,
        render: (row: any, rowIndex: number) => {
          return h(
            NButton,
            {
              ghost: true,
              class: 'p-[8px_9px]',
              onClick: () => {
                data.value.splice(rowIndex, 1);
              },
            },
            { default: () => h(CrmIcon, { type: 'iconicon_minus_circle1' }) }
          );
        },
      });
    }
    return cols as TableColumns;
  });
  const scrollXWidth = computed(() =>
    realColumns.value.reduce((prev, curr) => {
      const width = typeof curr.width === 'number' ? curr.width : 0;
      return prev + width;
    }, 0)
  );

  const summary: DataTableCreateSummary = (pageData) => {
    const summaryRes: Record<string, any> = {
      [SpecialColumnEnum.ORDER]: {
        value: h('div', { class: 'flex items-center justify-center' }, t('crmFormDesign.sum')),
      },
    };
    renderColumns.value.forEach((col) => {
      if (props.sumColumns?.includes(col.key as string)) {
        summaryRes[col.key || ''] = {
          value: h(
            'div',
            { class: 'flex items-center ml-[4px]' },
            {
              default: () => {
                const sum = (pageData as unknown as RowData[]).reduce(
                  (prevValue, row) => prevValue + row[col.key as keyof RowData],
                  0
                );
                if (col.filedType === FieldTypeEnum.INPUT_NUMBER && col.fieldConfig) {
                  return formatNumberValue(sum, col.fieldConfig);
                }
                return sum;
              },
            }
          ),
        };
      }
    });
    return summaryRes;
  };

  function addLine() {
    const newRow: Record<string, any> = {};
    props.subFields.forEach((field) => {
      const key = field.businessKey || field.id;
      newRow[key] = field.type === FieldTypeEnum.INPUT_NUMBER ? null : '';
    });
    data.value.push(newRow);
  }
</script>

<style lang="less">
  .crm-sub-table {
    .n-data-table-th {
      padding: 12px 4px;
    }
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
