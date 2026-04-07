import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';

import { FormCreateField } from '@/components/business/crm-form-create/types';
import { safeParseFormula } from '@/components/business/crm-formula-editor/utils';

import { flatAllFields, getFormulaDataSourceDisplayValue, hydrateIRNumberType, keepDecimal } from '../../utils';
import registerBuiltinFunctions from '../functions';
import { FieldMeta, FieldTypeMap, FormulaExecutorContext, FormulaExecutorResult, ValueType } from '../types';
import evaluateIR from './evaluator';

registerBuiltinFunctions();

export function getValueType(field: FormCreateField): ValueType {
  switch (field.type) {
    case FieldTypeEnum.INPUT_NUMBER:
      return 'number';
    case FieldTypeEnum.DATE_TIME:
      return 'date';
    case FieldTypeEnum.INPUT:
    case FieldTypeEnum.DATA_SOURCE:
    case FieldTypeEnum.DATA_SOURCE_MULTIPLE:
    case FieldTypeEnum.SERIAL_NUMBER:
    case FieldTypeEnum.SELECT:
      return 'string';
    case FieldTypeEnum.RADIO:
    case FieldTypeEnum.CHECKBOX:
      return 'boolean';
    default:
      return 'unknown';
  }
}

function buildFieldTypeInfo(field: FormCreateField): FieldMeta {
  const baseInfo = {
    valueType: getValueType(field),
    fieldType: field.type,
    name: field.name,
    resourceFieldId: field.resourceFieldId,
  };

  const typeSpecificInfo: Record<string, any> = {
    [FieldTypeEnum.INPUT_NUMBER]: {
      numberType: field.numberFormat === 'percent' ? 'percent' : 'number',
    },
    [FieldTypeEnum.SERIAL_NUMBER]: {
      defaultValueType: field.prefixType,
    },
    [FieldTypeEnum.INPUT]: {
      defaultValueType: field.defaultValueType,
    },
  };

  return {
    ...baseInfo,
    ...(typeSpecificInfo[field.type] || {}),
  };
}

export function buildFieldTypeMap(fields: FormCreateField[]): FieldTypeMap {
  const map: FieldTypeMap = {};

  flatAllFields(fields).forEach((field) => {
    const fieldInfo = buildFieldTypeInfo(field);

    // 注册完整路径的字段
    map[field.id] = fieldInfo;

    // 如果字段 ID 包含子表路径
    if (field.id.includes('.')) {
      field.id.split('.').forEach((fieldId) => {
        map[fieldId] = fieldInfo;
      });
    }
  });

  return map;
}

export function getFormulaRuntimeContext(path?: string) {
  const contextMatch = path?.match(/^([^[]+)\[(\d+)\]\./);

  if (!contextMatch) return undefined;

  return {
    tableKey: contextMatch[1],
    rowIndex: Number(contextMatch[2]),
  };
}

export function normalizeFormulaResult(
  result: any,
  options?: {
    decimalPlaces?: number;
    expectedType?: ValueType;
  }
): any {
  const decimalPlaces = options?.decimalPlaces ?? 2;
  const expectedType = options?.expectedType;

  if (result == null) {
    return '';
  }

  if (expectedType === 'string') {
    return String(result);
  }

  if (expectedType === 'number') {
    const num = Number(result);
    if (Number.isNaN(num)) return 0;
    return keepDecimal(num, decimalPlaces);
  }

  switch (typeof result) {
    case 'number':
      return keepDecimal(result, decimalPlaces);
    case 'string':
      return result;
    case 'boolean':
      return result ? 'TRUE' : 'FALSE';
    default:
      return String(result);
  }
}

function cloneIRIfNeeded<T>(value: T, clone = true): T {
  if (!clone) return value;

  if (typeof structuredClone === 'function') {
    return structuredClone(value);
  }

  return JSON.parse(JSON.stringify(value));
}

/**
 * 表单场景统一执行公式
 */
export function executeFormFormula(ctx: FormulaExecutorContext): FormulaExecutorResult | undefined {
  const {
    formula,
    path,
    formDetail,
    fields = [],
    formulaDataSource,
    evaluationNow,
    decimalPlaces = 2,
    expectedType,
    cloneIR = true,
    warn,
  } = ctx;

  const { ir } = safeParseFormula(formula ?? '');

  if (!ir) {
    return;
  }

  const runtimeIR = cloneIRIfNeeded(ir, cloneIR);
  const fieldTypeMap = buildFieldTypeMap(fields);
  hydrateIRNumberType(runtimeIR, fieldTypeMap);

  const runtimeContext = getFormulaRuntimeContext(path);

  const getScalarFieldValue = (
    fieldId: string,
    context?: {
      tableKey?: string;
      rowIndex?: number;
    }
  ) => {
    if (context?.tableKey && context.rowIndex != null) {
      const row = formDetail?.[context.tableKey]?.[context.rowIndex];
      return row?.[fieldId];
    }

    return formDetail?.[fieldId];
  };

  const getTableColumnValues = (fieldPath: string): any[] => {
    const [tableKey, fieldKey] = fieldPath.split('.');
    const rows = formDetail?.[tableKey];
    if (!Array.isArray(rows)) return [];
    return rows.map((row) => row?.[fieldKey]);
  };

  const resolveFieldRuntimeValue = (fieldId: string, rawValue: any) => {
    return getFormulaDataSourceDisplayValue(formulaDataSource, fieldId, rawValue);
  };

  const rawResult = evaluateIR(runtimeIR, {
    evaluationNow,
    context: runtimeContext,
    getScalarFieldValue,
    getTableColumnValues,
    getFieldMeta: (fieldId: string) => {
      return fieldTypeMap[fieldId];
    },
    resolveFieldRuntimeValue,
    warn: (msg: string) => {
      warn?.(msg);
    },
  } as any);

  const normalizedResult = normalizeFormulaResult(rawResult, {
    decimalPlaces,
    expectedType,
  });

  return {
    rawResult,
    normalizedResult,
  };
}

/**
 * 取单值字段
 */
export function getScalarFieldValueByFormDetail(
  formDetail: Record<string, any> | undefined,
  fieldId: string,
  context?: {
    tableKey?: string;
    rowIndex?: number;
  }
) {
  // 子表当前行上下文
  if (context?.tableKey && context.rowIndex != null) {
    const row = formDetail?.[context.tableKey]?.[context.rowIndex];
    return row?.[fieldId];
  }

  // 主表字段
  return formDetail?.[fieldId];
}
