import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

import { FormulaErrorCode } from '../config';
import { ASTNode, BinaryNode, FormulaDiagnostic } from '../types';

const { t } = useI18n();

function isNumericLikeNode(node: ASTNode): boolean {
  if (!node) return false;

  if (node.type === 'literal') {
    return node.valueType === 'number';
  }

  if (node.type === 'field') {
    return [FieldTypeEnum.INPUT_NUMBER, FieldTypeEnum.DATE_TIME, FieldTypeEnum.FORMULA].includes(node.fieldType as any);
  }

  if (node.type === 'binary') {
    // 二元表达式先认为是数值表达式，具体内部是否合法由递归 walk 时继续报
    return ['+', '-', '*', '/'].includes(node.operator);
  }

  if (node.type === 'function') {
    const name = node.name?.toUpperCase();

    // 当前这些函数运行结果可参与数值运算
    return ['SUM', 'DAYS', 'NOW', 'TODAY'].includes(name);
  }

  return false;
}

function getOperatorText(operator: string) {
  switch (operator) {
    case '-':
      return t('formulaEditor.diagnostics.minusOperator');
    case '*':
      return t('formulaEditor.diagnostics.multiplyOperator');
    case '/':
      return t('formulaEditor.diagnostics.divideOperator');
    default:
      return operator;
  }
}

export default function diagnoseBinaryOperandType(node: ASTNode): FormulaDiagnostic[] {
  const diagnostics: FormulaDiagnostic[] = [];

  if (node.type !== 'binary') {
    return diagnostics;
  }

  const binNode = node as BinaryNode;

  // - * / 左右等号运算类型限制
  if (!['-', '*', '/'].includes(binNode.operator)) {
    return diagnostics;
  }
  // 左 operand 必须是数值表达式
  if (!isNumericLikeNode(binNode.left)) {
    diagnostics.push({
      type: 'error',
      code: FormulaErrorCode.INVALID_FUNCTION_CALL,
      message: t('formulaEditor.diagnostics.invalidLeftOperandTypeOfBinary', {
        operator: getOperatorText(binNode.operator),
      }),
      highlight: {
        tokenRange: [binNode.left.startTokenIndex, binNode.left.endTokenIndex],
      },
    });
  }
  // 右 operand 必须是数值表达式
  if (!isNumericLikeNode(binNode.right)) {
    diagnostics.push({
      type: 'error',
      code: FormulaErrorCode.INVALID_FUNCTION_CALL,
      message: t('formulaEditor.diagnostics.invalidRightOperandTypeOfBinary', {
        operator: getOperatorText(binNode.operator),
      }),
      highlight: {
        tokenRange: [binNode.right.startTokenIndex, binNode.right.endTokenIndex],
      },
    });
  }

  return diagnostics;
}
