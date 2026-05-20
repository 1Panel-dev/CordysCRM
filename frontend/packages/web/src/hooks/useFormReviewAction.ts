import { computed, type Ref, ref } from 'vue';

import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ProcessStatusEnum } from '@lib/shared/enums/process';
import { useI18n } from '@lib/shared/hooks/useI18n';

import { getApprovalConfigDetail } from '@/api/modules';
import { useUserStore } from '@/store';

export interface FormReviewAction {
  visible: boolean;
  text: string;
}

export interface GetFormReviewActionParams {
  enabledApproval: boolean;
  isEdit: boolean;
  approvalStatus?: ProcessStatusEnum;
  canReview: boolean;
  createExecute: boolean;
  updateExecute: boolean;
}

interface UseFormReviewActionOptions {
  formKey: Ref<FormDesignKeyEnum>;
  isEdit: Ref<boolean | undefined>;
  approvalStatus: Ref<ProcessStatusEnum | undefined>;
  detail?: Ref<Record<string, any> | undefined>;
}

export default function useFormReviewAction(options: UseFormReviewActionOptions) {
  const { t } = useI18n();
  const userStore = useUserStore();
  const enabledApproval = ref(false);
  const createExecute = ref(false);
  const updateExecute = ref(false);
  const approvalFormKeys = [
    FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
    FormDesignKeyEnum.CONTRACT,
    FormDesignKeyEnum.ORDER,
    FormDesignKeyEnum.INVOICE,
  ];

  function getFormReviewAction(params: GetFormReviewActionParams): FormReviewAction {
    const {
      isEdit,
      approvalStatus,
      canReview,
      createExecute: canCreateReview,
      updateExecute: canUpdateReview,
    } = params;

    if (!enabledApproval.value) {
      return {
        visible: false,
        text: '',
      };
    }

    if (!isEdit && !canCreateReview) {
      return {
        visible: false,
        text: '',
      };
    }

    if (isEdit && !canUpdateReview) {
      return {
        visible: false,
        text: '',
      };
    }

    if (isEdit && !canReview) {
      return {
        visible: false,
        text: '',
      };
    }

    if (!isEdit || approvalStatus === ProcessStatusEnum.PENDING || !approvalStatus) {
      return {
        visible: true,
        text: t('common.review'),
      };
    }

    if ([ProcessStatusEnum.REVOKED, ProcessStatusEnum.UNAPPROVED].includes(approvalStatus || ProcessStatusEnum.NONE)) {
      return {
        visible: true,
        text: t('common.resubmit'),
      };
    }

    return {
      visible: false,
      text: '',
    };
  }

  const isApprovalForm = computed(() => approvalFormKeys.includes(options.formKey.value));

  const reviewAction = computed(() =>
    getFormReviewAction({
      enabledApproval: enabledApproval.value && isApprovalForm.value,
      isEdit: Boolean(options.isEdit.value),
      approvalStatus: options.isEdit.value
        ? options.approvalStatus.value
        : options.approvalStatus.value ?? ProcessStatusEnum.PENDING,
      canReview:
        !options.isEdit.value ||
        options.detail?.value?.createUser === userStore.userInfo.id ||
        options.detail?.value?.owner === userStore.userInfo.id,
      createExecute: createExecute.value,
      updateExecute: updateExecute.value,
    })
  );

  async function initApprovalReviewConfig() {
    if (!isApprovalForm.value) {
      enabledApproval.value = false;
      return;
    }

    try {
      const result = await getApprovalConfigDetail(options.formKey.value);
      enabledApproval.value = Boolean(result?.enable);
      createExecute.value = Boolean(result?.createExecute);
      updateExecute.value = Boolean(result?.updateExecute);
    } catch (error) {
      enabledApproval.value = false;
      createExecute.value = false;
      updateExecute.value = false;
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  return {
    enabledApproval,
    createExecute,
    updateExecute,
    isApprovalForm,
    getFormReviewAction,
    reviewAction,
    initApprovalReviewConfig,
  };
}
