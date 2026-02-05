<template>
  <n-data-table
    :columns="realColumns"
    :data="data || []"
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
  import { DataTableCreateSummary, NButton, NDataTable, NImage, NImageGroup, NTooltip, useMessage } from 'naive-ui';
  import { isEqual } from 'lodash-es';

  import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';
  import { FieldRuleEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { SpecialColumnEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatTimeValue, getCityPath, getIndustryPath } from '@lib/shared/method';
  import { formatNumberValue, formatNumberValueToString, normalizeNumber } from '@lib/shared/method/formCreate';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import dataSource from '@/components/business/crm-form-create/components/advanced/dataSource.vue';
  import formula from '@/components/business/crm-form-create/components/advanced/formula.vue';
  import upload from '@/components/business/crm-form-create/components/advanced/upload.vue';
  import inputNumber from '@/components/business/crm-form-create/components/basic/inputNumber.vue';
  import select from '@/components/business/crm-form-create/components/basic/select.vue';
  import singleText from '@/components/business/crm-form-create/components/basic/singleText.vue';

  import { formKeyMap } from '../crm-data-source-select/config';
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
  const Message = useMessage();

  const data = defineModel<Record<string, any>[]>('value', {
    required: true,
    default: () => [],
  });

  function makeTitle(field: FormCreateField) {
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
                h(
                  'div',
                  { class: 'overflow-hidden text-ellipsis' },
                  {
                    default: () => [
                      field.name,
                      field.resourceFieldId ? h(CrmIcon, { type: 'iconicon_correlation' }) : null,
                    ],
                  }
                ),
                field.rules.some((rule) => rule.key === FieldRuleEnum.REQUIRED)
                  ? h('div', { class: 'text-[var(--error-red)] ml-[4px]' }, '*')
                  : null,
              ],
            }
          ),
        default: () => h('div', {}, { default: () => field.name }),
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
        return formatNumberValueToString(value, field);
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

  function makeNewRow() {
    const newRow: Record<string, any> = {};
    props.subFields.forEach((field) => {
      const key = field.resourceFieldId ? field.id : field.businessKey || field.id;
      if (field.type === FieldTypeEnum.INPUT_NUMBER) {
        newRow[key] = field.resourceFieldId
          ? formatNumberValue(field.defaultValue ?? 0, field)
          : field.defaultValue ?? null;
      } else if (field.type === FieldTypeEnum.FORMULA) {
        newRow[key] = field.defaultValue ?? 0;
      } else if (
        [FieldTypeEnum.SELECT_MULTIPLE, FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.PICTURE].includes(field.type)
      ) {
        newRow[key] = [];
      } else {
        newRow[key] = field.defaultValue ?? '';
      }
    });
    return newRow;
  }

  function addLine() {
    const newRow = makeNewRow();
    data.value.push(newRow);
  }

  function applyDataSourceShowFields(
    field: FormCreateField,
    val: any[],
    row: Record<string, any>,
    source: Record<string, any>[],
    rowId?: string
  ) {
    if (field.showFields?.length) {
      // 数据源显示字段联动
      const showFields = props.subFields.filter((f) => f.resourceFieldId === field.id);
      const targetSource = source.filter((e) => !e.parentId).find((e) => val.includes(e.id)); // 子表格数据源是单选，所以目标数据源只有一个
      showFields.forEach((sf) => {
        let fieldVal: string | string[] = '';
        if (targetSource) {
          const sourceFieldVal = targetSource[sf.id]; // 数据源的显示字段都使用id 读取
          if (sf.subTableFieldId) {
            // 如果数据源显示字段是数据源的子表格字段，则需要 rowId 定位数据源子表格的行
            const subTableData = targetSource[sf.subTableFieldId];
            if (Array.isArray(subTableData) && rowId) {
              const subTableRow = subTableData.find((stRow) => stRow.id === rowId);
              if (subTableRow) {
                fieldVal = subTableRow[sf.id];
              }
            }
          } else {
            fieldVal = sourceFieldVal;
          }
        }
        if (Array.isArray(fieldVal)) {
          row[sf.id] = fieldVal.join(',');
        } else if (sf.type === FieldTypeEnum.INPUT_NUMBER && typeof fieldVal === 'number') {
          row[sf.id] = formatNumberValueToString(fieldVal, sf) ?? null;
        } else {
          row[sf.id] = fieldVal;
        }
      });
    }
  }

  const sumInitialOptions = ref<Record<string, any>[]>([]); // 记录子表格内数据源列的初始选项
  const pictureFields = computed<FormCreateField[]>(() => {
    return props.subFields.filter((field) => field.type === FieldTypeEnum.PICTURE);
  });
  const maxPictureCountMap = computed<Record<string, number>>(() => {
    return data.value.reduce((prev, curr) => {
      pictureFields.value.forEach((field) => {
        const key = field.businessKey || field.id;
        const currCount = Array.isArray(curr[key]) ? curr[key].length : 0;
        if (!prev[key] || currCount > prev[key]) {
          prev[key] = currCount;
        }
      });
      return prev;
    }, {} as Record<string, number>);
  });

  const isProcessingDataSourceChange = ref(false);
  function handleDataSourceChange(
    val: any[],
    source: Record<string, any>[],
    field: FormCreateField,
    row: Record<string, any>,
    rowIndex: number,
    isPriceSubTableShowSubField?: boolean
  ) {
    if (isProcessingDataSourceChange.value) {
      // 子表格添加多行会触发 change，避免重复处理
      return;
    }
    isProcessingDataSourceChange.value = true;
    const key = field.businessKey || field.id;
    const parents = source.filter((s) => !s.parentId);
    if (isPriceSubTableShowSubField && val.filter((e) => parents.some((p) => p.id === e)).length > 0) {
      // 价格表子表格特殊处理，需要填充多行
      const children = source.filter(
        (s) => s.parentId && data.value.every((r) => r.price_sub !== s.id) // 过滤已存在的行
      );
      if (children.length === 0 && val.length > 0) {
        Message.warning(t('crm.subTable.repeatAdd'));
      }
      if (children.length === 0 || !source.some((s) => s.parentId)) {
        // 没有选中子项或没有选中父项，都表示当前为清空
        row[key] = [];
        row.price_sub = '';
        applyDataSourceShowFields(field, [], row, source, row.price_sub);
        emit('change', data.value);
        nextTick(() => {
          isProcessingDataSourceChange.value = false;
        });
        return;
      }
      if (children.length > 1) {
        // 多选行时，新增多行且选中值为子项的父项信息
        row.price_sub = children[0]?.id;
        row[key] = [children[0].parentId, row.price_sub]; // 价格表 id 在第一位，因为当前是单选数据源，提交表单时只会保存第一个值
        applyDataSourceShowFields(field, row[key], row, source, row.price_sub); // 数据源显示字段读取值并显示
        for (let i = 1; i < children.length; i++) {
          // 补充新增行
          addLine();
        }
        nextTick(() => {
          // 等待行添加完成后，给新增的行补充行号和选中价格表数据源
          for (let i = rowIndex + 1; i < rowIndex + children.length; i++) {
            const newRow = data.value[i];
            newRow.price_sub = children[i - rowIndex]?.id;
            newRow[key] = [children[i - rowIndex]?.parentId, newRow.price_sub]; // 选中值为父项以及当前行
            applyDataSourceShowFields(field, newRow[key], newRow, source, newRow.price_sub); // 回显价格表带出的显示字段
          }
        });
      } else {
        // 单选行只有一个父级
        row.price_sub = children[0]?.id;
        row[key] = val
          .filter((e) => children.some((p) => p.parentId === e))
          .sort((a, b) => {
            // 保证父项在前，子项在后
            if (a === children[0].parentId) {
              return -1;
            }
            if (b === children[0].parentId) {
              return 1;
            }
            return 0;
          });
        applyDataSourceShowFields(field, row[key], row, source, row.price_sub);
      }
    } else {
      row[key] = val.filter((e) => parents.some((p) => p.id === e)).length > 0 ? val : [];
      applyDataSourceShowFields(field, val, row, source, row.price_sub);
      if (row[key].length === 0) {
        // 清空时把行号也清理
        row.price_sub = '';
      }
    }
    sumInitialOptions.value = sumInitialOptions.value.concat(
      ...source.filter((s) => !sumInitialOptions.value.some((io) => io.id === s.id))
    );
    nextTick(() => {
      isProcessingDataSourceChange.value = false;
    });
    emit('change', data.value);
  }

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
            width:
              maxPictureCountMap.value[field.id] > 0 && field.type === FieldTypeEnum.PICTURE
                ? maxPictureCountMap.value[field.id] * 112
                : 'auto',
            key,
            fieldId: key,
            // ellipsis:
            //   field.type === FieldTypeEnum.PICTURE
            //     ? undefined
            //     : {
            //         tooltip: true,
            //       },
            render: (row: any) =>
              field.type === FieldTypeEnum.PICTURE
                ? h(
                    NImageGroup,
                    {},
                    {
                      default: () =>
                        (row[key] || []).map((img: string) =>
                          h(NImage, { src: `${PreviewPictureUrl}/${img}`, class: 'w-[100px] h-[100px] mr-[8px]' })
                        ),
                    }
                  )
                : h(
                    NTooltip,
                    { trigger: 'hover' },
                    {
                      default: () => initFieldValueText(field, key, row[key]),
                      trigger: () => h('div', { class: '' }, initFieldValueText(field, key, row[key])),
                    }
                  ),
            filedType: field.type,
            fieldConfig: field,
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
        let title: any = field.name;
        if (field.showLabel === false) {
          title = '';
        } else {
          title = () => makeTitle(field);
        }
        if (field.resourceFieldId) {
          return {
            title,
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
            filedType: field.type,
            fieldConfig: field,
          };
        }
        if (field.type === FieldTypeEnum.DATA_SOURCE) {
          const isPriceSubTableShowSubField =
            field.dataSourceType && formKeyMap[field.dataSourceType] === FormDesignKeyEnum.PRICE;
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
                hideChildTag: isPriceSubTableShowSubField,
                onChange: (val, source) => {
                  handleDataSourceChange(val, source, field, row, rowIndex, isPriceSubTableShowSubField);
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
            filedType: field.type,
            fieldConfig: field,
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
            filedType: field.type,
            fieldConfig: field,
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
        if (field.type === FieldTypeEnum.PICTURE) {
          let finalPictureColWidth = 0;
          if (maxPictureCountMap.value[field.id]) {
            if (field.uploadLimit && maxPictureCountMap.value[field.id] >= field.uploadLimit) {
              finalPictureColWidth = field.uploadLimit * 112;
            } else {
              finalPictureColWidth = maxPictureCountMap.value[field.id] * 112 + 32;
            }
          }
          return {
            title,
            width: finalPictureColWidth || 150, // 每个卡片 100px + 10px间距 + 2px 冗余 + 上传按钮宽度 32px
            key,
            fieldId: key,
            render: (row: any, rowIndex: number) =>
              h(upload, {
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
            fieldConfig: field,
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
    props.readonly
      ? undefined
      : realColumns.value.reduce((prev, curr) => {
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
                if (
                  [FieldTypeEnum.INPUT_NUMBER, FieldTypeEnum.FORMULA].includes(col.filedType as FieldTypeEnum) &&
                  col.fieldConfig
                ) {
                  return formatNumberValueToString(sum, col.fieldConfig);
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

  watch(
    () => data.value,
    (val) => {
      if (val === null || val === undefined) {
        data.value = [];
      }
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less">
  .crm-sub-table {
    .n-data-table-th {
      padding: 12px 4px;
      .n-data-table-th__title-wrapper {
        height: auto;
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
    }
    .n-data-table-td {
      padding: 8px 4px;
      line-height: normal;
      vertical-align: middle;
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
