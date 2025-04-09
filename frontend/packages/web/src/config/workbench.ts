import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

const { t } = useI18n();

export const quickAccessList = [
  {
    key: FormDesignKeyEnum.CUSTOMER,
    icon: 'newCustomer',
    label: t('customer.new'),
    permission: ['CUSTOMER_MANAGEMENT:ADD'],
  },
  {
    key: FormDesignKeyEnum.CONTACT,
    icon: 'newContact',
    label: t('customManagement.newContact'),
    permission: ['CUSTOMER_MANAGEMENT_CONTACT:ADD'],
  },
  {
    key: FormDesignKeyEnum.CLUE,
    icon: 'newClue',
    label: t('clueManagement.newClue'),
    permission: ['CLUE_MANAGEMENT:ADD'],
  },
  {
    key: FormDesignKeyEnum.BUSINESS,
    icon: 'newOpportunity',
    label: t('opportunity.new'),
    permission: ['OPPORTUNITY_MANAGEMENT:ADD'],
  },
  // 这版本先不上
  // {
  //   key: FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS,
  //   icon: 'newRecord',
  //   label: t('workbench.createFollowUpRecord'),
  //   permission: [],
  // },
  // {
  //   key: FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS,
  //   icon: 'newPlan',
  //   label: t('workbench.createFollowUpPlan'),
  //   permission: [],
  // },
];

export default {};
