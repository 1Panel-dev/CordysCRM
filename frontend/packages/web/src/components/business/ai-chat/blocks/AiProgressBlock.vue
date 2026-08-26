<template>
  <div class="ai-chat-block ai-chat-block-progress text-[var(--text-n2)]">
    <div class="flex min-w-0 max-w-full items-start gap-[8px]">
      <CrmIcon type="iconicon_set_up" :size="16" class="mt-[3px] shrink-0 text-[var(--text-n4)]" />
      <span class="min-w-0 flex-1 whitespace-normal break-words">
        {{ progress?.title || t('aiChat.progress') }}
      </span>
    </div>
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

  const progress = computed<AgentChatProgressData>(() => props.part.data as AgentChatProgressData);
</script>

<style scoped lang="less">
  .ai-chat-block-progress {
    width: 100%;
  }
</style>
