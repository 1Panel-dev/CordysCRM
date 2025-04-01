import { ClueStatusEnum } from '@lib/shared/enums/clueEnum';
import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';

import { FilterFormItem } from '@/components/pure/crm-advance-filter/type';

import { useI18n } from '@/hooks/useI18n';

const { t } = useI18n();

export const filterConfigList: FilterFormItem[] = [
  {
    title: t('common.createTime'),
    dataIndex: 'createTime',
    type: FieldTypeEnum.DATE_TIME,
  },
  {
    title: t('common.updateUserName'),
    dataIndex: 'updateUser',
    type: FieldTypeEnum.USER_SELECT,
  },
  {
    title: t('common.updateTime'),
    dataIndex: 'updateTime',
    type: FieldTypeEnum.DATE_TIME,
  },
];

export const clueBaseSteps = [
  {
    value: ClueStatusEnum.NEW,
    label: t('common.newCreate'),
  },
  {
    value: ClueStatusEnum.FOLLOWING,
    label: t('clue.followingUp'),
  },
  {
    value: ClueStatusEnum.INTERESTED,
    label: t('clue.interested'),
  },
];
