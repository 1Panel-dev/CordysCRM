import { type Ref, ref } from 'vue';

import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ProcessStatusEnum } from '@lib/shared/enums/process';
import { useI18n } from '@lib/shared/hooks/useI18n';

import { loadApprovalConfig } from '@/hooks/useApprovalConfigCache';
import useUserStore from '@/store/modules/user';

interface FormReviewAction {
  visible: boolean;
  text: string;
}

interface UseFormReviewActionOptions {
  formKey: Ref<FormDesignKeyEnum>;
  isEdit: Ref<boolean>;
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
  const approvalFormKeys = [FormDesignKeyEnum.CLUE, FormDesignKeyEnum.CUSTOMER, FormDesignKeyEnum.BUSINESS];

  const enabledApproval = ref(false);
  const createExecute = ref(false);
  const updateExecute = ref(false);

  const isApprovalForm = computed(() => approvalFormKeys.includes(options.formKey.value));

  function canShowReviewAction() {
    if (!enabledApproval.value || !isApprovalForm.value) {
      return false;
    }

    if (!options.isEdit.value) {
      return createExecute.value;
    }

    const canReview =
      options.detail?.value?.createUser === userStore.userInfo.id ||
      options.detail?.value?.owner === userStore.userInfo.id;

    return canReview && createExecute.value && updateExecute.value && !options.detail?.value?.approved;
  }

  const reviewAction = computed(() => {
    if (!canShowReviewAction()) {
      return hiddenAction;
    }

    const approvalStatus = options.isEdit.value
      ? options.approvalStatus.value ?? ProcessStatusEnum.NONE
      : ProcessStatusEnum.PENDING;

    if (!options.isEdit.value || approvalStatus === ProcessStatusEnum.PENDING) {
      return {
        visible: true,
        text: t('common.review'),
      };
    }

    if ([ProcessStatusEnum.REVOKED, ProcessStatusEnum.UNAPPROVED].includes(approvalStatus)) {
      return {
        visible: true,
        text: t('common.resubmit'),
      };
    }

    return hiddenAction;
  });

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
      console.log(error);
    }
  }

  return {
    reviewAction,
    initApprovalReviewConfig,
  };
}
