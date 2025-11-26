import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ValidateInfo } from '@lib/shared/models/system/org';

import {
  downloadAccountTemplate,
  downloadContactTemplate,
  downloadLeadTemplate,
  downloadOptTemplate,
  downloadProductTemplate,
  importAccount,
  importContact,
  importLead,
  importOpportunity,
  importProduct,
  preCheckImportAccount,
  preCheckImportContact,
  preCheckImportLead,
  preCheckImportOpt,
  preCheckImportProduct,
} from '@/api/modules';

export type ImportApiType =
  | FormDesignKeyEnum.CLUE
  | FormDesignKeyEnum.BUSINESS
  | FormDesignKeyEnum.CUSTOMER
  | FormDesignKeyEnum.CONTACT
  | FormDesignKeyEnum.PRODUCT
  | FormDesignKeyEnum.PRICE;

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
    // TODO:
    preCheck: () => Promise.resolve({ data: {} as ValidateInfo }),
    save: () => Promise.resolve(),
    download: () => Promise.resolve(new File([], '')),
  },
};
