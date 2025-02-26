import { OperatorEnum } from '@lib/shared/enums/commonEnum';

import { useI18n } from '@/hooks/useI18n';

const { t } = useI18n();

export const EQUAL = { value: OperatorEnum.EQUALS, label: t('common.equal') }; // 等于
export const NOT_EQUAL = { value: OperatorEnum.NOT_EQUALS, label: t('common.notEqual') }; // 不等于
export const GT = { value: OperatorEnum.GT, label: t('common.gt') }; // 大于
export const GE = { value: OperatorEnum.GE, label: t('common.ge') }; // 大于等于
export const LT = { value: OperatorEnum.LT, label: t('common.lt') }; // 小于
export const LE = { value: OperatorEnum.LE, label: t('common.le') }; // 小于等于
