import type { UIMessage, UITools } from 'ai';

export interface AiChatError {
  message: string;
}

// 附件的粗粒度类型，用于决定默认图标、预览方式等
export type AiFileKind = 'file' | 'image' | 'audio' | 'video';

// 附件
export interface AiChatAttachment {
  id: string;
  name: string;
  url?: string;
  mimeType?: string;
  size?: number;
  kind?: AiFileKind;
  metadata?: Record<string, unknown>;
}

// mcp
export interface AiChatMcp {
  id: string;
  name: string;
  description?: string;
  permission: 'read' | 'write' | 'delete';
}

export interface AiChatMeta {
  model?: string;
  mcps?: AiChatMcp[];
  attachments?: AiChatAttachment[];
  extra?: Record<string, unknown>;
}

export type AiChatDataParts = Record<string, unknown> & {
  error: AiChatError;
};

export type AiChatMessage = UIMessage<AiChatMeta, AiChatDataParts, UITools>;

export type AiChatMessagePart = AiChatMessage['parts'][number];

/**
 * 发送选项。
 */
export interface AiChatSendOptions {
  metadata?: Record<string, unknown>;
  model?: string;
  mcps?: AiChatMcp[];
}

/**
 * Composer 提交给 Runtime 的输入载荷。
 */
export interface AiChatSubmitPayload {
  content?: string;
  attachments?: AiChatAttachment[];
  options?: AiChatSendOptions;
}

export type AiComposerSubmitPayload = AiChatSubmitPayload & {
  content: string;
};
