import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

import { useI18n } from '@/hooks/useI18n';

const { t } = useI18n();

export const quickAccessList = [
  {
    key: FormDesignKeyEnum.CUSTOMER,
    icon: 'newCustomer',
    label: t('customer.new'),
  },
  {
    key: FormDesignKeyEnum.CONTACT,
    icon: 'newContact',
    label: t('customManagement.newContact'),
  },
  {
    key: FormDesignKeyEnum.CLUE,
    icon: 'newClue',
    label: t('clueManagement.newClue'),
  },
  {
    key: FormDesignKeyEnum.BUSINESS,
    icon: 'newOpportunity',
    label: t('opportunity.new'),
  },
  {
    key: FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS,
    icon: 'newRecord',
    label: t('workbench.createFollowUpRecord'),
  },
  {
    key: FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS,
    icon: 'newPlan',
    label: t('workbench.createFollowUpPlan'),
  },
];

export default {};
