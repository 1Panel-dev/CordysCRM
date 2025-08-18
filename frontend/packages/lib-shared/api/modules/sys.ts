import type { CordysAxios } from '@lib/shared/api/http/Axios';
import { VersionUrl } from '@lib/shared/api/requrls/sys';
import type { SystemVersion } from '@lib/shared/models/common';

export default function useSysApi(CDR: CordysAxios) {
  // 获取系统版本信息
  function getSystemVersion() {
    return CDR.get<SystemVersion>({ url: VersionUrl });
  }

  return {
    getSystemVersion,
  };
}
