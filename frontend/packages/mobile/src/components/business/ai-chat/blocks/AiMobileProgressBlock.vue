<template>
  <div class="ai-chat-block ai-mobile-progress">
    <van-collapse v-model="activeNames" :border="false">
      <van-collapse-item :name="partId">
        <template #title>
          <div class="mr-[4px] flex min-w-0 items-center gap-[8px]">
            <CrmIcon name="iconicon_set_up" width="16px" height="16px" color="var(--text-n4)" />
            <span class="ai-mobile-progress__title">
              {{ progress?.title || t('aiChat.progress') }}
            </span>
          </div>
        </template>

        <div v-if="hasDetails" class="space-y-[8px]">
          <div v-if="progress?.details?.input" class="ai-mobile-progress__detail">
            <div class="ai-mobile-progress__label">{{ t('aiChat.progressInput') }}</div>
            <pre class="ai-mobile-progress__content">{{ progress.details.input }}</pre>
          </div>

          <div v-if="progress?.details?.output" class="ai-mobile-progress__detail">
            <div class="ai-mobile-progress__label">{{ t('aiChat.progressOutput') }}</div>
            <pre class="ai-mobile-progress__content">{{ progress.details.output }}</pre>
          </div>
        </div>
      </van-collapse-item>
    </van-collapse>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';

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
  const partId = computed(() => `${props.part.type}_${props.index ?? 0}`);
  const activeNames = ref<string[]>(props.isGenerating ? [partId.value] : []);
  const hasDetails = computed(() => Boolean(progress.value?.details?.input || progress.value?.details?.output));

  watch(
    () => partId.value,
    (id) => {
      activeNames.value = props.isGenerating ? [id] : [];
    }
  );

  watch(
    () => props.isGenerating,
    (isGenerating) => {
      activeNames.value = isGenerating ? [partId.value] : [];
    }
  );
</script>

<style scoped lang="less">
  .ai-mobile-progress {
    width: 100%;
    color: var(--text-n2);
    :deep(.van-collapse) {
      width: 100%;
    }
    :deep(.van-cell) {
      padding: 0;
      color: var(--text-n2);
      background: transparent;
    }
    :deep(.van-cell::after) {
      display: none;
    }
    :deep(.van-cell__title) {
      flex: none;
      max-width: calc(100% - 20px);
    }
    :deep(.van-collapse-item__content) {
      padding-right: 0;
      padding-left: 0;
      color: var(--text-n2);
    }
  }
  .ai-mobile-progress__title {
    overflow: hidden;
    min-width: 0;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .ai-mobile-progress__detail {
    overflow: hidden;
    border-radius: 6px;
    background: var(--text-n9);
  }
  .ai-mobile-progress__label {
    padding: 6px 8px;
    font-size: 12px;
    color: var(--text-n2);
    background: var(--text-n8);
  }
  .ai-mobile-progress__content {
    overflow: auto;
    margin: 0;
    padding: 8px;
    max-height: 180px;
    font-size: 12px;
    white-space: pre-wrap;
    color: var(--text-n2);
    word-break: break-word;
  }
</style>
