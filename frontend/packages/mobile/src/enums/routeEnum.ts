export enum OpportunityRouteEnum {
  OPPORTUNITY = 'opportunity',
  OPPORTUNITY_INDEX = 'opportunityIndex',
}

export enum ClueRouteEnum {
  CLUE = 'clue',
  CLUE_INDEX = 'clueIndex',
  CLUE_DETAIL = 'clueDetail',
  CLUE_POOL_DETAIL = 'cluePoolDetail',
}

export enum CustomerRouteEnum {
  CUSTOMER = 'customer',
  CUSTOMER_INDEX = 'customerIndex',
  CUSTOMER_DETAIL = 'customerDetail',
  CUSTOMER_TRANSFER = 'customerTransfer',
  CUSTOMER_DISTRIBUTE = 'customerDistribute',
  CUSTOMER_OPENSEA_DETAIL = 'customerOpenSeaDetail',
}

export enum CommonRouteEnum {
  COMMON = 'common',
  FORM_CREATE = 'formCreate',
  CONTACT_DETAIL = 'contactDetail',
  FOLLOW_DETAIL = 'followDetail',
}

export enum ProductRouteEnum {
  PRODUCT = 'product',
  PRODUCT_PRO = 'productPro',
}

export enum MineRouteEnum {
  MINE = 'mine',
  MINE_INDEX = 'mineIndex',
  MINE_MESSAGE = 'mineMessage',
  MINE_DETAIL = 'mineDetail',
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
  ...CommonRouteEnum,
  ...ProductRouteEnum,
  ...MineRouteEnum,
  ...WorkbenchRouteEnum,
};
