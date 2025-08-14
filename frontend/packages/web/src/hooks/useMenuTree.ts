import { computed } from 'vue';
import { RouteRecordNormalized, RouteRecordRaw } from 'vue-router';
import { cloneDeep } from 'lodash-es';

import usePermission from '@/hooks/usePermission';
import appClientMenus from '@/router/app-menus';
import { allMenuRouteMap, featureRouteMap } from '@/router/constants';
import useAppStore from '@/store/modules/app';
import useLicenseStore from '@/store/modules/setting/license';

import { DashboardRouteEnum, SystemRouteEnum } from '@/enums/routeEnum';

/**
 * 获取菜单树
 * @returns
 */
export default function useMenuTree() {
  const appStore = useAppStore();
  const permission = usePermission();
  const licenseStore = useLicenseStore();

  const menuTree = computed(() => {
    const copyRouter = cloneDeep(appClientMenus) as RouteRecordNormalized[];

    const currentMenuConfig: string[] = appStore.moduleConfigList.filter((e) => e.enable).map((e) => e.moduleKey);

    copyRouter.sort((a: RouteRecordNormalized, b: RouteRecordNormalized) => {
      const getModuleOrder = (route: RouteRecordNormalized) => {
        const moduleId = Object.keys(allMenuRouteMap).find((key) => (route.name as string)?.includes(key));
        if (moduleId) {
          return currentMenuConfig.indexOf(allMenuRouteMap[moduleId]);
        }
        return Infinity;
      };
      return getModuleOrder(a) - getModuleOrder(b);
    });

    function travel(_routes: RouteRecordRaw[], layer: number) {
      if (!_routes) return null;

      const collector = _routes.map((element) => {
        if (element.meta?.hideInMenu === true) {
          return null;
        }

        // 如果是隐藏的模块，则不显示菜单
        const moduleId = Object.keys(featureRouteMap).find((key) => (element.name as string)?.includes(key));
        if (moduleId && featureRouteMap[moduleId] && !currentMenuConfig.includes(featureRouteMap[moduleId])) {
          return null;
        }

        // 权限校验不通过
        if (!permission.accessRouter(element)) {
          return null;
        }
        // TODO license 先放开
        // if (
        //   (!licenseStore.isEnterpriseVersion() && element.name === SystemRouteEnum.SYSTEM_LICENSE) ||
        //   (!licenseStore.hasLicense() && element.name === DashboardRouteEnum.DASHBOARD)
        // ) {
        //   return null;
        // }
        //  社区版去掉license菜单
        if (!licenseStore.isEnterpriseVersion() && element.name === SystemRouteEnum.SYSTEM_LICENSE) {
          return null;
        }

        // 叶子菜单
        if (element.meta?.hideChildrenInMenu || !element.children) {
          element.children = [];
          return element;
        }

        // 过滤隐藏的菜单
        element.children = element.children.filter((x) => x.meta?.hideInMenu !== true);

        // 解析子菜单
        const subItem = travel(element.children, layer + 1);

        if (subItem && subItem.length) {
          element.children = subItem as RouteRecordRaw[];
          return element;
        }

        if (layer > 1) {
          element.children = subItem as RouteRecordRaw[];
          return element;
        }

        if (element.meta?.hideInMenu === false) {
          return element;
        }

        return null;
      });
      return collector.filter(Boolean);
    }
    return travel(copyRouter, 0);
  });

  return {
    menuTree,
  };
}
