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
    isEnterpriseVersion() {
      return this.licenseInfo?.status !== 'not_found';
    },
    getExpirationTime(resTime: string) {
      if (!this.isEnterpriseVersion()) {
        this.expiredDuring = false;
        this.expiredDays = 0;
        return;
      }

      if (!resTime && !this.hasLicense() && this.isEnterpriseVersion()) {
        this.expiredDuring = true;
        this.expiredDays = 0;
        return;
      }

      const today = dayjs().startOf('day'); // 今天的开始时间
      const expirationDate = dayjs(resTime).startOf('day'); // license到期日的开始时间

      // 计算天数差（不包含当天）
      const daysDifference = expirationDate.diff(today, 'day');
      if (daysDifference < 0) {
        // 已过期情况
        const daysExpired = Math.abs(daysDifference);
        this.expiredDays = -daysExpired; // 负数表示已过期天数
        this.expiredDuring = daysExpired <= 30; // 过期后只显示30天
      } else if (daysDifference === 0) {
        // 今天到期（显示"剩余1天"）
        this.expiredDays = 1;
        this.expiredDuring = true;
      } else if (daysDifference <= 29) {
        // 1-29天后到期（显示剩余天数+1）
        this.expiredDays = daysDifference + 1;
        this.expiredDuring = true;
      } else if (daysDifference === 30) {
        // 30天后到期（显示"剩余30天"）
        this.expiredDays = daysDifference + 1;
        this.expiredDuring = false;
      } else {
        // 31+天后到期
        this.expiredDays = daysDifference + 1;
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
        /* if (!result || !result.status || !result.license || !result.license.count) {
          return;
        } */
        this.setLicenseInfo(result);
        // 计算license时间
        if (result) {
          this.getExpirationTime(result.expired);
        }
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    },
  },
});

export default useLicenseStore;
