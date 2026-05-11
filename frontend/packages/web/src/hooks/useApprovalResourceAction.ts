import { useMessage } from 'naive-ui';

import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

import { reviewResource, revokeResource } from '@/api/modules';

interface ApprovalResourceActionHandlers {
  onSuccess?: (resourceId: string) => void | Promise<void>;
  onError?: (error: unknown) => void | Promise<void>;
}

interface UseApprovalResourceActionOptions {
  formKey: FormDesignKeyEnum;
}

export default function useApprovalResourceAction(options: UseApprovalResourceActionOptions) {
  const { t } = useI18n();
  const Message = useMessage();

  const reviewLoading = ref(false);
  const revokeLoading = ref(false);

  async function submitReview(resourceId: string, callback?: ApprovalResourceActionHandlers) {
    if (!resourceId) {
      return;
    }

    try {
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
    await submitRevoke(resourceId, callback);
  }

  return {
    reviewLoading,
    revokeLoading,
    reviewByFormResult,
    reviewByResourceId,
    revokeByResourceId,
  };
}
