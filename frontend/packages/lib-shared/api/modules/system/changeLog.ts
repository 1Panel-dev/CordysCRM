import type { CordysAxios } from '@lib/shared/api/http/Axios';
import { ChangeLogPageUrl } from '@lib/shared/api/requrls/system/log';
import type { ChangeRecordQueryParams } from '@lib/shared/models/changeRecord';
import type { CommonList } from '@lib/shared/models/common';
import type { OperationLogDetail } from '@lib/shared/models/system/log';

export default function useChangeLogApi(CDR: CordysAxios) {
  function getChangeLogPage(formKey: string, data: ChangeRecordQueryParams) {
    const { sourceId, ...params } = data;
    return CDR.post<CommonList<OperationLogDetail>>({
      url: `${ChangeLogPageUrl}/${formKey}/${sourceId}/page`,
      data: params,
    });
  }

  return {
    getChangeLogPage,
  };
}
