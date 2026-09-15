import { ref } from 'vue';

import type { AiChatModel } from '@lib/shared/ai-chat';

import { getAgentModelOptions } from '@/api/modules';

const modelOptions = ref<AiChatModel[]>([]);
const selectedModel = ref<AiChatModel | null>(null);
const loading = ref(false);
let loadingPromise: Promise<void> | undefined;

async function doLoadModelOptions(): Promise<void> {
  try {
    loading.value = true;
    const currentSelectedModelId = selectedModel.value?.id;
    const result = await getAgentModelOptions();
    let defaultOption: AiChatModel | null = null;

    modelOptions.value = result.map((model) => {
      const option: AiChatModel = {
        id: model.id,
        name: model.name,
        source: model.scope === 'USER' ? 'personal' : 'system',
      };

      if (model.defaultModel) {
        defaultOption = option;
      }

      return option;
    });
    const currentOption = modelOptions.value.find((model) => model.id === currentSelectedModelId) ?? null;
    selectedModel.value = currentOption ?? defaultOption ?? modelOptions.value[0] ?? null;
  } catch (error) {
    // eslint-disable-next-line no-console
    console.log(error);
  } finally {
    loading.value = false;
  }
}

async function loadModelOptions(): Promise<void> {
  if (loadingPromise) {
    return loadingPromise;
  }

  loadingPromise = doLoadModelOptions().finally(() => {
    loadingPromise = undefined;
  });

  return loadingPromise;
}

function selectModel(model: AiChatModel): void {
  selectedModel.value = model;
}

export default function useAiModelOptions() {
  return {
    modelOptions,
    selectedModel,
    loading,
    loadModelOptions,
    selectModel,
  };
}
