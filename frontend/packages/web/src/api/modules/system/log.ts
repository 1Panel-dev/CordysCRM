import {
  AiExecutionLogListUrl,
  GetAiExecutionLogDetailUrl,
  GetOperationLogDetailUrl,
  LoginLogListUrl,
  OperationLogListUrl,
} from '@lib/shared/api/requrls/system/log';
import type { CommonList } from '@lib/shared/models/common';
import type {
  AiExecutionLogItem,
  AiExecutionLogParams,
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

// AI 执行日志
export function aiExecutionLogList(data: AiExecutionLogParams) {
  return CDR.post<CommonList<AiExecutionLogItem>>({ url: AiExecutionLogListUrl, data });
}

// AI 执行日志-详情
export function aiExecutionLogDetail(id: string) {
  return CDR.get<AiExecutionLogItem>({ url: `${GetAiExecutionLogDetailUrl}/${id}` });
}
