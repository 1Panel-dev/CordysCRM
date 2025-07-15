import type { CordysAxios } from '@lib/shared/api/http/Axios';
import { AddLicenseUrl, GetLicenseUrl } from '@lib/shared/api/requrls/system/authorizedManagement';
import type { LicenseInfo } from '@lib/shared/models/system/authorizedManagement';

export default function useProductApi(CDR: CordysAxios) {
  /**
   * 授权管理相关API
   */
  // 获取License
  function getLicense() {
    return CDR.get<LicenseInfo>({ url: GetLicenseUrl }, { ignoreCancelToken: true });
  }

  // 添加License
  function addLicense(data: string) {
    return CDR.post({ url: AddLicenseUrl, data });
  }

  return {
    getLicense,
    addLicense,
  };
}
