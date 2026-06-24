<template>
  <CrmProcessDrawer
    v-model:visible="visible"
    v-model:active-tab="activeTab"
    :tabList="tabList"
    :loading="loading"
    :readonly="isDetail"
    :before-change-tab="handleBeforeChangeTab"
    @pointerdown.capture="handleUserInteraction"
    @keydown.capture="handleUserInteraction"
    @save="handleSave"
    @next-step="handleNextStep"
    @cancel="handleCancel"
  >
    <template #title>
      <div class="process-name-header flex max-w-full flex-1 overflow-hidden">
        <CrmEditableText
          :status="errorStatus"
          size="small"
          :value="form.basicConfig.name"
          :permission="['PROCESS_SETTING:UPDATE']"
          :click-to-edit="!isDetail"
          :disabled="isDetail"
          :emptyTextTip="t('common.notNull', { value: t('process.process.processName') })"
          @handle-edit="handleEditName"
          @input="handleInput"
        >
          <n-tooltip trigger="hover" :delay="300" :disabled="!form.basicConfig.name">
            <template #trigger>
              <div class="process-name one-line-text">
                {{ form.basicConfig.name ?? '-' }}
              </div>
            </template>
            {{ form.basicConfig.name ?? '-' }}
          </n-tooltip>
        </CrmEditableText>
      </div>
    </template>
    <template v-if="visible">
      <BasicForm
        v-show="activeTab === 'basic'"
        ref="basicFormRef"
        v-model:basicConfig="form.basicConfig"
        :need-detail="!!props.sourceId"
        :readonly="isDetail"
      />
      <ApprovalFlowDesign
        v-show="activeTab === 'process'"
        ref="approvalFlowDesignRef"
        v-model:basicConfig="form.basicConfig"
        :need-detail="!!props.sourceId"
        :readonly="isDetail"
        :option-map="detailOptionMap"
        @change="markUnsaved"
        @switch-more-setting="activeTab = 'moreSetting'"
      />
      <moreSetting
        v-show="activeTab === 'moreSetting'"
        v-model:moreConfig="form.moreConfig"
        :need-detail="!!props.sourceId"
        :form-type="form.basicConfig.formType"
        :readonly="isDetail"
      />
    </template>
  </CrmProcessDrawer>
</template>

