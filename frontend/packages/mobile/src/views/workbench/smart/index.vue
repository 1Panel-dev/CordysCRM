<template>
  <div class="flex-1">
    <div v-if="dataOverviewAIRenderString" class="mb-[16px]">
      <van-loading v-if="dataOverviewLoading" class="py-[12px]" />
      <div v-html="dataOverviewAIRenderString"></div>
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
          @click="activeSmartActionTab = item.value"
        >
          {{ item.label }}
        </van-button>
      </div>

      <div class="mt-[16px]">
        <CrmList
          v-show="activeSmartActionTab === SmartActionTabEnum.SUGGESTION"
          v-model="suggestionList"
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
                <CrmIcon name="iconicon_close" class="shrink-0 text-[var(--text-n2)]" width="16px" height="16px" />
              </div>
              <div class="mt-[16px] whitespace-pre-wrap text-[var(--text-n2)]">
                {{ item.summary || '-' }}
              </div>
              <div class="mt-[16px] flex flex-wrap gap-[8px]">
                <van-button v-if="item.actions" size="small" plain type="primary">{{ item.actions }}</van-button>
                <van-button size="small" plain type="primary">{{ t('workbench.smart.ignore') }}</van-button>
              </div>
            </div>
          </template>
        </CrmList>

        <CrmList
          v-show="activeSmartActionTab === SmartActionTabEnum.APPROVE"
          v-model="approveList"
          :load-list-api="getAgentActionApprovePage"
          :item-gap="16"
        >
          <template #item="{ item }">
            <div class="mobile-smart-action-card">
              <div class="flex items-center justify-between gap-[12px]">
                <div class="flex min-w-0 items-center gap-[8px]">
                  <CrmTag
                    class="shrink-0"
                    :bg-color="stageStyle('info').bgColor"
                    :tag="item.type"
                    :text-color="stageStyle('info').color"
                  />
                  <div class="one-line-text font-semibold">
                    {{ item.topic || '-' }}
                  </div>
                </div>
                <CrmIcon name="iconicon_close" class="shrink-0 text-[var(--text-n2)]" width="16px" height="16px" />
              </div>
              <div class="mt-[16px] whitespace-pre-wrap text-[var(--text-n2)]">
                {{ item.summary || '-' }}
              </div>
              <div class="mt-[16px] flex flex-wrap gap-[8px]">
                <van-button size="small" plain type="success">{{ t('common.confirm') }}</van-button>
                <van-button size="small" plain type="danger">{{ t('workbench.smart.reject') }}</van-button>
              </div>
            </div>
          </template>
        </CrmList>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AgentActionApproveItem, AgentActionSuggestionItem } from '@lib/shared/models/ai';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';

  import { getAgentActionApprovePage, getAgentActionSuggestionPage, getSmartDataOverview } from '@/api/modules';

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

  async function loadDataOverview() {
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

  const suggestionList = ref<AgentActionSuggestionItem[]>([]);
  const approveList = ref<AgentActionApproveItem[]>([]);

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

  onMounted(loadDataOverview);
</script>

<style lang="less" scoped>
  .mobile-smart-action-card {
    padding: 16px;
    border: 1px solid var(--text-n8);
    border-radius: var(--border-radius-small);
  }
</style>
