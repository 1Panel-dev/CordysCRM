import { h, ref } from 'vue';
import { NFormItem, NInput, useMessage } from 'naive-ui';

import { useI18n } from '@lib/shared/hooks/useI18n';

import useModal from '@/hooks/useModal';

export default function useUpdateReviewChange() {
  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  function openUpdateChangeModal() {
    return new Promise<string | undefined>((resolve) => {
      const changeDescription = ref('');
      let modalReactive: ReturnType<typeof openModal> | null = null;
      const syncPositiveDisabled = (disabled: boolean) => {
        if (!modalReactive) {
          return;
        }
        modalReactive.positiveButtonProps = {
          ...(modalReactive.positiveButtonProps ?? {}),
          disabled,
        };
      };

      modalReactive = openModal({
        maskClosable: false,
        size: 'medium',
        title: t('crm.approval.change'),
        positiveText: t('crm.approval.confirmChange'),
        negativeText: t('common.cancel'),
        positiveButtonProps: {
          size: 'medium',
          disabled: true,
        },
        content: () =>
          h(
            NFormItem,
            {
              label: t('crm.approval.changeDescription'),
              required: true,
              showFeedback: false,
            },
            {
              default: () =>
                h(NInput, {
                  value: changeDescription.value,
                  type: 'textarea',
                  maxlength: 300,
                  showCount: true,
                  autosize: {
                    minRows: 3,
                  },
                  onUpdateValue: (value: string) => {
                    changeDescription.value = value;
                    syncPositiveDisabled(!value.trim().length);
                  },
                }),
            }
          ),
        onPositiveClick: () => {
          const description = changeDescription.value.trim();
          if (!description.length) {
            Message.warning(t('common.notNull', { value: t('crm.approval.changeDescription') }));
            return false;
          }
          resolve(description);
        },
        onNegativeClick: () => {
          resolve(undefined);
        },
        onAfterLeave: () => {
          resolve(undefined);
        },
      });
    });
  }

  async function getUpdateReviewExtraParams(shouldConfirm: boolean) {
    if (!shouldConfirm) {
      return {};
    }

    const comment = await openUpdateChangeModal();
    if (!comment) {
      return false;
    }

    return { comment };
  }

  return {
    openUpdateChangeModal,
    getUpdateReviewExtraParams,
  };
}