<script setup lang="ts">
  import { nextTick, ref } from 'vue';
  import { NTooltip, useMessage } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { ApprovalProcessForm, BasicFormParams, MoreSettingsParams } from '@lib/shared/models/system/process';

  import CrmEditableText from '@/components/business/crm-editable-text/index.vue';
  import CrmProcessDrawer from '@/components/business/crm-process-drawer/index.vue';
  import BasicForm from './approval-flow/basicForm.vue';
  import ApprovalFlowDesign from './approval-flow/index.vue';
  import moreSetting from './moreSetting.vue';

  import { addApprovalProcess, approvalProcessDetail, updateApprovalProcess } from '@/api/modules';
  import { defaultBasicForm, defaultMoreConfig } from '@/config/process';
  import { clearApprovalConfigCache } from '@/hooks/useApprovalConfigCache';
  import useModal from '@/hooks/useModal';

  const props = defineProps<{
    sourceId?: string;
    readonly?: boolean;
    isDetail?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'cancel'): void;
    (e: 'refresh'): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const activeTab = ref('basic');

  const initForm: ApprovalProcessForm = {
    id: '',
    enable: false,
    basicConfig: {
      ...cloneDeep(defaultBasicForm),
    },
    moreConfig: {
      ...cloneDeep(defaultMoreConfig),
    },
  };

  const form = ref(cloneDeep(initForm));

  const editingName = ref('');
  const basicFormRef = ref<InstanceType<typeof BasicForm> | null>(null);
  const approvalFlowDesignRef = ref<InstanceType<typeof ApprovalFlowDesign> | null>(null);
  const detailOptionMap = ref<Record<string, any[]>>({});
  const tabList = [
    {
      name: 'basic',
      tab: t('common.baseInfo'),
    },
    {
      name: 'process',
      tab: t('process.processDesign'),
    },
    {
      name: 'moreSetting',
      tab: t('process.processDesign.moreSetting'),
    },
  ];

  const unsaved = ref(false);
  const userInteracted = ref(false); // 防止没编辑就弹出提示
  const loading = ref(false);

  function markUnsaved() {
    if (!props.readonly && !props.isDetail && userInteracted.value) {
      unsaved.value = true;
    }
  }

  function handleUserInteraction() {
    if (!props.readonly && !props.isDetail) {
      userInteracted.value = true;
    }
  }

  function closeDrawer() {
    unsaved.value = false;
    userInteracted.value = false;
    visible.value = false;
    form.value = cloneDeep(initForm);
    detailOptionMap.value = {};
    emit('cancel');
  }

  function showUnsavedLeaveTip() {
    openModal({
      type: 'warning',
      title: t('common.unSaveLeaveTitle'),
      content: t('common.editUnsavedLeave'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        closeDrawer();
      },
    });
  }

  function handleCancel() {
    if (!loading.value) {
      if (unsaved.value) {
        showUnsavedLeaveTip();
      } else {
        closeDrawer();
      }
    }
  }

  function handleBeforeChangeTab(tab: string | number) {
    const { createExecute, updateExecute, deleteExecute } = form.value.basicConfig;
    if (tab !== 'process' || createExecute || updateExecute || deleteExecute) {
      return true;
    }

    Message.warning(t('common.notNull', { value: t('process.process.basic.executionTiming') }));
    return false;
  }

  function handleNextStep() {
    const index = tabList.findIndex((item) => item.name === activeTab.value);
    if (index === tabList.length - 1) {
      return;
    }
    const nextTab = tabList[index + 1].name;
    if (handleBeforeChangeTab(nextTab)) {
      activeTab.value = nextTab;
    }
  }

  async function handleSubmit() {
    try {
      loading.value = true;
      const timingProcessData = approvalFlowDesignRef.value?.getTimingProcessData() ?? {};

      const params = {
        ...form.value,
        ...form.value.basicConfig,
        ...form.value.moreConfig,
        ...timingProcessData,
      };
      if (props.sourceId) {
        await updateApprovalProcess(params);
        Message.success(t('common.updateSuccess'));
      } else {
        await addApprovalProcess(params);
        Message.success(t('common.addSuccess'));
      }
      clearApprovalConfigCache(form.value.basicConfig.formType);
      emit('refresh');
      closeDrawer();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function handleSave() {
    const basicValid = (await basicFormRef.value?.validate()) ?? false;
    if (!basicValid) {
      activeTab.value = 'basic';
      return;
    }

    if (approvalFlowDesignRef.value && !approvalFlowDesignRef.value.validateFlowNodes()) {
      openModal({
        type: 'warning',
        title: t('common.saveFailed'),
        positiveText: t('process.process.flow.toConfig'),
        content: t('process.process.flow.nodeNameNotSet'),
        negativeText: t('common.cancel'),
        onPositiveClick: async () => {
          activeTab.value = 'process';
          nextTick(() => {
            approvalFlowDesignRef.value?.refreshCanvas();
          });
        },
      });
      return;
    }

    handleSubmit();
  }

  function pickFormConfig<T extends Record<string, any>>(source: Record<string, any>, defaultConfig: T): T {
    const result = { ...defaultConfig };

    Object.keys(defaultConfig).forEach((key) => {
      if (source[key] !== undefined) {
        result[key as keyof T] = source[key];
      }
    });

    return result;
  }

  async function getDetail(val: string) {
    try {
      const result = await approvalProcessDetail(val);

      detailOptionMap.value = result.optionMap ?? {};
      const basicConfig = pickFormConfig<BasicFormParams>(result, defaultBasicForm);
      const moreConfig = pickFormConfig<MoreSettingsParams>(result, defaultMoreConfig);
      form.value = {
        ...result,
        basicConfig,
        moreConfig,
      };
      editingName.value = result.name;
      nextTick(() => {
        approvalFlowDesignRef.value?.setProcessData(result);
        nextTick(() => {
          unsaved.value = false;
          userInteracted.value = false;
        });
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function handleEditName(newName: string, done?: () => void) {
    if (props.isDetail) {
      try {
        loading.value = true;
        const timingProcessData = approvalFlowDesignRef.value?.getTimingProcessData() ?? {};
        const params = {
          ...form.value,
          ...form.value.basicConfig,
          ...form.value.moreConfig,
          ...timingProcessData,
          name: newName,
        };
        await updateApprovalProcess(params);
        clearApprovalConfigCache(form.value.basicConfig.formType);
        form.value.basicConfig.name = newName;
        editingName.value = newName;
        done?.();
        getDetail(form.value.id);
        emit('refresh');
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      } finally {
        loading.value = false;
      }
    } else {
      form.value.basicConfig.name = newName;
      editingName.value = newName;
      done?.();
    }
  }

  const errorStatus = computed(() => (editingName.value.trim().length ? '' : 'error'));
  function handleInput(value: string) {
    editingName.value = value;
  }

  watch(
    () => props.sourceId,
    (val) => {
      if (val) {
        getDetail(val);
      }
    }
  );

  watch(
    () => visible.value,
    (val) => {
      if (!val) {
        activeTab.value = 'basic';
        userInteracted.value = false;
        return;
      }

      if (!props.sourceId) {
        unsaved.value = false;
        userInteracted.value = false;
      }
    }
  );

  function refreshApprovalFlowCanvas() {
    nextTick(() => {
      requestAnimationFrame(() => {
        approvalFlowDesignRef.value?.refreshCanvas(true);
      });
    });
  }

  watch(
    () => activeTab.value,
    (tab) => {
      if (tab === 'process') {
        refreshApprovalFlowCanvas();
      }
    }
  );

  watch(
    () => [form.value.basicConfig, form.value.moreConfig],
    () => {
      markUnsaved();
    },
    {
      deep: true,
    }
  );
</script>

<style lang="less">
  .process-name-header {
    min-width: 0;
    > * {
      min-width: 0;
      max-width: 100%;
      flex: 1 1 auto;
    }
    .table-row-edit {
      @apply invisible;
    }
    &:hover {
      .table-row-edit {
        color: var(--primary-8);
        @apply visible;
      }
    }
    .process-name {
      overflow: hidden;
      min-width: 0;
      max-width: 100%;
      font-size: 14px;
      font-weight: 400;
      border-bottom: 2px solid var(--text-n6);
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
</style>
