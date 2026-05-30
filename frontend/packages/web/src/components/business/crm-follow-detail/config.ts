import { TabPaneProps } from 'naive-ui';

import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

import type { Description } from '@/components/pure/crm-detail-card/index.vue';

interface DescriptionItem extends Description {
  formConfigField?: string;
}

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

export const descriptionList: DescriptionItem[] = [
  {
    key: 'contactName',
    label: t('common.contact'),
    value: 'contactName',
  },
  {
    key: 'phone',
    label: t('common.phoneNumber'),
    value: 'phone',
  },
  {
    key: 'ownerName',
    label: '',
    value: 'ownerName',
    formConfigField: 'owner',
  },
  {
    key: 'createTime',
    label: t('common.createTime'),
    value: 'createTime',
  },
  {
    key: 'createUserName',
    label: t('common.creator'),
    value: 'createUserName',
  },
  {
    key: 'updateTime',
    label: t('common.updateTime'),
    value: 'updateTime',
  },
  {
    key: 'updateUserName',
    label: t('common.updateUserName'),
    value: 'updateUserName',
  },
];

export default {};
