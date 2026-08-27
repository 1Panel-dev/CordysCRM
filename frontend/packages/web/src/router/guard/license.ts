import useLicenseStore from '@/store/modules/setting/license';

import type { Router } from 'vue-router';

export default function setupLicenseGuard(router: Router) {
  router.beforeEach(async (to) => {
    const licenseRoute = [...to.matched].reverse().find((route) => route.meta.licenseRequired);
    if (!licenseRoute) {
      return true;
    }

    const licenseStore = useLicenseStore();
    await licenseStore.ensureLicenseValidated();

    if (licenseStore.hasLicense()) {
      return true;
    }

    const fallbackRoute = licenseRoute.meta.licenseFallbackRoute;
    if (!fallbackRoute) {
      return false;
    }

    const parentRouteName = to.matched.at(-2)?.name;
    if (to.redirectedFrom?.name !== parentRouteName) {
      licenseStore.setNoLicenseFeature(String(to.name));
    }

    return {
      name: fallbackRoute,
      replace: true,
    };
  });
}
