<template>
  <CrmProcessDrawer
    v-model:visible="visible"
    v-model:active-tab="activeTab"
    :tabList="tabList"
    :loading="loading"
    :readonly="isDetail"
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
  import ApprovalFlowDesign from './approval-flow/index.vue';
  import moreSetting from './moreSetting.vue';

  import { addApprovalProcess, approvalProcessDetail, updateApprovalProcess } from '@/api/modules';
  import { defaultBasicForm, defaultMoreConfig } from '@/config/process';
  import { clearApprovalReviewConfigCache } from '@/hooks/useFormReviewAction';
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

  const activeTab = ref('process');

  const initForm: ApprovalProcessForm = {
    id: '',
    enable: false,
    nodes: [],
    links: [],
    basicConfig: {
      ...cloneDeep(defaultBasicForm),
    },
    moreConfig: {
      ...cloneDeep(defaultMoreConfig),
    },
  };

  const form = ref(cloneDeep(initForm));

  const editingName = ref('');
  const approvalFlowDesignRef = ref<InstanceType<typeof ApprovalFlowDesign> | null>(null);
  const detailOptionMap = ref<Record<string, any[]>>({});
  const tabList = [
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

  function handleNextStep() {
    const index = tabList.findIndex((item) => item.name === activeTab.value);
    if (index === tabList.length - 1) {
      return;
    }
    activeTab.value = tabList[index + 1].name;
  }

  async function handleSubmit() {
    try {
      loading.value = true;

      const params = {
        ...form.value,
        ...form.value.basicConfig,
        ...form.value.moreConfig,
        nodes: approvalFlowDesignRef.value?.getProcessNodes() ?? [],
        links: approvalFlowDesignRef.value?.getProcessLinks() ?? [],
      };
      if (props.sourceId) {
        await updateApprovalProcess(params);
        Message.success(t('common.updateSuccess'));
      } else {
        await addApprovalProcess(params);
        Message.success(t('common.addSuccess'));
      }
      clearApprovalReviewConfigCache(form.value.basicConfig.formType);
      emit('refresh');
      closeDrawer();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function handleSave() {
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
      result[key as keyof T] = source[key];
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
        approvalFlowDesignRef.value?.setProcessData(result.nodes ?? [], result.links ?? []);
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
        const params = {
          ...form.value,
          ...form.value.basicConfig,
          ...form.value.moreConfig,
          nodes: approvalFlowDesignRef.value?.getProcessNodes() ?? [],
          links: approvalFlowDesignRef.value?.getProcessLinks() ?? [],
          name: newName,
        };
        await updateApprovalProcess(params);
        clearApprovalReviewConfigCache(form.value.basicConfig.formType);
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
        activeTab.value = 'process';
        userInteracted.value = false;
        return;
      }

      if (!props.sourceId) {
        unsaved.value = false;
        userInteracted.value = false;
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
