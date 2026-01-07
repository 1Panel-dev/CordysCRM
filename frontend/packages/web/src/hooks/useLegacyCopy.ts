import { useI18n } from '@lib/shared/hooks/useI18n';

import useDiscreteApi from './useDiscreteApi';

export default function useLegacyCopy(containerClass?: string) {
  const { message } = useDiscreteApi();
  const { t } = useI18n();

  async function legacyCopy(val: string) {
    const fallbackCopy = () => {
      const textarea = document.createElement('textarea');
      textarea.value = val;
      // 不设置 readonly，某些浏览器 readonly 元素选区有问题
      textarea.style.pointerEvents = 'none';
      if (containerClass) {
        document.querySelector(containerClass)?.appendChild(textarea);
      } else {
        document.body.appendChild(textarea);
      }

      // 先聚焦，再选中，再设置选区范围
      textarea.focus();
      textarea.select();
      textarea.setSelectionRange(0, textarea.value.length);

      let ok = false;
      try {
        ok = document.execCommand('copy');
      } catch (_) {
        ok = false;
      } finally {
        if (containerClass) {
          document.querySelector(containerClass)?.removeChild(textarea);
        } else {
          document.body.removeChild(textarea);
        }
      }
      return ok;
    };

    // 优先现代 API，失败再回退
    if (navigator.clipboard?.writeText) {
      try {
        await navigator.clipboard.writeText(val);
        message.success(t('common.copySuccess'));
        return;
      } catch (_) {
        /* fall through */
      }
    }

    const copied = fallbackCopy();
    if (copied) {
      message.success(t('common.copySuccess'));
    } else {
      message.warning(t('common.copyNotSupport'));
    }
  }

  return {
    legacyCopy,
  };
}
