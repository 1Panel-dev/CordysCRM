import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ValidateInfo } from '@lib/shared/models/system/org';

import {
  downloadAccountTemplate,
  downloadContactTemplate,
  downloadLeadTemplate,
  downloadOptTemplate,
  importAccount,
  importContact,
  importLead,
  importOpportunity,
  preCheckImportAccount,
  preCheckImportContact,
  preCheckImportLead,
  preCheckImportOpt,
} from '@/api/modules';

export type ImportApiType =
  | FormDesignKeyEnum.CLUE
  | FormDesignKeyEnum.BUSINESS
  | FormDesignKeyEnum.CUSTOMER
  | FormDesignKeyEnum.CONTACT;

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
};
