<template>
  <n-scrollbar class="h-full">
    <div class="smart-workbench flex min-h-full flex-col gap-[16px]">
      <AiChatProvider :runtime="composerRuntime">
        <AiComposer
          class="rounded-[4px] !shadow-none"
          :mcp-options="mcpOptions"
          submit-mode="emit"
          :placeholder="t('workbench.smart.composerPlaceholder')"
          @mcp-updated="loadMcpOptions"
          @submit="handleComposerSubmit"
        />
      </AiChatProvider>
      <n-spin v-if="dataOverviewAIRenderString" :show="dataOverviewLoading" class="bg-[var(--text-n10)]">
        <div
          ref="dataOverviewRef"
          class="h-full w-full"
          @click="handleSmartContentClick"
          v-html="dataOverviewAIRenderString"
        >
        </div>

        <n-spin v-if="aiSummaryVisible" :show="aiSummaryLoading" class="bg-[var(--text-n10)] px-[24px] pb-[24px]">
          <div class="rounded-[4px] bg-[var(--primary-7)] p-[16px] text-[var(--primary-8)]">
            <div class="flex items-center gap-[8px] font-semibold">
              <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
              <span>{{ t('workbench.smart.AIRead') }}</span>
            </div>
            <div v-if="aiSummaryContent" class="mt-[8px] whitespace-pre-wrap">
              {{ aiSummaryContent }}
            </div>
            <n-empty v-else :description="t('common.noData')" :show-icon="false" class="mt-[8px]" />
            <n-button
              class="n-btn-outline-primary mt-[8px] bg-[var(--text-n10)]"
              size="small"
              type="primary"
              ghost
              :loading="aiSummaryLoading"
              @click="regenerateAiSummary()"
            >
              {{ t('workbench.smart.reInterpret') }}
            </n-button>
          </div>
        </n-spin>
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
                      <CrmIcon
                        class="shrink-0 cursor-pointer text-[var(--text-n2)]"
                        type="iconicon_close"
                        :size="16"
                        @click="handleSuggestionIgnore(item)"
                      />
                    </div>
                    <div class="mt-[16px] whitespace-pre-wrap text-[var(--text-n2)]">
                      {{ item.summary || '-' }}
                    </div>
                    <div class="mt-[16px] flex flex-wrap gap-[8px]">
                      <n-button
                        v-if="item.actions"
                        size="small"
                        type="primary"
                        ghost
                        :loading="operatingSuggestionId === item.id"
                        @click="handleSuggestionSubmit(item, item.actions)"
                      >
                        {{ item.actions }}
                      </n-button>
                      <n-button
                        size="small"
                        type="primary"
                        ghost
                        :loading="operatingSuggestionId === item.id"
                        @click="handleSuggestionIgnore(item)"
                      >
                        {{ t('workbench.smart.ignore') }}
                      </n-button>
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
          <div class="px-[24px] pb-[24px]">
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
  import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
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
    getAgentMcpConfigList,
    getSmartAiSummary,
    getSmartDataOverview,
    ignoreAgentActionApprove,
    ignoreAgentActionSuggestion,
    regenerateSmartAiSummary,
    regenerateSmartDataOverview,
    submitAgentActionSuggestion,
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
  const aiSummaryLoading = ref(false);
  const aiSummaryVisible = ref(true);
  const aiSummaryContent = ref('');
  const aiSummaryFocus = ref('');
  const dataOverviewRef = ref<HTMLElement>();

  function syncAiSummaryButtonActive() {
    const interpretButton = dataOverviewRef.value?.querySelector<HTMLElement>('[data-overview-action="interpret"]');

    interpretButton?.classList.toggle('bg-[var(--primary-7)]', aiSummaryVisible.value);
    interpretButton?.classList.toggle('!bg-[var(--text-n10)]', !aiSummaryVisible.value);
  }

  function syncAiSummaryFocusFromOverview(content: string) {
    const doc = new DOMParser().parseFromString(content, 'text/html');
    const interpretButton = doc.querySelector<HTMLElement>('[data-overview-action="interpret"]');

    aiSummaryFocus.value = interpretButton?.dataset.overviewPrompt || '';
  }

  async function loadDataOverview() {
    try {
      dataOverviewLoading.value = true;
      const res = await getSmartDataOverview();
      dataOverviewAIRenderString.value = decodeContent(res);
      syncAiSummaryFocusFromOverview(dataOverviewAIRenderString.value);
      await nextTick();
      syncAiSummaryButtonActive();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      dataOverviewLoading.value = false;
    }
  }

  async function regenerateDataOverview() {
    try {
      dataOverviewLoading.value = true;
      const res = await regenerateSmartDataOverview();
      dataOverviewAIRenderString.value = decodeContent(res);
      syncAiSummaryFocusFromOverview(dataOverviewAIRenderString.value);
      await nextTick();
      syncAiSummaryButtonActive();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      dataOverviewLoading.value = false;
    }
  }

  async function loadAiSummary() {
    try {
      aiSummaryLoading.value = true;
      const res = await getSmartAiSummary({ focus: aiSummaryFocus.value });
      aiSummaryContent.value = res || '';
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      aiSummaryLoading.value = false;
    }
  }

  async function regenerateAiSummary() {
    try {
      aiSummaryLoading.value = true;
      const res = await regenerateSmartAiSummary({ focus: aiSummaryFocus.value });
      aiSummaryContent.value = res || '';
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      aiSummaryLoading.value = false;
    }
  }

  function handleSmartContentClick(event: MouseEvent): void {
    const actionElement =
      event.target instanceof HTMLElement ? event.target.closest<HTMLElement>('[data-overview-action]') : null;

    if (!actionElement) {
      return;
    }

    const action = actionElement.dataset.overviewAction || '';

    if (!action) {
      return;
    }

    event.preventDefault();
    if (action === 'interpret') {
      aiSummaryVisible.value = !aiSummaryVisible.value;
      syncAiSummaryButtonActive();
      return;
    }

    if (action === 'regenerate') {
      regenerateDataOverview();
    }
  }

  const mcpOptions = ref<AiChatMcp[]>([]);
  async function loadMcpOptions() {
    try {
      mcpOptions.value = await getAgentMcpConfigList();
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
  const operatingSuggestionId = ref('');
  const suggestionPager = createActionPager(suggestionList, suggestionLoading, getAgentActionSuggestionPage);

  async function handleSuggestionAction(item: AgentActionSuggestionItem, action: (id: string) => Promise<unknown>) {
    if (!item.id || operatingSuggestionId.value) {
      return;
    }

    try {
      operatingSuggestionId.value = item.id;
      await action(item.id);
      Message.success(t('common.operationSuccess'));
      await suggestionPager.load();
    } finally {
      operatingSuggestionId.value = '';
    }
  }

  function handleSuggestionSubmit(item: AgentActionSuggestionItem, label: string) {
    return handleSuggestionAction(item, (id) => submitAgentActionSuggestion(id, label));
  }

  function handleSuggestionIgnore(item: AgentActionSuggestionItem) {
    return handleSuggestionAction(item, ignoreAgentActionSuggestion);
  }

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

  onMounted(async () => {
    loadMcpOptions();
    await loadDataOverview();
    loadAiSummary();
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
