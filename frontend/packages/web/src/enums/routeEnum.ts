export enum SystemRouteEnum {
  SYSTEM = 'system',
  SYSTEM_ORG = 'systemOrg',
  SYSTEM_ROLE = 'systemRole',
  SYSTEM_MODULE = 'systemModule',
  SYSTEM_BUSINESS = 'systemBusiness',
  SYSTEM_LOG = 'systemLog',
}

export enum OpportunityRouteEnum {
  OPPORTUNITY = 'opportunity',
  OPPORTUNITY_OPT = 'opportunityOpt',
}

export enum ClueRouteEnum {
  CLUE_MANAGEMENT = 'clueManagement',
  CLUE_MANAGEMENT_CLUE = 'clueManagementClue',
  CLUE_MANAGEMENT_POOL = 'clueManagementPool',
}

export enum CustomerRouteEnum {
  CUSTOMER = 'customer',
  CUSTOMER_INDEX = 'customerIndex',
  CUSTOMER_CONTRACT = 'customerContract',
  CUSTOMER_OPEN_SEA = 'customerOpenSea',
}

export const AppRouteEnum = {
  ...SystemRouteEnum,
  ...OpportunityRouteEnum,
  ...ClueRouteEnum,
  ...CustomerRouteEnum,
};
