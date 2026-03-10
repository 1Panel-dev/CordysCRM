import { IRNodeType } from '@lib/shared/enums/formula';

import { FieldTypeMap, FormulaDataSourceMap, IRNode } from './formula-runtime/types';

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
