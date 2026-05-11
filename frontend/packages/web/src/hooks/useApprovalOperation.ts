import { type Ref, ref } from 'vue';

import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ProcessStatusEnum } from '@lib/shared/enums/process';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { ApprovalPermissionsDetail, StatusPermissions } from '@lib/shared/models/system/process';

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
  getApprovalStatus?: (row: Row) => ProcessStatusEnum;
  getBizStatus?: (row: Row) => string | undefined;
  identityResolver?: {
    isApplicant?: (row: Row, currentUserId: string) => boolean;
  };
  specialActionFilter?: (row: Row, actionKeys: string[]) => string[];
  shouldUseRolePermissionOnly?: (row: Row) => boolean; // 应该回退成“只按角色权限”处理，例如报价单作废状态下，但是审批状态还是审批中，此刻按照角色权限处理只展示删除
}

function buildStatusPermissionMap(statusPermissions: StatusPermissions[]) {
  const permissionMap = new Map<ProcessStatusEnum, Set<string>>();

  statusPermissions.forEach((item) => {
    if (!permissionMap.has(item.approvalStatus)) {
      permissionMap.set(item.approvalStatus, new Set<string>());
    }

    if (!item.enabled) {
      return;
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
  const statusPermissionMap = ref<Map<ProcessStatusEnum, Set<string>>>(new Map());

  const enableApproval = ref(false);

  function getApprovalStatus(row: Row) {
    return options.getApprovalStatus?.(row) ?? (row.approvalStatus as ProcessStatusEnum);
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

  function shouldUseRolePermissionOnly(row: Row) {
    return options.shouldUseRolePermissionOnly?.(row) ?? false;
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
      return Object.values(dataActionMap).filter((action) => {
        if (!action.key) {
          return false;
        }

        return !action.permission?.length || hasAnyPermission(action.permission);
      });
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

  function hasStatusPermissions(row: Row, permissions: string[]) {
    const currentStatusPermissions = statusPermissionMap.value.get(getApprovalStatus(row));

    if (!currentStatusPermissions) {
      return false;
    }

    return permissions.some((permission) => currentStatusPermissions.has(permission));
  }

  // 获取对应数据权限是否允许操作
  function hasApprovalScopedPermission(row: Row, permissions: string[]) {
    if (!permissions.length) {
      return false;
    }

    const hasRolePermission = hasAnyPermission(permissions);

    if (!enableApproval.value || shouldUseRolePermissionOnly(row)) {
      return hasRolePermission;
    }

    const currentStatusPermissions = statusPermissionMap.value.get(getApprovalStatus(row));

    if (!currentStatusPermissions) {
      return hasRolePermission;
    }

    return hasStatusPermissions(row, permissions) && hasRolePermission;
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
    const actions =
      !enableApproval.value || shouldUseRolePermissionOnly(row)
        ? createNormalActions(row)
        : [...createApprovalActions(row), ...createDataPermissionActions(row)];

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
    hasApprovalScopedPermission,
    approvalPermissionsDetail,
    statusPermissionMap,
    getApprovalStatus,
    getBizStatus,
    enableApproval,
  };
}
