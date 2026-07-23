import type { AgentChatStreamEvent } from '@lib/shared/models/ai';

import type { AiChatMessage, AiChatMeta } from '../types';
import type { ChatTransport, UIMessageChunk } from 'ai';

type MarkdownDeltaKind = 'text' | 'reasoning';

const THINK_START_TAG = '<think>';
const THINK_END_TAG = '</think>';

interface MarkdownDeltaPiece {
  kind: MarkdownDeltaKind;
  content: string;
}

export interface AgentChatTransportOptions {
  /**
   * 业务侧真正的请求函数。
   * Transport 只关心它返回的 CRM agent 事件流，不关心接口地址、鉴权和会话字段如何拼装。
   */
  send: (context: {
    messages: AiChatMessage[];
    content: string;
    signal?: AbortSignal;
    metadata?: AiChatMeta;
  }) => AsyncIterable<AgentChatStreamEvent>;
}

// 流式文本可能把 <think> 标签切成几段，这里先保留可疑尾巴，等下一段再判断。
function getPendingTagLength(content: string, tag: string): number {
  const maxLength = Math.min(content.length, tag.length - 1);

  for (let length = maxLength; length > 0; length -= 1) {
    if (tag.startsWith(content.slice(-length))) {
      return length;
    }
  }

  return 0;
}

function createThinkingMarkdownParser() {
  let buffer = '';
  let insideThinking = false;

  function getCurrentKind(): MarkdownDeltaKind {
    return insideThinking ? 'reasoning' : 'text';
  }

  // 后端当前把思考内容混在普通 chunk 中，用 <think>...</think> 包裹。
  // 这里拆成 AI SDK 原生的 text/reasoning 两类 part，UI 就不用再理解后端标签协议。
  function readAvailableContent(forceFlush = false): MarkdownDeltaPiece[] {
    const pieces: MarkdownDeltaPiece[] = [];

    function readNext(): void {
      if (!buffer) {
        return;
      }

      const targetTag = insideThinking ? THINK_END_TAG : THINK_START_TAG;
      const targetTagIndex = buffer.indexOf(targetTag);

      if (targetTagIndex >= 0) {
        pieces.push({
          kind: getCurrentKind(),
          content: buffer.slice(0, targetTagIndex),
        });
        buffer = buffer.slice(targetTagIndex + targetTag.length);
        insideThinking = !insideThinking;
        readNext();
        return;
      }

      const pendingTagLength = forceFlush ? 0 : getPendingTagLength(buffer, targetTag);
      const readableLength = buffer.length - pendingTagLength;

      if (readableLength <= 0) {
        return;
      }

      pieces.push({
        kind: getCurrentKind(),
        content: buffer.slice(0, readableLength),
      });
      buffer = buffer.slice(readableLength);
    }

    readNext();

    return pieces.filter((piece) => piece.content);
  }

  return {
    append(content: string) {
      buffer += content;
      return readAvailableContent();
    },
    flush() {
      return readAvailableContent(true);
    },
  };
}

function getLastUserText(messages: AiChatMessage[]): string {
  const lastUserMessage = [...messages].reverse().find((message) => message.role === 'user');

  return (
    lastUserMessage?.parts
      .filter((part) => part.type === 'text')
      .map((part) => part.text)
      .join('\n')
      .trim() ?? ''
  );
}

// AI SDK ChatTransport 需要返回 ReadableStream<UIMessageChunk>。
// 这里把 CRM 的 session/chunk/error/done 事件转换成 AI SDK 可消费的 UI message stream。
function createReadableAgentUiStream(events: AsyncIterable<AgentChatStreamEvent>): ReadableStream<UIMessageChunk> {
  const parser = createThinkingMarkdownParser();

  return new ReadableStream<UIMessageChunk>({
    async start(controller) {
      let activePart: MarkdownDeltaKind | undefined;
      let activePartId = '';
      let partIndex = 0;
      let started = false;
      let finished = false;

      function enqueueStart(): void {
        if (started) {
          return;
        }

        controller.enqueue({ type: 'start' });
        started = true;
      }

      // AI SDK 的 text/reasoning part 都需要 start/delta/end 三段式事件。
      function closeActivePart(): void {
        if (!activePart) {
          return;
        }

        controller.enqueue({
          type: activePart === 'reasoning' ? 'reasoning-end' : 'text-end',
          id: activePartId,
        });
        activePart = undefined;
        activePartId = '';
      }

      function enqueuePiece(piece: MarkdownDeltaPiece): void {
        enqueueStart();

        if (activePart !== piece.kind) {
          closeActivePart();
          activePart = piece.kind;
          activePartId = `${piece.kind}_${partIndex}`;
          partIndex += 1;
          controller.enqueue({
            type: piece.kind === 'reasoning' ? 'reasoning-start' : 'text-start',
            id: activePartId,
          });
        }

        controller.enqueue({
          type: piece.kind === 'reasoning' ? 'reasoning-delta' : 'text-delta',
          id: activePartId,
          delta: piece.content,
        });
      }

      // finish 时一定 flush parser，避免流结束时残留半个标签或最后一段文本。
      function finish(): void {
        if (finished) {
          return;
        }

        parser.flush().forEach(enqueuePiece);
        closeActivePart();
        enqueueStart();
        controller.enqueue({ type: 'finish' });
        finished = true;
        controller.close();
      }

      async function consume(): Promise<void> {
        const iterator = events[Symbol.asyncIterator]();
        let result = await iterator.next();

        while (!result.done) {
          const event = result.value;

          if (event.type === 'chunk') {
            parser.append(event.content ?? '').forEach(enqueuePiece);
          } else if (event.type === 'error') {
            closeActivePart();
            enqueueStart();
            controller.enqueue({
              type: 'data-error',
              id: `error_${partIndex}`,
              data: {
                message: event.errorMessage || '',
              },
            });
            finish();
            return;
          } else if (event.type === 'done') {
            finish();
            return;
          }

          // eslint-disable-next-line no-await-in-loop
          result = await iterator.next();
        }

        finish();
      }

      try {
        await consume();
      } catch (error) {
        controller.error(error);
      }
    },
  });
}

export default function createAgentChatTransport(options: AgentChatTransportOptions): ChatTransport<AiChatMessage> {
  return {
    async sendMessages({ messages, abortSignal, metadata }) {
      return createReadableAgentUiStream(
        options.send({
          messages,
          content: getLastUserText(messages),
          signal: abortSignal,
          metadata: metadata as AiChatMeta | undefined,
        })
      );
    },
    async reconnectToStream() {
      return null;
    },
  };
}
