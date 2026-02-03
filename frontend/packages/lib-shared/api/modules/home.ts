import type { FollowOptStatisticDetail, GetHomeStatisticParams, HomeLeadStatisticDetail, HomeWinOrderDetail } from '../../models/home';
import { HomeDepartmentTree, HomeFollowOpportunity, HomeLeadStatistic, HomeSuccessOpportunity, HomeOpportunityUnderwayUrl } from '../requrls/home';
import { CrmTreeNodeData } from '@cordys/web/src/components/pure/crm-tree/type';
import type { CordysAxios } from '@lib/shared/api/http/Axios';

export default function useHomeApi(CDR: CordysAxios) {
  // 用户部门权限树
  function getHomeDepartmentTree() {
    return CDR.get<CrmTreeNodeData[]>({ url: HomeDepartmentTree });
  }

  // 跟进商机统计
  function getHomeFollowOpportunity(data: GetHomeStatisticParams) {
    return CDR.post<FollowOptStatisticDetail>({ url: HomeFollowOpportunity, data });
  }

  // 线索统计
  function getHomeLeadStatistic(data: GetHomeStatisticParams) {
    return CDR.post<HomeLeadStatisticDetail>({ url: HomeLeadStatistic, data });
  }

  function getHomeSuccessOptStatistic(data: GetHomeStatisticParams) {
    return CDR.post<HomeWinOrderDetail>({ url: HomeSuccessOpportunity, data });
  }

  function getHomeOpportunityUnderwayStatistic(data: GetHomeStatisticParams) {
    return CDR.post<HomeWinOrderDetail>({ url: HomeOpportunityUnderwayUrl, data });
  }

  return {
    getHomeDepartmentTree,
    getHomeFollowOpportunity,
    getHomeLeadStatistic,
    getHomeSuccessOptStatistic,
    getHomeOpportunityUnderwayStatistic,
  };
}
