export interface GetHomeStatisticParams {
  searchType: string;
  deptIds: string[];
  timeField?: string;
  priorPeriodEnable?: boolean;
}

export interface DimPeriodValue {
  value: number;
  priorPeriodCompareRate: number;
}
// 跟进商机
export interface FollowOptStatisticDetail {
  todayOpportunity: DimPeriodValue;
  thisWeekOpportunity: DimPeriodValue;
  thisMonthOpportunity: DimPeriodValue;
  thisYearOpportunity: DimPeriodValue;
  thisYearOpportunityAmount: DimPeriodValue;
  thisMonthOpportunityAmount: DimPeriodValue;
  thisWeekOpportunityAmount: DimPeriodValue;
  todayOpportunityAmount: DimPeriodValue;
}
// 线索统计
export interface HomeLeadStatisticDetail {
  thisYearClue: DimPeriodValue;
  thisMonthClue: DimPeriodValue;
  thisWeekClue: DimPeriodValue;
  todayClue: DimPeriodValue;
}
// 赢单统计
export interface HomeWinOrderDetail {
  thisYearOpportunity: DimPeriodValue;
  thisMonthOpportunity: DimPeriodValue;
  thisWeekOpportunity: DimPeriodValue;
  todayOpportunity: DimPeriodValue;
  thisYearOpportunityAmount: DimPeriodValue;
  thisMonthOpportunityAmount: DimPeriodValue;
  thisWeekOpportunityAmount: DimPeriodValue;
  todayOpportunityAmount: DimPeriodValue;
}
