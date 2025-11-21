import { OperationTypeEnum } from '@lib/shared/enums/systemEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

import type { ActionItem } from '@/store/modules/app/types';

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
    value: OperationTypeEnum.MERGE,
    label: 'customer.merge',
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
  {
    value: OperationTypeEnum.APPROVAL,
    label: 'common.approval',
  },
  {
    value: OperationTypeEnum.VOIDED,
    label: 'common.voided',
  },
  {
    value: OperationTypeEnum.CANCEL_VOID,
    label: 'common.cancelVoid',
  },
  {
    value: OperationTypeEnum.ARCHIVE,
    label: 'common.archive',
  },
  {
    value: OperationTypeEnum.UNARCHIVE,
    label: 'common.unarchive',
  },
];

export const defaultNavList: ActionItem[] = [
  {
    label: t('settings.search'),
    key: 'search',
    iconType: 'iconicon_search-outline_outlined',
    slotName: 'searchSlot',
  },
  {
    label: t('menu.agent'),
    key: 'agent',
    iconType: 'iconicon_bot',
    slotName: 'agentSlot',
  },
  {
    label: t('settings.navbar.alerts'),
    key: 'notify',
    iconType: 'iconicon-alarmclock',
    slotName: 'alertsSlot',
  },
  {
    label: t('settings.help.versionInfo'),
    key: 'about',
    iconType: 'iconicon_info_circle',
    slotName: 'versionInfoSlot',
  },
  {
    label: t('settings.language'),
    key: 'language',
    iconType: '',
    slotName: 'languageSlot',
  },
  {
    label: t('settings.navbar.help'),
    key: 'help',
    iconType: 'iconicon_help_circle',
    slotName: 'helpSlot',
  },
  {
    label: t('settings.navbar.event'),
    key: 'event',
    iconType: 'iconicon_data_plan',
    slotName: 'eventSlot',
  },
];
