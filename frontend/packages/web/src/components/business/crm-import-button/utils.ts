import { ImportTypeExcludeFormDesignEnum } from '@lib/shared/enums/commonEnum';
import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ValidateInfo } from '@lib/shared/models/system/org';

import {
  downloadAccountTemplate,
  downloadBusinessTitleTemplate,
  downloadContactTemplate,
  downloadContractPaymentRecordTemplate,
  downloadLeadTemplate,
  downloadOptTemplate,
  downloadProductPriceTemplate,
  downloadProductTemplate,
  importAccount,
  importBusinessTitle,
  importContact,
  importContractPaymentRecord,
  importLead,
  importOpportunity,
  importProduct,
  importProductPrice,
  preCheckImportAccount,
  preCheckImportBusinessTitle,
  preCheckImportContact,
  preCheckImportContractPaymentRecord,
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
  | FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD
  | FormDesignKeyEnum.PRICE
  | ImportTypeExcludeFormDesignEnum.CONTRACT_BUSINESS_TITLE_IMPORT;

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
  [FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD]: {
    preCheck: preCheckImportContractPaymentRecord,
    save: importContractPaymentRecord,
    download: downloadContractPaymentRecordTemplate,
  },
  [ImportTypeExcludeFormDesignEnum.CONTRACT_BUSINESS_TITLE_IMPORT]: {
    preCheck: preCheckImportBusinessTitle,
    save: importBusinessTitle,
    download: downloadBusinessTitleTemplate,
  },
};
