import { computed, markRaw, ref, shallowRef } from 'vue';

import type { AgentChatConfirmData } from '@lib/shared/models/ai';

import type { AiChatAttachment, AiChatMessage, AiChatMeta, AiChatSubmitPayload } from '../types';
import type { AiChatRuntime, CreateAiChatRuntimeOptions } from './types';
import { getAiChatMessageText } from '../utils/message';
import { Chat } from '@ai-sdk/vue';
import type { ChatTransport, FileUIPart } from 'ai';

function createDefaultAiChatId(): string {
  return `ai_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
}

// 每次提交时把 MCP、附件等业务上下文挂到 message metadata。
// Transport 会从 metadata 中读取这些信息并转换成后端参数。
function toMessageMetadata(payload: AiChatSubmitPayload): AiChatMeta {
  return {
    mcps: payload.options?.mcps,
    attachments: payload.attachments,
  };
}

// AI SDK 的用户消息附件使用 file part；本地未上传成功、没有 URL 的附件只保留在 metadata 中展示。
function toFileParts(attachments: AiChatAttachment[] = []): FileUIPart[] {
  return attachments
    .filter((attachment) => Boolean(attachment.url))
    .map((attachment) => ({
      type: 'file',
      url: attachment.url as string,
      filename: attachment.name,
      mediaType: attachment.mimeType || 'application/octet-stream',
    }));
}

/**
 * 创建 AI Chat Runtime。
 * Runtime 是组件层访问 AI SDK Chat 的门面：UI 只操作 Runtime，不直接依赖 Chat 实例细节。
 */
export default function createAiChatRuntime(options: CreateAiChatRuntimeOptions = {}): AiChatRuntime {
  const createId = options.createId ?? createDefaultAiChatId;
  const input = ref(options.initialInput ?? '');
  const attachments = ref<AiChatAttachment[]>([...(options.initialAttachments ?? [])]);
  const selectedMcps = ref([...(options.initialSelectedMcps ?? [])]);
  const transport = shallowRef<ChatTransport<AiChatMessage> | undefined>(options.transport);
  const currentConfirm = ref<AgentChatConfirmData>();
  const editingMessageId = ref('');
  const editingContent = ref('');

  // Chat 负责消息追加、流式合并、停止、重试和编辑后的重新请求。
  // Runtime 只补充 CRM 需要的输入草稿、附件和 MCP 状态。
  const chat = shallowRef(
    new Chat<AiChatMessage>({
      id: options.id,
      messages: options.initialMessages ?? [],
      generateId: createId,
      transport: transport.value,
      onError(error) {
        currentConfirm.value = undefined;
        options.onError?.(error);
      },
      onData(dataPart) {
        if (dataPart.type === 'data-confirm') {
          currentConfirm.value = dataPart.data as AgentChatConfirmData;
        } else if (dataPart.type === 'data-error') {
          currentConfirm.value = undefined;
        }
      },
      async onFinish() {
        currentConfirm.value = undefined;
        await options.onFinish?.();
      },
    })
  );

  const messages = computed(() => chat.value.messages);
  const status = computed(() => chat.value.status);
  const loading = computed(() => ['submitted', 'streaming'].includes(status.value));
  const streaming = computed(() => status.value === 'streaming');
  const error = computed(() => chat.value.error);
  const canSubmit = computed(() => !loading.value && (input.value.trim().length > 0 || attachments.value.length > 0));
  const canStop = computed(() => ['submitted', 'streaming'].includes(status.value));
  const pendingConfirm = computed(() => currentConfirm.value);
  const editingMessage = computed(() => chat.value.messages.find((message) => message.id === editingMessageId.value));
  const canSubmitEdit = computed(
    () => !loading.value && Boolean(editingMessageId.value) && editingContent.value.trim().length > 0
  );

  function setInput(value: string): void {
    input.value = value;
  }

  function setAttachments(value: AiChatAttachment[]): void {
    attachments.value = value;
  }

  function setSelectedMcps(value: typeof selectedMcps.value): void {
    selectedMcps.value = value;
  }

  function removeAttachment(attachmentId: string): void {
    attachments.value = attachments.value.filter((attachment) => attachment.id !== attachmentId);
  }

  function setEditingContent(value: string): void {
    editingContent.value = value;
  }

  function cancelEditMessage(): void {
    editingMessageId.value = '';
    editingContent.value = '';
  }

  function startEditMessage(messageId: string): void {
    if (loading.value) {
      return;
    }

    const targetMessage = chat.value.messages.find((message) => message.id === messageId);

    if (!targetMessage || targetMessage.role !== 'user') {
      return;
    }

    editingMessageId.value = messageId;
    editingContent.value = getAiChatMessageText(targetMessage, '\n').trim();
  }

  function appendMessage(message: AiChatMessage): void {
    chat.value.messages = [...chat.value.messages, message];
  }

  function updateMessage(messageId: string, patch: (message: AiChatMessage) => AiChatMessage): void {
    chat.value.messages = chat.value.messages.map((message) => (message.id === messageId ? patch(message) : message));
  }

  function reset(nextMessages: AiChatMessage[] = []): void {
    chat.value.stop();
    chat.value.messages = nextMessages;
    chat.value.clearError();
    currentConfirm.value = undefined;
    input.value = '';
    attachments.value = [];
    selectedMcps.value = [];
    cancelEditMessage();
  }

  function clear(): void {
    reset();
  }

  async function submit(payload: AiChatSubmitPayload = {}): Promise<void> {
    const content = payload.content ?? input.value;
    const submitAttachments = payload.attachments ?? attachments.value;

    if (!content.trim() && submitAttachments.length === 0) {
      return;
    }

    const metadata = toMessageMetadata({
      ...payload,
      attachments: submitAttachments,
    });

    // 发送后立即清空输入草稿，消息列表由 AI SDK Chat 自己追加 user message。
    input.value = '';
    attachments.value = [];
    selectedMcps.value = [];
    currentConfirm.value = undefined;

    await chat.value.sendMessage(
      {
        role: 'user',
        metadata,
        parts: [
          ...toFileParts(submitAttachments),
          ...(content.trim() ? [{ type: 'text' as const, text: content.trim() }] : []),
        ],
      },
      {
        metadata,
      }
    );
  }

  async function stop(): Promise<void> {
    if (!canStop.value) {
      return;
    }

    try {
      await chat.value.stop();
      await options.onStop?.();
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  async function retry(messageId?: string): Promise<void> {
    if (loading.value) {
      return;
    }

    await chat.value.regenerate({ messageId });
  }

  async function edit(messageId: string, content: string, options: AiChatSubmitPayload['options'] = {}): Promise<void> {
    if (loading.value) {
      return;
    }

    const targetMessage = chat.value.messages.find((message) => message.id === messageId);

    if (!targetMessage || targetMessage.role !== 'user') {
      return;
    }

    const metadata: AiChatMeta = {
      ...targetMessage.metadata,
      mcps: options.mcps ?? targetMessage.metadata?.mcps,
    };

    await chat.value.sendMessage(
      {
        messageId,
        role: 'user',
        metadata,
        parts: [
          ...toFileParts(metadata?.attachments),
          {
            type: 'text',
            text: content.trim(),
          },
        ],
      },
      {
        metadata,
      }
    );
  }

  async function submitEditMessage(): Promise<void> {
    if (!canSubmitEdit.value) {
      return;
    }

    const messageId = editingMessageId.value;
    const content = editingContent.value.trim();

    cancelEditMessage();
    await edit(messageId, content);
  }

  async function confirm(data: AgentChatConfirmData, answers: Record<string, string>): Promise<void> {
    await options.onConfirm?.(data, answers);
    currentConfirm.value = undefined;
  }

  const runtime: AiChatRuntime = {
    state: {
      messages,
      input,
      attachments,
      selectedMcps,
      status,
      loading,
      streaming,
      error,
      canSubmit,
      canStop,
      pendingConfirm,
      editingMessageId,
      editingContent,
      editingMessage,
      canSubmitEdit,
    },
    chat,
    transport,
    setInput,
    setAttachments,
    setSelectedMcps,
    removeAttachment,
    submit,
    stop,
    retry,
    edit,
    startEditMessage,
    cancelEditMessage,
    setEditingContent,
    submitEditMessage,
    confirm,
    appendMessage,
    updateMessage,
    reset,
    clear,
  };

  return markRaw(runtime);
}
