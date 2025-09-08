import type { GetHomeStatisticParams } from '../../models/home';
import { HomeDepartmentTree, HomeFollowOpportunity, HomeLeadStatistic, HomeSuccessOpportunity } from '../requrls/home';
import { CrmTreeNodeData } from '@cordys/web/src/components/pure/crm-tree/type';
import type { CordysAxios } from '@lib/shared/api/http/Axios';

export default function useHomeApi(CDR: CordysAxios) {
  // 用户部门权限树
  function getHomeDepartmentTree() {
    return CDR.get<CrmTreeNodeData[]>({ url: HomeDepartmentTree });
  }

  return {
    getHomeDepartmentTree,
  };
}
