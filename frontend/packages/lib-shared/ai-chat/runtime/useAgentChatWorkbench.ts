import { computed, ref } from 'vue';

import type {
  AgentChatConfirmData,
  AgentChatStreamEvent,
  AgentConversationDetail,
  AgentConversationItem,
  AgentConversationPageResult,
} from '@lib/shared/models/ai';

import createAgentChatTransport from './createAgentChatTransport';
import createAiChatRuntime from './createAiChatRuntime';
import type { AiChatRuntime } from './types';
import type { AiChatAttachment, AiChatMcp, AiChatMessage } from '../types';
import { toAiChatMessage } from '../utils/conversation';

interface AgentChatWorkbenchApis {
  streamAgentChat: (
    data: {
      message: string;
      conversationId?: string;
      mcpIds?: string[];
      attachmentIds?: string[];
      picIds?: string[];
    },
    options: {
      signal?: AbortSignal;
      onSession: (sessionId: string, conversationId?: string) => void;
    }
  ) => AsyncIterable<AgentChatStreamEvent>;
  cancelAgentChat: (data: { conversationId: string; sessionId: string }) => Promise<unknown>;
  confirmAgentChat: (dialogId: string, answers: Record<string, string>) => Promise<unknown>;
  getAgentConversationPage: (data: {
    current: number;
    pageSize: number;
    keyword?: string;
  }) => Promise<AgentConversationPageResult>;
  getAgentConversationDetail: (conversationId: string) => Promise<AgentConversationDetail>;
  deleteAgentConversation: (conversationId: string) => Promise<unknown>;
  renameAgentConversation: (conversationId: string, data: { title: string }) => Promise<unknown>;
}

interface UseAgentChatWorkbenchOptions {
  historyPageSize?: number;
  apis: AgentChatWorkbenchApis;
  onError?: (error: Error) => void;
}

interface ConversationDraft {
  input: string;
  attachments: AiChatAttachment[];
  selectedMcps: AiChatMcp[];
}

function getAttachmentId(attachment: AiChatAttachment): string {
  const fileId = attachment.metadata?.fileId;

  return typeof fileId === 'string' ? fileId : attachment.id;
}

function getAttachmentIds(attachments: AiChatAttachment[] = []): string[] {
  return attachments
    .filter((attachment) => attachment.kind !== 'image')
    .map(getAttachmentId)
    .filter(Boolean);
}

function getPicIds(attachments: AiChatAttachment[] = []): string[] {
  return attachments
    .filter((attachment) => attachment.kind === 'image')
    .map(getAttachmentId)
    .filter(Boolean);
}

