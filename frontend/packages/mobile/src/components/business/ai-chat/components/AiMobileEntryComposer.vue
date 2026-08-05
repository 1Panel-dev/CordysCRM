<template>
  <div class="sticky bottom-0 z-[8] p-[16px]">
    <div
      class="flex items-center gap-[8px] rounded-full bg-[var(--text-n10)] p-[16px] shadow-[0_4px_10px_-1px_#6467671a]"
    >
      <van-field
        v-model="inputValue"
        autosize
        type="textarea"
        rows="1"
        :border="false"
        :placeholder="props.placeholder || t('aiChat.inputPlaceholder')"
        class="min-w-0 flex-1 !bg-transparent !p-0"
        @keypress.enter.prevent="handleSubmit"
      />
      <div
        class="inline-flex h-[24px] w-[24px] flex-none items-center justify-center rounded-full bg-[var(--primary-8)]"
        :class="{ 'opacity-35': !canSubmit }"
        @click="handleSubmit"
      >
        <CrmIcon name="iconicon_send" width="16px" height="16px" color="var(--text-n10)" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  const props = defineProps<{
    placeholder?: string;
  }>();

  const emit = defineEmits<{
    (e: 'submit', content: string): void;
  }>();

  const { t } = useI18n();

  const inputValue = ref('');
  const canSubmit = computed(() => inputValue.value.trim().length > 0);

  function handleSubmit(): void {
    const content = inputValue.value.trim();

    if (!content) {
      return;
    }

    emit('submit', content);
    inputValue.value = '';
  }
</script>
