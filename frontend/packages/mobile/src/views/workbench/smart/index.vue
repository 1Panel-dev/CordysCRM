<template>
  <div class="flex-1">
    <div
      v-if="dataOverviewAIRenderString || dataOverviewLoading"
      class="mb-[16px] bg-[var(--text-n10)]"
      :class="{ 'pb-[16px]': aiSummaryVisible }"
    >
      <div
        v-if="dataOverviewAIRenderString"
        ref="dataOverviewRef"
        @click="handleSmartContentClick"
        v-html="dataOverviewAIRenderString"
      ></div>
      <div v-else class="px-[20px] py-[20px]">
        <div class="flex items-center gap-[8px] font-semibold text-[var(--text-n1)]">
          <CrmIcon name="iconicon_star1" width="16px" height="16px" color="var(--primary-8)" />
          {{ t('workbench.dataOverview') }}
        </div>
        <div class="mt-[16px] py-[28px] text-center text-[var(--text-n4)]">
          {{ t('workbench.smart.dataOverviewGenerating') }}
        </div>
      </div>
      <div v-if="dataOverviewAIRenderString && aiSummaryVisible" class="bg-[var(--primary-7)] px-[20px] py-[8px]">
        <div class="flex items-center gap-[8px] font-semibold text-[var(--primary-8)]">
          <CrmIcon name="iconicon_star1" width="16px" height="16px" color="var(--primary-8)" />
          <span>{{ t('workbench.smart.AIRead') }}</span>
        </div>
        <div v-if="aiSummaryLoading" class="py-[12px] text-center">
          <van-loading />
        </div>
        <AiMobileMarkdownBlock
          v-else-if="!aiSummaryLoading && aiSummaryContent"
          class="smart-ai-summary-markdown mt-[8px]"
          :part="aiSummaryPart"
        />
        <van-empty v-else :description="t('common.noData')" image-size="0" />
        <van-button
          size="mini"
          plain
          type="primary"
          class="!mt-[8px] !bg-[var(--text-n10)]"
          :loading="aiSummaryLoading"
          @click="regenerateAiSummary()"
        >
          {{ t('workbench.smart.reInterpret') }}
        </van-button>
      </div>
    </div>

    <div class="bg-[var(--text-n10)] px-[24px] pt-[20px]">
      <div class="flex gap-[8px]">
        <van-button
          v-for="item of smartActionTabOptions"
          :key="item.value"
          round
          size="small"
          class="!flex-1 !border-none !px-[16px] !py-[4px] !text-[14px]"
          :class="
            activeSmartActionTab === item.value
              ? '!bg-[var(--primary-7)] font-semibold !text-[var(--primary-8)]'
              : '!bg-[var(--text-n9)] !text-[var(--text-n1)]'
          "
          block
          @click="handleSmartActionTabChange(item.value)"
        >
          {{ item.label }}
        </van-button>
      </div>

      <div class="mt-[16px]">
        <CrmList
          ref="suggestionListRef"
          v-show="activeSmartActionTab === SmartActionTabEnum.SUGGESTION"
          v-model="suggestionList"
          class="!h-auto !overflow-visible"
          :load-list-api="getAgentActionSuggestionPage"
          :item-gap="16"
        >
          <template #item="{ item }">
            <div class="mobile-smart-action-card">
              <div class="flex items-center justify-between gap-[12px]">
                <div class="flex min-w-0 items-center gap-[8px]">
                  <CrmTag
                    class="shrink-0"
                    :bg-color="getSuggestionPriorityMeta(item).style.bgColor"
                    :tag="getSuggestionPriorityMeta(item).label"
                    :text-color="getSuggestionPriorityMeta(item).style.color"
                  />
                  <div class="one-line-text font-semibold">
                    {{ item.topic || '-' }}
                  </div>
                </div>
                <CrmIcon
                  name="iconicon_close"
                  class="shrink-0 text-[var(--text-n2)]"
                  width="16px"
                  height="16px"
                  @click="handleSuggestionIgnore(item)"
                />
              </div>
              <div class="mt-[16px] whitespace-pre-wrap text-[var(--text-n2)]">
                {{ item.summary || '-' }}
              </div>
              <div class="mt-[16px] flex flex-wrap gap-[8px]">
                <van-button
                  v-for="action in splitSuggestionActions(item.actions)"
                  :key="action"
                  size="small"
                  plain
                  type="primary"
                  :loading="operatingSuggestionId === item.id"
                  @click="handleSuggestionSubmit(item, action)"
                >
                  {{ action }}
                </van-button>
                <van-button
                  size="small"
                  plain
                  type="primary"
                  :loading="operatingSuggestionId === item.id"
                  @click="handleSuggestionIgnore(item)"
                >
                  {{ t('workbench.smart.ignore') }}
                </van-button>
              </div>
            </div>
          </template>
        </CrmList>

        <CrmList
          ref="approveListRef"
          v-show="activeSmartActionTab === SmartActionTabEnum.APPROVE"
          v-model="approveList"
          class="!h-auto !overflow-visible"
          :load-list-api="getAgentActionApprovePage"
          :item-gap="16"
        >
          <template #item="{ item }">
            <div class="mobile-smart-action-card">
              <div class="flex items-center justify-between gap-[12px]">
                <div class="flex min-w-0 items-center gap-[8px]">
                  <CrmTag
                    class="shrink-0"
                    :bg-color="stageStyle('warning').bgColor"
                    :tag="item.type"
                    :text-color="stageStyle('warning').color"
                  />
                  <div class="one-line-text font-semibold">
                    {{ item.topic || '-' }}
                  </div>
                </div>
                <CrmIcon
                  name="iconicon_close"
                  class="shrink-0 text-[var(--text-n2)]"
                  width="16px"
                  height="16px"
                  @click="handleApproveIgnore(item)"
                />
              </div>
              <div class="mt-[16px] whitespace-pre-wrap text-[var(--text-n2)]">
                {{ item.summary || '-' }}
              </div>
              <div class="mt-[16px] flex flex-wrap gap-[8px]">
                <van-button
                  size="small"
                  plain
                  type="success"
                  :loading="operatingApproveId === item.id"
                  @click="handleApproveConfirm(item)"
                >
                  {{ t('common.confirm') }}
                </van-button>
                <van-button
                  size="small"
                  plain
                  type="danger"
                  :loading="operatingApproveId === item.id"
                  @click="handleApproveIgnore(item)"
                >
                  {{ t('workbench.smart.reject') }}
                </van-button>
              </div>
            </div>
          </template>
        </CrmList>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { closeToast, showLoadingToast, showSuccessToast } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AgentActionApproveItem, AgentActionSuggestionItem } from '@lib/shared/models/ai';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import AiMobileMarkdownBlock from '@/components/business/ai-chat/blocks/AiMobileMarkdownBlock.vue';

  import {
    confirmAgentActionApprove,
    getAgentActionApprovePage,
    getAgentActionSuggestionPage,
    getSmartAiSummary,
    getSmartDataOverview,
    ignoreAgentActionApprove,
    ignoreAgentActionSuggestion,
    regenerateSmartAiSummary,
    regenerateSmartDataOverview,
    submitAgentActionSuggestion,
  } from '@/api/modules';

  const { t } = useI18n();

  enum SmartActionTabEnum {
    SUGGESTION = 'suggestion',
    APPROVE = 'approve',
  }

  const activeSmartActionTab = ref(SmartActionTabEnum.SUGGESTION);
  const smartActionTabOptions = computed(() => [
    {
      label: t('workbench.smart.AIAction'),
      value: SmartActionTabEnum.SUGGESTION,
    },
    {
      label: t('workbench.smart.AIActionApproval'),
      value: SmartActionTabEnum.APPROVE,
    },
  ]);
  const dataOverviewLoading = ref(false);
  const dataOverviewAIRenderString = ref('');
  const aiSummaryLoading = ref(false);
  const aiSummaryVisible = ref(true);
  const aiSummaryContent = ref('');
  const aiSummaryPart = computed(() => ({
    type: 'text' as const,
    text: aiSummaryContent.value,
  }));
  const aiSummaryFocus = ref('');
  const dataOverviewRef = ref<HTMLElement>();

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

  function syncAiSummaryFocusFromOverview(content: string): void {
    const doc = new DOMParser().parseFromString(content, 'text/html');
    const interpretButton = doc.querySelector<HTMLElement>('[data-overview-action="interpret"]');

    aiSummaryFocus.value = interpretButton?.dataset.overviewPrompt || '';
  }

  function syncAiSummaryButtonActive() {
    const interpretButton = dataOverviewRef.value?.querySelector<HTMLElement>('[data-overview-action="interpret"]');

    interpretButton?.classList.toggle('bg-[var(--primary-7)]', aiSummaryVisible.value);
    interpretButton?.classList.toggle('!bg-[var(--text-n10)]', !aiSummaryVisible.value);
  }

  async function loadDataOverview() {
    try {
      dataOverviewLoading.value = true;
      showLoadingToast(t('common.loading'));
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
      closeToast();
    }
  }

  async function regenerateDataOverview() {
    try {
      dataOverviewLoading.value = true;
      showLoadingToast(t('common.loading'));
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
      closeToast();
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

  function handleSmartContentClick(event: MouseEvent) {
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

  const suggestionList = ref<AgentActionSuggestionItem[]>([]);
  const approveList = ref<AgentActionApproveItem[]>([]);
  const operatingSuggestionId = ref('');
  const operatingApproveId = ref('');
  const suggestionListRef = ref<InstanceType<typeof CrmList>>();
  const approveListRef = ref<InstanceType<typeof CrmList>>();

  async function handleSmartActionTabChange(tab: SmartActionTabEnum) {
    activeSmartActionTab.value = tab;
    await nextTick();

    if (tab === SmartActionTabEnum.SUGGESTION) {
      await suggestionListRef.value?.loadList(true);
    } else if (tab === SmartActionTabEnum.APPROVE) {
      await approveListRef.value?.loadList(true);
    }
  }

  function splitSuggestionActions(actions?: string) {
    return (actions || '')
      .split(',')
      .map((action) => action.trim())
      .filter((action) => action);
  }

  async function handleSuggestionAction(item: AgentActionSuggestionItem, action: (id: string) => Promise<unknown>) {
    if (!item.id || operatingSuggestionId.value) {
      return;
    }

    try {
      operatingSuggestionId.value = item.id;
      await action(item.id);
      showSuccessToast(t('common.operationSuccess'));
      await suggestionListRef.value?.loadList(true);
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

  async function handleApproveAction(item: AgentActionApproveItem, action: (id: string) => Promise<unknown>) {
    if (!item.id || operatingApproveId.value) {
      return;
    }

    try {
      operatingApproveId.value = item.id;
      await action(item.id);
      showSuccessToast(t('common.operationSuccess'));
      await approveListRef.value?.loadList(true);
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

  function stageStyle(type: 'success' | 'error' | 'info' | 'warning') {
    const map = {
      success: { bgColor: 'var(--success-5)', color: 'var(--success-green)' },
      error: { bgColor: 'var(--error-5)', color: 'var(--error-red)' },
      info: { bgColor: 'var(--info-5)', color: 'var(--info-blue)' },
      warning: { bgColor: 'var(--warning-5)', color: 'var(--warning-yellow)' },
    };
    return map[type];
  }

  function getSuggestionPriorityMeta(item: AgentActionSuggestionItem) {
    if (item.priority && item.priority >= 5) {
      return {
        label: t('workbench.smart.urgent'),
        style: stageStyle('error'),
      };
    }

    if (item.priority && item.priority >= 4) {
      return {
        label: t('workbench.smart.important'),
        style: stageStyle('warning'),
      };
    }

    return {
      label: t('workbench.smart.suggestion'),
      style: stageStyle('info'),
    };
  }

  onMounted(async () => {
    await loadDataOverview();
    loadAiSummary();
  });
</script>

<style lang="less" scoped>
  .mobile-smart-action-card {
    padding: 16px;
    border: 1px solid var(--text-n8);
    border-radius: var(--border-radius-small);
  }
  .smart-ai-summary-markdown {
    color: var(--primary-8) !important;
    :deep(*) {
      color: inherit;
    }
  }
</style>
