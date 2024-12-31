import { RouteLocationNormalized, RouteRecordRaw } from 'vue-router';

/**
 * 用户权限
 * @returns 调用方法
 */
export default function usePermission() {
  return {
    /**
     * 是否为允许访问的路由
     * @param route 路由信息
     * @returns 是否
     */
    accessRouter(route: RouteLocationNormalized | RouteRecordRaw) {
      // TODO 判断当前一级菜单是否有权限
      // 常规权限检测
      return (
        route.meta?.requiresAuth === false || // 不需要认证
        !route.meta?.roles || // 没有角色限制
        (Array.isArray(route.meta?.roles) && route.meta?.roles?.includes('*')) // 所有人可访问
        // TODO 匹配权限列表
      );
    },
  };
}
