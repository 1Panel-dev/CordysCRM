import { OperationTypeEnum } from '@lib/shared/enums/systemEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

import { NavTopConfigItem } from '@/store/modules/app/types';

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
    label: t('customer.moveToSeaOrPool'),
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
  {
    value: OperationTypeEnum.ADD_USER,
    label: t('common.addUser'),
  },
  {
    value: OperationTypeEnum.REMOVE_USER,
    label: t('common.removeUser'),
  },
];

export const defaultNavList: NavTopConfigItem[] = [
  {
    label: t('settings.search'),
    key: 'search',
    iconType: 'iconicon_search-outline_outlined',
    slotName: 'searchSlot',
  },
  {
    label: t('settings.navbar.alerts'),
    key: 'alerts',
    iconType: 'iconicon-alarmclock',
    slotName: 'alertsSlot',
  },
  {
    label: t('settings.help.versionInfo'),
    key: 'versionInfo',
    iconType: 'iconicon_info_circle',
    slotName: 'versionInfoSlot',
  },
];

export default {};
