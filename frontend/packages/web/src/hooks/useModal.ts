import { DialogOptions, useDialog } from 'naive-ui';

export type DialogType = 'info' | 'success' | 'warning' | 'error' | 'default';

export type DialogSize = 'small' | 'medium' | 'large' | 'full';

export type DialogMode = 'default' | 'weak';

export interface DialogOptionsConfig extends DialogOptions {
  type?: DialogType;
  mode?: DialogMode;
  size?: DialogSize;
  hideCancel?: boolean;
  onPositiveClick: () => Promise<void>;
  [key: string]: any;
}

export interface DeleteDialogOptions {
  title: string;
  content: string;
  onBeforeOk: () => Promise<void>;
}

export default function useModal() {
  const dialog = useDialog();
  return {
    openDialog: (options: DialogOptionsConfig) => {
      const defaultButtonProps = {
        positiveButtonProps: {
          size: 'medium',
          quaternary: options.mode === 'weak',
          class: options.mode === 'weak' ? `text-btn-${options.type}` : '',
        },
        negativeButtonProps: {
          size: 'medium',
          secondary: options.mode !== 'weak',
          ghost: false,
          disabled: false,
          quaternary: options.mode === 'weak',
        },
      };
      const dialogReactive = dialog.create({
        maskClosable: false,
        ...defaultButtonProps,
        class: `${options.class} crm-modal-${options.size || 'small'}`,
        ...options,
        onPositiveClick: async () => {
          try {
            dialogReactive.loading = true;
            if (dialogReactive.negativeButtonProps) {
              dialogReactive.negativeButtonProps.disabled = true;
            }
            if (options.onPositiveClick) {
              await options.onPositiveClick();
            }
          } catch (error) {
            // eslint-disable-next-line no-console
            console.error(error);
          } finally {
            dialogReactive.loading = false;
            if (dialogReactive.negativeButtonProps) {
              dialogReactive.negativeButtonProps.disabled = false;
            }
          }
        },
        onClose: () => {
          return !dialogReactive.loading;
        },
      } as DialogOptionsConfig);
    },
  };
}
