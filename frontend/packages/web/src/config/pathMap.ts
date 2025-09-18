import { AppRouteEnum } from '@/enums/routeEnum';

export type PathMapKey = keyof typeof AppRouteEnum;

export type PathMapRoute = (typeof AppRouteEnum)[PathMapKey];

export interface PathMapItem {
  key: PathMapKey | string; // 系统设置
  locale: string;
  route: PathMapRoute | string;
  children?: PathMapItem[];
  routeQuery?: Record<string, any>;
}

/**
 * 路由与菜单、tab、权限、国际化信息的映射关系，用于通过路由直接跳转到各页面及携带 tab 参数
 * key 是与后台商定的映射 key
 * locale 是国际化的 key
 * route 是路由的 name
 * routeQuery 是路由的固定参数集合，与routeParamKeys互斥，用于跳转同一个路由但不同 tab 时或其他需要固定参数的情况
 * children 是子路由/tab集合
 */
export const pathMap: PathMapItem[] = [
  {
    key: 'WORKBENCH',
    locale: 'menu.workbench',
    route: AppRouteEnum.WORKBENCH_INDEX,
  },
  {
    key: 'PRODUCT_MANAGEMENT',
    locale: 'module.productManagement',
    route: AppRouteEnum.PRODUCT_PRO,
  },
  {
    key: 'DASHBOARD',
    locale: 'menu.dashboard',
    route: AppRouteEnum.DASHBOARD_INDEX,
  },
  {
    key: 'AGENT',
    locale: 'menu.agent',
    route: AppRouteEnum.AGENT_INDEX,
  },
  {
    key: 'CUSTOMER',
    route: AppRouteEnum.CUSTOMER,
    locale: 'module.customerManagement',
    children: [
      {
        key: 'CUSTOMER_INDEX',
        route: AppRouteEnum.CUSTOMER_INDEX,
        locale: 'menu.customer',
      },
      {
        key: 'CUSTOMER_CONTACT',
        route: AppRouteEnum.CUSTOMER_CONTACT,
        locale: 'menu.contact',
      },
      {
        key: 'CUSTOMER_POOL',
        route: AppRouteEnum.CUSTOMER_OPEN_SEA,
        locale: 'module.openSea',
      },
    ],
  },
  {
    key: 'CLUE_MANAGEMENT',
    route: AppRouteEnum.CLUE_MANAGEMENT,
    locale: 'module.clueManagement',
    children: [
      {
        key: 'CLUE_MANAGEMENT_CLUE',
        route: AppRouteEnum.CLUE_MANAGEMENT_CLUE,
        locale: 'menu.clue',
      },
      {
        key: 'CLUE_MANAGEMENT_POOL',
        route: AppRouteEnum.CLUE_MANAGEMENT_POOL,
        locale: 'module.cluePool',
      },
    ],
  },
  {
    key: 'OPPORTUNITY',
    route: AppRouteEnum.OPPORTUNITY_OPT,
    locale: 'menu.opportunity',
  },
  {
    key: 'FOLLOW_UP_RECORD',
    route: AppRouteEnum.OPPORTUNITY_OPT, // TODO lmy 跟进记录
    locale: 'module.customer.followRecord',
  },
  {
    key: 'FOLLOW_UP_PLAN',
    route: AppRouteEnum.OPPORTUNITY_OPT, // TODO lmy 跟进计划
    locale: 'module.customer.followPlan',
  },
  {
    key: 'SYSTEM',
    route: AppRouteEnum.SYSTEM,
    locale: 'menu.settings',
    children: [
      {
        key: 'SYSTEM_ORGANIZATION',
        route: AppRouteEnum.SYSTEM_ORG,
        locale: 'menu.settings.org',
      },
      {
        key: 'SYSTEM_ROLE',
        route: AppRouteEnum.SYSTEM_ROLE,
        locale: 'menu.settings.permission',
      },
      {
        key: 'SYSTEM_MODULE',
        route: AppRouteEnum.SYSTEM_MODULE,
        locale: 'menu.settings.moduleSetting',
      },
      {
        key: 'SYSTEM_MESSAGE',
        route: AppRouteEnum.SYSTEM_MESSAGE,
        locale: 'menu.settings.messageSetting',
        children: [
          {
            key: 'SYSTEM_MESSAGE_MESSAGE',
            route: AppRouteEnum.SYSTEM_MESSAGE,
            locale: 'system.message.notify',
            routeQuery: {
              tab: 'notify',
            },
          },
          {
            key: 'SYSTEM_MESSAGE_ANNOUNCEMENT',
            route: AppRouteEnum.SYSTEM_MESSAGE,
            locale: 'system.message.announcement',
            routeQuery: {
              tab: 'announcement',
            },
          },
        ],
      },
      {
        key: 'SYSTEM_BUSINESS',
        route: AppRouteEnum.SYSTEM_BUSINESS,
        locale: 'menu.settings.businessSetting',
        children: [
          {
            key: 'SYSTEM_BUSINESS_UI',
            route: AppRouteEnum.SYSTEM_BUSINESS,
            locale: 'system.business.tab.interfaceSettings',
            routeQuery: {
              tab: 'pageSettings',
            },
          },
          {
            key: 'SYSTEM_BUSINESS_THIRD',
            route: AppRouteEnum.SYSTEM_BUSINESS,
            locale: 'system.business.tab.third',
            routeQuery: {
              tab: 'syncOrganization',
            },
          },
          {
            key: 'SYSTEM_BUSINESS_MAIL',
            route: AppRouteEnum.SYSTEM_BUSINESS,
            locale: 'system.business.tab.mailSettings',
            routeQuery: {
              tab: 'mailSettings',
            },
          },
          {
            key: 'SYSTEM_BUSINESS_AUTH',
            route: AppRouteEnum.SYSTEM_BUSINESS,
            locale: 'system.business.tab.authenticationSettings',
            routeQuery: {
              tab: 'authenticationSettings',
            },
          },
        ],
      },
      {
        key: 'OPERATION_LOG',
        route: AppRouteEnum.SYSTEM_LOG,
        locale: 'menu.settings.log',
      },
    ],
  },
];