export default function useAgentChatWorkbench(options: UseAgentChatWorkbenchOptions) {
  const runtime = ref<AiChatRuntime>();
  const agentConversationId = ref('');
  const agentSessionId = ref('');
  const activeHistoryId = ref('');
  const historyItems = ref<AgentConversationItem[]>([]);
  const historyLoading = ref(false);
  const historyNoMore = ref(true);
  const historyKeyword = ref('');
  const historyCurrent = ref(1);
  const pendingConfirm = computed(() => runtime.value?.state.pendingConfirm.value);
  const historyPageSize = options.historyPageSize ?? 20;
  const conversationDrafts = new Map<string, ConversationDraft>();

  const NEW_CONVERSATION_DRAFT_KEY = '__new__';
  function getCurrentDraftKey(): string {
    return activeHistoryId.value || agentConversationId.value || NEW_CONVERSATION_DRAFT_KEY;
  }

  function hasDraft(draft: ConversationDraft): boolean {
    return Boolean(draft.input.trim() || draft.attachments.length || draft.selectedMcps.length);
  }

  function saveCurrentDraft(): void {
    if (!runtime.value) {
      return;
    }

    const draft: ConversationDraft = {
      input: runtime.value.state.input.value,
      attachments: [...runtime.value.state.attachments.value],
      selectedMcps: [...runtime.value.state.selectedMcps.value],
    };
    const draftKey = getCurrentDraftKey();

    if (hasDraft(draft)) {
      conversationDrafts.set(draftKey, draft);
    } else {
      conversationDrafts.delete(draftKey);
    }
  }

  function restoreDraft(draftKey: string): void {
    const draft = conversationDrafts.get(draftKey);

    runtime.value?.setInput(draft?.input ?? '');
    runtime.value?.setAttachments(draft?.attachments ?? []);
    runtime.value?.setSelectedMcps(draft?.selectedMcps ?? []);
  }

  function clearDraft(draftKey = getCurrentDraftKey()): void {
    conversationDrafts.delete(draftKey);
  }

  async function loadHistory(loadOptions: { reset?: boolean; keyword?: string } = {}): Promise<void> {
    const reset = loadOptions.reset ?? false;

    if (historyLoading.value && !reset) {
      return;
    }

    if (typeof loadOptions.keyword === 'string') {
      historyKeyword.value = loadOptions.keyword;
    }

    if (reset) {
      historyCurrent.value = 1;
      historyNoMore.value = false;
    }

    historyLoading.value = true;

    try {
      const res = await options.apis.getAgentConversationPage({
        current: historyCurrent.value,
        pageSize: historyPageSize,
        keyword: historyKeyword.value || undefined,
      });
      const list = res.list ?? [];

      historyItems.value = reset ? list : [...historyItems.value, ...list];
      historyNoMore.value = historyItems.value.length >= (res.total ?? 0);
      historyCurrent.value += 1;
    } finally {
      historyLoading.value = false;
    }
  }

  function createRuntime(initialMessages: AiChatMessage[] = []): AiChatRuntime {
    return createAiChatRuntime({
      initialMessages,
      transport: createAgentChatTransport({
        send(context) {
          return options.apis.streamAgentChat(
            {
              message: context.content,
              conversationId: agentConversationId.value || undefined,
              mcpIds: context.metadata?.mcps?.map((mcp) => mcp.id),
              attachmentIds: getAttachmentIds(context.metadata?.attachments),
              picIds: getPicIds(context.metadata?.attachments),
            },
            {
              signal: context.signal,
              onSession(sessionId, conversationId) {
                agentConversationId.value = conversationId || agentConversationId.value;
                agentSessionId.value = sessionId;
              },
            }
          );
        },
      }),
      async onStop() {
        if (agentConversationId.value && agentSessionId.value) {
          await options.apis.cancelAgentChat({
            conversationId: agentConversationId.value,
            sessionId: agentSessionId.value,
          });
          return true;
        }
        return false;
      },
      async onConfirm(data: AgentChatConfirmData, answerMap) {
        if (data.dialogId) {
          await options.apis.confirmAgentChat(data.dialogId, answerMap);
        }
      },
      async onFinish() {
        const conversationId = agentConversationId.value;

        if (!conversationId) {
          return;
        }

        clearDraft(conversationId);
        clearDraft(NEW_CONVERSATION_DRAFT_KEY);
        await loadHistory({ reset: true });
        activeHistoryId.value = conversationId;
      },
      onError: options.onError,
    });
  }

  function createConversation(initialMessages: AiChatMessage[] = []): AiChatRuntime {
    if (getCurrentDraftKey() === NEW_CONVERSATION_DRAFT_KEY) {
      clearDraft(NEW_CONVERSATION_DRAFT_KEY);
    } else {
      saveCurrentDraft();
    }

    agentConversationId.value = '';
    agentSessionId.value = '';
    activeHistoryId.value = '';

    if (runtime.value) {
      runtime.value.reset(initialMessages);
    } else {
      runtime.value = createRuntime(initialMessages);
    }
    restoreDraft(NEW_CONVERSATION_DRAFT_KEY);

    return runtime.value;
  }

  async function loadMoreHistory(): Promise<void> {
    if (historyNoMore.value) {
      return;
    }

    await loadHistory();
  }

  async function searchHistory(keyword: string): Promise<void> {
    await loadHistory({ reset: true, keyword });
  }

  async function openHistoryConversation(conversationId: string): Promise<AiChatRuntime> {
    saveCurrentDraft();
    const detail = await options.apis.getAgentConversationDetail(conversationId);
    const messages = (detail.messages ?? []).map(toAiChatMessage);

    agentConversationId.value = conversationId;
    agentSessionId.value = '';
    activeHistoryId.value = conversationId;

    if (runtime.value) {
      runtime.value.reset(messages);
    } else {
      runtime.value = createRuntime(messages);
    }
    restoreDraft(conversationId);

    return runtime.value;
  }

  async function deleteHistoryConversation(conversationId: string): Promise<void> {
    await options.apis.deleteAgentConversation(conversationId);
    conversationDrafts.delete(conversationId);
    historyItems.value = historyItems.value.filter((item) => item.id !== conversationId);

    if (activeHistoryId.value === conversationId) {
      agentConversationId.value = '';
      agentSessionId.value = '';
      activeHistoryId.value = '';
      runtime.value?.reset();
      restoreDraft(NEW_CONVERSATION_DRAFT_KEY);
    }
  }

  async function renameHistoryConversation(conversationId: string, title: string): Promise<void> {
    await options.apis.renameAgentConversation(conversationId, { title });
    historyItems.value = historyItems.value.map((item) => (item.id === conversationId ? { ...item, title } : item));
  }

  function clear(): void {
    runtime.value?.clear();
  }

  return {
    runtime,
    activeHistoryId,
    historyItems,
    historyLoading,
    historyNoMore,
    pendingConfirm,
    createConversation,
    loadHistory,
    loadMoreHistory,
    searchHistory,
    openHistoryConversation,
    deleteHistoryConversation,
    renameHistoryConversation,
    clear,
  };
}
