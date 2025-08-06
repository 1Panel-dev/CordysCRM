import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';

interface ScriptOptions {
  identifier: string; // 脚本标识
}

const scriptElementsMap = new Map<string, string>();

function extractScriptId(input: string): string | null {
  const regex = /id\s*=\s*(["']?)([a-zA-Z0-9-]+\d+)\1/;
  const match = input.match(regex);
  return match ? match[2] : null;
}

export function loadScript(scriptContent: string, options: ScriptOptions): Promise<void> {
  return new Promise((resolve, reject) => {
    const content = scriptContent.trim();
    if (scriptElementsMap.has(options.identifier)) return;

    // 处理IIFE格式
    if (content.startsWith('(function')) {
      const scriptId = extractScriptId(content);
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
    // 删除script
    const scriptElement = document.getElementById(scriptId);
    if (scriptElement) scriptElement.remove();
    // 删除页面上渲染的div
    const replacedId = scriptId.replace('float-script', 'root');
    const replacedElement = document.getElementById(replacedId);
    if (replacedElement?.parentNode) (replacedElement.parentNode as Element).remove();
    scriptElementsMap.delete(identifier);
  }
}
