import { OperationTypeEnum } from '@lib/shared/enums/systemEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

import type { NavTopConfigItem } from '@/store/modules/app/types';

const { t } = useI18n();

export const logTypeOption = [
  {
    value: OperationTypeEnum.ADD,
    label: 'common.new',
  },
  {
    value: OperationTypeEnum.UPDATE,
    label: 'common.modify',
  },
  {
    value: OperationTypeEnum.DELETE,
    label: 'common.delete',
  },
  {
    value: OperationTypeEnum.IMPORT,
    label: 'common.import',
  },
  {
    value: OperationTypeEnum.EXPORT,
    label: 'common.export',
  },
  {
    value: OperationTypeEnum.SYNC,
    label: 'common.sync',
  },
  {
    value: OperationTypeEnum.MOVE_TO_CUSTOMER_POOL,
    label: 'customer.moveToSeaOrPool',
  },
  {
    value: OperationTypeEnum.PICK,
    label: 'common.claim',
  },
  {
    value: OperationTypeEnum.ASSIGN,
    label: 'common.distribute',
  },
  {
    value: OperationTypeEnum.CANCEL,
    label: 'common.cancel',
  },
  {
    value: OperationTypeEnum.ADD_USER,
    label: 'common.addUser',
  },
  {
    value: OperationTypeEnum.REMOVE_USER,
    label: 'common.removeUser',
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
  {
    label: t('settings.language'),
    key: 'language',
    iconType: '',
    slotName: 'languageSlot',
  },
];
