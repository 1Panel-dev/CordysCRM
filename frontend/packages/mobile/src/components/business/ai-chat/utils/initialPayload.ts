import type { AiChatAttachment } from '@lib/shared/ai-chat';

export interface AiMobileChatInitialPayload {
  content: string;
  attachments?: AiChatAttachment[];
}

let initialPayload: AiMobileChatInitialPayload | undefined;

export function setAiMobileChatInitialPayload(payload: AiMobileChatInitialPayload): void {
  initialPayload = payload;
}

export function consumeAiMobileChatInitialPayload(): AiMobileChatInitialPayload | undefined {
  const payload = initialPayload;

  initialPayload = undefined;

  return payload;
}
