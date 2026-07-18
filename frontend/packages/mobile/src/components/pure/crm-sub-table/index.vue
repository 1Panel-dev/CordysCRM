<template>
  <div class="sub-table-wrapper">
    <table class="sub-table">
      <thead>
        <tr>
          <th v-for="h in realColumns" class="p-[12px_4px]">
            <div class="flex h-full gap-[4px] text-nowrap font-medium text-[var(--text-n4)]">
              {{ h.title }}
              <CrmIcon
                v-if="h.fieldConfig?.resourceFieldId"
                name="iconicon_correlation"
                class="text-[var(--text-n4)]"
              />
            </div>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, rowIndex) in data">
          <td>{{ rowIndex + 1 }}</td>
          <td v-for="col in realColumns.filter((e) => e.key !== '_table_order')">
            {{ col.fieldConfig ? initFieldValueText(col.fieldConfig, col.key, row[col.key]) : '-' }}
          </td>
        </tr>
        <tr v-if="props.sumColumns?.length">
          <td v-for="col in realColumns">
            <div v-if="col.key === '_table_order'">{{ t('common.sum') }}</div>
            <div v-else>{{ summary[col.key] }}</div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
<script setup lang="ts">
  import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { formatNumberValueToString, normalizeNumber } from '@lib/shared/method/formCreate';
  import { formatTimeValue, getCityPath, getIndustryPath } from '@lib/shared/method';

  const props = defineProps<{
    subFields: FormCreateField[];
    sumColumns?: string[];
    optionMap?: Record<string, any[]>;
    data: Record<string, any>[];
  }>();

  const { t } = useI18n();

  interface TableColumn {
    title: string;
    key: string;
    width: number | string;
    fieldConfig?: FormCreateField;
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
    if (
      [
        FieldTypeEnum.DATA_SOURCE,
        FieldTypeEnum.MEMBER,
        FieldTypeEnum.MEMBER_MULTIPLE,
        FieldTypeEnum.DEPARTMENT,
        FieldTypeEnum.DEPARTMENT_MULTIPLE,
      ].includes(field.type)
    ) {
      // 选项字段找不到 optionMap 对应值，则显示不存在
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
      case FieldTypeEnum.INPUT_MULTIPLE:
        return Array.isArray(value) ? value.join(', ') || '-' : value || '-';
      case FieldTypeEnum.INDUSTRY:
        return value ? getIndustryPath(value) : '-';
      default:
        return value || '-';
    }
  }

  const pictureFields = computed<FormCreateField[]>(() => {
    return props.subFields.filter((field) => field.type === FieldTypeEnum.PICTURE);
  });
  const maxPictureCountMap = computed<Record<string, number>>(() => {
    return (
      props.data.reduce((prev, curr) => {
        pictureFields.value.forEach((field) => {
          const key = field.businessKey || field.id;
          const currCount = Array.isArray(curr[key]) ? curr[key].length : 0;
          if (!prev[key] || currCount > prev[key]) {
            prev[key] = currCount;
          }
        });
        return prev;
      }, {} as Record<string, number>) || {}
    );
  });

  const renderColumns = computed<TableColumn[]>(() => {
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
              : `${(field.showLabel ? field.name : '').length * 16}px`,
          key,
          fieldConfig: field,
        };
      });
  });

  const realColumns = computed(() => {
    const cols: TableColumn[] = [
      {
        title: '',
        width: '30px',
        key: '_table_order',
      },
      ...renderColumns.value,
    ];
    return cols;
  });

  const summary = computed(() => {
    const summaryRes: Record<string, any> = {
      _table_order: {
        value: h('div', { class: 'flex items-center justify-center' }, t('crmFormDesign.sum')),
      },
    };
    renderColumns.value.forEach((col) => {
      if (props.sumColumns?.includes(col.key)) {
        const sum =
          props.data.reduce((prev, row) => {
            const rowVal = normalizeNumber(row[col.key]);
            return prev + Math.round(rowVal * 100);
          }, 0) / 100;
        if ([FieldTypeEnum.INPUT_NUMBER, FieldTypeEnum.FORMULA].includes(col.fieldConfig?.type!) && col.fieldConfig) {
          summaryRes[col.key] = formatNumberValueToString(sum, col.fieldConfig);
        }
        summaryRes[col.key] = sum;
      }
    });
    return summaryRes;
  });
</script>
<style lang="less" scoped>
  .sub-table-wrapper {
    overflow: auto;
    width: calc(100vw - 72px);
    max-height: 240px;
    border: 1px solid var(--text-n8);
    border-radius: 6px;
  }
  .sub-table {
    width: max-content;
    min-width: 100%;
    border-collapse: collapse;
    th,
    td {
      padding: 12px 4px;
      border-right: 0;
      border-bottom: 1px solid var(--text-n8);
      border-left: 0;
      text-align: left;
      white-space: nowrap;
    }
    th {
      position: sticky;
      top: 0;
      z-index: 1;
      background-color: var(--text-n10);
    }
    thead th {
      border-top: 0;
    }
    tbody tr:last-child td {
      border-bottom: 0;
    }
  }
</style>
