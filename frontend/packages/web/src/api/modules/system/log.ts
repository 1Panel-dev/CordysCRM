import { GetOperationLogDetailUrl, LoginLogListUrl, OperationLogListUrl } from '@lib/shared/api/requrls/system/log';
import type { CommonList } from '@lib/shared/models/common';
import type {
  LoginLogItem,
  LoginLogParams,
  OperationLogDetail,
  OperationLogItem,
  OperationLogParams,
} from '@lib/shared/models/system/log';

import CDR from '@/api/http/index';

// 登录日志
export function loginLogList(data: LoginLogParams) {
  return CDR.post<CommonList<LoginLogItem>>({ url: LoginLogListUrl, data });
}

// 操作日志
export function operationLogList(data: OperationLogParams) {
  return CDR.post<CommonList<OperationLogItem>>({ url: OperationLogListUrl, data });
}

// 操作日志-详情
export function operationLogDetail(id: string) {
  return CDR.get<OperationLogDetail>({ url: `${GetOperationLogDetailUrl}/${id}` });
}
