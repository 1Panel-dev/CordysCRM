import { ImportTypeExcludeFormDesignEnum } from '@lib/shared/enums/commonEnum';
import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ValidateInfo } from '@lib/shared/models/system/org';

import {
  downloadAccountTemplate,
  downloadBusinessNameTemplate,
  downloadContactTemplate,
  downloadLeadTemplate,
  downloadOptTemplate,
  downloadProductPriceTemplate,
  downloadProductTemplate,
  importAccount,
  importBusinessName,
  importContact,
  importLead,
  importOpportunity,
  importProduct,
  importProductPrice,
  preCheckImportAccount,
  preCheckImportBusinessName,
  preCheckImportContact,
  preCheckImportLead,
  preCheckImportOpt,
  preCheckImportProduct,
  preCheckImportProductPrice,
} from '@/api/modules';

export type ImportApiType =
  | FormDesignKeyEnum.CLUE
  | FormDesignKeyEnum.BUSINESS
  | FormDesignKeyEnum.CUSTOMER
  | FormDesignKeyEnum.CONTACT
  | FormDesignKeyEnum.PRODUCT
  | FormDesignKeyEnum.PRICE
  | ImportTypeExcludeFormDesignEnum.CONTRACT_BUSINESS_NAME_IMPORT;

export interface importRequestType {
  preCheck: (file: File) => Promise<{ data: ValidateInfo }>;
  save: (file: File) => Promise<any>;
  download?: () => Promise<File>;
}

export const importApiMap: Record<ImportApiType, importRequestType> = {
  [FormDesignKeyEnum.CLUE]: {
    preCheck: preCheckImportLead,
    save: importLead,
    download: downloadLeadTemplate,
  },
  [FormDesignKeyEnum.CUSTOMER]: {
    preCheck: preCheckImportAccount,
    save: importAccount,
    download: downloadAccountTemplate,
  },
  [FormDesignKeyEnum.CONTACT]: {
    preCheck: preCheckImportContact,
    save: importContact,
    download: downloadContactTemplate,
  },
  [FormDesignKeyEnum.BUSINESS]: {
    preCheck: preCheckImportOpt,
    save: importOpportunity,
    download: downloadOptTemplate,
  },
  [FormDesignKeyEnum.PRODUCT]: {
    preCheck: preCheckImportProduct,
    save: importProduct,
    download: downloadProductTemplate,
  },
  [FormDesignKeyEnum.PRICE]: {
    preCheck: preCheckImportProductPrice,
    save: importProductPrice,
    download: downloadProductPriceTemplate,
  },
  [ImportTypeExcludeFormDesignEnum.CONTRACT_BUSINESS_NAME_IMPORT]: {
    preCheck: preCheckImportBusinessName,
    save: importBusinessName,
    download: downloadBusinessNameTemplate,
  },
};
