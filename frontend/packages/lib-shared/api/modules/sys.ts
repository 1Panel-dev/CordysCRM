import type { CordysAxios } from '@lib/shared/api/http/Axios';
import { VersionUrl } from '@lib/shared/api/requrls/sys';

export default function useSysApi(CDR: CordysAxios) {
  // 获取系统版本号
  function getSystemVersion() {
    return CDR.get<string>({ url: VersionUrl });
  }

  return {
    getSystemVersion,
  };
}
