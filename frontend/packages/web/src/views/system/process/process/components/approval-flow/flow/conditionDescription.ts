import dayjs from 'dayjs';

import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

import { operatorOptionsMap } from '@/components/pure/crm-advance-filter';
import type { FilterForm, FilterFormItem } from '@/components/pure/crm-advance-filter/type';
import { timeOptions, unitOptions } from '@/components/business/crm-time-range-picker/config';

const { t } = useI18n();

export interface ResolveConditionDescriptionOptions {
  optionMap?: Record<string, any[]>;
  fieldConfigMap?: Record<string, FilterFormItem>;
}

export function hasConfiguredCondition(conditionConfig?: FilterForm) {
  return Boolean(
    conditionConfig?.conditions?.some((item) => item.name) || conditionConfig?.list?.some((item) => item.dataIndex)
  );
}

function getConditionOperatorLabel(item: FilterFormItem) {
  const options = item.operatorOption?.length ? item.operatorOption : operatorOptionsMap[item.type] ?? [];
  const operatorLabel = options.find((option) => option.value === item.operator)?.label;
  return operatorLabel ? t(operatorLabel) : item.operator;
}

// 处理时间值显示
function getConditionTimeRangeLabel(item: FilterFormItem) {
  if (![FieldTypeEnum.DATE_TIME, FieldTypeEnum.TIME_RANGE_PICKER].includes(item.type)) {
    return '';
  }

  const values = String(item.value ?? '')
    .split(',')
    .filter(Boolean);
  if (item.operator === OperatorEnum.DYNAMICS) {
    const [type, count, unit] = values;
    const timeLabel = timeOptions.find((option) => option.value === type)?.label ?? type;
    if (type !== 'CUSTOM') {
      return timeLabel;
    }

    const unitLabel = unitOptions.find((option) => option.value === unit)?.label ?? unit;
    return [timeLabel, count, unitLabel].filter(Boolean).join(' ');
  }

  return values.map((value) => dayjs(Number(value)).format('YYYY-MM-DD HH:mm:ss')).join('、');
}

function getConditionValueLabel(item: FilterFormItem, options?: ResolveConditionDescriptionOptions) {
  if (
    item.operator === OperatorEnum.EMPTY ||
    item.operator === OperatorEnum.NOT_EMPTY ||
    item.operator === OperatorEnum.NEW_NOT_EQUALS_OLD
  ) {
    return '';
  }

  if (item.selectedRows?.length) {
    return item.selectedRows
      .map((option) => option.name)
      .filter(Boolean)
      .join('、');
  }

  if (item.selectedUserList?.length) {
    return item.selectedUserList
      .map((option) => option.name)
      .filter(Boolean)
      .join('、');
  }

  const timeRangeLabel = getConditionTimeRangeLabel(item);
  if (timeRangeLabel) {
    return timeRangeLabel;
  }

  const values = Array.isArray(item.value) ? item.value : [item.value];
  return values
    .filter((value) => value !== null && value !== undefined && value !== '')
    .map((value) => {
      // 条件里只保存 value，卡片描述需要从字段自身 options 和后端 optionMap 里反查显示名。
      const optionList = [
        ...(item.selectProps?.options ?? []),
        ...(item.treeSelectProps?.options ?? []),
        ...(item.cascaderProps?.options ?? []),
        ...(item.dataIndex ? options?.optionMap?.[item.dataIndex] ?? [] : []),
      ];
      const option = optionList.find((o: Record<string, any>) => [o.id, o.value].includes(value as string));
      return option ? option.label ?? option.name : value;
    })
    .join('、');
}

function mergeConditionFieldConfig(item: FilterFormItem, options?: ResolveConditionDescriptionOptions) {
  if (!item.dataIndex) {
    return item;
  }

  // 后端 conditions 只有字段 key、操作符和值；生成描述前先补回字段标题、类型和操作符配置。
  const fieldConfig = options?.fieldConfigMap?.[item.dataIndex];
  return fieldConfig
    ? ({
        ...fieldConfig,
        ...item,
        title: item.title || fieldConfig.title,
        type: item.type || fieldConfig.type,
      } as FilterFormItem)
    : item;
}

// 处理每一条条件
function resolveConditionItemDescription(rawItem: FilterFormItem, options?: ResolveConditionDescriptionOptions) {
  const item = mergeConditionFieldConfig(rawItem, options);
  if (!item.dataIndex || !item.operator) {
    return '';
  }

  const operatorLabel = getConditionOperatorLabel(item);
  const valueLabel = getConditionValueLabel(item, options);
  return [item.title || item.dataIndex, operatorLabel, valueLabel].filter(Boolean).join(' ');
}

// 抽屉编辑态使用 list，后端保存态使用 conditions，这里统一成 FilterContent 行结构
function getConditionDescriptionList(conditionConfig?: FilterForm): FilterFormItem[] {
  return conditionConfig?.list?.length
    ? conditionConfig.list
    : conditionConfig?.conditions?.map((item) => ({
        ...item,
        dataIndex: item.name ?? null,
        type: item.type ?? FieldTypeEnum.INPUT,
      })) ?? [];
}

// 条件卡片的描述拼接
export function resolveConditionDescription(
  conditionConfig?: FilterForm,
  options?: ResolveConditionDescriptionOptions
) {
  if (!hasConfiguredCondition(conditionConfig)) {
    return t('process.process.flow.conditionUnset');
  }

  const descriptions = getConditionDescriptionList(conditionConfig)
    .map((item) => resolveConditionItemDescription(item, options))
    .filter(Boolean);
  if (!descriptions.length) {
    return t('process.process.flow.conditionUnset');
  }

  const connector =
    conditionConfig?.searchMode === 'OR'
      ? t('process.process.flow.conditionConnector.or')
      : t('process.process.flow.conditionConnector.and');
  return descriptions.join(` ${connector} `);
}
