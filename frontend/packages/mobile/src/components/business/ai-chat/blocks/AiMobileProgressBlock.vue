<template>
  <div class="ai-chat-block ai-mobile-progress">
    <CrmIcon name="iconicon_set_up" width="16px" height="16px" color="var(--text-n4)" class="mt-[3px]" />
    <span class="ai-mobile-progress__title">
      {{ progress?.title || t('aiChat.progress') }}
    </span>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';

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
  const progress = computed(() => props.part.data as AgentChatProgressData | undefined);
</script>

<style scoped lang="less">
  .ai-mobile-progress {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    width: 100%;
    color: var(--text-n2);
  }
  .ai-mobile-progress__title {
    flex: 1;
    min-width: 0;
    line-height: 22px;
    white-space: normal;
    word-break: break-word;
  }
</style>
