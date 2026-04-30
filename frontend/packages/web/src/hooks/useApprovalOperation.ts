import { type Ref, ref } from 'vue';

import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ProcessStatusEnum } from '@lib/shared/enums/process';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { ApprovalPermissionsDetail, ProcessStatusType, StatusPermissions } from '@lib/shared/models/system/process';

import type { ActionsItem } from '@/components/pure/crm-more-action/type';

import { getApprovalConfigDetail } from '@/api/modules';
import { useUserStore } from '@/store';
import { hasAnyPermission } from '@/utils/permission';

export type ApprovalConfigType =
  | FormDesignKeyEnum.CONTRACT
  | FormDesignKeyEnum.INVOICE
  | FormDesignKeyEnum.ORDER
  | FormDesignKeyEnum.OPPORTUNITY_QUOTATION;

export interface UseApprovalOperationOptions<Row extends Record<string, any>> {
  formType: ApprovalConfigType;
  dataActionMap: Record<string, ActionsItem> | ((row: Row) => Record<string, ActionsItem>);
  isDetail?: boolean;
  maxVisibleActions?: number;
  getApprovalStatus?: (row: Row) => ProcessStatusType;
  getBizStatus?: (row: Row) => string | undefined;
  identityResolver?: {
    isApplicant?: (row: Row, currentUserId: string) => boolean;
    isApprover?: (row: Row, currentUserId: string) => boolean;
  };
  specialActionFilter?: (row: Row, actionKeys: string[]) => string[];
}

function buildStatusPermissionMap(statusPermissions: StatusPermissions[]) {
  const permissionMap = new Map<ProcessStatusType, Set<string>>();

  statusPermissions.forEach((item) => {
    if (!item.enabled) {
      return;
    }

    if (!permissionMap.has(item.approvalStatus)) {
      permissionMap.set(item.approvalStatus, new Set<string>());
    }

    permissionMap.get(item.approvalStatus)?.add(item.permission);
  });

  return permissionMap;
}

export default function useApprovalOperation<Row extends Record<string, any>>(
  options: UseApprovalOperationOptions<Row>
) {
  const { t } = useI18n();
  const userStore = useUserStore();

  const approvalPermissionsDetail = ref<ApprovalPermissionsDetail | null>(null);
  const statusPermissionMap = ref<Map<ProcessStatusType, Set<string>>>(new Map());

  const enableApproval = ref(false);

  function getApprovalStatus(row: Row) {
    return options.getApprovalStatus?.(row) ?? (row.approvalStatus as ProcessStatusType);
  }

  function getBizStatus(row: Row) {
    return options.getBizStatus?.(row) ?? row.status;
  }

  function getDataActionMap(row: Row) {
    return typeof options.dataActionMap === 'function' ? options.dataActionMap(row) : options.dataActionMap;
  }

  function isApplicant(row: Row) {
    return (
      options.identityResolver?.isApplicant?.(row, userStore.userInfo.id) ?? row.createUser === userStore.userInfo.id
    );
  }

  function isApprover(row: Row) {
    return options.identityResolver?.isApprover?.(row, userStore.userInfo.id) ?? false;
  }

  function createApprovalActions(row: Row): ActionsItem[] {
    const approvalStatus = getApprovalStatus(row);

    switch (approvalStatus) {
      case ProcessStatusEnum.PENDING:
        // 返回提审
        return [
          {
            label: t('common.review'),
            key: 'review',
          },
        ];
      case ProcessStatusEnum.UNAPPROVED:
      case ProcessStatusEnum.REVOKED:
        return [
          {
            label: t('common.resubmit'),
            key: 'review',
          },
        ];
      case ProcessStatusEnum.APPROVING:
        return [
          ...(options.isDetail && isApprover(row)
            ? [
                {
                  label: t('common.approve'),
                  key: 'pass',
                },
                {
                  label: t('common.reject'),
                  key: 'unPass',
                  danger: true,
                },
              ]
            : []),
          ...(isApplicant(row)
            ? [
                {
                  label: t('common.revoke'),
                  key: 'revoke',
                },
              ]
            : []),
        ];
      case ProcessStatusEnum.APPROVED:
      default:
        return [];
    }
  }

  function createDataPermissionActions(row: Row): ActionsItem[] {
    const currentStatusPermissions = statusPermissionMap.value.get(getApprovalStatus(row));
    const dataActionMap = getDataActionMap(row);

    if (!currentStatusPermissions) {
      return [];
    }

    return Object.values(dataActionMap).filter((action) => {
      if (!action.key || !action.permission?.length) {
        return false;
      }

      return (
        action.permission.some((permissionId) => currentStatusPermissions.has(permissionId)) &&
        hasAnyPermission(action.permission)
      );
    });
  }

  function createNormalActions(row: Row): ActionsItem[] {
    const dataActionMap = getDataActionMap(row);

    return Object.values(dataActionMap).filter((action) => {
      if (!action.key) {
        return false;
      }

      return !action.permission?.length || hasAnyPermission(action.permission);
    });
  }

  function applySpecialActionFilter(row: Row, actions: ActionsItem[]) {
    const filteredKeys = options.specialActionFilter?.(
      row,
      actions.map((item) => item.key as string)
    );

    if (!filteredKeys) {
      return actions;
    }

    return actions.filter((item) => filteredKeys.includes(item.key as string));
  }

  function splitActions(actions: ActionsItem[]) {
    // 默认最多展示3个操作
    const maxVisibleActions = options.maxVisibleActions ?? 3;
    const realVisibleNumber = maxVisibleActions - 1;
    const newActions = options.isDetail
      ? actions.map((e) => ({ ...e, danger: ['delete', 'unPass'].includes(e.key ?? '') }))
      : actions;

    if (actions.length <= realVisibleNumber) {
      return {
        groupList: newActions,
        moreList: [] as ActionsItem[],
      };
    }

    const moreList = newActions.slice(realVisibleNumber);

    return {
      groupList: [
        ...newActions.slice(0, realVisibleNumber),
        ...(moreList.length === 1
          ? moreList
          : [
              {
                label: 'more',
                key: 'more',
                slotName: 'more',
              },
            ]),
      ],
      moreList: moreList.length === 1 ? [] : newActions.slice(realVisibleNumber),
    };
  }

  function resolveRowActions(row: Row) {
    const actions = enableApproval.value
      ? [...createApprovalActions(row), ...createDataPermissionActions(row)]
      : createNormalActions(row);

    return applySpecialActionFilter(row, actions);
  }

  function resolveRowOperation(row: Row) {
    return splitActions(resolveRowActions(row));
  }

  async function initApprovalPermission() {
    try {
      const result = await getApprovalConfigDetail(options.formType);

      if (result) {
        approvalPermissionsDetail.value = result;
        enableApproval.value = result.enable;
        statusPermissionMap.value = buildStatusPermissionMap(result.statusPermissions);
      } else {
        approvalPermissionsDetail.value = result;
        enableApproval.value = false;
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  return {
    initApprovalPermission,
    resolveRowActions,
    resolveRowOperation,
    approvalPermissionsDetail,
    statusPermissionMap,
    getApprovalStatus,
    getBizStatus,
    enableApproval,
  };
}
