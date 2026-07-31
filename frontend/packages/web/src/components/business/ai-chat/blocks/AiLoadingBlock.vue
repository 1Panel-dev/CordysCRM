<template>
  <div class="ai-chat-block flex items-center gap-[4px] px-[2px] py-[4px]">
    <span
      v-for="index in 3"
      :key="index"
      class="h-[6px] w-[6px] animate-bounce rounded-full bg-[var(--text-n4)]"
      :style="{ animationDelay: `${(index - 1) * 120}ms` }"
    />
  </div>
</template>
