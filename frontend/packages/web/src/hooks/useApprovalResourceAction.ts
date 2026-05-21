import { useMessage } from 'naive-ui';

import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

import { reviewResource, revokeResource } from '@/api/modules';
import useModal from '@/hooks/useModal';

interface ApprovalResourceActionHandlers {
  onSuccess?: (resourceId: string) => void | Promise<void>;
  onError?: (error: unknown) => void | Promise<void>;
}

interface UseApprovalResourceActionOptions {
  formKey: FormDesignKeyEnum;
  preventDuplicateByResourceId?: boolean;
}

export default function useApprovalResourceAction(options: UseApprovalResourceActionOptions) {
  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const enableDuplicateGuard = options.preventDuplicateByResourceId ?? true;

  const reviewLoading = ref(false);
  const revokeLoading = ref(false);
  const reviewPendingResourceIds = new Set<string>();

  async function submitReview(resourceId: string, callback?: ApprovalResourceActionHandlers) {
    if (!resourceId) {
      return;
    }

    if (enableDuplicateGuard && reviewPendingResourceIds.has(resourceId)) {
      return;
    }

    try {
      reviewPendingResourceIds.add(resourceId);
      reviewLoading.value = true;
      await reviewResource({
        resourceId,
        formKey: options.formKey,
      });
      Message.success(t('common.reviewSuccess'));
      await callback?.onSuccess?.(resourceId);
    } catch (error) {
      await callback?.onError?.(error);
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      reviewPendingResourceIds.delete(resourceId);
      reviewLoading.value = false;
    }
  }

  async function submitRevoke(resourceId: string, callback?: ApprovalResourceActionHandlers) {
    if (!resourceId) {
      return;
    }

    try {
      revokeLoading.value = true;
      await revokeResource({
        resourceId,
        formKey: options.formKey,
      });
      Message.success(t('common.revokeSuccess'));
      await callback?.onSuccess?.(resourceId);
    } catch (error) {
      await callback?.onError?.(error);
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      revokeLoading.value = false;
    }
  }

  async function reviewByFormResult(res: { id: string }, callback?: ApprovalResourceActionHandlers) {
    await submitReview(res?.id, callback);
  }

  async function reviewByResourceId(resourceId: string, callback?: ApprovalResourceActionHandlers) {
    await submitReview(resourceId, callback);
  }

  async function revokeByResourceId(resourceId: string, callback?: ApprovalResourceActionHandlers) {
    if (!resourceId) {
      return;
    }

    openModal({
      type: 'error',
      title: t('crm.approval.cancelApprovalConfirm'),
      content: t('crm.approval.cancelApprovalTip'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        await submitRevoke(resourceId, callback);
      },
    });
  }

  return {
    reviewLoading,
    revokeLoading,
    reviewByFormResult,
    reviewByResourceId,
    revokeByResourceId,
  };
}
