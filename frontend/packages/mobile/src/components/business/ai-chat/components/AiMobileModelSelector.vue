<template>
  <div class="ai-mobile-model-selector fixed left-0 right-0 top-[48px] z-[10]">
    <div class="ai-mobile-model-selector__trigger" @click="showPicker = true">
      <CrmIcon name="iconicon_star1" width="16px" height="16px" color="var(--text-n1)" />
      <span class="max-w-[180px] truncate">{{ selectedModel?.name || t('aiChat.model') }}</span>
      <CrmIcon
        :name="showPicker ? 'iconicon_chevron_up' : 'iconicon_chevron_down'"
        width="16px"
        height="16px"
        color="var(--text-n4)"
      />
    </div>

    <van-popup v-model:show="showPicker" round position="bottom">
      <div class="p-[16px]">
        <van-tabs v-model:active="activeModelSource">
          <van-tab :title="t('aiChat.personalModel')" name="personal" />
          <van-tab :title="t('aiChat.systemModel')" name="system" />
        </van-tabs>

        <div class="mt-[12px] max-h-[45vh] overflow-y-auto">
          <div v-if="loading" class="flex h-[120px] items-center justify-center">
            <van-loading color="var(--primary-8)" />
          </div>

          <template v-else-if="activeModelOptions.length">
            <div
              v-for="model in activeModelOptions"
              :key="model.id"
              class="ai-mobile-model-selector__option"
              :class="{ 'ai-mobile-model-selector__option--active': selectedModel?.id === model.id }"
              @click="handleSelect(model)"
            >
              <span class="truncate">{{ model.name }}</span>
              <CrmIcon
                v-if="selectedModel?.id === model.id"
                name="iconicon_check"
                width="16px"
                height="16px"
                color="var(--primary-8)"
              />
            </div>
          </template>

          <van-empty v-else :description="t('aiChat.noModel')" :image-size="72" class="py-[16px]" />
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue';

  import type { AiChatModel, AiChatModelSource } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import useAiModelOptions from '../composables/useAiModelOptions';

  const { t } = useI18n();
  const { modelOptions, selectedModel, loading, loadModelOptions, selectModel } = useAiModelOptions();

  const showPicker = ref(false);
  const activeModelSource = ref<AiChatModelSource>('personal');
  const activeModelOptions = computed(() =>
    modelOptions.value.filter((model) => model.source === activeModelSource.value)
  );

  function syncActiveSource(): void {
    if (selectedModel.value) {
      activeModelSource.value = selectedModel.value.source;
      return;
    }

    if (!activeModelOptions.value.length && modelOptions.value.length) {
      activeModelSource.value = modelOptions.value[0].source;
    }
  }

  function handleSelect(model: AiChatModel): void {
    selectModel(model);
    activeModelSource.value = model.source;
    showPicker.value = false;
  }

  onMounted(async () => {
    await loadModelOptions();
    syncActiveSource();
  });
</script>

<style scoped lang="less">
  .ai-mobile-model-selector {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 42px;
    background: var(--text-n10);
  }
  .ai-mobile-model-selector__trigger {
    display: inline-flex;
    justify-content: center;
    align-items: center;
    gap: 4px;
    overflow: hidden;
    padding: 0 4px;
    height: 26px;
    font-size: 14px;
    border-radius: 4px;
    color: var(--text-n1);
    background: var(--text-n9);
    line-height: 20px;
  }
  .ai-mobile-model-selector__option {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 12px;
    width: 100%;
    height: 44px;
    border-radius: 6px;
    text-align: left;
    background: transparent;
    gap: 12px;
    line-height: 22px;
  }
  .ai-mobile-model-selector__option--active {
    color: var(--primary-8);
    background: var(--primary-7);
  }
</style>
