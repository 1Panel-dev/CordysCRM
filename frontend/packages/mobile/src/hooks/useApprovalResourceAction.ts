import { type MaybeRef, unref } from 'vue';
import { showConfirmDialog, showSuccessToast } from 'vant';

import { useI18n } from '@lib/shared/hooks/useI18n';

import { reviewResource, revokeResource } from '@/api/modules';

interface ApprovalResourceActionHandlers {
  onSuccess?: (resourceId: string) => void | Promise<void>;
  onError?: (error: unknown) => void | Promise<void>;
}

interface UseApprovalResourceActionOptions {
  formKey: MaybeRef<string>;
  preventDuplicateByResourceId?: boolean;
}

export default function useApprovalResourceAction(options: UseApprovalResourceActionOptions) {
  const { t } = useI18n();
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
        formKey: unref(options.formKey),
      });
      showSuccessToast(t('common.reviewSuccess'));
      await callback?.onSuccess?.(resourceId);
    } catch (error) {
      await callback?.onError?.(error);
      // eslint-disable-next-line no-console
      console.log(error);
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
        formKey: unref(options.formKey),
      });
      showSuccessToast(t('common.revokeSuccess'));
      await callback?.onSuccess?.(resourceId);
    } catch (error) {
      await callback?.onError?.(error);
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      revokeLoading.value = false;
    }
  }

  async function reviewByResourceId(resourceId: string, callback?: ApprovalResourceActionHandlers) {
    await submitReview(resourceId, callback);
  }

  async function revokeByResourceId(resourceId: string, callback?: ApprovalResourceActionHandlers) {
    if (!resourceId) {
      return;
    }

    showConfirmDialog({
      title: t('crm.approval.cancelApprovalConfirm'),
      message: t('crm.approval.cancelApprovalTip'),
      confirmButtonText: t('common.confirm'),
      confirmButtonColor: 'var(--error-red)',
      beforeClose: async (action) => {
        if (action !== 'confirm') {
          return Promise.resolve(true);
        }

        await submitRevoke(resourceId, callback);
        return Promise.resolve(true);
      },
    });
  }

  return {
    reviewLoading,
    revokeLoading,
    reviewByResourceId,
    revokeByResourceId,
  };
}
