<template>
  <n-scrollbar class="h-full">
    <div class="smart-workbench min-h-full">
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
      :key="chatSessionId"
      :runtime="chatRuntime"
      :history-items="historyItems"
      :model-name="currentModelName"
      :mcp-options="mcpOptions"
    />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
  import { NScrollbar } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiModelItem } from '@lib/shared/models/system/aiModel';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import {
    AiChat,
    type AiChatMcp,
    AiChatProvider,
    type AiChatRuntime,
    AiComposer,
    type AiComposerSubmitPayload,
    createAgentChatTransport,
    createAiChatRuntime,
  } from '@/components/business/ai-chat';

  import { cancelAgentChat, getAiModelList, streamAgentChat } from '@/api/modules';

  const { t } = useI18n();
  const showChatDrawer = ref(false);
  const chatRuntime = ref<AiChatRuntime>();
  const chatSessionId = ref('');
  const agentConversationId = ref('');
  const agentSessionId = ref('');
  const activeModel = ref<AiModelItem>();
  const historyItems = ref<{ id: string; title: string }[]>([]);
  const currentModelName = computed(() => activeModel.value?.displayName || activeModel.value?.modelName || '');

  const mcpOptions: AiChatMcp[] = [];

  async function loadDefaultModel(): Promise<void> {
    const res = await getAiModelList({ current: 1, pageSize: 100 });
    activeModel.value = res.list.find((model) => model.enable) ?? res.list[0];
  }

  function createWorkbenchRuntime(): AiChatRuntime {
    return createAiChatRuntime({
      initialModelName: currentModelName.value,
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
    });
  }

  const composerRuntime = createAiChatRuntime({
    initialModelName: currentModelName.value,
  });

  function createChatSession(title = ''): AiChatRuntime {
    const sessionId = `chat_${Date.now()}`;

    chatSessionId.value = sessionId;
    agentConversationId.value = '';
    agentSessionId.value = '';
    historyItems.value = title
      ? [
          {
            id: sessionId,
            title,
          },
        ]
      : [];
    chatRuntime.value = createWorkbenchRuntime();

    return chatRuntime.value;
  }

  function openChatDrawer(): void {
    if (!chatRuntime.value) {
      createChatSession();
    }

    showChatDrawer.value = true;
  }

  async function handleComposerSubmit(payload: AiComposerSubmitPayload): Promise<void> {
    if (!activeModel.value) {
      await loadDefaultModel();
    }

    const modelName = currentModelName.value || payload.options?.model || '';
    const selectedMcps = payload.options?.mcps ?? [];
    const runtime = createChatSession(payload.content);

    runtime.setSelectedMcps(selectedMcps);
    runtime.setModelName(modelName);
    showChatDrawer.value = true;
    composerRuntime.clear();

    await runtime.submit({
      content: payload.content,
      attachments: payload.attachments,
      options: {
        model: modelName,
        mcps: selectedMcps,
      },
    });
  }

  onMounted(() => {
    loadDefaultModel().catch(() => undefined);
  });

  onBeforeUnmount(() => {
    composerRuntime.clear();
    chatRuntime.value?.clear();
  });
</script>

<style scoped lang="scss"></style>
