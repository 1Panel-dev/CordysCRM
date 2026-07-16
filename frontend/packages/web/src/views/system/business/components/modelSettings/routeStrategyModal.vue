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
        <n-select v-model:value="form.defaultModelId" :options="modelOptions" :placeholder="t('common.pleaseSelect')" />
      </n-form-item>
      <n-form-item :label="t('system.business.modelSettings.insightModel')">
        <n-select v-model:value="form.insightModelId" :options="modelOptions" :placeholder="t('common.pleaseSelect')" />
      </n-form-item>
      <n-form-item>
        <template #label>
          {{ t('system.business.modelSettings.classifyModel') }}
          <span class="text-[var(--text-n4)]">{{ t('system.business.modelSettings.classifyModelTip') }}</span>
        </template>
        <n-select
          v-model:value="form.classifyModelId"
          :options="modelOptions"
          :placeholder="t('common.pleaseSelect')"
        />
      </n-form-item>
      <div class="flex items-center gap-[8px]">
        <n-switch v-model:value="form.autoFallback" :rubber-band="false" />
        <div class="text-[var(--text-n1)]">
          {{ t('system.business.modelSettings.autoFallback') }}
        </div>
      </div>
    </n-form>
  </CrmModal>
</template>

<script setup lang="ts">
  import { computed, reactive, ref, watch } from 'vue';
  import { NForm, NFormItem, NSelect, NSwitch, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AiModelRouteStrategy } from '@lib/shared/models/system/aiModel';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import { getAiModelList, getAiModelRouteStrategy, updateAiModelRouteStrategy } from '@/api/modules';

  import type { SelectOption } from 'naive-ui';

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const { t } = useI18n();
  const Message = useMessage();

  const loading = ref(false);
  const modelOptions = ref<SelectOption[]>([]);

  const form = reactive<AiModelRouteStrategy>({
    autoFallback: true,
  });

  async function loadStrategy() {
    try {
      loading.value = true;
      const [strategy, modelList] = await Promise.all([
        getAiModelRouteStrategy(),
        getAiModelList({ current: 1, pageSize: 9999 }),
      ]);
      Object.assign(form, strategy);
      modelOptions.value = modelList.list
        .filter((model) => model.enable)
        .map((model) => ({
          label: model.displayName,
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

  async function save() {
    try {
      loading.value = true;
      await updateAiModelRouteStrategy({ ...form });
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
