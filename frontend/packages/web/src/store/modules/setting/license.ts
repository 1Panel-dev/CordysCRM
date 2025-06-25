import { defineStore } from 'pinia';
import dayjs from 'dayjs';

import { LicenseInfo } from '@lib/shared/models/system/authorizedManagement';

import { getLicense } from '@/api/modules';

const useLicenseStore = defineStore('license', {
  persist: true,
  state: (): { licenseInfo: LicenseInfo | null; expiredDuring: boolean; expiredDays: number } => ({
    licenseInfo: null,
    expiredDuring: false,
    expiredDays: 0,
  }),
  actions: {
    setLicenseInfo(info: LicenseInfo) {
      this.licenseInfo = info;
    },
    removeLicenseStatus() {
      if (this.licenseInfo) {
        this.licenseInfo.status = null;
      }
    },
    hasLicense() {
      return this.licenseInfo?.status === 'valid';
    },
    getExpirationTime(resTime: string) {
      const today = Date.now();
      const startDate = dayjs(today).format('YYYY-MM-DD');
      const endDate = dayjs(resTime);

      const daysDifference = endDate.diff(startDate, 'day');
      this.expiredDays = daysDifference;
      if (daysDifference <= 30 && daysDifference >= 0) {
        this.expiredDuring = true;
      } else if (daysDifference <= 0 && daysDifference >= -30) {
        this.expiredDuring = true;
      } else {
        this.expiredDuring = false;
      }
    },
    // license校验
    async getValidateLicense() {
      try {
        const result = await getLicense();
        // 检查返回结果是否有效，不存在license自身值
        if (!result || !result.status) {
          return;
        }
        /*if (!result || !result.status || !result.license || !result.license.count) {
          return;
        }*/
        this.setLicenseInfo(result);
        // 计算license时间
        if (result.license && result.license.count){
          this.getExpirationTime(result.license.expired);
        }
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    },
  },
});

export default useLicenseStore;
