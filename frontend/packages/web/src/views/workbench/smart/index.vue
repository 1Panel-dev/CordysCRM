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
  import { NButton, NEmpty, NGradientText, NScrollbar } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AgentConversationDetail, AgentConversationItem, AgentConversationMessage } from '@lib/shared/models/ai';
  import type { AiModelItem } from '@lib/shared/models/system/aiModel';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import {
    AiChat,
    type AiChatMcp,
    type AiChatMessage,
    AiChatProvider,
    type AiChatRuntime,
    AiComposer,
    type AiComposerSubmitPayload,
    createAgentChatTransport,
    createAiChatRuntime,
  } from '@/components/business/ai-chat';

  import {
    cancelAgentChat,
    confirmAgentChat,
    deleteAgentConversation,
    getAgentConversationDetail,
    getAgentConversationPage,
    getAiModelList,
    renameAgentConversation,
    streamAgentChat,
  } from '@/api/modules';

  const { t } = useI18n();
  const showChatDrawer = ref(false);
  const aiChatRef = ref<InstanceType<typeof AiChat>>();
  const chatRuntime = ref<AiChatRuntime>();
  const chatSessionId = ref('');
  const agentConversationId = ref('');
  const agentSessionId = ref('');
  const activeModel = ref<AiModelItem>();
  const activeHistoryId = ref('');
  const historyItems = ref<AgentConversationItem[]>([]);
  const historyLoading = ref(false);
  const historyNoMore = ref(true);
  const historyKeyword = ref('');
  const historyCurrent = ref(1);
  const historyPageSize = 50;
  let historyRequestIndex = 0;
  const currentModelName = computed(() => activeModel.value?.displayName || activeModel.value?.modelName || '');
  const mcpOptions: AiChatMcp[] = [];
  const dataOverviewAIRenderString = ref('');
  const AIActionRenderString = ref('');
  const AIActionApprovalRenderString = ref('');

  async function loadHistory(options: { reset?: boolean; keyword?: string } = {}): Promise<void> {
    const reset = options.reset ?? false;

    if (historyLoading.value && !reset) {
      return;
    }

    historyRequestIndex += 1;
    const requestIndex = historyRequestIndex;

    if (typeof options.keyword === 'string') {
      historyKeyword.value = options.keyword;
    }

    if (reset) {
      historyCurrent.value = 1;
      historyNoMore.value = false;
    }

    historyLoading.value = true;

    try {
      const res = await getAgentConversationPage({
        current: historyCurrent.value,
        pageSize: historyPageSize,
        keyword: historyKeyword.value || undefined,
      });

      if (requestIndex !== historyRequestIndex) {
        return;
      }

      const list = res.list ?? [];

      historyItems.value = reset ? list : [...historyItems.value, ...list];
      historyNoMore.value = historyItems.value.length >= (res.total ?? 0);
      historyCurrent.value += 1;
    } finally {
      if (requestIndex === historyRequestIndex) {
        historyLoading.value = false;
      }
    }
  }

  async function loadDefaultModel(): Promise<void> {
    const res = await getAiModelList({ current: 1, pageSize: 100 });
    activeModel.value = res.list.find((model) => model.enable) ?? res.list[0];
  }

  function createWorkbenchRuntime(initialMessages: AiChatMessage[] = []): AiChatRuntime {
    return createAiChatRuntime({
      initialModelName: currentModelName.value,
      initialMessages,
      transport: createAgentChatTransport({
        send(context) {
          if (!activeModel.value) {
            throw new Error(t('workbench.smart.noModelTip'));
          }

          return streamAgentChat(
            {
              message: context.content,
              conversationId: agentConversationId.value || undefined,
              mcpNames: context.metadata?.mcps?.map((mcp) => mcp.name),
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
          await cancelAgentChat({
            conversationId: agentConversationId.value,
            sessionId: agentSessionId.value,
          });
        }
      },
      async onConfirm(data, answerMap) {
        const { dialogId } = data;

        if (dialogId) {
          await confirmAgentChat(dialogId, answerMap);
        }
      },
      async onFinish() {
        const conversationId = agentConversationId.value;

        if (!conversationId) {
          return;
        }

        await loadHistory({ reset: true });
        activeHistoryId.value = conversationId;
      },
    });
  }

  const composerRuntime = createAiChatRuntime({
    initialModelName: currentModelName.value,
  });

  function createChatSession(initialMessages: AiChatMessage[] = []): AiChatRuntime {
    const sessionId = `chat_${Date.now()}`;

    chatSessionId.value = sessionId;
    agentConversationId.value = '';
    agentSessionId.value = '';
    activeHistoryId.value = '';
    chatRuntime.value = createWorkbenchRuntime(initialMessages);

    return chatRuntime.value;
  }

  async function handleComposerSubmit(payload: AiComposerSubmitPayload): Promise<void> {
    if (!activeModel.value) {
      await loadDefaultModel();
    }

    const modelName = currentModelName.value;
    const selectedMcps = payload.options?.mcps ?? [];
    const runtime = createChatSession();

    runtime.setSelectedMcps(selectedMcps);
    runtime.setModelName(modelName);
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

  function toChatMessage(message: AgentConversationMessage, index: number): AiChatMessage {
    return {
      id: message.id || `history_${message.conversationId}_${index}`,
      role: message.role,
      metadata: {
        tokens: message.totalTokens,
      },
      parts: [
        {
          type: 'text',
          text: message.content || '',
        },
      ],
    };
  }

  function openChatDrawer(): void {
    if (!chatRuntime.value) {
      createChatSession();
    }

    showChatDrawer.value = true;
    loadHistory({ reset: true }).catch(() => undefined);
  }

  async function loadMoreHistory(): Promise<void> {
    if (historyNoMore.value) {
      return;
    }

    await loadHistory();
  }

  async function handleHistorySearch(keyword: string): Promise<void> {
    await loadHistory({ reset: true, keyword });
  }

  function handleNewConversation(): void {
    chatRuntime.value?.clear();
    agentConversationId.value = '';
    agentSessionId.value = '';
    activeHistoryId.value = '';
    chatSessionId.value = `chat_${Date.now()}`;
  }

  async function handleHistoryClick(conversationId: string): Promise<void> {
    const detail = (await getAgentConversationDetail(conversationId)) as AgentConversationDetail;
    const messages = (detail.messages ?? []).map(toChatMessage);

    agentConversationId.value = conversationId;
    agentSessionId.value = '';
    activeHistoryId.value = conversationId;
    chatSessionId.value = conversationId;
    chatRuntime.value = createWorkbenchRuntime(messages);
  }

  async function handleHistoryDelete(conversationId: string): Promise<void> {
    await deleteAgentConversation(conversationId);

    historyItems.value = historyItems.value.filter((item) => item.id !== conversationId);

    if (activeHistoryId.value === conversationId) {
      handleNewConversation();
    }
  }

  async function handleHistoryRename(conversationId: string, title: string): Promise<void> {
    try {
      await renameAgentConversation(conversationId, { title });

      historyItems.value = historyItems.value.map((item) => (item.id === conversationId ? { ...item, title } : item));
      aiChatRef.value?.finishHistoryRename(conversationId);
    } finally {
      aiChatRef.value?.resetHistoryRenameLoading(conversationId);
    }
  }

  onMounted(() => {
    loadDefaultModel().catch(() => undefined);
  });

  onBeforeUnmount(() => {
    composerRuntime.clear();
    chatRuntime.value?.clear();
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
