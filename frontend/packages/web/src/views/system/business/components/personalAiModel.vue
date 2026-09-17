<template>
  <CrmCard hide-footer no-content-padding :special-height="64">
    <div class="flex min-h-0 flex-col overflow-hidden p-[24px]">
      <n-button class="self-start" type="primary" @click="openAddModal">
        {{ t('system.business.modelSettings.addModel') }}
      </n-button>

      <n-spin class="mt-[16px] min-h-0 w-full" content-class="h-full" :show="loading && modelList.length === 0">
        <div ref="modelListScrollRef" class="model-list-scroll" @scroll="handleModelListScroll">
          <div class="model-list-content">
            <div v-for="item of modelList" :key="item.id" class="model-item">
              <div class="min-w-0 flex-1">
                <template v-for="field in getModelCardFields(item)" :key="field.label">
                  <div class="model-item-label">{{ field.label }}</div>
                  <n-tooltip trigger="hover" :delay="300">
                    <template #trigger>
                      <div class="model-item-value">{{ field.value }}</div>
                    </template>
                    {{ field.value }}
                  </n-tooltip>
                </template>
              </div>
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-[8px]">
                  <n-button class="n-btn-outline-primary" type="primary" ghost @click="openEditModal(item)">
                    {{ t('common.edit') }}
                  </n-button>
                  <n-button type="error" ghost class="n-btn-outline-error" @click="handleDeleteModel(item)">
                    {{ t('common.delete') }}
                  </n-button>
                </div>
                <n-switch :value="item.enable" :rubber-band="false" @click="handleBeforeEnableChange(item)" />
              </div>
            </div>
            <div
              v-if="!loading && modelList.length === 0"
              class="col-span-full flex w-full items-center justify-center p-[44px]"
            >
              {{ t('system.personal.empty') }}
            </div>
          </div>
        </div>
      </n-spin>
    </div>

    <CrmModal
      v-model:show="modelModalVisible"
      :width="560"
      :title="isEdit ? t('system.personal.model.update') : t('system.business.modelSettings.addModel')"
      :positive-text="isEdit ? t('common.update') : t('common.add')"
      :show-continue="!isEdit"
      :continue-text="t('common.saveAndContinue')"
      :ok-loading="saving"
      @confirm="submit(false)"
      @continue="submit(true)"
      @cancel="resetFormState"
    >
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="top" require-mark-placement="right">
        <n-form-item :label="t('system.business.modelSettings.modelName')" path="displayName">
          <n-input
            v-model:value="form.displayName"
            clearable
            :maxlength="255"
            :placeholder="t('system.business.modelSettings.modelNamePlaceholder')"
          />
        </n-form-item>
        <n-form-item :label="t('system.business.modelSettings.modelId')" path="modelName">
          <n-input
            v-model:value="form.modelName"
            clearable
            :maxlength="255"
            :placeholder="t('system.business.modelSettings.modelIdPlaceholder')"
          />
        </n-form-item>
        <n-form-item :label="t('system.business.modelSettings.provider')" path="provider">
          <n-select v-model:value="form.provider" :options="providerOptions" />
        </n-form-item>
        <n-form-item :label="t('system.business.modelSettings.apiBaseUrl')" path="apiUrl">
          <n-input
            v-model:value="form.apiUrl"
            clearable
            :maxlength="255"
            :placeholder="t('system.business.modelSettings.apiBaseUrlPlaceholder')"
          />
        </n-form-item>
        <n-form-item :label="t('system.business.modelSettings.apiKey')" path="apiKey">
          <n-input
            v-model:value="form.apiKey"
            clearable
            show-password-on="click"
            type="password"
            placeholder="sk-..."
            :maxlength="255"
            :input-props="{ autocomplete: 'new-password', name: 'personal_agent_model_api_key' }"
          />
        </n-form-item>
      </n-form>

      <template #footerLeft>
        <div class="flex items-center gap-[8px]">
          <n-switch v-model:value="form.enable" :rubber-band="false" size="small" />
          <span>{{ t('common.enable') }}</span>
        </div>
      </template>
    </CrmModal>
  </CrmCard>
</template>

