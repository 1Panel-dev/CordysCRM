import { mergeStyles } from '@lib/shared/method/dom';

export interface UseFullScreen {
  isFullScreen: Ref<boolean>;
  toggleFullScreen: () => void;
  exitFullscreen: () => void;
}

/**
 * 全屏 hook
 * @param domRef dom ref
 */
export default function useFullScreen(
  domRef: Ref<HTMLElement | Element | null | undefined> | HTMLElement | Element | null | undefined,
  disabledShortcutKeys = false
): UseFullScreen {
  const isFullScreen = ref(false);
  const originalStyle = ref('');
  const lastKeyPressTime = ref(0);
  const keyPressTimeout = ref<NodeJS.Timeout>();

  function enterFullScreen() {
    const dom = unref(domRef);
    if (dom) {
      originalStyle.value = dom.getAttribute('style') || '';
      mergeStyles(
        dom,
        'position: fixed; top: 0; left: 0; right: 0; bottom: 0; z-index: 1000; width: 100%; height: 100%;'
      );
      isFullScreen.value = true;
    }
  }

  function exitFullscreen() {
    const dom = unref(domRef);
    if (dom) {
      dom.setAttribute('style', originalStyle.value);
      isFullScreen.value = false;
    }
  }

  function toggleFullScreen() {
    if (isFullScreen.value) {
      exitFullscreen();
    } else {
      enterFullScreen();
    }
  }
  // 快捷键全屏和退出
  function handleKeyDown(event: KeyboardEvent) {
    // 检查是否是 Command/Ctrl + E
    if ((event.metaKey || event.ctrlKey) && event.key === 'e') {
      event.preventDefault();
      const now = Date.now();
      if (keyPressTimeout.value) {
        clearTimeout(keyPressTimeout.value);
      }
      if (now - lastKeyPressTime.value < 500) {
        exitFullscreen();
      } else {
        toggleFullScreen();
      }
      lastKeyPressTime.value = now;
      keyPressTimeout.value = setTimeout(() => {
        lastKeyPressTime.value = 0;
      }, 500);
    }
  }

  // 添加键盘事件监听
  onMounted(() => {
    if (!disabledShortcutKeys) {
      window.addEventListener('keydown', handleKeyDown);
    }
  });

  // 移除键盘事件监听
  onUnmounted(() => {
    window.removeEventListener('keydown', handleKeyDown);
    if (keyPressTimeout.value) {
      clearTimeout(keyPressTimeout.value);
    }
  });

  return {
    isFullScreen,
    toggleFullScreen,
    exitFullscreen,
  };
}
