import { useI18n } from '@lib/shared/hooks/useI18n';

import useDiscreteApi from './useDiscreteApi';

export default function useLegacyCopy(containerClass?: string) {
  const { message } = useDiscreteApi();
  const { t } = useI18n();

  async function legacyCopy(val: string) {
    const fallbackCopy = () => {
      const selection = window.getSelection();
      const ranges = selection
        ? Array.from({ length: selection.rangeCount }, (_, index) => selection.getRangeAt(index).cloneRange())
        : [];
      const activeElement = document.activeElement instanceof HTMLElement ? document.activeElement : null;
      const textarea = document.createElement('textarea');
      const appendTarget = (containerClass ? document.querySelector(containerClass) : null) || document.body;

      textarea.value = val;
      // 不设置 readonly，某些浏览器 readonly 元素选区有问题
      textarea.style.position = 'fixed';
      textarea.style.top = '0';
      textarea.style.left = '0';
      textarea.style.width = '1px';
      textarea.style.height = '1px';
      textarea.style.opacity = '0';
      textarea.style.pointerEvents = 'none';
      appendTarget.appendChild(textarea);

      // 先聚焦，再选中，再设置选区范围
      textarea.focus();
      textarea.select();
      textarea.setSelectionRange(0, textarea.value.length);

      let ok = false;
      const handleCopy = (event: ClipboardEvent) => {
        event.clipboardData?.setData('text/plain', val);
        event.preventDefault();
        ok = true;
      };

      try {
        document.addEventListener('copy', handleCopy);
        ok = document.execCommand('copy') || ok;
      } catch (_) {
        ok = false;
      } finally {
        document.removeEventListener('copy', handleCopy);
        appendTarget.removeChild(textarea);

        if (selection) {
          selection.removeAllRanges();
          ranges.forEach((range) => selection.addRange(range));
        }

        activeElement?.focus();
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
