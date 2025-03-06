import { OptFollowPlanPageUrl, OptFollowRecordListUrl, OptPageUrl } from '@lib/shared/api/requrls/opportunity';
import type { CommonList, TableQueryParams } from '@lib/shared/models/common';
import type {
  CustomerFollowPlanTableParams,
  CustomerFollowRecordTableParams,
  FollowDetailItem,
} from '@lib/shared/models/customer';
import type { OpportunityItem } from '@lib/shared/models/opportunity';

import CDR from '@/api/http/index';

// 商机列表
export function getOpportunityList(data: TableQueryParams) {
  return CDR.post<CommonList<OpportunityItem>>({ url: OptPageUrl, data });
}

// 跟进记录列表
export function getOptFollowRecordList(data: CustomerFollowRecordTableParams) {
  return CDR.post<CommonList<FollowDetailItem>>({ url: OptFollowRecordListUrl, data });
}

// 跟进计划列表
export function getOptFollowPlanList(data: CustomerFollowPlanTableParams) {
  return CDR.post<CommonList<FollowDetailItem>>({ url: OptFollowPlanPageUrl, data });
}
