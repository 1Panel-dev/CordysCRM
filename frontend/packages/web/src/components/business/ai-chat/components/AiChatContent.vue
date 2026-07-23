<template>
  <section class="flex h-full w-full flex-col bg-[var(--text-n10)]">
    <div class="flex-1 overflow-hidden">
      <!-- 消息列表区域 -->
      <slot name="thread">
        <AiThread />
      </slot>
    </div>

    <!-- 输入区 -->
    <div>
      <slot name="composer">
        <AiComposer />
      </slot>
    </div>
  </section>
</template>

<script setup lang="ts">
  import AiComposer from './AiComposer.vue';
  import AiThread from './AiThread.vue';
</script>
