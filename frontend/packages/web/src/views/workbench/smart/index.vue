<template>
  <n-scrollbar class="h-full">
    <div class="smart-workbench flex min-h-full flex-col gap-[16px]">
      <AiChatProvider :runtime="composerRuntime">
        <AiComposer
          class="rounded-[4px] !shadow-none"
          :model-name="currentModelName"
          :mcp-options="mcpOptions"
          submit-mode="emit"
          :placeholder="t('workbench.smart.composerPlaceholder')"
          @submit="handleComposerSubmit"
        />
      </AiChatProvider>
      <CrmCard no-content-padding hide-footer>
        <template #header>
          <div class="text-[14px] font-semibold">{{ t('workbench.dataOverView') }}</div>
        </template>
        <template #header-extra>
          <n-button type="primary" class="mr-[16px]" ghost @click="">{{ t('workbench.smart.reBuild') }}</n-button>
          <button class="gradient-border-button">
            <n-gradient-text
              :style="{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                fontWeight: 400,
              }"
              gradient="linear-gradient(96.9deg, #3370FF 0%, #E22E23 47.65%, #00C261 100%)"
            >
              <CrmIcon
                type="iconicon_star1"
                :size="16"
                color="linear-gradient(130.1deg, #FFA200 -30.47%, #E22E23 42.7%, #00C261 113.44%)"
              />
              {{ t('workbench.smart.AIRead') }}
            </n-gradient-text>
          </button>
        </template>
        <div class="p-[16px_24px]">
          <n-empty v-if="!dataOverviewAIRenderString" :description="t('common.noData')" />
          <div class="h-full w-full" v-html="dataOverviewAIRenderString"> </div>
        </div>
      </CrmCard>
      <div class="flex w-full gap-[16px]">
        <CrmCard class="flex-1" no-content-padding hide-footer>
          <template #header>
            <div class="flex items-center gap-[8px]">
              <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
              <div class="text-[14px] font-semibold">{{ t('workbench.smart.AIAction') }}</div>
            </div>
          </template>
          <div class="p-[16px_24px]">
            <n-empty v-if="!AIActionRenderString" :description="t('common.noData')" />
            <div class="h-full w-full" v-html="AIActionRenderString"> </div>
          </div>
        </CrmCard>
        <CrmCard class="flex-1" no-content-padding hide-footer>
          <template #header>
            <div class="flex items-center gap-[8px]">
              <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
              <div class="text-[14px] font-semibold">{{ t('workbench.smart.AIActionApproval') }}</div>
            </div>
          </template>
          <div class="p-[16px_24px]">
            <n-empty v-if="!AIActionApprovalRenderString" :description="t('common.noData')" />
            <div class="h-full w-full" v-html="AIActionApprovalRenderString"> </div>
          </div>
        </CrmCard>
      </div>
      <!-- 悬浮的 -->
      <div
        class="large-box-shadow n-btn-outline-primary fixed bottom-[24px] right-[24px] z-10 flex h-[48px] w-[48px] cursor-pointer items-center justify-center rounded-full border-[0.5px] border-[var(--primary-8)] bg-[var(--primary-7)] text-[var(--primary-8)]"
        @click="openChatDrawer"
      >
        <CrmIcon type="iconicon_bot" :size="24" />
      </div>
    </div>
  </n-scrollbar>

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
      :model-name="currentModelName"
      :mcp-options="mcpOptions"
      @new="handleNewConversation"
      @search-history="handleHistorySearch"
      @history-reach-bottom="loadMoreHistory"
      @history-click="handleHistoryClick"
      @history-delete="handleHistoryDelete"
      @history-rename="handleHistoryRename"
    />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { onBeforeUnmount, ref } from 'vue';
  import { NButton, NEmpty, NGradientText, NScrollbar } from 'naive-ui';

  import { useAgentChatWorkbench } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import {
    AiChat,
    type AiChatMcp,
    AiChatProvider,
    AiComposer,
    type AiComposerSubmitPayload,
    createAiChatRuntime,
  } from '@/components/business/ai-chat';

  import {
    cancelAgentChat,
    confirmAgentChat,
    deleteAgentConversation,
    getAgentConversationDetail,
    getAgentConversationPage,
    renameAgentConversation,
    streamAgentChat,
  } from '@/api/modules';

  const { t } = useI18n();
  const showChatDrawer = ref(false);
  const aiChatRef = ref<InstanceType<typeof AiChat>>();
  const chatSessionId = ref('');
  const currentModelName = 'CORDYS AI'; // TODO lmy 从后端获取name

  const mcpOptions: AiChatMcp[] = [];
  const dataOverviewAIRenderString = ref('');
  const AIActionRenderString = ref('');
  const AIActionApprovalRenderString = ref('');
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
    modelName: currentModelName,
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

  const composerRuntime = createAiChatRuntime({
    initialModelName: currentModelName,
  });

  function createChatSession(): ReturnType<typeof createConversation> {
    const sessionId = `chat_${Date.now()}`;

    chatSessionId.value = sessionId;
    return createConversation();
  }

  async function handleComposerSubmit(payload: AiComposerSubmitPayload): Promise<void> {
    const selectedMcps = payload.options?.mcps ?? [];
    const runtime = createChatSession();

    runtime.setSelectedMcps(selectedMcps);
    runtime.setModelName(currentModelName);
    showChatDrawer.value = true;
    composerRuntime.clear();

    await runtime.submit({
      content: payload.content,
      attachments: payload.attachments,
      options: {
        mcps: selectedMcps,
      },
    });
  }

  function openChatDrawer(): void {
    if (!chatRuntime.value) {
      createChatSession();
    }

    showChatDrawer.value = true;
    loadHistory({ reset: true }).catch(() => undefined);
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

  onBeforeUnmount(() => {
    composerRuntime.clear();
    clear();
  });
</script>

<style scoped lang="less">
  .gradient-border-button {
    padding: 4px 12px;
    border: 1px solid transparent;
    border-radius: 4px;
    background-clip: padding-box, border-box;

    /* background layer: button fill; border layer: gradient */
    background-image: linear-gradient(var(--primary-7), var(--primary-7)),
      linear-gradient(96.9deg, #3370ff 0%, #e22e23 47.65%, #00c261 100%);
    background-origin: border-box;
    &:hover {
      border: 1px solid transparent;
      background-image: linear-gradient(var(--n-button-color, #ffffff), var(--n-button-color, #ffffff)),
        linear-gradient(96.9deg, #3370ff 0%, #e22e23 47.65%, #00c261 100%) !important;
    }
  }
</style>
