<template>
  <div class="ai-chat-block text-[var(--error-red)]">
    {{ errorMessage || t('common.operationFailed') }}
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  import type { AiChatDataParts, AiChatError } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import type { DataUIPart } from 'ai';

  const props = defineProps<{
    part: DataUIPart<AiChatDataParts>;
  }>();

  const { t } = useI18n();

  function isAiChatError(data: unknown): data is AiChatError {
    return Boolean(data && typeof data === 'object' && 'message' in data);
  }

  const errorMessage = computed(() => (isAiChatError(props.part.data) ? props.part.data.message : ''));
</script>
