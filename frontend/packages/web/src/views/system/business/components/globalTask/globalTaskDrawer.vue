<template>
  <CrmDrawer
    v-model:show="showDrawer"
    :title="drawerTitle"
    :width="680"
    :ok-text="isEdit ? t('common.update') : t('common.add')"
    :show-continue="!isEdit"
    :loading="saving"
    @confirm="submit(false)"
    @continue="submit(true)"
    @cancel="resetFormState()"
  >
    <n-form ref="formRef" :model="form" :rules="rules" label-placement="top" require-mark-placement="right">
      <n-form-item :label="t('system.business.globalTask.taskName')" path="name">
        <n-input
          v-model:value="form.name"
          clearable
          :maxlength="255"
          :placeholder="t('system.business.globalTask.taskNamePlaceholder')"
        />
      </n-form-item>

      <n-form-item :label="t('system.business.globalTask.triggerCondition')" path="triggerType">
        <n-radio-group v-model:value="form.triggerType" name="triggerType">
          <n-space>
            <n-radio
              v-for="item in triggerTypeOptions"
              :key="item.value as string"
              :value="item.value"
              :label="item.label as string"
            />
          </n-space>
        </n-radio-group>
      </n-form-item>

      <n-form-item :label="t('system.business.globalTask.triggerConditionDetail')" path="executionCondition">
        <n-input v-model:value="form.executionCondition" clearable :placeholder="conditionPlaceholder" />
      </n-form-item>

      <n-form-item :label="t('system.business.globalTask.executionAction')" path="executionAction">
        <n-input
          v-model:value="form.executionAction"
          type="textarea"
          :autosize="{ minRows: 2 }"
          :placeholder="t('system.business.globalTask.executionActionPlaceholder')"
        />
      </n-form-item>

      <n-form-item :label="t('system.business.globalTask.confirmationLevel')" path="confirmationLevel">
        <n-radio-group v-model:value="form.confirmationLevel" name="confirmationLevel" class="w-full">
          <div class="flex flex-col gap-[8px]">
            <label
              v-for="item in confirmationLevelOptions"
              :key="item.value"
              class="cursor-pointer rounded-[4px] border p-[8px]"
              :class="
                form.confirmationLevel === item.value
                  ? 'border-[var(--primary-8)] bg-[var(--primary-7)]'
                  : 'border-[var(--text-n8)] bg-[var(--text-n10)]'
              "
            >
              <n-radio :value="item.value">
                <div>
                  <div>{{ item.label }}</div>
                  <div class="mt-[4px] text-[var(--text-n4)]">{{ item.description }}</div>
                </div>
              </n-radio>
            </label>
          </div>
        </n-radio-group>
      </n-form-item>

      <n-form-item :label="t('system.business.globalTask.applicableModel')" path="applicableModel">
        <n-select
          v-model:value="form.applicableModel"
          filterable
          clearable
          :loading="modelLoading"
          :options="modelOptions"
          :placeholder="t('common.pleaseSelect')"
        />
      </n-form-item>
    </n-form>

    <template #footerLeft>
      <div class="flex items-center gap-[8px]">
        <n-switch v-model:value="form.enable" :rubber-band="false" />
        <span>{{ t('system.business.globalTask.enableTask') }}</span>
      </div>
    </template>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { computed, reactive, ref, watch } from 'vue';
  import {
    FormInst,
    NForm,
    NFormItem,
    NInput,
    NRadio,
    NRadioGroup,
    NSelect,
    NSpace,
    NSwitch,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import {
    AgentTaskConfirmationLevelEnum,
    type AgentTaskItem,
    type AgentTaskParams,
    AgentTaskTriggerTypeEnum,
  } from '@lib/shared/models/system/agentTask';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';

  import { addAgentTask, getAiModelOptions, updateAgentTask } from '@/api/modules';
  import { confirmationLevelOptions, triggerTypeOptions } from '@/config/globalTask';

  import type { FormRules, SelectOption } from 'naive-ui';

  type AgentTaskForm = Omit<AgentTaskParams, 'applicableModel'> & {
    applicableModel?: string | null;
  };

  const props = defineProps<{
    task?: AgentTaskItem | null;
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

  const formRef = ref<FormInst | null>(null);
  const saving = ref(false);
  const modelLoading = ref(false);
  const modelOptions = ref<SelectOption[]>([]);

  const defaultForm: AgentTaskForm = {
    id: undefined,
    name: '',
    triggerType: AgentTaskTriggerTypeEnum.MANUAL,
    executionCondition: '',
    executionAction: '',
    confirmationLevel: AgentTaskConfirmationLevelEnum.ASK,
    applicableModel: null,
    enable: true,
  };

  const form = reactive<AgentTaskForm>({ ...defaultForm });
  const isEdit = computed(() => !!props.task?.id);
  const drawerTitle = computed(() =>
    isEdit.value ? t('system.business.globalTask.updateTask') : t('system.business.globalTask.addTask')
  );
  const conditionPlaceholder = computed(() =>
    form.triggerType === AgentTaskTriggerTypeEnum.MANUAL
      ? t('system.business.globalTask.conditionEventPlaceholder')
      : t('system.business.globalTask.conditionScheduledPlaceholder')
  );
  const rules: FormRules = {
    name: [
      {
        required: true,
        message: t('common.notNull', { value: t('system.business.globalTask.taskName') }),
        trigger: ['blur', 'input'],
      },
    ],
  };

  function resetFormState(task?: Partial<AgentTaskItem>) {
    Object.assign(form, {
      ...defaultForm,
      ...(task ?? {}),
    });
    formRef.value?.restoreValidation();
  }

  async function loadModelOptions() {
    try {
      modelLoading.value = true;
      const result = await getAiModelOptions();
      modelOptions.value = result.map((model) => ({
        label: model.name,
        value: model.id,
      }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      modelLoading.value = false;
    }
  }

  watch(
    () => showDrawer.value,
    async (visible) => {
      if (!visible) {
        return;
      }

      resetFormState(isEdit.value && props.task ? props.task : undefined);
      await loadModelOptions();
    }
  );

  async function submit(continueAdd: boolean) {
    await formRef.value?.validate();

    try {
      saving.value = true;
      const payload: AgentTaskParams = {
        ...form,
        applicableModel: form.applicableModel ?? '',
      };
      let refreshId = payload.id;

      if (form.id) {
        await updateAgentTask(payload);
        Message.success(t('common.updateSuccess'));
      } else {
        await addAgentTask(payload);
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
