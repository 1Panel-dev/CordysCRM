<template>
  <div class="ai-chat-block ai-chat-block-progress text-[var(--text-n2)]">
    <n-collapse v-model:expanded-names="expandedNames" arrow-placement="right">
      <n-collapse-item :name="partId">
        <template #header>
          <div class="inline-flex min-w-0 max-w-full items-start gap-[8px]">
            <CrmIcon type="iconicon_set_up" :size="16" class="mt-[3px] shrink-0 text-[var(--text-n4)]" />
            <span class="min-w-0 flex-1 whitespace-normal break-words">
              {{ progress?.title || t('aiChat.progress') }}
            </span>
          </div>
        </template>
        <div v-if="hasDetails" class="space-y-[8px]">
          <div v-if="progress?.details?.input" class="ai-chat-progress__detail">
            <div class="ai-chat-progress__label">{{ t('aiChat.progressInput') }}</div>
            <pre class="ai-chat-progress__content">{{ progress.details.input }}</pre>
          </div>

          <div v-if="progress?.details?.output" class="ai-chat-progress__detail">
            <div class="ai-chat-progress__label">{{ t('aiChat.progressOutput') }}</div>
            <pre class="ai-chat-progress__content">{{ progress.details.output }}</pre>
          </div>
        </div>
      </n-collapse-item>
    </n-collapse>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { NCollapse, NCollapseItem } from 'naive-ui';

  import type { AiChatDataParts } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AgentChatProgressData } from '@lib/shared/models/ai';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import type { DataUIPart } from 'ai';

  const props = defineProps<{
    part: DataUIPart<AiChatDataParts>;
    index?: number;
    isGenerating?: boolean;
  }>();

  const { t } = useI18n();

  const progress = computed<AgentChatProgressData>(() => props.part.data as AgentChatProgressData);
  const partId = computed(() => `${props.part.type}_${props.index ?? 0}`);
  const expandedNames = ref<string[]>([]);
  const hasDetails = computed(() => Boolean(progress.value?.details?.input || progress.value?.details?.output));
</script>

<style scoped lang="less">
  .ai-chat-block-progress {
    width: 100%;
    :deep(.n-collapse) {
      width: 100%;
    }
    :deep(.n-collapse-item) {
      margin-left: 0;
    }
    :deep(.n-collapse-item__header) {
      min-width: 0;
    }
    :deep(.n-collapse-item__header .n-collapse-item__header-main) {
      overflow: hidden;
      min-width: 0;
      max-width: 100%;
      color: var(--text-n2);
    }
    :deep(.n-collapse-item__content-wrapper .n-collapse-item__content-inner) {
      padding-top: 8px;
      color: var(--text-n2);
    }
    :deep(.n-collapse-item__content-wrapper .n-collapse-item__content-inner *) {
      color: inherit;
    }
  }
  .ai-chat-progress__detail {
    overflow: hidden;
    border-radius: 4px;
    background: var(--text-n9);
  }
  .ai-chat-progress__label {
    padding: 6px 8px;
    font-size: 12px;
    color: var(--text-n2);
    background: var(--text-n8);
  }
  .ai-chat-progress__content {
    overflow: auto;
    margin: 0;
    padding: 8px;
    font-size: 12px;
    white-space: pre-wrap;
    color: var(--text-n2);
    word-break: break-word;
  }
</style>
