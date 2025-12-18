import { FieldDataSourceTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { CommonList } from '@lib/shared/models/common';

import {
  getCustomerOptions,
  getFieldClueList,
  getFieldContactList,
  getFieldContractList,
  getFieldCustomerList,
  getFieldOpportunityList,
  getFieldPriceList,
  getFieldProductList,
  getFieldQuotationList,
  getUserOptions,
} from '@/api/modules';

export const sourceApi: Record<FieldDataSourceTypeEnum, (data: any) => Promise<CommonList<any>>> = {
  [FieldDataSourceTypeEnum.BUSINESS]: getFieldOpportunityList,
  [FieldDataSourceTypeEnum.CLUE]: getFieldClueList,
  [FieldDataSourceTypeEnum.CONTACT]: getFieldContactList,
  [FieldDataSourceTypeEnum.CUSTOMER]: getFieldCustomerList,
  [FieldDataSourceTypeEnum.PRODUCT]: getFieldProductList,
  [FieldDataSourceTypeEnum.CUSTOMER_OPTIONS]: getCustomerOptions,
  [FieldDataSourceTypeEnum.USER_OPTIONS]: getUserOptions,
  [FieldDataSourceTypeEnum.CONTRACT]: getFieldContractList,
  [FieldDataSourceTypeEnum.PRICE]: getFieldPriceList,
  [FieldDataSourceTypeEnum.QUOTATION]: getFieldQuotationList,
};
export const formKeyMap: Partial<Record<FieldDataSourceTypeEnum, FormDesignKeyEnum>> = {
  [FieldDataSourceTypeEnum.BUSINESS]: FormDesignKeyEnum.BUSINESS,
  [FieldDataSourceTypeEnum.CLUE]: FormDesignKeyEnum.CLUE,
  [FieldDataSourceTypeEnum.CONTACT]: FormDesignKeyEnum.CONTACT,
  [FieldDataSourceTypeEnum.CUSTOMER]: FormDesignKeyEnum.CUSTOMER,
  [FieldDataSourceTypeEnum.PRODUCT]: FormDesignKeyEnum.PRODUCT,
  [FieldDataSourceTypeEnum.CONTRACT]: FormDesignKeyEnum.CONTRACT,
  [FieldDataSourceTypeEnum.PRICE]: FormDesignKeyEnum.PRICE,
  [FieldDataSourceTypeEnum.QUOTATION]: FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
};
