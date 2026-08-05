<template>
  <div class="flex h-full flex-col overflow-hidden bg-[var(--text-n10)]">
    <van-nav-bar title="CORDYS AI" class="h-[48px]" fixed>
      <template #left>
        <div class="inline-flex items-center gap-[14px]">
          <CrmIcon name="iconview-list" width="24px" height="24px" @click="showHistory = true" />
          <CrmIcon name="iconicon_close" width="24px" height="24px" @click="handleBack" />
        </div>
      </template>
      <template #right>
        <CrmIcon name="iconicon_add" width="24px" height="24px" @click="handleNewConversation" />
      </template>
    </van-nav-bar>

    <AiChatProvider v-if="runtime" :runtime="runtime">
      <main class="mt-[48px] min-h-0 flex-1 overflow-hidden">
        <AiMobileThread />
      </main>

      <AiMobileConfirmDialog v-if="pendingConfirm" :confirm="pendingConfirm" />

      <AiMobileComposer />

      <AiMobileHistoryDrawer
        v-model:show="showHistory"
        :items="historyItems"
        :active-id="activeHistoryId"
        :loading="historyLoading"
        :no-more="historyNoMore"
        @search="searchHistory"
        @reach-bottom="loadMoreHistory"
        @click="openHistoryConversation"
        @delete="deleteHistoryConversation"
        @rename="handleHistoryRename"
        @new="handleHistoryNewConversation"
      />
    </AiChatProvider>
  </div>
</template>

<script setup lang="ts">
  import { onBeforeUnmount, onMounted, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { showFailToast } from 'vant';

  import { AiChatProvider, useAgentChatWorkbench } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import AiMobileComposer from '@/components/business/ai-chat/components/AiMobileComposer.vue';
  import AiMobileConfirmDialog from '@/components/business/ai-chat/components/AiMobileConfirmDialog.vue';
  import AiMobileHistoryDrawer from '@/components/business/ai-chat/components/AiMobileHistoryDrawer.vue';
  import AiMobileThread from '@/components/business/ai-chat/components/AiMobileThread.vue';

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
  const router = useRouter();
  const route = useRoute();

  const currentModelName = 'CORDYS AI'; // TODO lmy 从后端获取name
  const showHistory = ref(false);
  const {
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
  } = useAgentChatWorkbench({
    modelName: currentModelName,
    historyPageSize: 20,
    apis: {
      streamAgentChat,
      cancelAgentChat,
      confirmAgentChat,
      getAgentConversationPage,
      getAgentConversationDetail,
      deleteAgentConversation,
      renameAgentConversation,
    },
    onError(error) {
      showFailToast(error.message || t('common.operationFailed'));
    },
  });

  function handleBack() {
    router.back();
  }

  function handleNewConversation() {
    createConversation();
  }

  function handleHistoryNewConversation() {
    createConversation();
    showHistory.value = false;
  }

  function handleHistoryRename(conversationId: string, title: string, done: () => void): void {
    renameHistoryConversation(conversationId, title).finally(done);
  }

  let initialPromptSubmitted = false;
  async function submitInitialPrompt() {
    if (initialPromptSubmitted) {
      return;
    }

    const prompt = String(route.query.prompt ?? '').trim(); // 从 URL 里取 prompt

    if (!prompt) {
      return;
    }

    initialPromptSubmitted = true;
    const chatRuntime = runtime.value ?? createConversation();

    await chatRuntime.submit({ content: prompt });
  }

  onMounted(async () => {
    createConversation();
    await loadHistory({ reset: true });
    await submitInitialPrompt();
  });

  onBeforeUnmount(() => {
    clear();
  });
</script>
