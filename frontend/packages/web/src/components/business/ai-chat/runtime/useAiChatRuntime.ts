import { inject } from 'vue';

import type { AiChatRuntime } from './types';
import type { InjectionKey } from 'vue';

/**
 * Runtime 的注入 key。
 * Provider 和 useAiChatRuntime 共用它，避免组件之间层层传 props。
 */
export const AI_CHAT_RUNTIME_KEY: InjectionKey<AiChatRuntime> = Symbol('AI_CHAT_RUNTIME_KEY');

/**
 * 获取当前聊天 Runtime。
 * 组件只通过这个 hook 访问 Runtime，不直接 import 页面里的实例。
 */
export function useAiChatRuntime(): AiChatRuntime {
  const runtime = inject(AI_CHAT_RUNTIME_KEY);

  if (!runtime) {
    throw new Error('useAiChatRuntime must be used inside AiChatProvider');
  }

  return runtime;
}
