import { useI18n } from '@/hooks/useI18n';

import { OperatorEnum } from '@lib/shared/enums/commonEnum';

const { t } = useI18n();

export const EQUAL = { value: OperatorEnum.EQ, label: t('common.equal') }; // 等于
export const NOT_EQUAL = { value: OperatorEnum.NE, label: t('common.notEqual') }; // 不等于
export const GT = { value: OperatorEnum.GT, label: t('common.gt') }; // 大于
export const GE = { value: OperatorEnum.GE, label: t('common.ge') }; // 大于等于
export const LT = { value: OperatorEnum.LT, label: t('common.lt') }; // 小于
export const LE = { value: OperatorEnum.LE, label: t('common.le') }; // 小于等于
