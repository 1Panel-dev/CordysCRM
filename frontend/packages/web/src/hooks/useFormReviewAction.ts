import { computed, type Ref, ref } from 'vue';

import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ProcessStatusEnum } from '@lib/shared/enums/process';
import { useI18n } from '@lib/shared/hooks/useI18n';

import { getApprovalConfigDetail } from '@/api/modules';

export interface FormReviewAction {
  visible: boolean;
  text: string;
}

export interface GetFormReviewActionParams {
  enabledApproval: boolean;
  isEdit: boolean;
  approvalStatus?: ProcessStatusEnum;
}

interface UseFormReviewActionOptions {
  formKey: Ref<FormDesignKeyEnum>;
  isEdit: Ref<boolean | undefined>;
  approvalStatus: Ref<ProcessStatusEnum | undefined>;
}

export default function useFormReviewAction(options: UseFormReviewActionOptions) {
  const { t } = useI18n();
  const enabledApproval = ref(false);
  const approvalFormKeys = [
    FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
    FormDesignKeyEnum.CONTRACT,
    FormDesignKeyEnum.ORDER,
    FormDesignKeyEnum.INVOICE,
  ];

  function getFormReviewAction(params: GetFormReviewActionParams): FormReviewAction {
    const { isEdit, approvalStatus } = params;

    if (!enabledApproval.value) {
      return {
        visible: false,
        text: '',
      };
    }

    if (!isEdit) {
      return {
        visible: true,
        text: t('common.review'),
      };
    }

    if (approvalStatus === ProcessStatusEnum.PENDING) {
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
    } catch (error) {
      enabledApproval.value = false;
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  return {
    enabledApproval,
    isApprovalForm,
    getFormReviewAction,
    reviewAction,
    initApprovalReviewConfig,
  };
}
