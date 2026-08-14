<template>
  <n-scrollbar class="h-full">
    <div class="smart-workbench flex min-h-full flex-col gap-[16px]">
      <AiChatProvider :runtime="composerRuntime">
        <AiComposer
          class="rounded-[4px] !shadow-none"
          :mcp-options="mcpOptions"
          submit-mode="emit"
          :placeholder="t('workbench.smart.composerPlaceholder')"
          @submit="handleComposerSubmit"
        />
      </AiChatProvider>
      <n-spin v-if="dataOverviewAIRenderString" :show="dataOverviewLoading" class="bg-[var(--text-n10)]">
        <div class="h-full w-full" v-html="dataOverviewAIRenderString"> </div>
      </n-spin>
      <div class="flex w-full gap-[16px]">
        <CrmCard class="flex-1" no-content-padding hide-footer>
          <template #header>
            <div class="flex items-center gap-[8px]">
              <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
              <div class="text-[14px] font-semibold">{{ t('workbench.smart.AIAction') }}</div>
            </div>
          </template>
          <div class="px-[24px] pb-[24px]">
            <n-spin :show="suggestionLoading" class="h-full" content-class="h-full">
              <n-empty v-if="!suggestionList.length" :description="t('common.noData')" />
              <n-scrollbar v-else class="h-full" @scroll="suggestionPager.handleReachBottom">
                <div class="flex flex-col gap-[16px]">
                  <div v-for="item in suggestionList" :key="item.id" class="smart-workbench-action-item">
                    <div class="flex items-center justify-between gap-[12px]">
                      <div class="flex min-w-0 items-center gap-[8px]">
                        <CrmTag
                          size="small"
                          theme="light"
                          :type="getSuggestionPriorityMeta(item).type"
                          tooltip-disabled
                        >
                          {{ getSuggestionPriorityMeta(item).label }}
                        </CrmTag>
                        <n-tooltip trigger="hover" :delay="300">
                          <template #trigger>
                            <span class="truncate font-semibold">
                              {{ item.topic }}
                            </span>
                          </template>
                          {{ item.topic }}
                        </n-tooltip>
                      </div>
                      <CrmIcon class="shrink-0 cursor-pointer text-[var(--text-n2)]" type="iconicon_close" :size="16" />
                    </div>
                    <div class="mt-[16px] whitespace-pre-wrap text-[var(--text-n2)]">
                      {{ item.summary || '-' }}
                    </div>
                    <div class="mt-[16px] flex flex-wrap gap-[8px]">
                      <n-button v-if="item.actions" size="small" type="primary" ghost>
                        {{ item.actions }}
                      </n-button>
                      <n-button size="small" type="primary" ghost>{{ t('workbench.smart.ignore') }}</n-button>
                    </div>
                  </div>
                </div>
              </n-scrollbar>
            </n-spin>
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
            <n-spin :show="approveLoading" class="h-full" content-class="h-full">
              <n-empty v-if="!approveList.length" :description="t('common.noData')" />
              <n-scrollbar v-else class="h-full" @scroll="approvePager.handleReachBottom">
                <div class="flex flex-col gap-[16px]">
                  <div v-for="item in approveList" :key="item.id" class="smart-workbench-action-item">
                    <div class="flex items-center justify-between gap-[12px]">
                      <div class="flex min-w-0 items-center gap-[8px]">
                        <CrmTag class="shrink-0" size="small" theme="light" type="warning" tooltip-disabled>
                          {{ item.type }}
                        </CrmTag>
                        <span class="truncate font-semibold">
                          {{ item.topic }}
                        </span>
                      </div>
                      <CrmIcon
                        class="shrink-0 cursor-pointer text-[var(--text-n2)]"
                        type="iconicon_close"
                        :size="16"
                        @click="handleApproveIgnore(item)"
                      />
                    </div>
                    <div class="mt-[16px] whitespace-pre-wrap text-[var(--text-n2)]">
                      {{ item.summary || '-' }}
                    </div>
                    <div class="mt-[16px] flex flex-wrap gap-[8px]">
                      <n-button
                        size="small"
                        type="success"
                        ghost
                        :loading="operatingApproveId === item.id"
                        @click="handleApproveConfirm(item)"
                      >
                        {{ t('common.confirm') }}
                      </n-button>
                      <n-button
                        size="small"
                        type="error"
                        ghost
                        :loading="operatingApproveId === item.id"
                        @click="handleApproveIgnore(item)"
                      >
                        {{ t('workbench.smart.reject') }}
                      </n-button>
                    </div>
                  </div>
                </div>
              </n-scrollbar>
            </n-spin>
          </div>
        </CrmCard>
      </div>
    </div>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { onBeforeUnmount, onMounted, ref } from 'vue';
  import { NButton, NEmpty, NScrollbar, NSpin, NTooltip, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AgentActionApproveItem, AgentActionSuggestionItem } from '@lib/shared/models/ai';
  import type { CommonList, TableQueryParams } from '@lib/shared/models/common';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import {
    type AiChatMcp,
    AiChatProvider,
    AiComposer,
    type AiComposerSubmitPayload,
    createAiChatRuntime,
  } from '@/components/business/ai-chat';

  import {
    confirmAgentActionApprove,
    getAgentActionApprovePage,
    getAgentActionSuggestionPage,
    getAgentConversationMcpTools,
    getSmartDataOverview,
    ignoreAgentActionApprove,
  } from '@/api/modules';

  const { t } = useI18n();
  const Message = useMessage();

  const composerRuntime = createAiChatRuntime();

  function handleComposerSubmit(payload: AiComposerSubmitPayload): void {
    composerRuntime.clear();
    window.dispatchEvent(
      new CustomEvent('crm-ai-chat-floating-open', {
        detail: {
          content: payload.content,
          attachments: payload.attachments,
          mcps: payload.options?.mcps ?? [],
        },
      })
    );
  }

  function decodeContent(content?: string): string {
    if (!content) {
      return '';
    }

    try {
      return decodeURIComponent(escape(window.atob(content)));
    } catch {
      return content;
    }
  }

  function getSuggestionPriorityMeta(item: AgentActionSuggestionItem) {
    if (item.priority && item.priority >= 5) {
      return {
        label: t('workbench.smart.urgent'),
        type: 'error' as const,
      };
    }

    if (item.priority && item.priority >= 4) {
      return {
        label: t('workbench.smart.important'),
        type: 'warning' as const,
      };
    }

    return {
      label: t('workbench.smart.suggestion'),
      type: 'info' as const,
    };
  }

  const dataOverviewLoading = ref(false);
  const dataOverviewAIRenderString = ref('');

  async function loadDataOverview(): Promise<void> {
    try {
      dataOverviewLoading.value = true;
      const res = await getSmartDataOverview();
      dataOverviewAIRenderString.value = decodeContent(res);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      dataOverviewLoading.value = false;
    }
  }

  const mcpOptions = ref<AiChatMcp[]>([]);
  async function loadMcpOptions() {
    try {
      const tools = await getAgentConversationMcpTools();
      mcpOptions.value = (tools ?? []).map((item) => ({
        id: item.name,
        name: item.name,
        description: item.description,
        permission: 'read',
      }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function createActionPager<T>(
    list: Ref<T[]>,
    loading: Ref<boolean>,
    loader: (params: TableQueryParams) => Promise<CommonList<T>>
  ) {
    const pagination = ref({
      total: 0,
      current: 1,
      pageSize: 10,
    });

    async function load(refresh = true): Promise<void> {
      if (loading.value) {
        return;
      }

      try {
        loading.value = true;

        if (refresh) {
          pagination.value.current = 1;
        }

        const res = await loader({
          current: pagination.value.current,
          pageSize: pagination.value.pageSize,
        });

        list.value = refresh ? res?.list || [] : list.value.concat(res?.list || []);
        pagination.value.total = res?.total || 0;
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      } finally {
        loading.value = false;
      }
    }

    function handleReachBottom(event: Event): void {
      const el = event.target as HTMLElement;

      if (el.scrollTop + el.clientHeight < el.scrollHeight - 24) {
        return;
      }

      const { current, pageSize, total } = pagination.value;

      if (current >= Math.ceil(total / pageSize)) {
        return;
      }

      pagination.value.current += 1;
      load(false);
    }

    return {
      load,
      handleReachBottom,
    };
  }

  const suggestionLoading = ref(false);
  const suggestionList = ref<AgentActionSuggestionItem[]>([]);
  const suggestionPager = createActionPager(suggestionList, suggestionLoading, getAgentActionSuggestionPage);

  const approveLoading = ref(false);
  const approveList = ref<AgentActionApproveItem[]>([]);
  const operatingApproveId = ref('');
  const approvePager = createActionPager(approveList, approveLoading, getAgentActionApprovePage);

  async function handleApproveAction(item: AgentActionApproveItem, action: (id: string) => Promise<unknown>) {
    if (!item.id || operatingApproveId.value) {
      return;
    }

    try {
      operatingApproveId.value = item.id;
      await action(item.id);
      Message.success(t('common.operationSuccess'));
      await approvePager.load();
    } finally {
      operatingApproveId.value = '';
    }
  }

  function handleApproveConfirm(item: AgentActionApproveItem) {
    return handleApproveAction(item, confirmAgentActionApprove);
  }

  function handleApproveIgnore(item: AgentActionApproveItem) {
    return handleApproveAction(item, ignoreAgentActionApprove);
  }

  onMounted(() => {
    loadMcpOptions();
    loadDataOverview();
    suggestionPager.load();
    approvePager.load();
  });

  onBeforeUnmount(() => {
    composerRuntime.clear();
  });
</script>

<style scoped lang="less">
  .smart-workbench-action-item {
    padding: 16px;
    border: 1px solid var(--text-n8);
    border-radius: 4px;
  }
</style>
