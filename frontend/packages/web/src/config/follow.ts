import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
import type { StatusTagKey } from '@lib/shared/models/customer';

export const statusMap: Record<
  StatusTagKey,
  {
    label: string;
    value: CustomerFollowPlanStatusEnum;
    type: 'default' | 'primary' | 'info' | 'success' | 'warning' | 'error';
    icon: string;
    color: string;
  }
> = {
  [CustomerFollowPlanStatusEnum.PREPARED]: {
    label: 'common.notStarted',
    value: CustomerFollowPlanStatusEnum.PREPARED,
    type: 'default',
    icon: 'iconicon_block_filled',
    color: 'var(--text-n4)',
  },
  [CustomerFollowPlanStatusEnum.UNDERWAY]: {
    label: 'common.inProgress',
    value: CustomerFollowPlanStatusEnum.UNDERWAY,
    type: 'info',
    icon: 'iconicon_testing',
    color: 'var(--info-blue)',
  },
  [CustomerFollowPlanStatusEnum.COMPLETED]: {
    label: 'common.completed',
    value: CustomerFollowPlanStatusEnum.COMPLETED,
    type: 'success',
    icon: 'iconicon_succeed_filled',
    color: 'var(--success-green)',
  },
  [CustomerFollowPlanStatusEnum.CANCELLED]: {
    label: 'common.canceled',
    value: CustomerFollowPlanStatusEnum.CANCELLED,
    type: 'default',
    icon: 'iconicon_block_filled',
    color: 'var(--text-n6)',
  },
};

export default {};
