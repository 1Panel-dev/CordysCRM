import { ImportTypeExcludeFormDesignEnum } from '@lib/shared/enums/commonEnum';
import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import type { ImportUploadParams } from '@lib/shared/models/common';
import { ValidateInfo } from '@lib/shared/models/system/org';

import {
  downloadAccountTemplate,
  downloadBusinessTitleTemplate,
  downloadContactTemplate,
  downloadContractPaymentRecordTemplate,
  downloadCustomFormTemplate,
  downloadLeadTemplate,
  downloadOptTemplate,
  downloadPoolAccountTemplate,
  downloadPoolLeadTemplate,
  downloadProductPriceTemplate,
  downloadProductTemplate,
  importAccount,
  importBusinessTitle,
  importContact,
  importContractPaymentRecord,
  importCustomForm,
  importLead,
  importOpportunity,
  importPoolAccount,
  importPoolLead,
  importProduct,
  importProductPrice,
  preCheckImportAccount,
  preCheckImportBusinessTitle,
  preCheckImportContact,
  preCheckImportContractPaymentRecord,
  preCheckImportCustomForm,
  preCheckImportLead,
  preCheckImportOpt,
  preCheckImportPoolAccount,
  preCheckImportPoolLead,
  preCheckImportProduct,
  preCheckImportProductPrice,
} from '@/api/modules';

export type ImportApiType =
  | FormDesignKeyEnum.CLUE
  | FormDesignKeyEnum.CLUE_POOL
  | FormDesignKeyEnum.BUSINESS
  | FormDesignKeyEnum.CUSTOMER
  | FormDesignKeyEnum.CUSTOMER_OPEN_SEA
  | FormDesignKeyEnum.CONTACT
  | FormDesignKeyEnum.PRODUCT
  | FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD
  | FormDesignKeyEnum.PRICE
  | ImportTypeExcludeFormDesignEnum.CONTRACT_BUSINESS_TITLE_IMPORT
  | FormDesignKeyEnum.CUSTOM_FORM;

export interface importRequestType {
  preCheck: (params: ImportRequestParams) => Promise<{ data: ValidateInfo }>;
  save: (params: ImportRequestParams) => Promise<any>;
  download?: (customFormId?: string) => Promise<File>;
}

export interface ImportRequestParams {
  uploadParams: ImportUploadParams;
  customFormId?: string;
}

export const importApiMap: Record<ImportApiType, importRequestType> = {
  [FormDesignKeyEnum.CLUE]: {
    preCheck: (params) => preCheckImportLead(params.uploadParams),
    save: (params) => importLead(params.uploadParams),
    download: downloadLeadTemplate,
  },
  [FormDesignKeyEnum.CLUE_POOL]: {
    preCheck: (params) => preCheckImportPoolLead(params.uploadParams),
    save: (params) => importPoolLead(params.uploadParams),
    download: downloadPoolLeadTemplate,
  },
  [FormDesignKeyEnum.CUSTOMER]: {
    preCheck: (params) => preCheckImportAccount(params.uploadParams),
    save: (params) => importAccount(params.uploadParams),
    download: downloadAccountTemplate,
  },
  [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: {
    preCheck: (params) => preCheckImportPoolAccount(params.uploadParams),
    save: (params) => importPoolAccount(params.uploadParams),
    download: downloadPoolAccountTemplate,
  },
  [FormDesignKeyEnum.CONTACT]: {
    preCheck: (params) => preCheckImportContact(params.uploadParams),
    save: (params) => importContact(params.uploadParams),
    download: downloadContactTemplate,
  },
  [FormDesignKeyEnum.BUSINESS]: {
    preCheck: (params) => preCheckImportOpt(params.uploadParams),
    save: (params) => importOpportunity(params.uploadParams),
    download: downloadOptTemplate,
  },
  [FormDesignKeyEnum.PRODUCT]: {
    preCheck: (params) => preCheckImportProduct(params.uploadParams),
    save: (params) => importProduct(params.uploadParams),
    download: downloadProductTemplate,
  },
  [FormDesignKeyEnum.PRICE]: {
    preCheck: (params) => preCheckImportProductPrice(params.uploadParams),
    save: (params) => importProductPrice(params.uploadParams),
    download: downloadProductPriceTemplate,
  },
  [FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD]: {
    preCheck: (params) => preCheckImportContractPaymentRecord(params.uploadParams),
    save: (params) => importContractPaymentRecord(params.uploadParams),
    download: downloadContractPaymentRecordTemplate,
  },
  [ImportTypeExcludeFormDesignEnum.CONTRACT_BUSINESS_TITLE_IMPORT]: {
    preCheck: (params) => preCheckImportBusinessTitle(params.uploadParams),
    save: (params) => importBusinessTitle(params.uploadParams),
    download: downloadBusinessTitleTemplate,
  },
  [FormDesignKeyEnum.CUSTOM_FORM]: {
    preCheck: (params) => preCheckImportCustomForm(params.uploadParams, params.customFormId),
    save: (params) => importCustomForm(params.uploadParams, params.customFormId),
    download: downloadCustomFormTemplate,
  },
};
