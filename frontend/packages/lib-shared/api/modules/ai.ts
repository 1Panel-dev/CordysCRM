import { AgentChatCancelUrl, AgentChatStreamUrl } from '../requrls/ai';
import { useI18n } from '../../hooks/useI18n';
import { getToken } from '../../method/auth';
import { getLocalStorage } from '../../method/local-storage';
import type { CordysAxios } from '../http/Axios';
import type {
  AgentChatCancelParams,
  AgentChatStreamEvent,
  AgentChatStreamOptions,
  AgentChatStreamParams,
} from '../../models/ai';

interface SseBlock {
  event: string;
  data: string;
  id?: string;
}

interface AgentErrorEventData {
  error?: string;
  message?: string;
}

function getApiUrl(path: string): string {
  const baseUrl = String(import.meta.env.VITE_API_BASE_URL ?? '')
    .trim()
    .replace(/^['"]|['"]$/g, '')
    .replace(/^\/|\/$/g, '');

  return `${window.location.origin}${baseUrl ? `/${baseUrl}` : ''}${path}`;
}

// 请求头处理
function getAgentHeaders(): Record<string, string> {
  const currentLocale = localStorage.getItem('CRM-locale') || 'zh-CN';
  const app = getLocalStorage<Record<string, any>>('app', true);
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (token) {
    const { sessionId, csrfToken } = token;

    return {
      ...headers,
      'X-AUTH-TOKEN': sessionId ?? '',
      'CSRF-TOKEN': csrfToken ?? '',
      'Accept-Language': currentLocale,
      'Organization-Id': app?.orgId,
    };
  }

  return headers;
}

// 拼接
function parseSseBlock(block: string): SseBlock {
  let event = 'message';
  let id: string | undefined;
  const data: string[] = [];

  block.split('\n').forEach((line) => {
    if (line.startsWith('id:')) {
      id = line.slice(3).trim();
      return;
    }

    if (line.startsWith('event:')) {
      event = line.slice(6).trim();
      return;
    }

    if (line.startsWith('data:')) {
      data.push(line.slice(5).replace(/^ /, ''));
    }
  });

  return {
    event,
    data: data.join('\n'),
    id,
  };
}

function getErrorMessage(data: string): string {
  const { t } = useI18n();

  try {
    const errorData = JSON.parse(data) as AgentErrorEventData;

    return errorData.error || errorData.message || data || t('common.operationFailed');
  } catch {
    return data || t('common.operationFailed');
  }
}

function toStreamEvent(block: SseBlock, options: AgentChatStreamOptions): AgentChatStreamEvent | undefined {
  if (block.event === 'session') {
    options.onSession?.(block.data, block.id);

    return {
      type: 'session',
      conversationId: block.id,
      sessionId: block.data,
      raw: block,
    };
  }

  if (block.event === 'chunk') {
    return {
      type: 'chunk',
      content: block.data,
      conversationId: block.id,
      raw: block,
    };
  }

  if (block.event === 'error') {
    return {
      type: 'error',
      errorMessage: getErrorMessage(block.data),
      conversationId: block.id,
      raw: block,
    };
  }

  if (block.data === '[DONE]' || block.event === 'done') {
    return {
      type: 'done',
      raw: block,
    };
  }

  return undefined;
}

async function* readAgentStream(
  reader: ReadableStreamDefaultReader<Uint8Array>,
  options: AgentChatStreamOptions,
  onErrorEvent: () => void
): AsyncIterable<AgentChatStreamEvent> {
  const decoder = new TextDecoder();
  let buffer = '';
  let hasErrorEvent = false;

  function readBufferedEvents(): AgentChatStreamEvent[] {
    const events: AgentChatStreamEvent[] = [];
    let boundary = buffer.indexOf('\n\n');

    while (boundary >= 0) {
      const rawBlock = buffer.slice(0, boundary);
      buffer = buffer.slice(boundary + 2);

      if (rawBlock.trim()) {
        const event = toStreamEvent(parseSseBlock(rawBlock), options);

        if (event) {
          events.push(event);
        }
      }

      boundary = buffer.indexOf('\n\n');
    }

    return events;
  }

  while (true) {
    const result = await reader.read();
    buffer = (buffer + decoder.decode(result.value || new Uint8Array(), { stream: !result.done })).replace(
      /\r\n/g,
      '\n'
    );

    const events = readBufferedEvents();

    for (let index = 0; index < events.length; index += 1) {
      const event = events[index];

      yield event;

      if (event.type === 'error') {
        hasErrorEvent = true;
        onErrorEvent();
        break;
      }
    }

    if (hasErrorEvent || result.done) {
      break;
    }
  }

  if (!hasErrorEvent && buffer.trim()) {
    const event = toStreamEvent(parseSseBlock(buffer), options);

    if (event) {
      yield event;

      if (event.type === 'error') {
        onErrorEvent();
      }
    }
  }
}

export default function useAiApi(CDR: CordysAxios) {
  async function* streamAgentChat(
    params: AgentChatStreamParams,
    options: AgentChatStreamOptions = {}
  ): AsyncIterable<AgentChatStreamEvent> {
    let reader: ReadableStreamDefaultReader<Uint8Array> | undefined;

    try {
      const response = await fetch(getApiUrl(AgentChatStreamUrl), {
        method: 'POST',
        headers: getAgentHeaders(),
        credentials: 'include',
        body: JSON.stringify(params),
        signal: options.signal,
      });

      if (!response.ok) {
        const { t } = useI18n();

        throw new Error((await response.text()) || `${t('common.operationFailed')}：${response.status}`);
      }

      if (!response.body) {
        throw new Error(useI18n().t('common.operationFailed'));
      }

      reader = response.body.getReader();

      let hasErrorEvent = false;

      yield* readAgentStream(reader, options, () => {
        hasErrorEvent = true;
      });

      if (!hasErrorEvent) {
        yield {
          type: 'done',
        };
      }
    } catch (error) {
      if (!options.signal?.aborted) {
        throw error;
      }
    } finally {
      reader?.releaseLock();
    }
  }

  async function cancelAgentChat(data: AgentChatCancelParams): Promise<void> {
    await CDR.post({ url: AgentChatCancelUrl, data });
  }

  return {
    streamAgentChat,
    cancelAgentChat,
  };
}
