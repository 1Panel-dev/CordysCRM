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

interface ApprovalReviewConfig {
  enable: boolean;
  createExecute: boolean;
  updateExecute: boolean;
}

const approvalReviewConfigCache = new Map<FormDesignKeyEnum, ApprovalReviewConfig>();
const approvalReviewConfigPendingMap = new Map<FormDesignKeyEnum, Promise<ApprovalReviewConfig>>();

export function clearApprovalReviewConfigCache(formKey?: FormDesignKeyEnum | string) {
  if (formKey) {
    approvalReviewConfigCache.delete(formKey as FormDesignKeyEnum);
    approvalReviewConfigPendingMap.delete(formKey as FormDesignKeyEnum);
    return;
  }

  approvalReviewConfigCache.clear();
  approvalReviewConfigPendingMap.clear();
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

    return params.isEdit ? params.updateExecute : params.createExecute;
  }

  function canShowByIdentity(params: GetFormReviewActionParams) {
    return !params.isEdit || params.canReview;
  }

  function getReviewActionTextByStatus(params: GetFormReviewActionParams) {
    const approvalStatus = params.approvalStatus ?? ProcessStatusEnum.NONE;

    if (!params.isEdit || [ProcessStatusEnum.NONE, ProcessStatusEnum.PENDING].includes(approvalStatus)) {
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

  async function loadApprovalReviewConfig(formKey: FormDesignKeyEnum) {
    const cachedConfig = approvalReviewConfigCache.get(formKey);

    if (cachedConfig) {
      return cachedConfig;
    }

    const pendingConfig = approvalReviewConfigPendingMap.get(formKey);

    if (pendingConfig) {
      return pendingConfig;
    }

    const request = getApprovalConfigDetail(formKey)
      .then((result) => {
        const config: ApprovalReviewConfig = {
          enable: Boolean(result?.enable),
          createExecute: Boolean(result?.createExecute),
          updateExecute: Boolean(result?.updateExecute),
        };

        approvalReviewConfigCache.set(formKey, config);
        return config;
      })
      .finally(() => {
        approvalReviewConfigPendingMap.delete(formKey);
      });

    approvalReviewConfigPendingMap.set(formKey, request);

    return request;
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
      createExecute.value = false;
      updateExecute.value = false;
      return;
    }

    try {
      const result = await loadApprovalReviewConfig(options.formKey.value);
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
