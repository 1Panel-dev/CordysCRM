import { type Ref, ref } from 'vue';

import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ProcessStatusEnum } from '@lib/shared/enums/process';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { ApprovalProcessDetail, StatusPermissions } from '@lib/shared/models/system/process';

import type { ActionsItem } from '@/components/pure/crm-more-action/type';

import { processStatusOptions } from '@/config/process';
import { loadApprovalConfig } from '@/hooks/useApprovalConfigCache';
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
    isOwner?: (row: Row, currentUserId: string) => boolean;
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

  const approvalPermissionsDetail = ref<ApprovalProcessDetail | null>(null);
  const statusPermissionMap = ref<Map<ProcessStatusEnum, Set<string>>>(new Map());

  const enableApproval = ref(false);
  const createExecute = ref(false);
  const updateExecute = ref(false);
  const deleteExecute = ref(false);

  function getApprovalStatus(row: Row) {
    if (!row) {
      return ProcessStatusEnum.NONE;
    }
    return options.getApprovalStatus?.(row) ?? (row.approvalStatus as ProcessStatusEnum);
  }

  function getBizStatus(row: Row) {
    return options.getBizStatus?.(row) ?? row.status;
  }

  function getDataActionMap(row: Row) {
    return typeof options.dataActionMap === 'function' ? options.dataActionMap(row) : options.dataActionMap;
  }

  function isApplicant(row: Row) {
    const isCreator =
      options.identityResolver?.isApplicant?.(row, userStore.userInfo.id) ?? row.createUser === userStore.userInfo.id;
    const isOwner =
      options.identityResolver?.isOwner?.(row, userStore.userInfo.id) ?? row.owner === userStore.userInfo.id;

    return isCreator || isOwner;
  }

  function shouldUseRolePermissionOnly(row?: Row) {
    if (!row) {
      return false;
    }
    return options?.shouldUseRolePermissionOnly?.(row) ?? false;
  }

  function canRevokeWhileApproving(row: Row) {
    if (!isApplicant(row)) {
      return false;
    }

    // 开启配置：审批中始终允许提交人撤销
    if (approvalPermissionsDetail.value?.submitterCanRevoke) {
      return true;
    }

    // 关闭配置：只允许第一个审批节点通过前撤销
    return !row.firstApproved;
  }

  function getReviewActionState() {
    const disabled = !enableApproval.value || !approvalPermissionsDetail.value;

    return {
      disabled,
      tooltipContent: disabled ? t('crm.approval.reviewDisabledTip') : undefined,
    };
  }

  function canShowReviewAction(row: Row) {
    if (!isApplicant(row)) {
      return false;
    }

    if (updateExecute.value) {
      return createExecute.value && !row.approved;
    }

    return createExecute.value;
  }

  function createApprovalActions(row: Row): ActionsItem[] {
    const approvalStatus = getApprovalStatus(row);
    const reviewActionState = getReviewActionState();
    const canReview = canShowReviewAction(row);

    switch (approvalStatus) {
      case ProcessStatusEnum.PENDING:
        // 返回提审
        return canReview
          ? [
              {
                label: t('common.review'),
                key: 'review',
                ...reviewActionState,
              },
            ]
          : [];
      case ProcessStatusEnum.UNAPPROVED:
      case ProcessStatusEnum.REVOKED:
        return canReview
          ? [
              {
                label: t('common.resubmit'),
                key: 'review',
                ...reviewActionState,
              },
            ]
          : [];
      case ProcessStatusEnum.APPROVING:
        return [
          ...(canRevokeWhileApproving(row)
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

  function getAllowedStatuses(permissions: string[]) {
    if (!enableApproval.value || !permissions.length) {
      return [] as ProcessStatusEnum[];
    }

    return processStatusOptions
      .map((item) => item.value as ProcessStatusEnum)
      .filter((status) => {
        const currentStatusPermissions = statusPermissionMap.value.get(status);

        if (!currentStatusPermissions) {
          return false;
        }

        return permissions.some((permission) => currentStatusPermissions.has(permission));
      });
  }

  function getApprovalStatusLabel(status: ProcessStatusEnum) {
    return processStatusOptions.find((item) => item.value === status)?.label || '-';
  }

  function getApprovalActionTip(permissions: string[], tipKey: string) {
    if (!enableApproval.value || !approvalPermissionsDetail.value || !hasAnyPermission(permissions)) {
      return '';
    }

    const allowedStatuses = Array.from(new Set([...getAllowedStatuses(permissions), ProcessStatusEnum.NONE]));

    if (!allowedStatuses.length) {
      return '';
    }

    const statusLabels = allowedStatuses.map(getApprovalStatusLabel).join('、');

    return statusLabels ? t(tipKey, { statuses: statusLabels }) : '';
  }

  // 获取对应数据权限是否允许操作
  function hasApprovalScopedPermission(row: Row, permissions: string[]) {
    if (!permissions.length || !row) {
      return false;
    }

    const hasRolePermission = hasAnyPermission(permissions);

    if (!enableApproval.value || shouldUseRolePermissionOnly?.(row)) {
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
    const dataActions =
      !enableApproval.value || shouldUseRolePermissionOnly(row)
        ? createNormalActions(row)
        : createDataPermissionActions(row);
    const actions = [...createApprovalActions(row), ...dataActions];

    return applySpecialActionFilter(row, actions);
  }

  function resolveRowOperation(row: Row) {
    return splitActions(resolveRowActions(row));
  }

  async function initApprovalPermission() {
    try {
      const result = await loadApprovalConfig(options.formType);

      if (result) {
        approvalPermissionsDetail.value = result;
        enableApproval.value = result.enable;
        createExecute.value = Boolean(result.createExecute);
        updateExecute.value = Boolean(result.updateExecute);
        deleteExecute.value = Boolean(result.deleteExecute);
        statusPermissionMap.value = buildStatusPermissionMap(result.statusPermissions);
      } else {
        approvalPermissionsDetail.value = result;
        enableApproval.value = false;
        createExecute.value = false;
        updateExecute.value = false;
        deleteExecute.value = false;
      }
    } catch (error) {
      createExecute.value = false;
      updateExecute.value = false;
      deleteExecute.value = false;
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  return {
    initApprovalPermission,
    resolveRowActions,
    resolveRowOperation,
    hasApprovalScopedPermission,
    getAllowedStatuses,
    getApprovalActionTip,
    approvalPermissionsDetail,
    statusPermissionMap,
    getApprovalStatus,
    getBizStatus,
    enableApproval,
    createExecute,
    updateExecute,
    deleteExecute,
  };
}
