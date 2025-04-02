export enum OpportunityRouteEnum {
  OPPORTUNITY = 'opportunity',
  OPPORTUNITY_INDEX = 'opportunityIndex',
}

export enum ClueRouteEnum {
  CLUE = 'clue',
  CLUE_INDEX = 'clueIndex',
  CLUE_POOL = 'cluePool',
}

export enum CustomerRouteEnum {
  CUSTOMER = 'customer',
  CUSTOMER_INDEX = 'customerIndex',
}

export enum ProductRouteEnum {
  PRODUCT = 'product',
  PRODUCT_PRO = 'productPro',
}

export enum MineRouteEnum {
  MINE = 'mine',
  MINE_INDEX = 'mineIndex',
  MINE_MESSAGE = 'mineMessage',
}
export enum WorkbenchRouteEnum {
  WORKBENCH = 'workbench',
  WORKBENCH_INDEX = 'workbenchIndex',
  WORKBENCH_DUPLICATE_CHECK = 'workbenchDuplicateCheck',
}

export const AppRouteEnum = {
  ...OpportunityRouteEnum,
  ...ClueRouteEnum,
  ...CustomerRouteEnum,
  ...ProductRouteEnum,
  ...MineRouteEnum,
  ...WorkbenchRouteEnum,
};
