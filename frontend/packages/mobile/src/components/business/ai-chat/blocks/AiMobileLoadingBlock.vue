<template>
  <div class="ai-chat-block ai-mobile-loading">
    <span />
    <span />
    <span />
  </div>
</template>

<style scoped lang="less">
  .ai-mobile-loading {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 6px 0;
    span {
      width: 5px;
      height: 5px;
      border-radius: 50%;
      background: var(--primary-8);
      animation: ai-mobile-loading 1.2s infinite ease-in-out;
      &:nth-child(2) {
        animation-delay: 0.16s;
      }
      &:nth-child(3) {
        animation-delay: 0.32s;
      }
    }
  }

  @keyframes ai-mobile-loading {
    0%,
    80%,
    100% {
      opacity: 0.35;
      transform: scale(0.8);
    }
    40% {
      opacity: 1;
      transform: scale(1);
    }
  }
</style>
