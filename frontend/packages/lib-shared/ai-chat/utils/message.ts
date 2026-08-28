import type { AiChatMessage } from '../types';

export function getAiChatMessageText(message: Pick<AiChatMessage, 'parts'>, separator = '\n\n'): string {
  return message.parts
    .filter((part) => ['text', 'reasoning'].includes(part.type))
    .map((part) => ('text' in part ? part.text : ''))
    .filter(Boolean)
    .join(separator);
}

export function getAiChatMessageCopyText(message: Pick<AiChatMessage, 'parts'>, separator = '\n\n'): string {
  return getAiChatMessageText(message, separator)
    .replace(/\[\[[^:\]]+:[^\]]+]]/g, '')
    .trim();
}
