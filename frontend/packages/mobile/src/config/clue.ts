import { ClueStatusEnum } from '@lib/shared/enums/clueEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

const { t } = useI18n();

export const clueBaseSteps = [
  {
    value: ClueStatusEnum.NEW,
    label: t('common.create'),
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
export default {};
