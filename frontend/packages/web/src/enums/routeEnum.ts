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
  OPPORTUNITY_QUOTATION = 'opportunityQuotation',
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

export enum ContractRouteEnum {
  CONTRACT = 'contract',
  CONTRACT_INDEX = 'contractIndex',
  CONTRACT_PAYMENT = 'contractPaymentPlan',
  CONTRACT_PAYMENT_RECORD = 'contractPaymentRecord',
  CONTRACT_BUSINESS_NAME = 'contractBusinessName',
}

export enum ProductRouteEnum {
  PRODUCT = 'product',
  PRODUCT_PRO = 'productPro',
  PRODUCT_PRICE = 'productPrice',
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

export enum AgentRouteEnum {
  AGENT = 'agent',
  AGENT_INDEX = 'agentIndex',
}

export enum DashboardRouteEnum {
  DASHBOARD = 'dashboard',
  DASHBOARD_INDEX = 'dashboardIndex',
  DASHBOARD_LINK = 'dashboardLink',
  DASHBOARD_MODULE = 'dashboardModule',
}

export enum TenderRouteEnum {
  TENDER = 'tender',
  TENDER_INDEX = 'tenderIndex',
}

export enum FullPageEnum {
  FULL_PAGE = 'fullPage',
  FULL_PAGE_DASHBOARD = 'fullPageDashboard',
  FULL_PAGE_EXPORT_QUOTATION = 'fullPageExportQuotation',
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
  ...AgentRouteEnum,
  ...ContractRouteEnum,
  ...TenderRouteEnum,
};
