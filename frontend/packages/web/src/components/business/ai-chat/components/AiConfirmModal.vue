<template>
  <CrmModal
    v-model:show="showModal"
    :title="t('aiChat.confirmTitle')"
    :width="680"
    :positive-text="confirmButtonLabel"
    :negative-text="cancelButtonLabel"
    :ok-loading="submitting"
    :ok-button-props="{ disabled: !canConfirm }"
    @confirm="handleConfirm"
    @cancel="handleCancel"
  >
    <n-alert type="warning" :show-icon="true" class="mb-[16px] text-[16px]">
      {{ t('aiChat.confirmWarning') }}
    </n-alert>

    <div v-for="(item, itemIndex) in confirmItems" :key="`${item.prompt}_${itemIndex}`">
      <div v-if="item.prompt" class="mb-[8px] font-[600] text-[var(--text-n1)]">
        {{ item.prompt }}
      </div>

      <n-radio-group
        v-if="!isMultipleItem(item)"
        v-model:value="singleValues[itemIndex]"
        class="flex flex-col gap-[8px]"
      >
        <label
          v-for="option in getItemOptions(item)"
          :key="option.label"
          class="flex cursor-pointer gap-[8px] rounded-[4px] border border-[var(--text-n8)] bg-[var(--text-n10)] p-[8px]"
          :class="{ '!border-[var(--primary-8)] !bg-[var(--primary-7)]': singleValues[itemIndex] === option.label }"
        >
          <n-radio :value="option.label" />
          <span class="flex min-w-0 flex-col gap-[4px]">
            <span>{{ option.label }}</span>
            <span v-if="option.description" class="text-[var(--text-n4)]">
              {{ option.description }}
            </span>
          </span>
        </label>
      </n-radio-group>

      <n-checkbox-group v-else v-model:value="multipleValues[itemIndex]" class="flex flex-col gap-[8px]">
        <label
          v-for="option in getItemOptions(item)"
          :key="option.label"
          class="flex cursor-pointer gap-[8px] rounded-[4px] border border-[var(--text-n8)] bg-[var(--text-n10)] p-[8px]"
          :class="{
            '!border-[var(--primary-8)] !bg-[var(--primary-7)]': multipleValues[itemIndex]?.includes(option.label),
          }"
        >
          <n-checkbox :value="option.label" />
          <span class="flex min-w-0 flex-col gap-[4px]">
            <span>{{ option.label }}</span>
            <span v-if="option.description" class="text-[var(--text-n4)]">
              {{ option.description }}
            </span>
          </span>
        </label>
      </n-checkbox-group>
    </div>
  </CrmModal>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { NAlert, NCheckbox, NCheckboxGroup, NRadio, NRadioGroup } from 'naive-ui';

  import { useAiChatRuntime } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AgentChatConfirmData, AgentChatConfirmItem } from '@lib/shared/models/ai';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  const props = defineProps<{
    confirm: AgentChatConfirmData;
  }>();

  const { t } = useI18n();
  const runtime = useAiChatRuntime();

  const showModal = ref(true);
  const submitting = ref(false);
  const closeHandled = ref(false);
  const singleValues = ref<string[]>([]);
  const multipleValues = ref<string[][]>([]);

  const confirmItems = computed(() => props.confirm.items ?? []);
  const confirmButtonLabel = computed(() => t('aiChat.confirmExecute'));
  const cancelButtonLabel = computed(() => t('aiChat.confirmCancel'));

  function isMultipleItem(item: AgentChatConfirmItem): boolean {
    return item.selectionType === 'MULTIPLE';
  }

  function getItemOptions(item: AgentChatConfirmItem) {
    return item.options ?? [];
  }

  function getSelectedLabels(item: AgentChatConfirmItem, itemIndex: number): string[] {
    if (isMultipleItem(item)) {
      return multipleValues.value[itemIndex] ?? [];
    }

    const singleValue = singleValues.value[itemIndex];

    return singleValue ? [singleValue] : [];
  }

  const canConfirm = computed(
    () =>
      confirmItems.value.length > 0 &&
      confirmItems.value.every(
        (item, index) => getItemOptions(item).length === 0 || getSelectedLabels(item, index).length > 0
      )
  );

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

  async function submitConfirm(answers: Record<string, string>) {
    if (submitting.value) {
      return;
    }

    try {
      submitting.value = true;
      await runtime.confirm(props.confirm, answers);
      closeHandled.value = true;
      showModal.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      showModal.value = true;
    } finally {
      submitting.value = false;
    }
  }

  async function handleConfirm() {
    const answers = confirmItems.value.reduce<Record<string, string>>((result, item, itemIndex) => {
      const selectedLabels = getSelectedLabels(item, itemIndex);

      if (item.prompt && (getItemOptions(item).length === 0 || selectedLabels.length > 0)) {
        result[item.prompt] = selectedLabels.join(', ');
      }

      return result;
    }, {});
    const submitAnswers = appendButtonLabelToLastAnswer(answers, confirmButtonLabel.value);

    if (!canConfirm.value || Object.keys(submitAnswers).length === 0) {
      return;
    }

    await submitConfirm(submitAnswers);
  }

  async function handleCancel() {
    const answers = confirmItems.value.reduce<Record<string, string>>((result, item) => {
      if (item.prompt) {
        result[item.prompt] = '';
      }

      return result;
    }, {});
    const submitAnswers = appendButtonLabelToLastAnswer(answers, cancelButtonLabel.value);

    if (Object.keys(submitAnswers).length === 0) {
      return;
    }

    await submitConfirm(submitAnswers);
  }

  watch(
    () => props.confirm,
    () => {
      submitting.value = false;
      closeHandled.value = false;
      showModal.value = true;
      singleValues.value = confirmItems.value.map((item) =>
        isMultipleItem(item) ? '' : getItemOptions(item)[0]?.label ?? ''
      );
      multipleValues.value = confirmItems.value.map(() => []);
    },
    { immediate: true }
  );

  watch(showModal, (value) => {
    if (!value && !submitting.value && !closeHandled.value) {
      handleCancel();
    }
  });
</script>
