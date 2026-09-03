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

        <van-field
          v-if="item.textInput"
          v-model="textValues[itemIndex]"
          :placeholder="t('common.pleaseInput')"
          clearable
          class="ai-mobile-confirm__input"
        />

        <van-checkbox-group v-else-if="isMultipleItem(item)" v-model="multipleValues[itemIndex]">
          <van-cell-group :border="false">
            <van-cell
              v-for="option in getItemOptions(item)"
              :key="option.value"
              clickable
              :border="false"
              class="!px-0"
            >
              <van-checkbox :name="option.value" class="ai-mobile-confirm__option">
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
            <van-cell
              v-for="option in getItemOptions(item)"
              :key="option.value"
              clickable
              :border="false"
              class="!px-0"
            >
              <van-radio :name="option.value" class="ai-mobile-confirm__option">
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
  const textValues = ref<string[]>([]);

  const confirmItems = computed(() => props.confirm.items ?? []);

  function isMultipleItem(item: AgentChatConfirmItem): boolean {
    return item.selectionType === 'MULTIPLE';
  }

  function getItemOptions(item: AgentChatConfirmItem) {
    return item.options ?? [];
  }

  function getSelectedValues(item: AgentChatConfirmItem, itemIndex: number): string[] {
    if (isMultipleItem(item)) {
      return multipleValues.value[itemIndex] ?? [];
    }

    const singleValue = singleValues.value[itemIndex];

    return singleValue ? [singleValue] : [];
  }

  function getAnswerValues(item: AgentChatConfirmItem, itemIndex: number): string[] {
    const textValue = textValues.value[itemIndex]?.trim();

    if (item.textInput && textValue) {
      return [textValue];
    }

    if (item.textInput) {
      return [];
    }

    return getSelectedValues(item, itemIndex);
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
      const answerValues = getAnswerValues(item, itemIndex);

      if (item.prompt && (action !== 'confirm' || getItemOptions(item).length === 0 || answerValues.length > 0)) {
        result[item.prompt] = action === 'confirm' ? answerValues.join(', ') : '';
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
      singleValues.value = confirmItems.value.map((item) =>
        isMultipleItem(item) || item.textInput ? '' : getItemOptions(item)[0]?.value ?? ''
      );
      multipleValues.value = confirmItems.value.map(() => []);
      textValues.value = confirmItems.value.map(() => '');
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
  .ai-mobile-confirm__input {
    padding: 8px 0;
    :deep(.van-field__control) {
      padding: 8px 10px;
      border: 1px solid var(--text-n8);
      border-radius: 4px;
      color: var(--text-n1);
      background: var(--text-n10);
    }
  }
</style>
