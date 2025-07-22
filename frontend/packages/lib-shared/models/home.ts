export interface GetHomeStatisticParams {
  searchType: string;
  deptIds: string[];
  period?: string;
  startTime?: number;
  endTime?: number;
}

export interface PriorPeriodCompareRateItem {
  value: number;
  priorPeriodCompareRate: number | string; // 较上期对比率
}

// 跟进商机统计
export interface FollowOptStatisticDetail {
  total: number;
  newOpportunity: PriorPeriodCompareRateItem;
  totalAmount: number;
}

// 客户统计
export interface AccountStatisticDetail {
  total: number;
  newCustomer: PriorPeriodCompareRateItem;
  unfollowedCustomer: PriorPeriodCompareRateItem;
  remainingCapacity: PriorPeriodCompareRateItem;
  unConfigured: boolean;
}

// 线索统计
export interface LeadStatisticDetail {
  total: number;
  newClue: PriorPeriodCompareRateItem;
  unfollowedClue: PriorPeriodCompareRateItem;
  remainingCapacity: PriorPeriodCompareRateItem;
  unConfigured: boolean;
}

// 跟进记录统计
export interface FollowRecordStatisticDetail {
  newFollowUpRecord: PriorPeriodCompareRateItem;
}

// 跟进计划统计
export interface FollowRecordPlanStatisticDetail {
  newFollowUpPlan: PriorPeriodCompareRateItem;
}

// 联系人统计
export interface ContactStatisticDetail {
  newContact: PriorPeriodCompareRateItem;
}

export interface BaseStatisticData {
  icon: string;
  iconColor: string;
  bgColor: string;
  name: string;
  total: number | null;
  routeName?: string;
  permission: string[];
}

export interface HomeStatisticAnalyticsItem {
  title: string;
  count: number;
  countValue: string;
  priorPeriodCompareRate: number | string;
}

export interface DefaultAnalyticsData extends BaseStatisticData {
  analytics: HomeStatisticAnalyticsItem[];
}

export interface AnalyticsDataWithValueKey extends BaseStatisticData {
  priorPeriodCompareRate: number;
  valueKey: string;
}

export type HomeDataAnalyticsItem = DefaultAnalyticsData | AnalyticsDataWithValueKey;
