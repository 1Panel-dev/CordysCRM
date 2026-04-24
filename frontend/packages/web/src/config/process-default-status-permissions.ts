import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ProcessStatusEnum } from '@lib/shared/enums/process';
import { ProcessStatusType } from '@lib/shared/models/system/process';

export type PermissionSuffix = 'READ' | 'UPDATE' | 'DELETE' | 'DOWNLOAD' | 'EXPORT' | 'VOIDED' | 'STAGE' | 'PAYMENT';

type PartialModuleDefaultPermissionConfig = Partial<Record<ProcessStatusType, PermissionSuffix[]>>;

export const processDefaultStatusPermissionMap: Partial<
  Record<FormDesignKeyEnum | string, PartialModuleDefaultPermissionConfig>
> = {
  [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: {
    [ProcessStatusEnum.APPROVED]: ['READ', 'DOWNLOAD', 'VOIDED'],
    [ProcessStatusEnum.APPROVING]: ['READ', 'VOIDED'],
    [ProcessStatusEnum.UNAPPROVED]: ['READ', 'UPDATE', 'DELETE', 'VOIDED'],
    [ProcessStatusEnum.REVOKED]: ['READ', 'UPDATE', 'DELETE', 'VOIDED'],
    [ProcessStatusEnum.PENDING]: ['READ', 'UPDATE', 'DELETE', 'VOIDED'],
  },
  [FormDesignKeyEnum.CONTRACT]: {
    [ProcessStatusEnum.APPROVED]: ['READ', 'EXPORT', 'STAGE', 'PAYMENT'],
    [ProcessStatusEnum.APPROVING]: ['READ', 'PAYMENT'],
    [ProcessStatusEnum.UNAPPROVED]: ['READ', 'UPDATE', 'DELETE', 'STAGE', 'PAYMENT'],
    [ProcessStatusEnum.REVOKED]: ['READ', 'UPDATE', 'DELETE', 'STAGE', 'PAYMENT'],
    [ProcessStatusEnum.PENDING]: ['READ', 'UPDATE', 'DELETE', 'STAGE', 'PAYMENT'],
  },
  [FormDesignKeyEnum.ORDER]: {
    [ProcessStatusEnum.APPROVED]: ['READ', 'DOWNLOAD'],
    [ProcessStatusEnum.APPROVING]: ['READ'],
    [ProcessStatusEnum.UNAPPROVED]: ['READ', 'UPDATE', 'DELETE'],
    [ProcessStatusEnum.REVOKED]: ['READ', 'UPDATE', 'DELETE'],
    [ProcessStatusEnum.PENDING]: ['READ', 'UPDATE', 'DELETE'],
  },
  [FormDesignKeyEnum.INVOICE]: {
    [ProcessStatusEnum.APPROVED]: ['READ', 'EXPORT'],
    [ProcessStatusEnum.APPROVING]: ['READ'],
    [ProcessStatusEnum.UNAPPROVED]: ['READ', 'UPDATE', 'DELETE'],
    [ProcessStatusEnum.REVOKED]: ['READ', 'UPDATE', 'DELETE'],
    [ProcessStatusEnum.PENDING]: ['READ', 'UPDATE', 'DELETE'],
  },
};

export function matchPermissionBySuffix(permissionId: string, suffix: PermissionSuffix) {
  return permissionId === suffix || permissionId.endsWith(`:${suffix}`);
}
