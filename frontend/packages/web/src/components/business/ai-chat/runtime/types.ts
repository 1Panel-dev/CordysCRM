import type { AiChatAttachment, AiChatMcp, AiChatMessage, AiChatSendOptions, AiChatSubmitPayload } from '../types';
import type { Chat } from '@ai-sdk/vue';
import type { ChatStatus, ChatTransport } from 'ai';
import type { ComputedRef, Ref, ShallowRef } from 'vue';

export interface AiChatRuntimeState {
  /**
   * AI SDK UIMessage 列表。渲染层直接读取 message.parts。
   */
  messages: ComputedRef<AiChatMessage[]>;
  /**
   * Composer 草稿状态，不放进 AI SDK Chat，只有 submit 时才转成 user message。
   */
  input: Ref<string>;
  attachments: Ref<AiChatAttachment[]>;
  selectedMcps: Ref<AiChatMcp[]>;
  modelName: Ref<string>;
  status: ComputedRef<ChatStatus>;
  loading: ComputedRef<boolean>;
  streaming: ComputedRef<boolean>;
  error: ComputedRef<Error | undefined>;
  canSubmit: ComputedRef<boolean>;
  canStop: ComputedRef<boolean>;
}

/**
 * 创建 Runtime 时的配置。业务页面通常只需要传 transport/onStop 和初始模型。
 */
export interface CreateAiChatRuntimeOptions {
  id?: string;
  initialMessages?: AiChatMessage[];
  initialInput?: string;
  initialAttachments?: AiChatAttachment[];
  initialSelectedMcps?: AiChatMcp[];
  initialModelName?: string;
  createId?: () => string;
  /**
   * AI SDK 的请求适配器。CRM 当前使用 createAgentChatTransport 把后端 SSE 转成 UIMessageChunk。
   */
  transport?: ChatTransport<AiChatMessage>;
  /**
   * 用户点击停止后，除 Chat.stop() 外额外执行的业务取消逻辑。
   */
  onStop?: () => Promise<void> | void;
  onError?: (error: Error) => void;
}

/**
 * AI Chat 的唯一状态入口。组件通过它读写草稿、发送、停止、重试和编辑。
 */
export interface AiChatRuntime {
  state: AiChatRuntimeState;
  /**
   * 底层 AI SDK Chat 实例。保留出口方便少数高级场景直接访问。
   */
  chat: ShallowRef<Chat<AiChatMessage>>;
  transport: ShallowRef<ChatTransport<AiChatMessage> | undefined>;
  setInput: (value: string) => void;
  setAttachments: (value: AiChatAttachment[]) => void;
  setSelectedMcps: (value: AiChatMcp[]) => void;
  setModelName: (value: string) => void;
  removeAttachment: (attachmentId: string) => void;
  submit: (payload?: AiChatSubmitPayload) => Promise<void>;
  stop: () => Promise<void>;
  retry: (messageId?: string) => Promise<void>;
  edit: (messageId: string, content: string, options?: AiChatSendOptions) => Promise<void>;
  appendMessage: (message: AiChatMessage) => void;
  updateMessage: (messageId: string, patch: (message: AiChatMessage) => AiChatMessage) => void;
  clear: () => void;
}

/**
 * provide/inject 使用的 key 类型。
 */
export type AiChatRuntimeKey = symbol;
