<template>
  <van-dialog
    v-model:show="showDialog"
    :title="t('aiChat.confirmTitle')"
    :show-cancel-button="true"
    :confirm-button-text="t('aiChat.confirmExecute')"
    :cancel-button-text="t('aiChat.confirmCancel')"
    :before-close="handleBeforeClose"
  >
    <div class="max-h-[60vh] overflow-y-auto px-[16px] pb-[4px] pt-[12px]">
      <div class="mb-[8px] text-[12px] text-[var(--text-n2)]">{{ t('aiChat.confirmWarning') }}</div>
      <div v-for="(item, itemIndex) in confirmItems" :key="`${item.prompt}_${itemIndex}`" class="[&+&]:mt-[12px]">
        <div v-if="item.prompt" class="mb-[4px] font-semibold text-[var(--text-n1)]">{{ item.prompt }}</div>

        <van-checkbox-group v-if="isMultipleItem(item)" v-model="multipleValues[itemIndex]">
          <van-cell-group :border="false">
            <van-cell v-for="option in item.options" :key="option.label" clickable :border="false" class="!px-0">
              <van-checkbox :name="option.label" class="ai-mobile-confirm__option">
                <div class="flex flex-col gap-[2px] text-left">
                  <span>{{ option.label }}</span>
                  <small v-if="option.description" class="text-[12px] text-[var(--text-n4)]">
                    {{ option.description }}
                  </small>
                </div>
              </van-checkbox>
            </van-cell>
          </van-cell-group>
        </van-checkbox-group>

        <van-radio-group v-else v-model="singleValues[itemIndex]">
          <van-cell-group :border="false">
            <van-cell v-for="option in item.options" :key="option.label" clickable :border="false" class="!px-0">
              <van-radio :name="option.label" class="ai-mobile-confirm__option">
                <div class="flex flex-col gap-[2px] text-left">
                  <span>{{ option.label }}</span>
                  <small v-if="option.description" class="text-[12px] text-[var(--text-n4)]">
                    {{ option.description }}
                  </small>
                </div>
              </van-radio>
            </van-cell>
          </van-cell-group>
        </van-radio-group>
      </div>
    </div>
  </van-dialog>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';

  import { useAiChatRuntime } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AgentChatConfirmData, AgentChatConfirmItem } from '@lib/shared/models/ai';

  const props = defineProps<{
    confirm: AgentChatConfirmData;
  }>();

  const { t } = useI18n();
  const runtime = useAiChatRuntime();

  const showDialog = ref(true);
  const submitting = ref(false);

  const singleValues = ref<string[]>([]);
  const multipleValues = ref<string[][]>([]);

  const confirmItems = computed(() => props.confirm.items ?? []);

  function isMultipleItem(item: AgentChatConfirmItem): boolean {
    return item.selectionType === 'MULTIPLE';
  }

  function getSelectedLabels(item: AgentChatConfirmItem, itemIndex: number): string[] {
    if (isMultipleItem(item)) {
      return multipleValues.value[itemIndex] ?? [];
    }

    const singleValue = singleValues.value[itemIndex];

    return singleValue ? [singleValue] : [];
  }

  function appendButtonLabelToLastAnswer(answers: Record<string, string>, buttonLabel: string): Record<string, string> {
    const answerKeys = Object.keys(answers);
    const lastAnswerKey = answerKeys.at(-1);

    if (!lastAnswerKey) {
      return answers;
    }

    return {
      ...answers,
      [lastAnswerKey]: [answers[lastAnswerKey], buttonLabel].filter(Boolean).join(', '),
    };
  }

  async function submitConfirm(action: string): Promise<boolean> {
    if (submitting.value) {
      return false;
    }

    const answers = confirmItems.value.reduce<Record<string, string>>((result, item, itemIndex) => {
      const selectedLabels = getSelectedLabels(item, itemIndex);

      if (item.prompt) {
        result[item.prompt] = action === 'confirm' ? selectedLabels.join(', ') : '';
      }

      return result;
    }, {});
    const submitAnswers = appendButtonLabelToLastAnswer(
      answers,
      action === 'confirm' ? t('aiChat.confirmExecute') : t('aiChat.confirmCancel')
    );

    if (Object.keys(submitAnswers).length === 0) {
      return true;
    }

    try {
      submitting.value = true;
      await runtime.confirm(props.confirm, submitAnswers);
      return true;
    } finally {
      submitting.value = false;
    }
  }

  function handleBeforeClose(action: string): Promise<boolean> {
    return submitConfirm(action);
  }

  watch(
    () => props.confirm,
    () => {
      showDialog.value = true;
      singleValues.value = confirmItems.value.map((item) => (isMultipleItem(item) ? '' : item.options[0]?.label ?? ''));
      multipleValues.value = confirmItems.value.map(() => []);
    },
    { immediate: true }
  );
</script>

<style scoped lang="less">
  .ai-mobile-confirm__option {
    align-items: flex-start;
    width: 100%;
    :deep(.van-radio__label),
    :deep(.van-checkbox__label) {
      margin-left: 8px;
      min-width: 0;
      text-align: left;
      color: var(--text-n1);
      flex: 1;
    }
  }
</style>
