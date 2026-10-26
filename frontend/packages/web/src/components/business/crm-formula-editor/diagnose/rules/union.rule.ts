import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

import { FormulaErrorCode } from '../../config';
import { FormulaDiagnostic, FormulaFunctionRule } from '../../types';

const { t } = useI18n();

const UNION_RULE: FormulaFunctionRule = {
  name: 'UNION',

  diagnose({ fnNode, args }) {
    const diagnostics: FormulaDiagnostic[] = [];
    const textFieldTypes = [
      FieldTypeEnum.INPUT,
      FieldTypeEnum.TEXTAREA,
      FieldTypeEnum.SELECT,
      FieldTypeEnum.SELECT_MULTIPLE,
      FieldTypeEnum.INPUT_MULTIPLE,
      FieldTypeEnum.DATA_SOURCE,
      FieldTypeEnum.DATA_SOURCE_MULTIPLE,
      FieldTypeEnum.SERIAL_NUMBER,
      FieldTypeEnum.LINK,
      FieldTypeEnum.FORMULA,
    ];
    const isAllowedArgument = (argument: (typeof args)[number]) => {
      if (argument.type === 'literal') return ['string', 'number'].includes(argument.valueType);
      if (argument.type !== 'field') return false;

      return (
        [FieldTypeEnum.INPUT_NUMBER, ...textFieldTypes].includes(argument.fieldType as FieldTypeEnum) &&
        Boolean(argument.fieldId)
      );
    };

    if (args.length === 0 || !args.every(isAllowedArgument)) {
      diagnostics.push({
        type: 'error',
        code: FormulaErrorCode.INVALID_FUNCTION_CALL,
        functionName: fnNode.name,
        message: t('formulaEditor.diagnostics.unionRequiresSubtableField'),
        highlight: {
          tokenRange: [fnNode.startTokenIndex, fnNode.endTokenIndex],
        },
      });
    }

    return diagnostics;
  },
};

export default UNION_RULE;
