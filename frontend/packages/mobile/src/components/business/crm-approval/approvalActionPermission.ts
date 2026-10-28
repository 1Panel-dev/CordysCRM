import { MultiApproverModeEnum, ProcessStatusEnum } from '@lib/shared/enums/process';
import type { ApprovalDetail, ApprovalNode, ApprovalProcessDetail } from '@lib/shared/models/system/process';

interface ApprovalActionPermissionParams {
  approvalInfo?: ApprovalDetail;
  approvalConfig?: ApprovalProcessDetail;
  currentApprovalNode?: ApprovalNode;
  currentApprovalNodeIndex: number;
  userId: string;
}

export default function resolveApprovalActionPermission(params: ApprovalActionPermissionParams) {
  const { approvalInfo, approvalConfig, currentApprovalNode, currentApprovalNodeIndex, userId } = params;
  const completedStatuses = [ProcessStatusEnum.APPROVED, ProcessStatusEnum.UNAPPROVED, ProcessStatusEnum.NONE];
  const isApprover =
    currentApprovalNode?.multiApproverMode === MultiApproverModeEnum.SEQUENTIAL
      ? currentApprovalNode.taskNodes?.find((item) => item.approvalStatus === ProcessStatusEnum.APPROVING)
          ?.approverId === userId
      : currentApprovalNode?.taskNodes?.some(
          (item) => item.approvalStatus === ProcessStatusEnum.APPROVING && item.approverId === userId
        ) ?? false;

  const canCancelApply = (() => {
    if (!approvalInfo || !currentApprovalNode || currentApprovalNode.endNode || approvalInfo.submitterId !== userId) {
      return false;
    }
    if (!approvalConfig?.submitterCanRevoke && approvalInfo.nodes[0].approvalStatus !== ProcessStatusEnum.APPROVING) {
      return false;
    }
    if (
      !approvalInfo.nodes.some((node) =>
        [ProcessStatusEnum.APPROVED, ProcessStatusEnum.UNAPPROVED].includes(node.approvalStatus)
      )
    ) {
      return true;
    }
    return (
      approvalInfo.nodes[0].approvalStatus === ProcessStatusEnum.APPROVED && Boolean(approvalConfig?.submitterCanRevoke)
    );
  })();

  const canCancelApproval = (() => {
    if (!approvalConfig?.allowWithdraw || !currentApprovalNode || currentApprovalNode.endNode) {
      return false;
    }
    const previousOwnTaskIndex = currentApprovalNode.taskNodes?.findIndex(
      (item) => completedStatuses.includes(item.approvalStatus) && item.approverId === userId
    );
    if (previousOwnTaskIndex !== -1) {
      if (currentApprovalNode.multiApproverMode === MultiApproverModeEnum.SEQUENTIAL) {
        const currentTaskIndex = currentApprovalNode.taskNodes?.findIndex(
          (item) => item.approvalStatus === ProcessStatusEnum.APPROVING
        );
        return currentApprovalNode.taskNodes?.[Number(currentTaskIndex) - 1]?.approverId === userId;
      }
      return true;
    }
    const previousNode = approvalInfo?.nodes[currentApprovalNodeIndex - 1];
    const followingTasksArePending = currentApprovalNode.taskNodes?.every((item) =>
      [ProcessStatusEnum.APPROVING, ProcessStatusEnum.PENDING].includes(item.approvalStatus)
    );
    if (previousNode?.taskNodes?.length === 1) {
      return previousNode.taskNodes[0].approverId === userId && followingTasksArePending;
    }
    if (previousNode?.multiApproverMode === MultiApproverModeEnum.ANY) {
      return (
        previousNode.taskNodes.some(
          (item) => completedStatuses.includes(item.approvalStatus) && item.approverId === userId
        ) && followingTasksArePending
      );
    }
    return false;
  })();

  const canFallback = currentApprovalNodeIndex > 0 && isApprover;
  const canAddSign = Boolean(approvalConfig?.allowAddSign && isApprover);
  const showActions = Boolean(
    approvalInfo?.approvalStatus === ProcessStatusEnum.APPROVING &&
      (isApprover || canFallback || canAddSign || canCancelApproval || canCancelApply)
  );

  return {
    isApprover,
    canFallback,
    canAddSign,
    canCancelApproval,
    canCancelApply,
    showActions,
  };
}
