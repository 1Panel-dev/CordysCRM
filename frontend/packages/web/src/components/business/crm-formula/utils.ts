import { IRNodeType } from '@lib/shared/enums/formula';

import { resolveFieldId } from '../crm-formula-editor/utils';
import { FieldTypeMap, FormulaDataSourceMap, IRNode, ValueType } from './formula-runtime/types';
import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

export function hydrateIRNumberType(node: IRNode | any, fieldTypeMap: FieldTypeMap): IRNode {
  // ---------- 历史数据兼容 as 旧数据，需要转换成新版本----------
  if (node.type === 'number') {
    return {
      type: IRNodeType.Literal,
      value: node.value,
      valueType: 'number',
    };
  }

  if (node.type === 'string') {
    return {
      type: IRNodeType.Literal,
      value: node.value,
      valueType: 'string',
    };
  }

  if (node.type === 'boolean') {
    return {
      type: IRNodeType.Literal,
      value: node.value,
      valueType: 'boolean',
    };
  }

  switch (node.type) {
    case IRNodeType.Literal:
      return node;

    case IRNodeType.Field: {
      const fieldType = fieldTypeMap[node.fieldId];

      if (fieldType) {
        node.numberType = fieldType;
      }

      return node;
    }

    case IRNodeType.Binary: {
      node.left = hydrateIRNumberType(node.left, fieldTypeMap);
      node.right = hydrateIRNumberType(node.right, fieldTypeMap);
      return node;
    }

    case IRNodeType.Compare: {
      node.left = hydrateIRNumberType(node.left, fieldTypeMap);
      node.right = hydrateIRNumberType(node.right, fieldTypeMap);
      return node;
    }

    case IRNodeType.Function: {
      node.args = node.args.map((arg: IRNode) => hydrateIRNumberType(arg, fieldTypeMap));
      return node;
    }

    case IRNodeType.Invalid:
      return node;

    default: {
      const _exhaustiveCheck: any = node;
      return _exhaustiveCheck;
    }
  }
}

export function getFormulaDataSourceDisplayValue(
  formulaDataSource: FormulaDataSourceMap,
  fieldId: string,
  rawValue: any
): any {
  const config = formulaDataSource[fieldId];

  // 不是数据源映射字段，保持原值
  if (!config?.parserName) {
    return rawValue;
  }

  // 空值
  if (rawValue == null || rawValue === '') {
    return [];
  }

  const options = config.options ?? [];

  const values = Array.isArray(rawValue) ? rawValue : [rawValue];

  const result = values.map((value) => {
    const target = String(value);

    const matched = options.find((item) => {
      const candidate = item.value ?? item.id;
      return String(candidate) === target;
    });

    return matched?.name ?? matched?.label ?? value;
  });
  return result;
}

export function flatAllFields(
  fields: FormCreateField[],
  options?: {
    isSubTableRender?: boolean;
  }
) {
  const result: (FormCreateField & {
    parentId?: string;
    parentName?: string;
    inSubTable?: boolean;
  })[] = [];

  fields?.forEach((field) => {
    if (field.subFields) {
      field.subFields.forEach((sub) => {
        result.push({
          ...sub,
          name: options?.isSubTableRender ? sub.name : `${field.name}.${sub.name}`,
          id: options?.isSubTableRender ? resolveFieldId(sub, true) : `${field.id}.${resolveFieldId(sub, true)}`,
          parentId: field.id,
          parentName: field.name,
          inSubTable: true,
        });
      });
    } else {
      result.push({
        ...field,
        inSubTable: false,
      });
    }
  });

  return result;
}

export function normalizeFormulaNumber(n: number): number {
  if (!Number.isFinite(n)) return n;
  return Number.parseFloat(n.toPrecision(15));
}

/**
 * 先消除浮点尾差，再按指定位数截断，不做四舍五入
 */
export function keepDecimal(value: number, digits = 2) {
  if (!Number.isFinite(value)) return value;
  const normalized = normalizeFormulaNumber(value);
  const str = normalized.toString();
  const dotIdx = str.indexOf('.');
  if (dotIdx === -1) return normalized;
  return Number(str.substring(0, dotIdx + 1 + digits));
}

/**
 * 获取公式执行结果的归一化配置。
 * 数值模式只约束数字结果的精度，文本结果保持原始字符串；
 * 文本模式统一按字符串结果处理，兼容历史空值配置。
 */
export function getFormulaResultFormatOptions(fieldConfig: FormCreateField): {
  decimalPlaces?: number;
  expectedType?: ValueType;
} {
  if (fieldConfig.formulaResultFormat === 'number') {
    return {
      decimalPlaces: fieldConfig.decimalPlaces ? fieldConfig.precision ?? 0 : 0,
    };
  }

  return {
    expectedType: 'string',
  };
}

/**
 * 按公式结果类型格式化展示值。
 * 数值模式支持小数位和千分位，文本结果不强制转换为数字；
 * emptyText 用于列表、详情等展示场景的空值占位。
 */
export function formatFormulaResultValue(result: any, fieldConfig: FormCreateField, emptyText = '') {
  if (result === undefined || result === null || result === '') {
    return emptyText;
  }

  if (fieldConfig.formulaResultFormat !== 'number') {
    return String(result);
  }

  if (typeof result === 'string') {
    const plainNumberPattern = /^-?(?:\d+|\d*\.\d+)$/;
    const thousandsNumberPattern = /^-?\d{1,3}(?:,\d{3})+(?:\.\d+)?$/;
    if (!plainNumberPattern.test(result) && !thousandsNumberPattern.test(result)) {
      return result;
    }
  }

  const num = Number(typeof result === 'string' ? result.replace(/,/g, '') : result);
  if (Number.isNaN(num)) return String(result);

  const precision = fieldConfig.decimalPlaces ? fieldConfig.precision ?? 0 : 0;
  if (fieldConfig.showThousandsSeparator) {
    if (precision > 0) {
      const [integerPart, decimalPart] = num.toFixed(precision).split('.');
      return `${integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}.${decimalPart}`;
    }
    return num.toLocaleString('en-US');
  }

  return precision > 0 ? num.toFixed(precision) : num.toString();
}
