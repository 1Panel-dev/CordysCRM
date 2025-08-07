import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';

interface ScriptOptions {
  identifier: string; // 脚本标识
}

const scriptElementsMap = new Map<string, string>();

// 设置拖拽功能
function setupDrag(button: HTMLElement | null) {
  if (!button) return;
  let isDragging = false;
  let startX: number;
  let startY: number;
  let startLeft: number;
  let startTop: number;

  // 移动
  const move = (e: MouseEvent) => {
    button.style.cursor = 'grabbing';

    // 移动超过5px才认为是拖动
    if (!isDragging && (Math.abs(e.clientX - startX) > 5 || Math.abs(e.clientY - startY) > 5)) {
      isDragging = true;
    }

    if (isDragging) {
      // 计算新位置
      const newLeft = startLeft + e.clientX - startX;
      const newTop = startTop + e.clientY - startY;

      // 边界检查(可选) - 确保元素不会移出视口
      const maxX = window.innerWidth - button.offsetWidth;
      const maxY = window.innerHeight - button.offsetHeight;

      // 设置新位置(限制在边界内)
      button.style.left = `${Math.max(0, Math.min(newLeft, maxX))}px`;
      button.style.top = `${Math.max(0, Math.min(newTop, maxY))}px`;

      button.style.right = 'auto';
      button.style.bottom = 'auto';
    }
  };

  // 停止拖动
  const stopDrag = () => {
    document.removeEventListener('mousemove', move);
    document.removeEventListener('mouseup', stopDrag);
    button.style.cursor = 'pointer';

    // 如果是拖拽操作(不是点击)，则阻止接下来的点击事件
    if (isDragging) {
      const clickHandler = (e: Event) => {
        e.stopImmediatePropagation();
        e.preventDefault();
        button.removeEventListener('click', clickHandler);
      };

      button.addEventListener('click', clickHandler, true);

      // 300ms后移除点击拦截
      setTimeout(() => {
        button.removeEventListener('click', clickHandler, true);
      }, 300);
    }
  };

  // 鼠标按下时记录初始位置
  button.addEventListener('mousedown', (e: MouseEvent) => {
    isDragging = false;
    startX = e.clientX;
    startY = e.clientY;

    const style = window.getComputedStyle(button);
    startLeft = parseInt(style.left, 10);
    startTop = parseInt(style.top, 10);

    // 添加移动和松开事件
    document.addEventListener('mousemove', move);
    document.addEventListener('mouseup', stopDrag);
  });
}

function extractSQLBotId(input: string) {
  const regex = /sqlbot-[^\s"']+/;
  const match = input.match(regex);
  return match ? match[0] : null;
}

export function loadScript(scriptContent: string, options: ScriptOptions): Promise<void> {
  return new Promise((resolve, reject) => {
    const content = scriptContent.trim();
    if (scriptElementsMap.has(options.identifier)) return;

    // 处理IIFE格式
    if (content.startsWith('(function')) {
      const scriptId = extractSQLBotId(content);
      if (scriptId) {
        scriptElementsMap.set(options.identifier, scriptId);
      }
      // eslint-disable-next-line no-eval
      eval(content);
      resolve();
      return;
    }

    // 处理<script>标签
    if (content.startsWith('<script')) {
      const div = document.createElement('div');
      div.innerHTML = content;
      const originalScript = div.querySelector('script');
      if (!originalScript) {
        reject(new Error('无效的script标签'));
        return;
      }
      const script = document.createElement('script');
      // 复制所有属性
      for (let i = 0; i < originalScript.attributes.length; i++) {
        const attr = originalScript.attributes[i];
        if (attr.name === 'id') {
          scriptElementsMap.set(options.identifier, attr.value);
        }
        script.setAttribute(attr.name, attr.value);
      }
      script.onload = () => {
        setTimeout(() => {
          const button = document.querySelector('.sqlbot-assistant-chat-button');
          setupDrag(button as HTMLElement);
        }, 300); // 等待DOM渲染
      };

      document.body.appendChild(script);
      resolve();
      return;
    }

    reject(new Error('不支持的脚本格式'));
  });
}

export function removeScript(identifier: string): void {
  const scriptId = scriptElementsMap.get(identifier);
  if (scriptId && identifier === CompanyTypeEnum.SQLBot) {
    // 清理全局单例标记
    const propName = `${scriptId}-state`;
    delete (window as any)[propName];
    if ((window as any).sqlbot_assistant_handler) {
      delete (window as any).sqlbot_assistant_handler;
    }
    // 删除页面上渲染的
    const floatingElements = document.querySelectorAll('[id^="sqlbot-"]');
    floatingElements.forEach((el) => {
      if (el.parentNode && !el.parentNode.isEqualNode(document.body) && !el.parentNode.isEqualNode(document.head)) {
        (el.parentNode as Element).remove();
      }
      el.remove();
    });
    scriptElementsMap.delete(identifier);
  }
}
