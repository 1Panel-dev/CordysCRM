import { useI18n } from '@lib/shared/hooks/useI18n';

import { FormulaErrorCode } from '../../config';
import { FormulaDiagnostic, FormulaFunctionRule } from '../../types';
import { isTextNumberDateOrLogicalNode } from './rule-utils';

const { t } = useI18n();

export const TEXT_RULE: FormulaFunctionRule = {
  name: 'TEXT',

  diagnose({ fnNode, args }) {
    const diagnostics: FormulaDiagnostic[] = [];

    // 1. 参数个数必须为 2
    if (args.length !== 2) {
      diagnostics.push({
        type: 'error',
        code: FormulaErrorCode.ARG_COUNT_ERROR,
        functionName: fnNode.name,
        message: t('formulaEditor.diagnostics.argCountErrorOfDAYS'),
        highlight: {
          tokenRange: [fnNode.startTokenIndex, fnNode.endTokenIndex],
        },
      });
      return diagnostics;
    }

    // 2. 第一个参数类型限制：数字 / 日期 / 逻辑值
    const valueArg = args[0];
    if (!isTextNumberDateOrLogicalNode(valueArg)) {
      diagnostics.push({
        type: 'error',
        code: FormulaErrorCode.INVALID_FUNCTION_CALL,
        functionName: fnNode.name,
        message: t('formulaEditor.diagnostics.invalidFormatArgOfFirstTEXT'),
        highlight: {
          tokenRange: [valueArg.startTokenIndex, valueArg.endTokenIndex],
        },
      });
    }

    // 3. 第二个参数必须是英文双引号包裹的字符串
    const formatArg = args[1];
    const isQuotedString = formatArg.type === 'literal' && formatArg.valueType === 'string';

    if (!isQuotedString) {
      diagnostics.push({
        type: 'error',
        code: FormulaErrorCode.SYNTAX_ERROR,
        functionName: fnNode.name,
        message: t('formulaEditor.diagnostics.invalidFormatArgOfSecondTEXT'),
        highlight: {
          tokenRange: [formatArg.startTokenIndex, formatArg.endTokenIndex],
        },
      });
    }

    return diagnostics;
  },
};

export default TEXT_RULE;
