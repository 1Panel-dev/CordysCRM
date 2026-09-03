<template>
  <div class="ai-chat-block py-[4px]">
    <span class="ai-chat-loading-text">{{ t('aiChat.thinkingInProgress') }}</span>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  const { t } = useI18n();
</script>

<style scoped lang="less">
  .ai-chat-loading-text {
    color: transparent;
    background: linear-gradient(90.26deg, #4d4d4d 0%, rgb(50 53 53 / 20%) 2.97%, #d6d6d6 5.62%);
    background-size: 1000% 100%;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    animation: ai-chat-loading-text 2.6s linear infinite;
  }

  @keyframes ai-chat-loading-text {
    to {
      background-position: -100% 0;
    }
  }
</style>
