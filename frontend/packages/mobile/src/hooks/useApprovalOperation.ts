import { type MaybeRef, unref } from 'vue';

import { ProcessStatusEnum } from '@lib/shared/enums/process';
import { useI18n } from '@lib/shared/hooks/useI18n';
import type { ApprovalProcessDetail, StatusPermissions } from '@lib/shared/models/system/process';

import { loadApprovalConfig } from '@/hooks/useApprovalConfigCache';
import useUserStore from '@/store/modules/user';
import { hasAnyPermission } from '@/utils/permission';

interface ApprovalActionLike {
  key: string;
  label?: string;
  text?: string;
  icon?: string;
  color?: string;
  permission?: string[];
  allPermission?: boolean;
  action?: (row: any) => void;
  [key: string]: any;
}

interface UseApprovalOperationOptions<Row extends Record<string, any>> {
  formType: MaybeRef<string>;
  getApprovalStatus?: (row: Row) => ProcessStatusEnum | undefined;
  identityResolver?: {
    isApplicant?: (row: Row, currentUserId: string) => boolean;
    isOwner?: (row: Row, currentUserId: string) => boolean;
  };
  shouldUseRolePermissionOnly?: (row: Row) => boolean;
  onReview?: (row: Row) => void;
  onRevoke?: (row: Row) => void;
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

  const approvalConfig = ref<ApprovalProcessDetail | null>(null);
  const statusPermissionMap = ref<Map<ProcessStatusEnum, Set<string>>>(new Map());

  const enableApproval = ref(false);
  const createExecute = ref(false);
  const updateExecute = ref(false);
  const deleteExecute = ref(false);
  let approvalPermissionRequestToken = 0;

  function getApprovalStatus(row: Row) {
    return options.getApprovalStatus?.(row) ?? row.approvalStatus ?? ProcessStatusEnum.NONE;
  }

  function hasRolePermission(permissions: string[]) {
    return !permissions.length || hasAnyPermission(permissions);
  }

  function isApplicant(row: Row) {
    const isCreator =
      options.identityResolver?.isApplicant?.(row, userStore.userInfo.id) ?? row.createUser === userStore.userInfo.id;
    const isOwner =
      options.identityResolver?.isOwner?.(row, userStore.userInfo.id) ?? row.owner === userStore.userInfo.id;

    return isCreator || isOwner;
  }

  function canCurrentUserRevoke(row: Row) {
    return row.owner === userStore.userInfo.id || row.submitterId === userStore.userInfo.id;
  }

  function shouldUseRolePermissionOnly(row?: Row) {
    if (!row) {
      return false;
    }
    return options.shouldUseRolePermissionOnly?.(row) ?? false;
  }

  function canRevokeWhileApproving(row: Row) {
    if (!canCurrentUserRevoke(row)) {
      return false;
    }

    if (approvalConfig.value?.submitterCanRevoke) {
      return true;
    }

    return !row.firstApproved;
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

  function createApprovalActions(row: Row): ApprovalActionLike[] {
    const approvalStatus = getApprovalStatus(row);
    const canReview = canShowReviewAction(row);

    switch (approvalStatus) {
      case ProcessStatusEnum.PENDING:
        return canReview
          ? [
              {
                key: 'review',
                label: t('common.review'),
                text: t('common.review'),
                icon: 'iconicon_check_circle',
                action: () => options.onReview?.(row),
              },
            ]
          : [];
      case ProcessStatusEnum.UNAPPROVED:
      case ProcessStatusEnum.REVOKED:
        return canReview
          ? [
              {
                key: 'review',
                label: t('common.resubmit'),
                text: t('common.resubmit'),
                icon: 'iconicon_check_circle',
                action: () => options.onReview?.(row),
              },
            ]
          : [];
      case ProcessStatusEnum.APPROVING:
        return canRevokeWhileApproving(row)
          ? [
              {
                key: 'revoke',
                label: t('common.revoke'),
                text: t('common.revoke'),
                icon: 'iconicon_skip_planarity',
                action: () => options.onRevoke?.(row),
              },
            ]
          : [];
      case ProcessStatusEnum.APPROVED:
      default:
        return [];
    }
  }

  function hasApprovalScopedPermission(row: Row, permissions: string[]) {
    const hasCurrentRolePermission = hasRolePermission(permissions);

    if (!enableApproval.value || shouldUseRolePermissionOnly(row)) {
      return hasCurrentRolePermission;
    }

    const currentStatusPermissions = statusPermissionMap.value.get(getApprovalStatus(row));

    if (!currentStatusPermissions) {
      return hasCurrentRolePermission;
    }

    return permissions.some((permission) => currentStatusPermissions.has(permission)) && hasCurrentRolePermission;
  }

  function resolveDataActions<T extends ApprovalActionLike>(row: Row, actions: T[]) {
    return actions.filter((action) => {
      return hasApprovalScopedPermission(row, action.permission ?? []);
    });
  }

  function resolveRowActions<T extends ApprovalActionLike>(row: Row, actions: T[]) {
    const approvalActions = enableApproval.value && !shouldUseRolePermissionOnly(row) ? createApprovalActions(row) : [];

    return [...approvalActions, ...resolveDataActions(row, actions)] as T[];
  }

  async function initApprovalPermission(forceRefresh = false) {
    const requestToken = ++approvalPermissionRequestToken;
    try {
      const result = await loadApprovalConfig(unref(options.formType), forceRefresh);
      if (requestToken !== approvalPermissionRequestToken) {
        return;
      }

      approvalConfig.value = result;
      enableApproval.value = Boolean(result?.enable);
      createExecute.value = Boolean(result?.createExecute);
      updateExecute.value = Boolean(result?.updateExecute);
      deleteExecute.value = Boolean(result?.deleteExecute);
      statusPermissionMap.value = buildStatusPermissionMap(result?.statusPermissions ?? []);
    } catch (error) {
      if (requestToken !== approvalPermissionRequestToken) {
        return;
      }
      approvalConfig.value = null;
      enableApproval.value = false;
      createExecute.value = false;
      updateExecute.value = false;
      deleteExecute.value = false;
      statusPermissionMap.value = new Map();
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  return {
    approvalConfig,
    enableApproval,
    createExecute,
    updateExecute,
    deleteExecute,
    hasApprovalScopedPermission,
    resolveRowActions,
    initApprovalPermission,
  };
}
