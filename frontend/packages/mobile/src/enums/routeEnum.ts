export enum OpportunityRouteEnum {
  OPPORTUNITY = 'opportunity',
  OPPORTUNITY_INDEX = 'opportunityIndex',
  OPPORTUNITY_DETAIL = 'opportunityDetail',
}

export enum ClueRouteEnum {
  CLUE = 'lead',
  CLUE_INDEX = 'leadIndex',
  CLUE_DETAIL = 'leadDetail',
  CLUE_POOL_DETAIL = 'leadPoolDetail',
  CONVERT = 'convert',
  MOVE_TO_POOL = 'moveToPool',
}

export enum CustomerRouteEnum {
  CUSTOMER = 'account',
  CUSTOMER_INDEX = 'accountIndex',
  CUSTOMER_DETAIL = 'accountDetail',
  CUSTOMER_TRANSFER = 'accountTransfer',
  CUSTOMER_DISTRIBUTE = 'accountDistribute',
  CUSTOMER_OPENSEA_DETAIL = 'accountOpenSeaDetail',
  CUSTOMER_RELATION = 'accountRelation',
  CUSTOMER_COLLABORATOR = 'accountCollaborator',
}

export enum CommonRouteEnum {
  COMMON = 'common',
  FORM_CREATE = 'formCreate',
  CONTACT_DETAIL = 'contactDetail',
  FOLLOW_DETAIL = 'followDetail',
  WORKFLOW_STAGE = 'workflowStage',
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
  WORKBENCH_DUPLICATE_CHECK_DETAIL = 'workbenchDuplicateCheckDetail',
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
