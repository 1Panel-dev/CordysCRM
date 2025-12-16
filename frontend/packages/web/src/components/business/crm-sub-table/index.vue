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
  import { DataTableCreateSummary, NButton, NDataTable, NTooltip } from 'naive-ui';
  import { isEqual } from 'lodash-es';

  import { FieldRuleEnum, FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { SpecialColumnEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatTimeValue, getCityPath, getIndustryPath } from '@lib/shared/method';
  import { formatNumberValue, normalizeNumber } from '@lib/shared/method/formCreate';

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
    disabled?: boolean;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: Record<string, any>[]): void;
  }>();

  const { t } = useI18n();

  const data = defineModel<Record<string, any>[]>('value', {
    required: true,
  });

  function makeRequiredTitle(title: string) {
    return h(
      NTooltip,
      { class: 'flex items-center' },
      {
        trigger: () =>
          h(
            'div',
            {
              class: 'flex items-center',
            },
            {
              default: () => [
                h('div', { class: 'overflow-hidden text-ellipsis' }, { default: () => title }),
                h('div', { class: 'text-[var(--error-red)] ml-[4px]' }, '*'),
              ],
            }
          ),
        default: () => h('div', {}, { default: () => title }),
      }
    );
  }

  function initFieldValueText(field: FormCreateField, id: string, value: any): string {
    const options = props.optionMap?.[id];
    let name: string | string[] = '';
    // 区分未选择空值和选项不存在
    if (
      value === null ||
      value === undefined ||
      (Array.isArray(value) && value.length === 0) ||
      (typeof value === 'string' && value.trim() === '')
    ) {
      return '-';
    }
    // 若字段值是选项值，则取选项值的name
    if (options) {
      if (Array.isArray(value)) {
        name = value.map((e) => {
          const option = options.find((opt) => opt.id === e);
          if (option) {
            return option.name || t('common.optionNotExist');
          }
          return t('common.optionNotExist');
        });
      } else {
        name = options.find((e) => e.id === value)?.name || t('common.optionNotExist');
      }
      return Array.isArray(name) ? name.join(', ') : name;
    }
    if (field.type === FieldTypeEnum.DATA_SOURCE) {
      // 数据源字段且找不到 optionMap 对应值，则显示不存在
      return t('common.optionNotExist');
    }
    switch (field.type) {
      case FieldTypeEnum.INPUT_NUMBER:
        return formatNumberValue(value, field);
      case FieldTypeEnum.DATE_TIME:
        return formatTimeValue(value, field.dateType);
      case FieldTypeEnum.LOCATION:
        const addressArr: string[] = value.split('-') || [];
        return addressArr.length
          ? `${getCityPath(addressArr[0])}-${addressArr.filter((e, i) => i > 0).join('-')}`
          : '-';
      case FieldTypeEnum.SELECT:
      case FieldTypeEnum.RADIO:
        return field.options?.find((e) => e.value === value)?.label || '-';
      case FieldTypeEnum.SELECT_MULTIPLE:
      case FieldTypeEnum.CHECKBOX:
        if (Array.isArray(value)) {
          const labels = field.options?.filter((e) => value.includes(e.value)).map((e) => e.label);
          return labels && labels.length ? labels.join(', ') : '-';
        }
        return '-';
      case FieldTypeEnum.INDUSTRY:
        return value ? getIndustryPath(value) : '-';
      default:
        return value || '-';
    }
  }

  function applyFieldLink(item: FormCreateField, row: Record<string, any> = {}) {
    const currentFieldValue = row[item.id];
    const linkField = props.subFields.find((f) => f.id === item.linkProp?.targetField);
    if (item.linkProp?.linkOptions) {
      for (let i = 0; i < item.linkProp?.linkOptions.length; i++) {
        const option = item.linkProp?.linkOptions[i];
        if (isEqual(currentFieldValue, option.current)) {
          if (linkField) {
            if (option.method === 'HIDDEN') {
              linkField.linkRange = Array.isArray(option.target) ? option.target : [option.target];
            } else {
              linkField.linkRange = undefined;
              row[linkField.id] = option.target;
            }
            return;
          }
        } else if (linkField) {
          linkField.linkRange = undefined;
        }
      }
    }
  }

  const sumInitialOptions = ref<Record<string, any>[]>([]);
  const renderColumns = computed<CrmDataTableColumn[]>(() => {
    if (props.readonly) {
      return props.subFields
        .filter((field) => field.readable)
        .map((field, index) => {
          let key = field.businessKey || field.id;
          if (field.resourceFieldId) {
            // 数据源引用字段用 id作为 key
            key = field.id;
          }
          return {
            title: field.showLabel ? field.name : '',
            width: 150,
            key,
            fieldId: key,
            ellipsis: {
              tooltip: true,
            },
            render: (row: any) =>
              h(
                NTooltip,
                { trigger: 'hover' },
                {
                  default: () => initFieldValueText(field, key, row[key]),
                  trigger: () =>
                    h('div', { class: 'one-line-text max-w-[200px]' }, initFieldValueText(field, key, row[key])),
                }
              ),
            filedType: field.type,
            fixed: props.fixedColumn && props.fixedColumn >= index + 1 ? 'left' : undefined,
          };
        });
    }
    return props.subFields
      .filter((field) => field.readable)
      .map((field, index) => {
        let key = field.businessKey || field.id;
        if (field.resourceFieldId) {
          // 数据源引用字段用 id作为 key
          key = field.id;
        }
        if (field.resourceFieldId) {
          return {
            title: field.showLabel ? field.name : '',
            width: 120,
            key,
            ellipsis: {
              tooltip: true,
            },
            fieldId: key,
            render: (row: any) =>
              h(
                NTooltip,
                { trigger: 'hover' },
                {
                  default: () => row[key],
                  trigger: () => h('div', { class: 'one-line-text max-w-[200px]' }, row[key]),
                }
              ),
            fixed: props.fixedColumn && props.fixedColumn >= index + 1 ? 'left' : undefined,
          };
        }
        let title: any = field.name;
        if (field.showLabel === false) {
          title = '';
        } else if (field.rules.some((rule) => rule.key === FieldRuleEnum.REQUIRED)) {
          title = () => makeRequiredTitle(field.name);
        }
        if (field.type === FieldTypeEnum.DATA_SOURCE) {
          return {
            title,
            width: 250,
            key,
            ellipsis: {
              tooltip: true,
            },
            fieldId: key,
            render: (row: any, rowIndex: number) => {
              return h(dataSource, {
                value: row[key],
                fieldConfig: {
                  ...field,
                  initialOptions: [...(field.initialOptions || []), ...sumInitialOptions.value],
                },
                path: `${props.parentId}[${rowIndex}].${key}`,
                isSubTableRender: true,
                needInitDetail: props.needInitDetail,
                formDetail: props.formDetail,
                disabled: props.disabled,
                class: 'w-[240px]',
                disabledSelection: (r: Record<string, any>) => {
                  if (key === 'product') {
                    // 产品列不允许重复选择
                    return data.value.some(
                      (dataRow, dataRowIndex) => dataRowIndex !== rowIndex && dataRow[key].includes(r.id)
                    );
                  }
                  return false;
                },
                onChange: (val, source) => {
                  row[key] = val;
                  sumInitialOptions.value = sumInitialOptions.value.concat(
                    ...source.filter((s) => !sumInitialOptions.value.some((io) => io.id === s.id))
                  );
                  if (field.showFields?.length) {
                    // 数据源显示字段联动
                    const showFields = props.subFields.filter((f) => f.resourceFieldId === field.id);
                    const targetSource = source.find((s) => s.id === val[0]);
                    showFields.forEach((sf) => {
                      let fieldVal: string | string[] = '';
                      if (targetSource) {
                        const sourceFieldVal = targetSource[sf.businessKey || sf.id];
                        if (sf.subTableFieldId) {
                          // 根据同一行选择的业务产品字段值去获取价格表内对应产品的字段值 TODO:后续应该使用字段联动配置去实现
                          if (targetSource.products && row.product?.length) {
                            fieldVal =
                              targetSource.products.find((st: any) => st.product === row.product[0])?.[sf.id] || '';
                          } else {
                            fieldVal = '';
                          }
                        } else {
                          fieldVal = sourceFieldVal;
                        }
                      }
                      row[sf.id] = Array.isArray(fieldVal) ? fieldVal.join(',') : fieldVal;
                    });
                  }
                  emit('change', data.value);
                },
              });
            },
            fixed: props.fixedColumn && props.fixedColumn >= index + 1 ? 'left' : undefined,
          };
        }
        if (field.type === FieldTypeEnum.FORMULA) {
          return {
            title,
            width: 200,
            key,
            ellipsis: {
              tooltip: true,
            },
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
            title,
            width: 200,
            key,
            ellipsis: {
              tooltip: true,
            },
            fieldId: key,
            render: (row: any, rowIndex: number) =>
              h(inputNumber, {
                value: row[key],
                fieldConfig: field,
                path: `${props.parentId}[${rowIndex}].${key}`,
                isSubTableRender: true,
                disabled: props.disabled,
                needInitDetail: props.needInitDetail,
                onChange: (val: any) => {
                  row[key] = val;
                  emit('change', data.value);
                },
              }),
            fixed: props.fixedColumn && props.fixedColumn >= index + 1 ? 'left' : undefined,
          };
        }
        if ([FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE].includes(field.type)) {
          return {
            title,
            width: 200,
            key,
            ellipsis: {
              tooltip: true,
            },
            fieldId: key,
            render: (row: any, rowIndex: number) =>
              h(select, {
                value: row[key],
                fieldConfig: field,
                path: `${props.parentId}[${rowIndex}].${key}`,
                isSubTableRender: true,
                disabled: props.disabled,
                needInitDetail: props.needInitDetail,
                class: 'w-[190px]',
                onChange: (val: any) => {
                  row[key] = val;
                  // 字段联动
                  if (field.linkProp?.targetField && field.linkProp?.linkOptions.length) {
                    applyFieldLink(field, row);
                  }
                  emit('change', data.value);
                },
              }),
            fixed: props.fixedColumn && props.fixedColumn >= index + 1 ? 'left' : undefined,
          };
        }
        return {
          title,
          width: 200,
          key,
          ellipsis: {
            tooltip: true,
          },
          fieldId: key,
          render: (row: any, rowIndex: number) =>
            h(singleText, {
              value: row[key],
              fieldConfig: field,
              path: `${props.parentId}[${rowIndex}].${key}`,
              isSubTableRender: true,
              disabled: props.disabled,
              needInitDetail: props.needInitDetail,
              onChange: (val: any) => {
                row[key] = val;
                emit('change', data.value);
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
                emit('change', data.value);
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
            { class: 'flex items-center' },
            {
              default: () => {
                const sum =
                  pageData.reduce((prev, row) => {
                    const rowVal = normalizeNumber(row[col.key as keyof RowData]);
                    return prev + Math.round(rowVal * 100);
                  }, 0) / 100;

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
      if (field.type === FieldTypeEnum.INPUT_NUMBER) {
        newRow[key] = field.defaultValue ?? null;
      } else if (field.type === FieldTypeEnum.FORMULA) {
        newRow[key] = field.defaultValue ?? 0;
      } else if ([FieldTypeEnum.SELECT_MULTIPLE, FieldTypeEnum.DATA_SOURCE].includes(field.type)) {
        newRow[key] = [];
      } else {
        newRow[key] = field.defaultValue ?? '';
      }
    });
    data.value.push(newRow);
  }
</script>

<style lang="less">
  .crm-sub-table {
    .n-data-table-th {
      padding: 12px 4px;
      .n-data-table-th__title {
        width: 100%;
        .n-ellipsis {
          max-width: 100%;
          span {
            overflow: hidden;
            text-overflow: ellipsis;
          }
        }
      }
    }
    .n-data-table-td {
      padding: 8px 4px;
    }
    .n-form-item-blank--error + .n-form-item-feedback-wrapper {
      @apply block;
    }
    .n-form-item-feedback-wrapper {
      @apply hidden;

      height: 18px;
      min-height: 0;
    }
  }
</style>
