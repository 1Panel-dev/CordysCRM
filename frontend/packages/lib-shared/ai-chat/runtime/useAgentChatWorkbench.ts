import { computed, ref, shallowReactive } from 'vue';

import type {
  AgentChatConfirmData,
  AgentChatConfirmRequest,
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
import { getAiChatMessageText } from '../utils/message';

interface AgentChatWorkbenchApis {
  streamAgentChat: (
    data: {
      message: string;
      requestId: string;
      conversationId?: string;
      modelId?: string;
      mcpIds?: string[];
      attachmentIds?: string[];
      picIds?: string[];
    },
    options: {
      signal?: AbortSignal;
      onSession: (sessionId: string, conversationId?: string) => void;
    }
  ) => AsyncIterable<AgentChatStreamEvent>;
  reconnectAgentChat: (
    data: {
      conversationId: string;
      requestId: string;
      lastSequence: number;
    },
    options: {
      signal?: AbortSignal;
      onSession?: (sessionId: string, conversationId?: string) => void;
    }
  ) => AsyncIterable<AgentChatStreamEvent>;
  getAgentChatStatus: (data: { requestIds: string[] }) => Promise<Record<string, boolean>>;
  cancelAgentChat: (data: { conversationId?: string; sessionId?: string; requestId: string }) => Promise<unknown>;
  confirmAgentChat: (dialogId: string, request: AgentChatConfirmRequest) => Promise<unknown>;
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

interface ConversationRuntimeEntry {
  // key 用来标识一次前端会话实例。新会话还没有 conversationId 时，也需要一个稳定 key
  key: string;
  conversationId: string;
  sessionId: string;
  // 本轮发送的请求级幂等键，用于未产生 runId 前的取消定位与保存兜底
  requestId: string;
  lastSequence: number;
  streamStatus: 'idle' | 'streaming' | 'disconnected' | 'done' | 'error' | 'cancelled';
  pendingDisconnectAfterRun: boolean;
  reconnecting: boolean;
  reconnectVersion: number;
  runtime: AiChatRuntime;
}

function createChatRequestId(): string {
  return `req_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
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
  const activeEntry = ref<ConversationRuntimeEntry>();
  const runtime = computed(() => activeEntry.value?.runtime);
  const activeHistoryId = computed(() => activeEntry.value?.conversationId ?? '');
  const activeRuntimeKey = computed(() => activeEntry.value?.key ?? '');
  const historyItems = ref<AgentConversationItem[]>([]);
  const historyLoading = ref(false);
  const historyNoMore = ref(true);
  const historyKeyword = ref('');
  const historyCurrent = ref(1);
  const pendingConfirm = computed(() => runtime.value?.state.pendingConfirm.value);
  const loading = computed(() => Boolean(runtime.value?.state.loading.value));
  const historyPageSize = options.historyPageSize ?? 20;
  // 同一个页面内允许多个会话同时存在；切走运行中会话时只断开前端 SSE，后端任务继续跑。
  const runtimeEntries = new Map<string, ConversationRuntimeEntry>();
  // Map 本身不是响应式的，用版本号通知 computed 重新收集 runtimeEntries。
  const runtimeEntryVersion = ref(0);
  // 切换会话时保留未发送的输入、附件和 MCP 选择。
  const conversationDrafts = new Map<string, ConversationDraft>();
  let newConversationIndex = 0;
  let runningStatusPollTimer: number | undefined;
  let runningStatusPolling = false;
  let historyRefreshTimer: number | undefined;

  const NEW_CONVERSATION_DRAFT_KEY = '__new__';
  function getCurrentDraftKey(): string {
    return activeHistoryId.value || NEW_CONVERSATION_DRAFT_KEY;
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

  function getUniqueRuntimeEntries(): ConversationRuntimeEntry[] {
    return Array.from(new Set(runtimeEntries.values()));
  }

  function touchRuntimeEntries(): void {
    runtimeEntryVersion.value += 1;
    refreshRunningStatusPoller();
  }

  function setActiveEntry(entry: ConversationRuntimeEntry): void {
    activeEntry.value = entry;
  }

  function isEntryRunning(entry: ConversationRuntimeEntry): boolean {
    return ['streaming', 'disconnected'].includes(entry.streamStatus);
  }

  function updateEntrySequence(entry: ConversationRuntimeEntry, event: AgentChatStreamEvent): void {
    if (typeof event.sequence === 'number' && event.sequence > entry.lastSequence) {
      entry.lastSequence = event.sequence;
    }
  }

  function getDisconnectedRunningEntries(): ConversationRuntimeEntry[] {
    return getUniqueRuntimeEntries().filter(
      (entry) => entry.requestId && entry.conversationId && entry.streamStatus === 'disconnected'
    );
  }

  async function pollDisconnectedRunningEntries(): Promise<void> {
    if (runningStatusPolling) {
      return;
    }

    const entries = getDisconnectedRunningEntries();

    if (!entries.length) {
      refreshRunningStatusPoller();
      return;
    }

    runningStatusPolling = true;

    try {
      const statusMap = await options.apis.getAgentChatStatus({
        requestIds: entries.map((entry) => entry.requestId),
      });
      let hasFinishedEntry = false;

      entries.forEach((entry) => {
        // true 表示已输出并入库；false 或缺省表示仍未确认完成，切回时还有 reconnect 兜底。
        if (statusMap[entry.requestId] === true) {
          entry.streamStatus = 'done';
          entry.lastSequence = 0;
          hasFinishedEntry = true;
        }
      });

      if (hasFinishedEntry) {
        touchRuntimeEntries();
        scheduleHistoryRefresh();
      }
    } catch (error) {
      // 状态轮询只影响左侧 loading 展示，失败时保留现状，切回时由 reconnect 兜底。
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      runningStatusPolling = false;
      refreshRunningStatusPoller();
    }
  }

  function refreshRunningStatusPoller(): void {
    const shouldPoll = getDisconnectedRunningEntries().length > 0;

    if (!shouldPoll && runningStatusPollTimer) {
      window.clearInterval(runningStatusPollTimer);
      runningStatusPollTimer = undefined;
      return;
    }

    if (shouldPoll && !runningStatusPollTimer) {
      runningStatusPollTimer = window.setInterval(() => {
        void pollDisconnectedRunningEntries();
      }, 8000);
      void pollDisconnectedRunningEntries();
    }
  }

  async function disconnectEntryStream(entry?: ConversationRuntimeEntry): Promise<void> {
    if (!entry || !['streaming', 'disconnected'].includes(entry.streamStatus)) {
      return;
    }

    if (!entry.conversationId) {
      entry.pendingDisconnectAfterRun = true;
      entry.reconnecting = false;
      entry.reconnectVersion += 1;
      return;
    }

    entry.streamStatus = 'disconnected';
    entry.reconnecting = false;
    entry.reconnectVersion += 1;
    touchRuntimeEntries();

    await entry.runtime.disconnectStream();
  }

  async function switchActiveEntry(entry: ConversationRuntimeEntry): Promise<void> {
    if (activeEntry.value && activeEntry.value !== entry) {
      void disconnectEntryStream(activeEntry.value);
    }

    setActiveEntry(entry);
  }

  function cacheRuntimeEntry(entry: ConversationRuntimeEntry): void {
    // 同一个 entry 会同时按临时 key 和 conversationId 缓存：
    // 1. 临时 key 负责新会话未落库前的 Provider remount。
    // 2. conversationId 负责点击历史会话时复用正在运行的 runtime。
    runtimeEntries.set(entry.key, entry);

    if (entry.conversationId) {
      runtimeEntries.set(entry.conversationId, entry);
    }

    touchRuntimeEntries();
  }

  function getRuntimeHistoryTitle(entry: ConversationRuntimeEntry): string {
    const firstUserMessage = entry.runtime.state.messages.value.find((message) => message.role === 'user');
    const title = firstUserMessage ? getAiChatMessageText(firstUserMessage, ' ').trim() : '';

    return title || entry.conversationId;
  }

  function toRuntimeHistoryItem(entry: ConversationRuntimeEntry): AgentConversationItem {
    return {
      id: entry.conversationId,
      title: getRuntimeHistoryTitle(entry),
      localPending: true,
    };
  }

  function getLocalRunningHistoryItems(): AgentConversationItem[] {
    return getUniqueRuntimeEntries()
      .filter((entry) => entry.conversationId && isEntryRunning(entry))
      .map(toRuntimeHistoryItem);
  }

  const runningHistoryIds = computed(() => {
    void runtimeEntryVersion.value;

    // 只暴露正在生成中的 conversationId，列表组件不需要知道 runtime 细节。
    return getUniqueRuntimeEntries()
      .filter((entry) => entry.conversationId && isEntryRunning(entry))
      .map((entry) => entry.conversationId);
  });

  function mergeLocalRunningHistoryItems(list: AgentConversationItem[]): AgentConversationItem[] {
    const mergedList = [...list];

    getLocalRunningHistoryItems().forEach((item) => {
      if (!mergedList.some((historyItem) => historyItem.id === item.id)) {
        mergedList.unshift(item);
      }
    });

    return mergedList;
  }

  function mergeHistoryItems(
    currentList: AgentConversationItem[],
    nextList: AgentConversationItem[]
  ): AgentConversationItem[] {
    return [...currentList, ...nextList].reduce<AgentConversationItem[]>((result, item) => {
      const index = result.findIndex((historyItem) => historyItem.id === item.id);

      if (index === -1) {
        result.push(item);
      } else if (result[index].localPending && !item.localPending) {
        result[index] = item;
      }

      return result;
    }, []);
  }

  function upsertRuntimeHistoryItem(entry: ConversationRuntimeEntry): void {
    if (!entry.conversationId || !isEntryRunning(entry)) {
      return;
    }

    // SSE 返回 conversationId 后，历史列表需要立刻出现这一项并显示 loading。
    const item = toRuntimeHistoryItem(entry);

    if (historyItems.value.some((historyItem) => historyItem.id === item.id)) {
      historyItems.value = historyItems.value.map((historyItem) =>
        historyItem.id === item.id ? { ...historyItem, title: historyItem.title || item.title } : historyItem
      );
      return;
    }

    historyItems.value = [item, ...historyItems.value];
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
      const list = mergeLocalRunningHistoryItems(res.list ?? []);

      historyItems.value = reset ? list : mergeHistoryItems(historyItems.value, list);
      historyNoMore.value = historyItems.value.length >= (res.total ?? 0);
      historyCurrent.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      historyLoading.value = false;
    }
  }

  function scheduleHistoryRefresh(): void {
    if (historyRefreshTimer) {
      window.clearTimeout(historyRefreshTimer);
    }

    historyRefreshTimer = window.setTimeout(() => {
      historyRefreshTimer = undefined;
      void loadHistory({ reset: true });
    }, 400);
  }

  function createRuntime(entry: ConversationRuntimeEntry, initialMessages: AiChatMessage[] = []): AiChatRuntime {
    async function loadConversationDetailToRuntime(): Promise<void> {
      if (!entry.conversationId) {
        return;
      }

      const detail = await options.apis.getAgentConversationDetail(entry.conversationId);
      const messages = (detail.messages ?? []).map(toAiChatMessage);

      entry.runtime.reset(messages);
      restoreDraft(entry.conversationId);
    }

    return createAiChatRuntime({
      initialMessages,
      transport: createAgentChatTransport({
        onEvent(event) {
          updateEntrySequence(entry, event);

          if (event.type === 'done') {
            entry.streamStatus = 'done';
            touchRuntimeEntries();
          } else if (event.type === 'error') {
            entry.streamStatus = 'error';
            touchRuntimeEntries();
          }
        },
        send(context) {
          // 每一轮发送分配唯一 requestId，作为未产生 runId 前的取消锚点与保存兜底
          entry.requestId = createChatRequestId();
          entry.lastSequence = 0;
          entry.streamStatus = 'streaming';
          entry.pendingDisconnectAfterRun = false;
          entry.reconnecting = false;
          entry.reconnectVersion += 1;
          touchRuntimeEntries();

          return options.apis.streamAgentChat(
            {
              message: context.content,
              requestId: entry.requestId,
              conversationId: entry.conversationId || undefined,
              modelId: context.metadata?.model?.id,
              mcpIds: context.metadata?.mcps?.map((mcp) => mcp.id),
              attachmentIds: getAttachmentIds(context.metadata?.attachments),
              picIds: getPicIds(context.metadata?.attachments),
            },
            {
              signal: context.signal,
              onSession(sessionId, conversationId) {
                entry.sessionId = sessionId;

                if (conversationId) {
                  // 新会话第一次发送后，后端才会返回真实 conversationId。
                  // 这里把同一个 runtime 重新挂到真实 conversationId 上，后续点击历史会复用它。
                  entry.conversationId = conversationId;
                  cacheRuntimeEntry(entry);
                  upsertRuntimeHistoryItem(entry);

                  if (entry.pendingDisconnectAfterRun && activeEntry.value !== entry) {
                    entry.pendingDisconnectAfterRun = false;
                    void disconnectEntryStream(entry);
                  }
                }
              },
            }
          );
        },
        reconnect() {
          return options.apis.reconnectAgentChat(
            {
              conversationId: entry.conversationId,
              requestId: entry.requestId,
              lastSequence: entry.lastSequence,
            },
            {
              onSession(sessionId, conversationId) {
                entry.sessionId = sessionId;

                if (conversationId) {
                  entry.conversationId = conversationId;
                  cacheRuntimeEntry(entry);
                  upsertRuntimeHistoryItem(entry);
                }
              },
            }
          );
        },
        async onReconnectFinished() {
          entry.streamStatus = 'done';
          entry.reconnecting = false;
          touchRuntimeEntries();
          await loadConversationDetailToRuntime();
          scheduleHistoryRefresh();
        },
      }),
      async onStop() {
        // 有 requestId 即可取消：未产生 runId / conversationId 时也能由后端按 requestId 定位、补停并保存部分块
        if (entry.requestId) {
          entry.streamStatus = 'cancelled';
          touchRuntimeEntries();
          await options.apis.cancelAgentChat({
            conversationId: entry.conversationId || undefined,
            sessionId: entry.sessionId || undefined,
            requestId: entry.requestId,
          });
          return true;
        }
        return false;
      },
      async onConfirm(data: AgentChatConfirmData, request) {
        if (data.dialogId) {
          await options.apis.confirmAgentChat(data.dialogId, request);
        }
      },
      async onFinish(event) {
        entry.reconnecting = false;

        if (event?.isAbort && (entry.streamStatus === 'disconnected' || entry.reconnecting)) {
          touchRuntimeEntries();
          return;
        }

        const conversationId = entry.conversationId;

        if (!conversationId) {
          return;
        }

        if (!['error', 'cancelled'].includes(entry.streamStatus)) {
          entry.streamStatus = 'done';
          touchRuntimeEntries();
        }

        clearDraft(conversationId);
        clearDraft(NEW_CONVERSATION_DRAFT_KEY);
        // 生成结束后刷新历史，拿后端最终标题和排序，同时移除本地临时补位。
        scheduleHistoryRefresh();
      },
      onError: options.onError,
    });
  }

  function createConversation(initialMessages: AiChatMessage[] = []): AiChatRuntime {
    void disconnectEntryStream(activeEntry.value);

    if (getCurrentDraftKey() === NEW_CONVERSATION_DRAFT_KEY) {
      clearDraft(NEW_CONVERSATION_DRAFT_KEY);
    } else {
      saveCurrentDraft();
    }

    const entry = shallowReactive<ConversationRuntimeEntry>({
      // 新会话还没有后端 id，用前端临时 key 区分多次新建。
      key: `${NEW_CONVERSATION_DRAFT_KEY}_${newConversationIndex}`,
      conversationId: '',
      sessionId: '',
      requestId: '',
      lastSequence: 0,
      streamStatus: 'idle',
      pendingDisconnectAfterRun: false,
      reconnecting: false,
      reconnectVersion: 0,
      runtime: undefined as unknown as AiChatRuntime,
    });
    newConversationIndex += 1;
    entry.runtime = createRuntime(entry, initialMessages);
    cacheRuntimeEntry(entry);
    setActiveEntry(entry);

    return entry.runtime;
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

  async function reconnectRuntimeEntry(entry: ConversationRuntimeEntry): Promise<void> {
    if (!entry.conversationId || !entry.requestId || entry.reconnecting || entry.streamStatus === 'streaming') {
      return;
    }

    const reconnectVersion = entry.reconnectVersion + 1;
    entry.reconnectVersion = reconnectVersion;
    entry.reconnecting = true;
    entry.streamStatus = 'streaming';
    touchRuntimeEntries();

    try {
      await entry.runtime.resumeStream();
    } catch (error) {
      if (entry.reconnectVersion === reconnectVersion) {
        entry.streamStatus = 'disconnected';
      }
      options.onError?.(error instanceof Error ? error : new Error(String(error)));
    } finally {
      if (entry.reconnectVersion === reconnectVersion) {
        entry.reconnecting = false;
        touchRuntimeEntries();
      }
    }
  }

  async function loadHistoryConversationIntoEntry(entry: ConversationRuntimeEntry): Promise<void> {
    const detail = await options.apis.getAgentConversationDetail(entry.conversationId);
    const messages = (detail.messages ?? []).map(toAiChatMessage);

    entry.runtime.reset(messages);
    entry.streamStatus = 'done';
    entry.lastSequence = 0;
    touchRuntimeEntries();
    restoreDraft(entry.conversationId);
  }

  async function openHistoryConversation(conversationId: string): Promise<AiChatRuntime> {
    saveCurrentDraft();

    const cachedEntry = runtimeEntries.get(conversationId);

    if (cachedEntry) {
      await switchActiveEntry(cachedEntry);

      if (cachedEntry.streamStatus === 'disconnected') {
        void reconnectRuntimeEntry(cachedEntry);
      } else if (!isEntryRunning(cachedEntry) && cachedEntry.streamStatus !== 'idle') {
        await loadHistoryConversationIntoEntry(cachedEntry);
      }

      return cachedEntry.runtime;
    }

    try {
      void disconnectEntryStream(activeEntry.value);
      const detail = await options.apis.getAgentConversationDetail(conversationId);
      const messages = (detail.messages ?? []).map(toAiChatMessage);

      const entry = shallowReactive<ConversationRuntimeEntry>({
        key: conversationId,
        conversationId,
        sessionId: '',
        requestId: '',
        lastSequence: 0,
        streamStatus: 'done',
        pendingDisconnectAfterRun: false,
        reconnecting: false,
        reconnectVersion: 0,
        runtime: undefined as unknown as AiChatRuntime,
      });
      entry.runtime = createRuntime(entry, messages);
      cacheRuntimeEntry(entry);
      setActiveEntry(entry);
      restoreDraft(conversationId);

      return entry.runtime;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      return runtime.value ?? createConversation();
    }
  }

  async function deleteHistoryConversation(conversationId: string): Promise<void> {
    try {
      await options.apis.deleteAgentConversation(conversationId);
      conversationDrafts.delete(conversationId);
      historyItems.value = historyItems.value.filter((item) => item.id !== conversationId);
      const deletedEntry = runtimeEntries.get(conversationId);

      if (deletedEntry) {
        deletedEntry.runtime.clear();
        Array.from(runtimeEntries.entries()).forEach(([key, entry]) => {
          if (entry === deletedEntry) {
            runtimeEntries.delete(key);
          }
        });
        touchRuntimeEntries();
      }

      if (activeHistoryId.value === conversationId) {
        createConversation();
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function renameHistoryConversation(conversationId: string, title: string): Promise<void> {
    try {
      await options.apis.renameAgentConversation(conversationId, { title });
      historyItems.value = historyItems.value.map((item) => (item.id === conversationId ? { ...item, title } : item));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function clear(): void {
    if (runningStatusPollTimer) {
      window.clearInterval(runningStatusPollTimer);
      runningStatusPollTimer = undefined;
    }

    if (historyRefreshTimer) {
      window.clearTimeout(historyRefreshTimer);
      historyRefreshTimer = undefined;
    }

    getUniqueRuntimeEntries().forEach((entry) => {
      entry.runtime.clear();
    });
    runtimeEntries.clear();
    touchRuntimeEntries();
    activeEntry.value = undefined;
  }

  async function disconnectActiveConversation(): Promise<void> {
    await disconnectEntryStream(activeEntry.value);
  }

  async function resumeActiveConversation(): Promise<void> {
    const entry = activeEntry.value;

    if (!entry) {
      return;
    }

    if (entry.streamStatus === 'disconnected') {
      void reconnectRuntimeEntry(entry);
    } else if (entry.streamStatus === 'done' && entry.conversationId && entry.requestId) {
      await loadHistoryConversationIntoEntry(entry);
    }
  }

  return {
    runtime,
    activeHistoryId,
    activeRuntimeKey,
    historyItems,
    historyLoading,
    historyNoMore,
    runningHistoryIds,
    pendingConfirm,
    loading,
    createConversation,
    loadHistory,
    loadMoreHistory,
    searchHistory,
    openHistoryConversation,
    deleteHistoryConversation,
    renameHistoryConversation,
    disconnectActiveConversation,
    resumeActiveConversation,
    clear,
  };
}
