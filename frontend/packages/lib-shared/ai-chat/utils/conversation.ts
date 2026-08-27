import type { AgentChatProgressData, AgentConversationMessage } from '../../models/ai';
import type { AiChatAttachment, AiChatMessage, AiChatMessagePart } from '../types';

type AiChatRole = AiChatMessage['role'];
type ParsedContentPiece =
  | {
      type: 'text';
      content: string;
    }
  | {
      type: 'json';
      content: Record<string, unknown>;
      raw: string;
    };

interface HistoryContentItem {
  type?: string;
  text?: string;
  fileId?: string;
  name?: string;
  contentType?: string;
  size?: number;
}

interface ParsedMessageContent {
  text: string;
  attachments: AiChatAttachment[];
}

const HISTORY_CONTENT_TYPES = new Set(['text', 'image', 'file']);

const THINK_START_TAG = '<think>';
const THINK_END_TAG = '</think>';

// 后端历史消息角色是大写枚举，前端 AI SDK UIMessage 使用小写角色。
function normalizeRole(role: AgentConversationMessage['role']): AiChatRole {
  return role === 'ASSISTANT' ? 'assistant' : 'user';
}

// 历史详情里的 assistant.content 会拼接多段 JSON。
// 其中有些只是 agent 运行统计/链路元信息，不应该作为消息内容渲染。
const AGENT_METADATA_KEYS = new Set([
  'agentName',
  'assistantMessageId',
  'averageLatencyMs',
  'callCount',
  'conversationId',
  'createdAt',
  'failureCount',
  'fallbackCount',
  'inputTokens',
  'lastUpdated',
  'modelBreakdown',
  'modelName',
  'orgId',
  'outputTokens',
  'runId',
  'successCount',
  'successRate',
  'totalLatencyMs',
  'totalTokens',
  'userId',
  'userMessageId',
]);

function isProgressData(value: unknown): value is AgentChatProgressData {
  if (!value || typeof value !== 'object') {
    return false;
  }

  return (
    'actionId' in value &&
    'stage' in value &&
    'status' in value &&
    typeof value.actionId === 'string' &&
    typeof value.stage === 'string' &&
    typeof value.status === 'string'
  );
}

// 仅当一个 JSON 对象全部由元信息字段组成，并且包含关键链路字段时，才认为它是可丢弃的 agent metadata。
// 这样可以避免误删模型正常输出里的普通 JSON。
function isAgentMetadata(value: Record<string, unknown>): boolean {
  const keys = Object.keys(value);

  return (
    keys.length > 0 &&
    keys.every((key) => AGENT_METADATA_KEYS.has(key)) &&
    keys.some((key) => ['conversationId', 'runId', 'totalTokens'].includes(key))
  );
}

function isHistoryContentItem(value: unknown): value is HistoryContentItem {
  return (
    Boolean(value) && typeof value === 'object' && HISTORY_CONTENT_TYPES.has(String((value as HistoryContentItem).type))
  );
}

function parseStructuredMessageContent(content: string): ParsedMessageContent | undefined {
  if (!content) {
    return undefined;
  }

  try {
    const parsedContent = JSON.parse(content) as unknown;
    const items = Array.isArray(parsedContent) ? parsedContent.filter(isHistoryContentItem) : [];

    if (!items.length) {
      return undefined;
    }

    return {
      text: items
        .filter((item) => item.type === 'text' && typeof item.text === 'string')
        .map((item) => item.text)
        .join('\n'),
      attachments: items.map(toHistoryAttachment).filter((item): item is AiChatAttachment => Boolean(item)),
    };
  } catch {
    return undefined;
  }
}

function toHistoryAttachment(item: HistoryContentItem): AiChatAttachment | undefined {
  if (!item.fileId || !['image', 'file'].includes(item.type ?? '')) {
    return undefined;
  }

  return {
    id: item.fileId,
    name: item.name || item.fileId,
    mimeType: item.contentType,
    size: item.size,
    kind: item.type === 'image' ? 'image' : 'file',
    status: 'done',
    metadata: {
      fileId: item.fileId,
    },
  };
}

function parseMessageContent(content: AgentConversationMessage['content']): ParsedMessageContent {
  const structuredContent = parseStructuredMessageContent(content);

  return structuredContent ?? { text: content || '', attachments: [] };
}

// 从 content 的某个 "{" 开始读取一个完整 JSON 对象。
// 这里不用正则，是因为 JSON 字符串内部也可能包含花括号或转义引号，需要按括号深度解析。
function tryReadJsonObject(
  content: string,
  startIndex: number
): { value: Record<string, unknown>; raw: string; endIndex: number } | undefined {
  let depth = 0;
  let inString = false;
  let escaped = false;

  for (let index = startIndex; index < content.length; index += 1) {
    const char = content[index];

    if (inString) {
      if (escaped) {
        escaped = false;
      } else if (char === '\\') {
        escaped = true;
      } else if (char === '"') {
        inString = false;
      }

      continue;
    }

    if (char === '"') {
      inString = true;
    } else if (char === '{') {
      depth += 1;
    } else if (char === '}') {
      depth -= 1;

      if (depth === 0) {
        const jsonText = content.slice(startIndex, index + 1);

        try {
          const value = JSON.parse(jsonText) as unknown;

          if (value && typeof value === 'object' && !Array.isArray(value)) {
            return {
              value: value as Record<string, unknown>,
              raw: jsonText,
              endIndex: index + 1,
            };
          }
        } catch {
          return undefined;
        }
      }
    }
  }

  return undefined;
}

