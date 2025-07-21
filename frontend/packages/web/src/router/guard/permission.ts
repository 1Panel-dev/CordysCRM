import usePermission from '@/hooks/usePermission';
import useAppStore from '@/store/modules/app';
import useLicenseStore from '@/store/modules/setting/license';

import { SystemRouteEnum } from '@/enums/routeEnum';

import { featureRouteMap, NO_RESOURCE_ROUTE_NAME, WHITE_LIST } from '../constants';
import NProgress from 'nprogress';
import type { Router } from 'vue-router';

export default function setupPermissionGuard(router: Router) {
  router.beforeEach(async (to, from, next) => {
    const Permission = usePermission();
    const permissionsAllow = Permission.accessRouter(to);
    const appStore = useAppStore();
    const licenseStore = useLicenseStore();

    const currentMenuConfig: string[] = appStore.moduleConfigList.filter((e) => e.enable).map((e) => e.moduleKey);
    const moduleId = Object.keys(featureRouteMap).find((key) => (to.name as string)?.includes(key));
    const isNotHasEnterprisePermission =
      !licenseStore.isEnterpriseVersion() && to.name === SystemRouteEnum.SYSTEM_LICENSE;
    if (
      (moduleId && featureRouteMap[moduleId] && !currentMenuConfig.includes(featureRouteMap[moduleId])) ||
      isNotHasEnterprisePermission
    ) {
      next({
        name: NO_RESOURCE_ROUTE_NAME,
      });
    }

    const exist = WHITE_LIST.find((el) => el.name === to.name);
    if (exist || permissionsAllow) {
      next();
    } else {
      next({
        name: NO_RESOURCE_ROUTE_NAME,
      });
    }
    NProgress.done();
  });
}
