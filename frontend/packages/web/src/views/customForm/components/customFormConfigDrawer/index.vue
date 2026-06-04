<template>
  <CrmProcessDrawer
    v-model:visible="visible"
    v-model:active-tab="activeTab"
    width="100%"
    :loading="loading"
    :title="customFormName"
    :tab-list="tabList"
    @cancel="handleBack"
  >
    <template #title>
      <div class="process-name-header flex max-w-full flex-1 overflow-hidden">
        <CrmEditableText
          :value="customFormName"
          :permission="[]"
          click-to-edit
          :emptyTextTip="t('common.notNull', { value: t('customForm.name') })"
          @handle-edit="handleEditTitle"
        >
          <n-tooltip trigger="hover" :delay="300" :disabled="!customFormName">
            <template #trigger>
              <div class="process-name one-line-text">
                {{ customFormName || '-' }}
              </div>
            </template>
            {{ customFormName || '-' }}
          </n-tooltip>
        </CrmEditableText>
      </div>
    </template>

    <template #headerActions>
      <n-button v-show="activeTab === 'design'" type="primary" :loading="loading" @click="handleSaveFormDesign">
        {{ t('common.save') }}
      </n-button>
    </template>

    <div v-show="activeTab === 'design'" class="h-full">
      <CrmFormDesign
        v-if="visible"
        ref="formDesignRef"
        v-model:form-config="formConfig"
        v-model:field-list="fieldList"
        :form-key="formKey"
      />
    </div>
    <MemberPermissionTab v-show="activeTab === 'memberPermission'" v-if="visible" :source-id="currentSourceId" />
  </CrmProcessDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, NTooltip, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmEditableText from '@/components/business/crm-editable-text/index.vue';
  import {
    createDefaultFormConfig,
    useFormDesignConfig,
  } from '@/components/business/crm-form-design-drawer/useFormDesignConfig';
  import CrmProcessDrawer from '@/components/business/crm-process-drawer/index.vue';
  import MemberPermissionTab from './memberPermissionTab.vue';

  import { addCustomForm, getCustomFormDetail, updateCustomForm } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  const CrmFormDesign = defineAsyncComponent(() => import('@/components/business/crm-form-design/index.vue'));

  const props = defineProps<{
    sourceId?: string;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const activeTab = ref<'design' | 'memberPermission'>('design');
  const tabList = [
    {
      name: 'design',
      tab: t('customForm.formDesign'),
    },
    {
      name: 'memberPermission',
      tab: t('customForm.formMember'),
    },
  ];

  const titleSaving = ref(false);
  const currentSourceId = ref('');
  const customFormName = ref('');
  const customFormEnable = ref(true);

  const formKey = ref(FormDesignKeyEnum.CUSTOM_FORM);
  const { loading, fieldList, formConfig, formDesignRef, unsaved, checkRepeat, buildSavePayload, setFormConfigDetail } =
    useFormDesignConfig({ formKey });

  function showUnsavedLeaveTip() {
    openModal({
      type: 'warning',
      title: t('common.unSaveLeaveTitle'),
      content: t('common.editUnsavedLeave'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        visible.value = false;
      },
    });
  }

  function handleBack() {
    if (loading.value || titleSaving.value) {
      return;
    }

    if (unsaved.value) {
      showUnsavedLeaveTip();
      return;
    }

    visible.value = false;
  }

  function buildCustomFormSaveRequest(name = customFormName.value) {
    const { fields, formProp } = buildSavePayload();
    return {
      id: currentSourceId.value || undefined,
      name,
      enable: customFormEnable.value,
      fields,
      formProp,
    };
  }

  async function handleSaveFormDesign() {
    if (!checkRepeat()) {
      activeTab.value = 'design';
      return;
    }

    try {
      loading.value = true;
      const params = buildCustomFormSaveRequest();
      const result = params.id ? await updateCustomForm(params) : await addCustomForm(params);
      currentSourceId.value = result.id;
      unsaved.value = false;
      Message.success(t('common.saveSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function handleEditTitle(value: string, done?: () => void) {
    const name = value.trim();
    if (name === customFormName.value) {
      done?.();
      return;
    }

    try {
      titleSaving.value = true;
      await updateCustomForm(buildCustomFormSaveRequest(name));
      customFormName.value = name;
      Message.success(t('common.saveSuccess'));
      done?.();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      titleSaving.value = false;
    }
  }

  async function initCustomFormConfig() {
    currentSourceId.value = props.sourceId || '';
    if (!currentSourceId.value) {
      customFormName.value = '';
      customFormEnable.value = true;
      setFormConfigDetail({
        fields: [],
        formProp: createDefaultFormConfig(t),
      });
      return;
    }

    try {
      loading.value = true;
      const detail = await getCustomFormDetail(currentSourceId.value);
      customFormName.value = detail.name;
      customFormEnable.value = detail.enable;
      setFormConfigDetail(detail);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  watch(
    () => visible.value,
    (value) => {
      if (!value) {
        activeTab.value = 'design';
        return;
      }

      activeTab.value = 'design';
      initCustomFormConfig();
    },
    {
      immediate: true,
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
