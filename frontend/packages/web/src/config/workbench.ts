import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { AnalyticsDataWithValueKey, DefaultAnalyticsData } from '@lib/shared/models/home';

import { AppRouteEnum } from '@/enums/routeEnum';

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

export const defaultClueData: DefaultAnalyticsData = {
  icon: 'iconicon_clue',
  iconColor: 'text-[var(--warning-yellow)]',
  bgColor: 'var(--warning-5)',
  name: t('workbench.clueTotal'),
  total: 0,
  permission: ['CLUE_MANAGEMENT:READ'],
  routeName: AppRouteEnum.CLUE_MANAGEMENT_CLUE,
  analytics: [
    {
      title: t('workbench.addCount'),
      count: 0,
      countValue: 'newClue',
      priorPeriodCompareRate: 0,
      priorPeriodCompareRateAbs: 0,
    },
    {
      title: t('workbench.noFollowUp'),
      count: 0,
      countValue: 'unfollowedClue',
      priorPeriodCompareRate: 0,
      priorPeriodCompareRateAbs: 0,
    },
    {
      title: t('workbench.remainingCapacity'),
      count: 0,
      countValue: 'remainingCapacity',
      priorPeriodCompareRate: 0,
      priorPeriodCompareRateAbs: 0,
    },
  ],
};

export const defaultAccountData: DefaultAnalyticsData = {
  icon: 'iconicon_customer',
  iconColor: 'text-[var(--success-green)]',
  bgColor: 'var(--success-5)',
  name: t('workbench.customerTotal'),
  total: 0,
  permission: ['CUSTOMER_MANAGEMENT:READ'],
  routeName: AppRouteEnum.CUSTOMER_INDEX,
  analytics: [
    {
      title: t('workbench.addCount'),
      count: 0,
      countValue: 'newCustomer',
      priorPeriodCompareRate: 0,
      priorPeriodCompareRateAbs: 0,
    },
    {
      title: t('workbench.noFollowUp'),
      count: 0,
      countValue: 'unfollowedCustomer',
      priorPeriodCompareRate: 0,
      priorPeriodCompareRateAbs: 0,
    },
    {
      title: t('workbench.remainingCapacity'),
      count: 0,
      countValue: 'remainingCapacity',
      priorPeriodCompareRate: 0,
      priorPeriodCompareRateAbs: 0,
    },
  ],
};

export const defaultOpportunityData: DefaultAnalyticsData = {
  icon: 'iconicon_business_opportunity',
  iconColor: 'text-[var(--primary-8)]',
  bgColor: 'var(--primary-6)',
  name: t('workbench.followOpportunityTotal'),
  total: 0,
  permission: ['OPPORTUNITY_MANAGEMENT:READ'],
  routeName: AppRouteEnum.OPPORTUNITY_OPT,
  analytics: [
    {
      title: t('workbench.addCount'),
      count: 0,
      countValue: 'newOpportunity',
      priorPeriodCompareRate: 0,
      priorPeriodCompareRateAbs: 0,
    },
    {
      title: t('workbench.opportunityCombination'),
      count: 0,
      countValue: 'totalAmount',
      priorPeriodCompareRate: 0,
      priorPeriodCompareRateAbs: 0,
    },
  ],
};

export const defaultContactsData: AnalyticsDataWithValueKey = {
  icon: 'iconicon_user_add',
  iconColor: 'text-[var(--info-blue)]',
  bgColor: 'var(--info-5)',
  name: t('workbench.addContacts'),
  total: 0,
  priorPeriodCompareRate: 0,
  priorPeriodCompareRateAbs: 0,
  routeName: AppRouteEnum.CUSTOMER_CONTACT,
  valueKey: 'newContact',
  permission: ['CUSTOMER_MANAGEMENT:READ'],
};

export const defaultFollowRecordData: AnalyticsDataWithValueKey = {
  icon: 'iconicon_data_record',
  iconColor: 'text-[var(--warning-yellow)]',
  bgColor: 'var(--warning-5)',
  name: t('workbench.addFollowRecords'),
  total: 0,
  priorPeriodCompareRate: 0,
  priorPeriodCompareRateAbs: 0,
  valueKey: 'newFollowUpRecord',
  permission: ['CUSTOMER_MANAGEMENT:READ', 'OPPORTUNITY_MANAGEMENT:READ', 'CLUE_MANAGEMENT:READ'],
};

export const defaultFollowPlanData: AnalyticsDataWithValueKey = {
  icon: 'iconicon_data_plan',
  iconColor: 'text-[#9170FD]',
  bgColor: 'rgba(145, 112, 253, 0.05)',
  name: t('workbench.addFollowPlans'),
  total: 0,
  priorPeriodCompareRate: 0,
  priorPeriodCompareRateAbs: 0,
  valueKey: 'newFollowUpPlan',
  permission: ['CUSTOMER_MANAGEMENT:READ', 'OPPORTUNITY_MANAGEMENT:READ', 'CLUE_MANAGEMENT:READ'],
};

export default {};
