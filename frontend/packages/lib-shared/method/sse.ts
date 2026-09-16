import { getToken } from './auth';
import { getLocalStorage } from './local-storage';
import { createParser } from 'eventsource-parser';

export interface SseConnection {
  onmessage: ((event: MessageEvent<string>) => void) | null;
  onerror: ((event: Event) => void) | null;
  close: () => void;
}

/** 使用与普通 API 相同的会话请求头，保持现有手动重连行为。 */
export function createAuthenticatedSSE(url: string): SseConnection {
  const controller = new AbortController();
  const connection: SseConnection = {
    onmessage: null,
    onerror: null,
    close: () => controller.abort(),
  };
  const { sessionId, csrfToken } = getToken();
  const app = getLocalStorage<{ orgId?: string }>('app', true);

  async function readStream() {
    let reader: ReadableStreamDefaultReader<Uint8Array> | undefined;
    try {
      if (!sessionId) {
        throw new Error('SSE requires an authenticated session');
      }
      const response = await fetch(url, {
        headers: {
          'Accept': 'text/event-stream',
          'X-AUTH-TOKEN': sessionId,
          'CSRF-TOKEN': csrfToken,
          'Accept-Language': localStorage.getItem('CRM-locale') || 'zh-CN',
          ...(app?.orgId ? { 'Organization-Id': app.orgId } : {}),
        },
        credentials: 'include',
        redirect: 'error',
        signal: controller.signal,
      });
      if (!response.ok || !response.headers.get('Content-Type')?.startsWith('text/event-stream') || !response.body) {
        throw new Error('SSE subscription failed');
      }
      const parser = createParser({
        onEvent: (event) => {
          if (!controller.signal.aborted && (!event.event || event.event === 'message')) {
            connection.onmessage?.(new MessageEvent('message', { data: event.data, lastEventId: event.id || '' }));
          }
        },
      });
      const decoder = new TextDecoder();
      reader = response.body.getReader();
      while (!controller.signal.aborted) {
        // SSE 帧和 UTF-8 字符均可能跨网络分块，交给增量解码器和解析器处理。
        // eslint-disable-next-line no-await-in-loop
        const { done, value } = await reader.read();
        if (done) break;
        parser.feed(decoder.decode(value, { stream: true }));
      }
      if (!controller.signal.aborted) {
        connection.onerror?.(new Event('error'));
      }
    } catch {
      if (!controller.signal.aborted) {
        connection.onerror?.(new Event('error'));
      }
    } finally {
      controller.abort();
      reader?.releaseLock();
    }
  }

  // 让调用方先绑定回调，缺少凭据等同步错误也能被处理。
  queueMicrotask(() => {
    readStream();
  });
  return connection;
}
