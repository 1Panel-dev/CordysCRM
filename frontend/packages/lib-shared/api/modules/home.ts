import type {
  AccountStatisticDetail,
  ContactStatisticDetail,
  FollowOptStatisticDetail,
  FollowRecordPlanStatisticDetail,
  FollowRecordStatisticDetail,
  GetHomeStatisticParams,
  LeadStatisticDetail,
} from '../../models/home';
import {
  HomeAccountStatistic,
  HomeContactStatistic,
  HomeDepartmentTree,
  HomeFollowOpportunity,
  HomeFollowPlanStatistic,
  HomeFollowStatistic,
  HomeLeadStatistic,
} from '../requrls/home';
import { CrmTreeNodeData } from '@cordys/web/src/components/pure/crm-tree/type';
import type { CordysAxios } from '@lib/shared/api/http/Axios';

export default function useHomeApi(CDR: CordysAxios) {
  // 跟进商机统计
  function getHomeFollowOpportunity(data: GetHomeStatisticParams) {
    return CDR.post<FollowOptStatisticDetail>({ url: HomeFollowOpportunity, data });
  }
  // 跟进记录统计
  function getHomeFollowStatistic(data: GetHomeStatisticParams) {
    return CDR.post<FollowRecordStatisticDetail>({ url: HomeFollowStatistic, data });
  }
  // 跟进计划统计
  function getHomeFollowPlanStatistic(data: GetHomeStatisticParams) {
    return CDR.post<FollowRecordPlanStatisticDetail>({ url: HomeFollowPlanStatistic, data });
  }
  // 客户统计
  function getHomeAccountStatistic(data: GetHomeStatisticParams) {
    return CDR.post<AccountStatisticDetail>({ url: HomeAccountStatistic, data });
  }
  // 联系人统计
  function getHomeContactStatistic(data: GetHomeStatisticParams) {
    return CDR.post<ContactStatisticDetail>({ url: HomeContactStatistic, data });
  }
  // 线索统计
  function getHomeLeadStatistic(data: GetHomeStatisticParams) {
    return CDR.post<LeadStatisticDetail>({ url: HomeLeadStatistic, data });
  }
  // 用户部门权限树
  function getHomeDepartmentTree() {
    return CDR.get<CrmTreeNodeData[]>({ url: HomeDepartmentTree });
  }

  return {
    getHomeFollowOpportunity,
    getHomeFollowStatistic,
    getHomeAccountStatistic,
    getHomeContactStatistic,
    getHomeLeadStatistic,
    getHomeDepartmentTree,
    getHomeFollowPlanStatistic,
  };
}
