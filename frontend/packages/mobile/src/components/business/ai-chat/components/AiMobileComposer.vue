<template>
  <div class="px-[12px] pb-[12px]">
    <div v-if="isEditing" class="mb-[8px] rounded-[8px] bg-[var(--primary-7)] px-[12px] py-[8px] text-[var(--text-n2)]">
      <div class="flex items-center justify-between">
        <span>{{ t('aiChat.editingMessage') }}</span>
        <CrmIcon
          name="iconicon_close"
          width="16px"
          height="16px"
          color="var(--text-n3)"
          @click="runtime.cancelEditMessage"
        />
      </div>
    </div>

    <div
      class="flex items-center gap-[8px] rounded-[30px] bg-[var(--text-n10)] p-[16px] shadow-[0_4px_10px_-1px_#6467671A]"
    >
      <van-field
        v-model="composerValue"
        :autosize="{ maxHeight: 140 }"
        type="textarea"
        rows="1"
        :border="false"
        :placeholder="placeholder || t('aiChat.inputPlaceholder')"
        class="flex-1 !bg-transparent !p-0"
        @keypress.enter.prevent="handleSubmit"
      />
      <van-button
        v-if="runtime.state.canStop.value"
        class="ai-mobile-composer__button"
        round
        size="mini"
        @click="runtime.stop"
      >
        <span class="block h-[8px] w-[8px] rounded-[2px] bg-[var(--text-n10)]" />
      </van-button>
      <van-button
        v-else
        class="ai-mobile-composer__button"
        :disabled="!canSubmit"
        round
        size="mini"
        @click="handleSubmit"
      >
        <CrmIcon name="iconicon_send" width="16px" height="16px" color="var(--text-n10)" />
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';

  import { useAiChatRuntime } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  defineProps<{
    placeholder?: string;
  }>();

  const { t } = useI18n();
  const runtime = useAiChatRuntime();

  const inputValue = ref(runtime.state.input.value);

  const isEditing = computed(() => Boolean(runtime.state.editingMessageId.value));
  const composerValue = computed({
    get: () => (isEditing.value ? runtime.state.editingContent.value : inputValue.value),
    set: (value: string) => {
      if (isEditing.value) {
        runtime.setEditingContent(value);
      } else {
        inputValue.value = value;
      }
    },
  });
  const canSubmit = computed(() =>
    isEditing.value
      ? runtime.state.canSubmitEdit.value
      : !runtime.state.loading.value && inputValue.value.trim().length > 0
  );

  async function handleSubmit(): Promise<void> {
    const content = composerValue.value.trim();

    if (!content || runtime.state.loading.value) {
      return;
    }

    if (isEditing.value) {
      await runtime.submitEditMessage();
    } else {
      await runtime.submit({ content });
    }
  }

  watch(inputValue, (value) => {
    runtime.setInput(value);
  });

  watch(runtime.state.input, (value) => {
    if (value !== inputValue.value) {
      inputValue.value = value;
    }
  });
</script>

<style scoped lang="less">
  .ai-mobile-composer__button {
    width: 24px;
    border: 0;
    color: var(--text-n10);
    background: var(--primary-8);
  }
</style>
