import { computed, type Ref, ref } from 'vue';

import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ProcessStatusEnum } from '@lib/shared/enums/process';
import { useI18n } from '@lib/shared/hooks/useI18n';

import { loadApprovalConfig } from '@/hooks/useApprovalConfigCache';
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
  approved?: boolean;
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
  const hiddenAction: FormReviewAction = {
    visible: false,
    text: '',
  };
  const enabledApproval = ref(false);
  const createExecute = ref(false);
  const updateExecute = ref(false);
  const approvalFormKeys = [
    FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
    FormDesignKeyEnum.CONTRACT,
    FormDesignKeyEnum.ORDER,
    FormDesignKeyEnum.INVOICE,
  ];

  function canShowByExecuteTiming(params: GetFormReviewActionParams) {
    if (!params.enabledApproval) {
      return false;
    }

    if (!params.isEdit) {
      return params.createExecute;
    }

    return params.updateExecute && !params.approved && params.createExecute;
  }

  function canShowByIdentity(params: GetFormReviewActionParams) {
    return !params.isEdit || params.canReview;
  }

  function getReviewActionTextByStatus(params: GetFormReviewActionParams) {
    const approvalStatus = params.approvalStatus ?? ProcessStatusEnum.NONE;

    if (!params.isEdit || approvalStatus === ProcessStatusEnum.PENDING) {
      return t('common.review');
    }

    if ([ProcessStatusEnum.REVOKED, ProcessStatusEnum.UNAPPROVED].includes(approvalStatus)) {
      return t('common.resubmit');
    }

    return '';
  }

  function getFormReviewAction(params: GetFormReviewActionParams): FormReviewAction {
    if (!canShowByExecuteTiming(params)) {
      return hiddenAction;
    }

    if (!canShowByIdentity(params)) {
      return hiddenAction;
    }

    const text = getReviewActionTextByStatus(params);

    if (!text) {
      return hiddenAction;
    }

    return {
      visible: true,
      text,
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
      approved: options.detail?.value?.approved,
      createExecute: createExecute.value,
      updateExecute: updateExecute.value,
    })
  );

  const shouldConfirmUpdateChange = computed(
    () =>
      isApprovalForm.value &&
      enabledApproval.value &&
      updateExecute.value &&
      Boolean(options.isEdit.value) &&
      Boolean(options.detail?.value?.approved)
  );

  async function initApprovalReviewConfig() {
    if (!isApprovalForm.value) {
      enabledApproval.value = false;
      createExecute.value = false;
      updateExecute.value = false;
      return;
    }

    try {
      const result = await loadApprovalConfig(options.formKey.value);
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
    shouldConfirmUpdateChange,
    getFormReviewAction,
    reviewAction,
    initApprovalReviewConfig,
  };
}