// 后端历史 content 形如：普通文本 + progress JSON + <think>...</think> + 正文 + metadata JSON。
// 这里先按“嵌入 JSON 对象”切成 text/json 片段，后续再分别转换成 UIMessage part。
function parseEmbeddedJsonContent(content: string): ParsedContentPiece[] {
  const pieces: ParsedContentPiece[] = [];
  let textStart = 0;
  let index = 0;

  while (index < content.length) {
    if (content[index] !== '{') {
      index += 1;
      continue;
    }

    const jsonObject = tryReadJsonObject(content, index);

    if (!jsonObject) {
      index += 1;
      continue;
    }

    if (textStart < index) {
      pieces.push({
        type: 'text',
        content: content.slice(textStart, index),
      });
    }

    pieces.push({
      type: 'json',
      content: jsonObject.value,
      raw: jsonObject.raw,
    });

    index = jsonObject.endIndex;
    textStart = index;
  }

  if (textStart < content.length) {
    pieces.push({
      type: 'text',
      content: content.slice(textStart),
    });
  }

  return pieces;
}

// 将 <think>...</think> 里的内容转换成 reasoning part，其余文本转换成普通 text part。
// AI SDK 渲染层会根据 part.type 使用不同的思考/正文样式。
function appendThinkingTextParts(parts: AiChatMessagePart[], content: string): void {
  let rest = content;
  let insideThinking = false;

  while (rest) {
    const targetTag = insideThinking ? THINK_END_TAG : THINK_START_TAG;
    const tagIndex = rest.indexOf(targetTag);
    const text = tagIndex >= 0 ? rest.slice(0, tagIndex) : rest;

    if (text.trim()) {
      parts.push({
        type: insideThinking ? 'reasoning' : 'text',
        text,
      } as AiChatMessagePart);
    }

    if (tagIndex < 0) {
      break;
    }

    rest = rest.slice(tagIndex + targetTag.length);
    insideThinking = !insideThinking;
  }
}

function dedupeProgressParts(parts: AiChatMessagePart[]): AiChatMessagePart[] {
  const lastProgressIndexMap = new Map<string, number>();

  parts.forEach((part, index) => {
    if (part.type !== 'data-progress') {
      return;
    }

    const progress = part.data as AgentChatProgressData | undefined;
    const actionId = progress?.actionId || part.id;

    if (actionId) {
      lastProgressIndexMap.set(actionId, index);
    }
  });

  return parts.filter((part, index) => {
    if (part.type !== 'data-progress') {
      return true;
    }

    const progress = part.data as AgentChatProgressData | undefined;
    const actionId = progress?.actionId || part.id;

    return !actionId || lastProgressIndexMap.get(actionId) === index;
  });
}

function toAiChatMessageParts(role: AiChatRole, parsedContent: ParsedMessageContent): AiChatMessagePart[] {
  // 用户消息目前只有纯文本，不需要解析 progress、metadata 或 think 标签。
  if (role !== 'assistant') {
    return [
      {
        type: 'text',
        text: parsedContent.text,
      } as AiChatMessagePart,
    ];
  }

  const parts: AiChatMessagePart[] = [];

  parseEmbeddedJsonContent(parsedContent.text).forEach((piece, index) => {
    if (piece.type === 'text') {
      appendThinkingTextParts(parts, piece.content);
      return;
    }

    if (isProgressData(piece.content)) {
      // progress JSON 是工具执行/思考过程事件，转成 data-progress 交给 ProgressBlock 渲染。
      parts.push({
        type: 'data-progress',
        id: piece.content.actionId || `progress_${index}`,
        data: piece.content,
      } as AiChatMessagePart);
      return;
    }

    if (!isAgentMetadata(piece.content)) {
      // 未识别的 JSON 不丢弃，按原文保留，避免吞掉模型真正想展示的结构化内容。
      appendThinkingTextParts(parts, piece.raw);
    }
  });

  // 理论上 assistant content 都能被上面的逻辑拆出 parts；这里兜底避免历史消息空白。
  const dedupedParts = dedupeProgressParts(parts);

  return dedupedParts.length
    ? dedupedParts
    : [
        {
          type: 'text',
          text: parsedContent.text,
        } as AiChatMessagePart,
      ];
}

export function toAiChatMessage(message: AgentConversationMessage, index: number): AiChatMessage {
  const parsedContent = parseMessageContent(message.content);
  const role = normalizeRole(message.role);

  return {
    id: message.id || `history_${index}`,
    role,
    metadata: {
      tokens: message.totalTokens ?? undefined,
      runId: message.runId,
      helpful: message.helpful ?? undefined,
      attachments: parsedContent.attachments,
    },
    parts: toAiChatMessageParts(role, parsedContent),
  };
}

// 判断消息是否已经有可渲染内容，用于控制流式加载占位是否继续显示。
export function hasRenderableAiChatContent(parts: AiChatMessagePart[]): boolean {
  return parts.some((part) => {
    if (part.type === 'data-error' || part.type === 'data-progress') {
      return true;
    }

    return (part.type === 'text' || part.type === 'reasoning') && 'text' in part && part.text.trim().length > 0;
  });
}
