export enum SystemRouteEnum {
  SYSTEM = 'system',
  SYSTEM_ORG = 'systemOrg',
  SYSTEM_ROLE = 'systemRole',
  SYSTEM_MODULE = 'systemModule',
  SYSTEM_BUSINESS = 'systemBusiness',
  SYSTEM_LICENSE = 'systemLicense',
  SYSTEM_LOG = 'systemLog',
  SYSTEM_MESSAGE = 'systemMessage',
}

export enum OpportunityRouteEnum {
  OPPORTUNITY = 'opportunity',
  OPPORTUNITY_OPT = 'opportunityOpt',
}

export enum ClueRouteEnum {
  CLUE_MANAGEMENT = 'leadManagement',
  CLUE_MANAGEMENT_CLUE = 'leadManagementLead',
  CLUE_MANAGEMENT_POOL = 'leadManagementPool',
}

export enum CustomerRouteEnum {
  CUSTOMER = 'account',
  CUSTOMER_INDEX = 'accountIndex',
  CUSTOMER_CONTACT = 'accountContact',
  CUSTOMER_OPEN_SEA = 'accountOpenSea',
}

export enum ProductRouteEnum {
  PRODUCT = 'product',
  PRODUCT_PRO = 'productPro',
}

export enum PersonalRouteEnum {
  PERSONAL_INFO = 'personalInfo',
  PERSONAL_PLAN = 'personalPlan',
  PERSONAL_EXPORT = 'personalExport',
  LOGOUT = 'logout',
}

export enum WorkbenchRouteEnum {
  WORKBENCH = 'workbench',
  WORKBENCH_INDEX = 'workbenchIndex',
}

export enum DashboardRouteEnum {
  DASHBOARD = 'dashboard',
  DASHBOARD_INDEX = 'dashboardIndex',
  DASHBOARD_LINK = 'dashboardLink',
  DASHBOARD_MODULE = 'dashboardModule',
}

export enum FullPageEnum {
  FULL_PAGE = 'fullPage',
  FULL_PAGE_DASHBOARD = 'fullPageDashboard',
}

export const AppRouteEnum = {
  ...SystemRouteEnum,
  ...OpportunityRouteEnum,
  ...ClueRouteEnum,
  ...CustomerRouteEnum,
  ...ProductRouteEnum,
  ...PersonalRouteEnum,
  ...WorkbenchRouteEnum,
  ...DashboardRouteEnum,
};
