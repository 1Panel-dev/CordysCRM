<template>
  <CrmDrawer
    v-model:show="showDrawer"
    :title="drawerTitle"
    :width="680"
    :ok-text="isEdit ? t('common.update') : t('common.add')"
    :show-continue="!isEdit"
    :continue-text="t('common.saveAndContinue')"
    :loading="saving"
    @confirm="submit(false)"
    @continue="submit(true)"
    @cancel="resetForm"
  >
    <n-form ref="formRef" :model="form" :rules="rules" label-placement="top" require-mark-placement="right-hanging">
      <div class="mb-[16px] font-semibold">
        {{ t('common.baseInfo') }}
      </div>
      <n-form-item :label="t('system.business.modelSettings.modelName')" path="displayName">
        <n-input
          v-model:value="form.displayName"
          clearable
          :maxlength="255"
          :placeholder="t('system.business.modelSettings.modelNamePlaceholder')"
        />
      </n-form-item>
      <n-form-item path="modelName">
        <template #label>
          {{ t('system.business.modelSettings.modelId') }}
          <span class="text-[var(--text-n4)]">{{ t('system.business.modelSettings.modelIdTip') }}</span>
        </template>
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
          :placeholder="t('system.business.modelSettings.apiBaseUrlPlaceholder')"
          :maxlength="255"
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
          :input-props="{ autocomplete: 'new-password', name: 'agent_model_api_key' }"
        />
      </n-form-item>

      <div class="mb-[16px] mt-[8px] font-semibold">
        {{ t('system.business.modelSettings.callLimit') }}
      </div>
      <div class="grid grid-cols-2 gap-x-[16px]">
        <n-form-item path="globalDailyLimit">
          <template #label>
            {{ t('system.business.modelSettings.globalDailyLimit') }}
            <span class="text-[var(--text-n4)]">{{ t('system.business.modelSettings.timesTip') }}</span>
          </template>
          <CrmInputNumber
            v-model:value="form.globalDailyLimit"
            :min="1"
            :max="Number.MAX_SAFE_INTEGER"
            :step="100"
            :precision="0"
            class="w-full"
          />
        </n-form-item>
        <n-form-item path="userDailyLimit">
          <template #label>
            {{ t('system.business.modelSettings.userDailyLimit') }}
            <span class="text-[var(--text-n4)]">{{ t('system.business.modelSettings.timesTip') }}</span>
          </template>
          <CrmInputNumber
            v-model:value="form.userDailyLimit"
            :min="1"
            :max="Number.MAX_SAFE_INTEGER"
            :step="10"
            :precision="0"
            class="w-full"
          />
        </n-form-item>
      </div>
    </n-form>

    <template #footerLeft>
      <div class="flex items-center gap-[8px]">
        <n-switch v-model:value="form.enable" :rubber-band="false" />
        <span>{{ t('system.business.modelSettings.enableModel') }}</span>
      </div>
    </template>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { computed, reactive, ref, watch } from 'vue';
  import { FormInst, NForm, NFormItem, NInput, NSelect, NSwitch, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { type AiModelItem, type AiModelSaveParams } from '@lib/shared/models/system/aiModel';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmInputNumber from '@/components/pure/crm-input-number/index.vue';

  import { addAiModel, updateAiModel } from '@/api/modules';

  import { DEFAULT_MODEL_PROVIDER, getModelProviderOptions } from './modelProviderOptions';
  import type { FormRules } from 'naive-ui';

  const props = defineProps<{
    model?: AiModelItem | null;
  }>();

  const emit = defineEmits<{
    (e: 'saved', refreshId?: string): void;
  }>();

  const showDrawer = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const { t } = useI18n();
  const Message = useMessage();

  const isEdit = computed(() => !!props.model?.id);
  const drawerTitle = computed(() =>
    isEdit.value ? t('system.business.modelSettings.updateModel') : t('system.business.modelSettings.addModel')
  );
  const providerOptions = computed(() => getModelProviderOptions(t));

  const formRef = ref<FormInst | null>(null);
  const saving = ref(false);

  type AiModelForm = AiModelSaveParams;

  const defaultForm: AiModelForm = {
    id: undefined,
    displayName: '',
    provider: DEFAULT_MODEL_PROVIDER,
    modelName: '',
    apiUrl: '',
    apiKey: '',
    enable: true,
    globalDailyLimit: 10000,
    userDailyLimit: 500,
  };

  function createEditForm(model: Partial<AiModelItem>): AiModelForm {
    return {
      id: model.id,
      displayName: model.displayName ?? '',
      provider: model.provider ?? '',
      modelName: model.modelName ?? '',
      apiUrl: model.apiUrl ?? '',
      apiKey: model.apiKey ?? '',
      enable: model.enable ?? defaultForm.enable,
      globalDailyLimit: model.globalDailyLimit,
      userDailyLimit: model.userDailyLimit,
      modelParams: model.modelParams,
    };
  }

  function createForm(model?: Partial<AiModelItem>): AiModelForm {
    return model ? createEditForm(model) : { ...defaultForm };
  }

  const form = reactive(createForm());

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

  function resetFormState(model?: Partial<AiModelItem>) {
    Object.assign(form, createForm(model));
    formRef.value?.restoreValidation();
  }

  watch(
    () => showDrawer.value,
    (visible) => {
      if (!visible) {
        return;
      }

      resetFormState(isEdit.value && props.model ? props.model : undefined);
    }
  );

  function resetForm() {
    resetFormState();
  }

  function createModelPayload(): AiModelSaveParams {
    const payload: AiModelSaveParams = {
      ...form,
      id: isEdit.value ? form.id : undefined,
    };
    return payload;
  }

  async function submit(continueAdd: boolean) {
    await formRef.value?.validate();
    try {
      saving.value = true;
      const payload = createModelPayload();
      let refreshId = payload.id;

      if (isEdit.value) {
        await updateAiModel(payload);
        Message.success(t('common.updateSuccess'));
      } else {
        await addAiModel(payload);
        refreshId = undefined;
        Message.success(t('common.addSuccess'));
      }

      emit('saved', refreshId);
      if (!continueAdd) {
        showDrawer.value = false;
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
</script>