<script setup lang="ts">
  import { computed, nextTick, onBeforeMount, ref } from 'vue';
  import { FormInst, NButton, NForm, NFormItem, NInput, NSelect, NSpin, NSwitch, NTooltip, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { AiModelItem, AiModelSaveParams } from '@lib/shared/models/system/aiModel';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import {
    addPersonalAiModel,
    deletePersonalAiModel,
    getPersonalAiModelDetail,
    getPersonalAiModelList,
    updatePersonalAiModel,
    updatePersonalAiModelStatus,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  import { DEFAULT_MODEL_PROVIDER, getModelProviderOptions } from './modelSettings/modelProviderOptions';
  import type { FormRules } from 'naive-ui';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const providerOptions = computed(() => getModelProviderOptions(t));

  const formRef = ref<FormInst | null>(null);

  const defaultForm: AiModelSaveParams = {
    id: undefined,
    displayName: '',
    modelName: '',
    provider: DEFAULT_MODEL_PROVIDER,
    apiUrl: '',
    apiKey: '',
    enable: true,
  };

  const form = ref<AiModelSaveParams>({ ...defaultForm });
  const isEdit = computed(() => !!form.value.id);

  const rules: FormRules = {
    displayName: [
      {
        required: true,
        message: t('common.notNull', { value: t('system.business.modelSettings.modelName') }),
        trigger: ['blur', 'input'],
      },
    ],
    apiKey: [
      {
        required: true,
        message: t('common.notNull', { value: t('system.business.modelSettings.apiKey') }),
        trigger: ['blur', 'input'],
      },
    ],
  };

  function getProviderLabel(provider: string): string {
    const label = providerOptions.value.find((option) => option.value === provider)?.label;
    return typeof label === 'string' ? label : provider;
  }

  function getModelCardFields(item: AiModelItem) {
    return [
      {
        label: t('system.business.modelSettings.modelName'),
        value: item.displayName || '-',
      },
      {
        label: t('system.business.modelSettings.modelId'),
        value: item.modelName || '-',
      },
      {
        label: t('system.personal.model.provider'),
        value: getProviderLabel(item.provider),
      },
    ];
  }

  function resetFormState() {
    form.value = { ...defaultForm };
    formRef.value?.restoreValidation();
  }

  const loading = ref(false);
  const modelList = ref<AiModelItem[]>([]);
  const modelListScrollRef = ref<HTMLElement | null>(null);
  const modelPageSize = 20;
  const modelPage = ref(1);
  const modelTotal = ref(0);
  const hasMoreModel = computed(() => modelList.value.length < modelTotal.value);

  async function loadModelList(reset = true) {
    if (loading.value) return;

    try {
      loading.value = true;
      const current = reset ? 1 : modelPage.value + 1;
      const result = await getPersonalAiModelList({ current, pageSize: modelPageSize });

      modelPage.value = result.current ?? current;
      modelTotal.value = result.total ?? 0;
      modelList.value = reset ? result.list ?? [] : [...modelList.value, ...(result.list ?? [])];
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function handleModelListScroll(event: Event) {
    if (loading.value || !hasMoreModel.value) return;

    const target = event.currentTarget as HTMLElement;
    const reachBottom = target.scrollTop + target.clientHeight >= target.scrollHeight - 24;

    if (reachBottom) {
      loadModelList(false);
    }
  }

  function updateModelInList(model: AiModelSaveParams) {
    const index = modelList.value.findIndex((item) => item.id === model.id);
    if (index === -1) return;

    modelList.value[index] = {
      ...modelList.value[index],
      ...model,
    };
  }

  function removeModelFromList(id: string) {
    modelList.value = modelList.value.filter((item) => item.id !== id);
    modelTotal.value = Math.max(modelTotal.value - 1, 0);
  }

  async function scrollModelListToTop() {
    await nextTick();
    modelListScrollRef.value?.scrollTo({ top: 0 });
  }

  const modelModalVisible = ref(false);
  function openAddModal() {
    resetFormState();
    modelModalVisible.value = true;
  }

  async function openEditModal(model: AiModelItem) {
    try {
      const modelDetail = await getPersonalAiModelDetail(model.id);
      form.value = { ...modelDetail };
      modelModalVisible.value = true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const saving = ref(false);
  async function submit(continueAdd: boolean) {
    await formRef.value?.validate();
    try {
      saving.value = true;
      const payload: AiModelSaveParams = {
        ...form.value,
        displayName: form.value.displayName.trim(),
      };
      if (isEdit.value) {
        await updatePersonalAiModel(payload);
        updateModelInList(payload);
        Message.success(t('common.updateSuccess'));
      } else {
        await addPersonalAiModel(payload);
        await loadModelList();
        await scrollModelListToTop();
        Message.success(t('common.addSuccess'));
      }
      if (!continueAdd) {
        modelModalVisible.value = false;
      } else {
        resetFormState();
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      saving.value = false;
    }
  }

  function handleDeleteModel(item: AiModelItem) {
    openModal({
      type: 'error',
      title: t('system.business.modelSettings.deleteConfirmTitle', { name: characterLimit(item.displayName) }),
      content: t('system.business.modelSettings.deleteConfirmContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deletePersonalAiModel(item.id);
          Message.success(t('common.deleteSuccess'));
          removeModelFromList(item.id);
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  async function handleBeforeEnableChange(item: AiModelItem) {
    if (item.enable) {
      openModal({
        type: 'error',
        title: t('common.confirmClose'),
        content: t('system.personal.model.closeTip'),
        positiveText: t('common.confirmClose'),
        negativeText: t('common.cancel'),
        onPositiveClick: async () => {
          try {
            await updatePersonalAiModelStatus({ id: item.id });
            Message.success(t('common.closeSuccess'));
            item.enable = false;
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
          }
        },
      });
    } else {
      try {
        await updatePersonalAiModelStatus({ id: item.id });
        Message.success(t('common.enableSuccess'));
        item.enable = true;
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    }
  }

  onBeforeMount(() => {
    loadModelList();
  });
</script>

<style scoped lang="less">
  .model-list-scroll {
    @apply min-h-0 overflow-y-auto;

    max-height: calc(100vh - 260px);

    .crm-scroll-bar();
  }
  .model-list-content {
    @apply grid;

    gap: 16px;
    align-items: start;
    grid-template-columns: repeat(auto-fill, minmax(318px, 2fr));
    padding: 16px;
    border-radius: 8px;
    background-color: var(--text-n9);
  }
  .model-item {
    @apply flex flex-col;

    padding: 24px;
    min-width: 0;
    border-radius: var(--border-radius-medium);
    background-color: var(--text-n10);
    .model-item-label {
      color: var(--text-n4);
    }
    .model-item-value {
      overflow: hidden;
      margin-bottom: 16px;
      text-overflow: ellipsis;
      white-space: nowrap;
      color: var(--text-n1);
    }
  }
</style>
