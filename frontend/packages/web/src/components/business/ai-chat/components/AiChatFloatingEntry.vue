<template>
  <div
    class="ai-chat-floating-entry large-box-shadow n-btn-outline-primary fixed z-[1000] flex h-[40px] w-[40px] cursor-pointer items-center justify-center rounded-full bg-[var(--text-n10)]"
    :style="floatingStyle"
    @pointerdown="handlePointerDown"
    @click="handleFloatingClick"
  >
    <CrmIcon type="iconicon_crmbot" :size="24" color="linear-gradient(180deg, #00A6AB 0%, #3370FF 70.19%)" />
  </div>

  <CrmDrawer
    v-model:show="showChatDrawer"
    title="CORDYS AI"
    :width="1200"
    :footer="false"
    no-padding
    body-content-class="h-full"
  >
    <AiChat
      v-if="chatRuntime"
      ref="aiChatRef"
      :key="chatSessionId"
      :runtime="chatRuntime"
      :history-items="historyItems"
      :active-history-id="activeHistoryId"
      :history-loading="historyLoading"
      :history-no-more="historyNoMore"
      :mcp-options="mcpOptions"
      @new="handleNewConversation"
      @mcp-updated="loadMcpOptions"
      @search-history="handleHistorySearch"
      @history-reach-bottom="loadMoreHistory"
      @history-click="handleHistoryClick"
      @history-delete="handleHistoryDelete"
      @history-rename="handleHistoryRename"
    />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

  import type { AiChatAttachment, AiChatMcp } from '@lib/shared/ai-chat';
  import { useAgentChatWorkbench } from '@lib/shared/ai-chat';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import { AiChat } from '@/components/business/ai-chat';

  import {
    cancelAgentChat,
    confirmAgentChat,
    deleteAgentConversation,
    getAgentConversationDetail,
    getAgentConversationPage,
    getAgentMcpConfigList,
    renameAgentConversation,
    streamAgentChat,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import useLicenseStore from '@/store/modules/setting/license';

  const AI_CHAT_FLOATING_OPEN_EVENT = 'crm-ai-chat-floating-open';

  interface AiChatFloatingOpenPayload {
    content?: string;
    attachments?: AiChatAttachment[];
    mcps?: AiChatMcp[];
  }

  interface FloatingPosition {
    right: number;
    bottom: number;
  }

  const FLOATING_SIZE = 40;
  const DEFAULT_GAP = 24;
  const DRAG_THRESHOLD = 4;
  const STORAGE_KEY = 'crm_ai_chat_floating_entry_position';

  const licenseStore = useLicenseStore();
  const { openModal } = useModal();
  const showChatDrawer = ref(false);
  const aiChatRef = ref<InstanceType<typeof AiChat>>();
  const chatSessionId = ref('');
  const position = ref<FloatingPosition>({ right: DEFAULT_GAP, bottom: DEFAULT_GAP });
  const isDragging = ref(false);
  const ignoreNextClick = ref(false);

  const mcpOptions = ref<AiChatMcp[]>([]);
  async function loadMcpOptions(): Promise<void> {
    try {
      mcpOptions.value = await getAgentMcpConfigList();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const {
    runtime: chatRuntime,
    activeHistoryId,
    historyItems,
    historyLoading,
    historyNoMore,
    createConversation,
    loadHistory,
    loadMoreHistory,
    searchHistory,
    openHistoryConversation,
    deleteHistoryConversation,
    renameHistoryConversation,
    clear,
  } = useAgentChatWorkbench({
    historyPageSize: 50,
    apis: {
      streamAgentChat,
      cancelAgentChat,
      confirmAgentChat,
      getAgentConversationPage,
      getAgentConversationDetail,
      deleteAgentConversation,
      renameAgentConversation,
    },
  });

  const floatingStyle = computed(() => ({
    right: `${position.value.right}px`,
    bottom: `${position.value.bottom}px`,
  }));

  function clampOffset(offset: number, viewportSize: number): number {
    const maxOffset = Math.max(DEFAULT_GAP, viewportSize - FLOATING_SIZE - DEFAULT_GAP);
    return Math.min(Math.max(DEFAULT_GAP, offset), maxOffset);
  }

  function normalizePosition(nextPosition = position.value): FloatingPosition {
    return {
      right: clampOffset(nextPosition.right, window.innerWidth),
      bottom: clampOffset(nextPosition.bottom, window.innerHeight),
    };
  }

  function initPosition(): void {
    try {
      const savedPosition = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null') as FloatingPosition | null;
      position.value = normalizePosition(savedPosition ?? position.value);
    } catch {
      position.value = normalizePosition();
    }
  }

  function savePosition(): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(position.value));
  }

  function createChatSession(): ReturnType<typeof createConversation> {
    const sessionId = `chat_${Date.now()}`;
    chatSessionId.value = sessionId;
    return createConversation();
  }

  function ensureLicense(): boolean {
    if (licenseStore.hasLicense()) {
      return true;
    }

    openModal(licenseStore.getNoLicenseModalConfig());
    return false;
  }

  function openChatDrawer(): void {
    if (!ensureLicense()) {
      return;
    }

    if (!chatRuntime.value) {
      createChatSession();
    }

    showChatDrawer.value = true;
    loadMcpOptions();
    loadHistory({ reset: true }).catch(() => undefined);
  }

  async function openWithPayload(payload: AiChatFloatingOpenPayload): Promise<void> {
    if (!ensureLicense()) {
      return;
    }

    const selectedMcps = payload.mcps ?? [];
    const runtime = createChatSession();

    runtime.setSelectedMcps(selectedMcps);
    showChatDrawer.value = true;
    loadMcpOptions();

    await runtime.submit({
      content: payload.content ?? '',
      attachments: payload.attachments,
      options: {
        mcps: selectedMcps,
      },
    });
  }

  function handleFloatingClick(): void {
    if (ignoreNextClick.value) {
      ignoreNextClick.value = false;
      return;
    }

    openChatDrawer();
  }

  function handlePointerDown(event: PointerEvent): void {
    if (event.button !== 0) {
      return;
    }

    const startPosition = { ...position.value };
    const startPointer = { x: event.clientX, y: event.clientY };

    const handlePointerMove = (moveEvent: PointerEvent) => {
      const deltaX = moveEvent.clientX - startPointer.x;
      const deltaY = moveEvent.clientY - startPointer.y;

      if (Math.abs(deltaX) > DRAG_THRESHOLD || Math.abs(deltaY) > DRAG_THRESHOLD) {
        isDragging.value = true;
      }

      if (isDragging.value) {
        position.value = normalizePosition({
          right: startPosition.right - deltaX,
          bottom: startPosition.bottom - deltaY,
        });
      }
    };

    const handlePointerUp = () => {
      window.removeEventListener('pointermove', handlePointerMove);
      window.removeEventListener('pointerup', handlePointerUp);

      if (isDragging.value) {
        ignoreNextClick.value = true;
        savePosition();
      }

      isDragging.value = false;
    };

    window.addEventListener('pointermove', handlePointerMove);
    window.addEventListener('pointerup', handlePointerUp);
  }

  async function handleGlobalOpen(event: Event): Promise<void> {
    const payload = (event as CustomEvent<AiChatFloatingOpenPayload>).detail ?? {};
    await openWithPayload(payload);
  }

  async function handleHistorySearch(keyword: string): Promise<void> {
    await searchHistory(keyword);
  }

  function handleNewConversation(): void {
    chatSessionId.value = `chat_${Date.now()}`;
    createConversation();
  }

  async function handleHistoryClick(conversationId: string): Promise<void> {
    await openHistoryConversation(conversationId);
    chatSessionId.value = conversationId;
  }

  async function handleHistoryDelete(conversationId: string): Promise<void> {
    const deletingActive = activeHistoryId.value === conversationId;

    await deleteHistoryConversation(conversationId);

    if (deletingActive) {
      chatSessionId.value = `chat_${Date.now()}`;
    }
  }

  async function handleHistoryRename(conversationId: string, title: string): Promise<void> {
    try {
      await renameHistoryConversation(conversationId, title);
      aiChatRef.value?.finishHistoryRename(conversationId);
    } finally {
      aiChatRef.value?.resetHistoryRenameLoading(conversationId);
    }
  }

  function handleResize(): void {
    position.value = normalizePosition();
    savePosition();
  }

  onMounted(() => {
    initPosition();
    window.addEventListener(AI_CHAT_FLOATING_OPEN_EVENT, handleGlobalOpen);
    window.addEventListener('resize', handleResize);
  });

  onBeforeUnmount(() => {
    window.removeEventListener(AI_CHAT_FLOATING_OPEN_EVENT, handleGlobalOpen);
    window.removeEventListener('resize', handleResize);
    clear();
  });
</script>
