import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ProcessStatusEnum } from '@lib/shared/enums/process';
import { ProcessStatusType } from '@lib/shared/models/system/process';

import { ApproverItem } from '@/components/business/crm-approver-avatar-list/index.vue';

export type ApprovalPopoverFormKeyType =
  | FormDesignKeyEnum.CONTRACT
  | FormDesignKeyEnum.INVOICE
  | FormDesignKeyEnum.OPPORTUNITY_QUOTATION
  | FormDesignKeyEnum.ORDER;

export interface ApprovalPopoverDetail {
  resourceId: string;
  approveStatus: ProcessStatusType;
  approveUserList: ApproverItem[];
}

// todo api
const detailApiMap: Record<ApprovalPopoverFormKeyType, (sourceId: string) => Promise<ApprovalPopoverDetail>> = {
  [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: () =>
    Promise.resolve({ approveStatus: ProcessStatusEnum.NONE, approveUserList: [], resourceId: '' }),
  [FormDesignKeyEnum.CONTRACT]: (sourceId: string) =>
    Promise.resolve({
      approveStatus: ProcessStatusEnum.NONE,
      approveUserList: [],
      resourceId: '',
    }),
  [FormDesignKeyEnum.INVOICE]: (sourceId: string) =>
    Promise.resolve({
      approveStatus: ProcessStatusEnum.NONE,
      approveUserList: [],
      resourceId: '',
    }),
  [FormDesignKeyEnum.ORDER]: (sourceId: string) =>
    Promise.resolve({
      approveStatus: ProcessStatusEnum.NONE,
      approveUserList: [],
      resourceId: '',
    }),
};

function transformDetail(detail: ApprovalPopoverDetail): ApproverItem[] {
  return detail.approveUserList.map((e) => {
    return {
      id: e.id,
      name: e.name,
      avatar: e.avatar ?? '',
      approveResult: e.approveResult,
      approveReason: e.approveReason,
    };
  });
}

export default function useApprovalPopoverDetail() {
  async function getApprovalPopoverDetail(formKey: ApprovalPopoverFormKeyType, sourceId: string) {
    const api = detailApiMap[formKey];
    if (!api) {
      return {
        approveUserList: [],
      };
    }

    const detail = await api(sourceId);
    return {
      ...detail,
      approveUserList: transformDetail(detail),
    };
  }

  return {
    getApprovalPopoverDetail,
  };
}
