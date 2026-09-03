<template>
  <div class="ai-chat-block ai-mobile-thought">
    <van-collapse v-model="activeNames" :border="false">
      <van-collapse-item :name="blockId">
        <template #title>
          <div class="mr-[4px] flex min-w-0 items-center">
            <span class="ai-mobile-thought__title">{{ titleText }}</span>
          </div>
        </template>

        <div>
          <template v-for="item in items" :key="item.key">
            <AiMobileMarkdownBlock
              v-if="item.part.type === 'reasoning'"
              :part="item.part"
              :index="item.index"
              :is-generating="isGenerating"
            />
            <AiMobileProgressBlock
              v-else-if="item.part.type === 'data-progress'"
              :part="item.part"
              :index="item.index"
              :is-generating="isGenerating"
            />
          </template>
        </div>
      </van-collapse-item>
    </van-collapse>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';

  import { type AiChatMessagePart, type AiChatThoughtStatus, formatAiChatDuration } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import AiMobileMarkdownBlock from './AiMobileMarkdownBlock.vue';
  import AiMobileProgressBlock from './AiMobileProgressBlock.vue';

  interface AiThoughtItem {
    key: string;
    part: AiChatMessagePart;
    index: number;
  }

  const props = defineProps<{
    items: AiThoughtItem[];
    messageId: string;
    isGenerating?: boolean;
    status?: AiChatThoughtStatus;
    duration?: number;
  }>();

  const { t } = useI18n();
  const blockId = computed(() => `thought_${props.messageId}`);
  const activeNames = ref<string[]>(props.isGenerating ? [blockId.value] : []);

  const durationText = computed(() => formatAiChatDuration(props.duration));
  const titleText = computed(() => {
    if (props.status === 'thinking' || props.isGenerating) {
      return t('aiChat.thinkingInProgress');
    }

    if (props.status === 'stopped') {
      return t('aiChat.thinkingStopped');
    }

    return [t('common.completed'), durationText.value].filter(Boolean).join(' ');
  });

  watch(
    () => blockId.value,
    (id) => {
      activeNames.value = props.isGenerating ? [id] : [];
    }
  );

  watch(
    () => props.isGenerating,
    (isGenerating) => {
      activeNames.value = isGenerating ? [blockId.value] : [];
    }
  );
</script>

<style scoped lang="less">
  .ai-mobile-thought {
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
  .ai-mobile-thought__title {
    overflow: hidden;
    min-width: 0;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
</style>
