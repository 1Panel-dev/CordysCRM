<template>
  <CrmModal
    v-model:show="showModal"
    :title="t('system.business.modelSettings.routeStrategy')"
    :width="680"
    :positive-text="t('common.save')"
    :ok-loading="loading"
    @confirm="save"
  >
    <n-form label-placement="top">
      <n-form-item>
        <template #label>
          {{ t('system.business.modelSettings.defaultModel') }}
          <span class="text-[var(--text-n4)]">{{ t('system.business.modelSettings.defaultModelTip') }}</span>
        </template>
        <n-select
          v-model:value="form.defaultModelId"
          clearable
          :options="modelOptions"
          :placeholder="t('common.pleaseSelect')"
        />
      </n-form-item>
      <n-form-item :label="t('system.business.modelSettings.insightModel')">
        <n-select
          v-model:value="form.insightModelId"
          clearable
          :options="modelOptions"
          :placeholder="t('common.pleaseSelect')"
        />
      </n-form-item>
      <n-form-item>
        <template #label>
          {{ t('system.business.modelSettings.classifyModel') }}
          <span class="text-[var(--text-n4)]">{{ t('system.business.modelSettings.classifyModelTip') }}</span>
        </template>
        <n-select
          v-model:value="form.classifyModelId"
          :options="modelOptions"
          clearable
          :placeholder="t('common.pleaseSelect')"
        />
      </n-form-item>
      <div class="flex items-center gap-[8px]">
        <n-switch v-model:value="form.fallback" :rubber-band="false" />
        <div class="text-[var(--text-n1)]">
          {{ t('system.business.modelSettings.autoFallback') }}
        </div>
      </div>
    </n-form>
  </CrmModal>
</template>

<script setup lang="ts">
  import { reactive, ref, watch } from 'vue';
  import { NForm, NFormItem, NSelect, NSwitch, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiModelRouteStrategy } from '@lib/shared/models/system/aiModel';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import { getAiModelOptions, getAiModelRouteStrategy, updateAiModelRouteStrategy } from '@/api/modules';

  import type { SelectOption } from 'naive-ui';

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const { t } = useI18n();
  const Message = useMessage();

  const loading = ref(false);
  const modelOptions = ref<SelectOption[]>([]);

  interface RouteStrategyForm {
    defaultModelId?: string;
    insightModelId?: string;
    classifyModelId?: string;
    fallback: boolean;
  }

  const form = reactive<RouteStrategyForm>({
    fallback: true,
  });

  async function loadStrategy() {
    try {
      loading.value = true;
      const [strategy, models] = await Promise.all([getAiModelRouteStrategy(), getAiModelOptions()]);
      form.defaultModelId = strategy?.chatModels?.[0];
      form.insightModelId = strategy?.taskModels?.[0];
      form.classifyModelId = strategy?.chatModels?.[1];
      form.fallback = strategy?.fallback ?? true;
      modelOptions.value = models.map((model) => ({
        label: model.name,
        value: model.id,
      }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  watch(
    () => showModal.value,
    (visible) => {
      if (visible) {
        loadStrategy();
      }
    }
  );

  function getStrategyParams(): AiModelRouteStrategy {
    return {
      chatModels: [form.defaultModelId ?? '', form.classifyModelId].filter(
        (modelId, index) => index === 0 || modelId
      ) as string[],
      taskModels: [form.insightModelId].filter(Boolean) as string[],
      fallback: form.fallback,
    };
  }

  async function save() {
    try {
      loading.value = true;
      await updateAiModelRouteStrategy(getStrategyParams());
      Message.success(t('common.saveSuccess'));
      showModal.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }
</script>
