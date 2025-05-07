import { OperationTypeEnum } from '@lib/shared/enums/systemEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

const { t } = useI18n();

export const logTypeOption = [
  {
    value: OperationTypeEnum.ADD,
    label: t('common.new'),
  },
  {
    value: OperationTypeEnum.UPDATE,
    label: t('common.modify'),
  },
  {
    value: OperationTypeEnum.DELETE,
    label: t('common.delete'),
  },
  {
    value: OperationTypeEnum.IMPORT,
    label: t('common.import'),
  },
  {
    value: OperationTypeEnum.EXPORT,
    label: t('common.export'),
  },
  {
    value: OperationTypeEnum.SYNC,
    label: t('common.sync'),
  },
  {
    value: OperationTypeEnum.MOVE_TO_CUSTOMER_POOL,
    label: t('customer.moveToOpenSea'),
  },
  {
    value: OperationTypeEnum.PICK,
    label: t('common.claim'),
  },
  {
    value: OperationTypeEnum.ASSIGN,
    label: t('common.distribute'),
  },
  {
    value: OperationTypeEnum.CANCEL,
    label: t('common.cancel'),
  },
];

export default {};
