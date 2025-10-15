import { TabPaneProps } from 'naive-ui';

import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

const { t } = useI18n();

// 跟进计划状态
export const statusTabList = ref<TabPaneProps[]>([
  {
    name: CustomerFollowPlanStatusEnum.ALL,
    tab: t('common.all'),
  },
  {
    name: CustomerFollowPlanStatusEnum.PREPARED,
    tab: t('common.notStarted'),
  },
  {
    name: CustomerFollowPlanStatusEnum.UNDERWAY,
    tab: t('common.inProgress'),
  },
  {
    name: CustomerFollowPlanStatusEnum.COMPLETED,
    tab: t('common.completed'),
  },
  {
    name: CustomerFollowPlanStatusEnum.CANCELLED,
    tab: t('common.canceled'),
  },
]);

export default {};
