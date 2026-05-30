import { RouteLocationNormalized, RouteRecordNormalized, RouteRecordRaw } from 'vue-router';

import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';

import appRoutes from '@/router/routes/index';
import useAppStore from '@/store/modules/app';
import useUserStore from '@/store/modules/user';

import { WorkbenchRouteEnum } from '@/enums/routeEnum';

export function hasPermission(permission: string) {
  const userStore = useUserStore();
  if (userStore.isAdmin) {
    return true;
  }

  if (userStore.userInfo.permissionIds.length === 0) {
    return false;
  }

  if (userStore.userInfo.permissionIds.includes(permission)) {
    return true;
  }
  return false;
}

/**
 * 判断是否有任一权限
 * @param permissions 权限列表
 */
export function hasAnyPermission(permissions?: string[]) {
  if (!permissions || permissions.length === 0) {
    return true;
  }
  return permissions.some((permission) => hasPermission(permission));
}

/**
 * 判断是否有所有权限
 * @param permissions 权限列表
 */
export function hasAllPermission(permissions?: string[]) {
  if (!permissions || permissions.length === 0) {
    return true;
  }
  return permissions.every((permission) => hasPermission(permission));
}

// 判断当前一级菜单是否有权限
export function topLevelMenuHasPermission(route: RouteLocationNormalized | RouteRecordRaw) {
  const userStore = useUserStore();
  const appStore = useAppStore();
  const { moduleConfigList } = appStore;

  if (
    moduleConfigList.length &&
    !moduleConfigList.filter((e) => e.enable).some((e) => e.moduleKey === (route.name as string))
  ) {
    // 没有配置的菜单不显示
    return false;
  }
  if (userStore.isAdmin) {
    // 如果是系统管理员, 包含所有菜单权限
    return true;
  }
  return hasAnyPermission(route.meta?.permissions || []);
}

// 有权限的第一个路由名，如果没有找到则返回IndexRoute
export function getFirstRouteNameByPermission(routerList: RouteRecordNormalized[]) {
  const currentRoute = routerList.filter((item) => hasAnyPermission(item.meta.permissions || []))[0]; // 排除没有权限的路由

  const appStore = useAppStore();
  // 首页模块开启默认首页，否则有权限的第一个路由
  return appStore.moduleConfigList.find((e) => e.moduleKey === ModuleConfigEnum.HOME && e.enable)
    ? WorkbenchRouteEnum.WORKBENCH
    : currentRoute?.name;
}

// 判断当前路由名有没有权限
export function routerNameHasPermission(routerName: string, routerList: RouteRecordNormalized[]) {
  const currentRoute = routerList.find((item) => item.name === routerName);
  return currentRoute ? hasAnyPermission(currentRoute.meta?.permissions || []) : false;
}

export function findRouteByName(name: string) {
  const queue: RouteRecordNormalized[] = [...appRoutes];
  while (queue.length > 0) {
    const currentRoute = queue.shift();
    if (!currentRoute) {
      return;
    }
    if (currentRoute.name === name) {
      return currentRoute;
    }
    if (currentRoute.children) {
      queue.push(...(currentRoute.children as RouteRecordNormalized[]));
    }
  }
  return null;
}

// 找到当前路由下 第一个由权限的子路由
export function getFirstRouterNameByCurrentRoute(parentName: string) {
  const currentRoute = findRouteByName(parentName);
  if (currentRoute) {
    const hasAuthChildrenRouter = currentRoute.children.find((item) => hasAnyPermission(item.meta?.permissions || []));
    return hasAuthChildrenRouter ? hasAuthChildrenRouter.name : parentName;
  }
  return parentName;
}
