<template>
  <CrmProcessDrawer
    v-model:visible="visible"
    v-model:active-tab="activeTab"
    width="100%"
    :loading="loading"
    :title="customFormName"
    :tab-list="tabList"
    :readonly="activeTab === 'memberPermission'"
    :before-change-tab="handleBeforeChangeTab"
    @cancel="handleBack"
  >
    <template #title>
      <div class="process-name-header flex max-w-full flex-1 overflow-hidden">
        <CrmEditableText
          :value="customFormName"
          :permission="[]"
          click-to-edit
          :emptyTextTip="t('common.notNull', { value: t('customForm.name') })"
          @input="customFormNameDraft = $event"
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
      <n-tooltip v-if="activeTab === 'design'" trigger="hover" :disabled="customFormEnable">
        <template #trigger>
          <span>
            <n-button type="primary" :loading="loading" :disabled="!customFormEnable" @click="handleSaveFormDesign">
              {{ t('common.save') }}
            </n-button>
          </span>
        </template>
        {{ t('customForm.formDisabled') }}
      </n-tooltip>
    </template>

    <div v-show="activeTab === 'design'" class="h-full">
      <CrmFormDesign
        v-if="visible"
        ref="formDesignRef"
        v-model:form-config="formConfig"
        v-model:field-list="fieldList"
        class="custom-form-design"
        :form-key="formKey"
      />
    </div>
    <MemberPermissionTab
      v-if="visible && activeTab === 'memberPermission'"
      :source-id="currentSourceId"
      :creator="customFormCreator"
    />
  </CrmProcessDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, NTooltip, useMessage } from 'naive-ui';

  import { FieldRuleEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getGenerateId } from '@lib/shared/method';

  import CrmEditableText from '@/components/business/crm-editable-text/index.vue';
  import {
    dividerDefaultFieldConfig,
    inputDefaultFieldConfig,
    memberDefaultFieldConfig,
  } from '@/components/business/crm-form-create/config';
  import type { FormCreateField } from '@/components/business/crm-form-create/types';
  import {
    createDefaultFormConfig,
    useFormDesignConfig,
  } from '@/components/business/crm-form-design-drawer/useFormDesignConfig';
  import CrmProcessDrawer from '@/components/business/crm-process-drawer/index.vue';
  import MemberPermissionTab from './memberPermissionTab.vue';

  import { addCustomForm, getCustomFormDetail, updateCustomForm } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import { useUserStore } from '@/store';

  const CrmFormDesign = defineAsyncComponent(() => import('@/components/business/crm-form-design/index.vue'));

  const props = defineProps<{
    sourceId?: string;
    defaultTab?: 'design' | 'memberPermission';
  }>();

  const emit = defineEmits<{
    (e: 'saved', id: string): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();
  const userStore = useUserStore();

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
  provide('customFormSourceId', readonly(currentSourceId));
  const customFormCreator = ref({
    id: '',
    name: '',
  });

  const formKey = ref(FormDesignKeyEnum.CUSTOM_FORM);
  const { loading, fieldList, formConfig, unsaved, formDesignRef, checkRepeat, buildSavePayload, setFormConfigDetail } =
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

  function disableSavedDataSourceType() {
    const addDisabledProp = (field: FormCreateField) => {
      if (![FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(field.type)) {
        return;
      }

      field.disabledProps = Array.from(new Set([...(field.disabledProps || []), 'dataSourceType']));
    };

    fieldList.value.forEach((field) => {
      addDisabledProp(field);
      field.subFields?.forEach(addDisabledProp);
    });
  }

  const customFormNameDraft = ref('');

  async function handleSaveFormDesign() {
    if (!customFormNameDraft.value.trim().length) {
      return false;
    }

    if (!checkRepeat()) {
      activeTab.value = 'design';
      return false;
    }

    try {
      loading.value = true;
      const params = buildCustomFormSaveRequest();
      const result = params.id ? await updateCustomForm(params) : await addCustomForm(params);
      if (result?.id) {
        currentSourceId.value = result.id;
      }
      disableSavedDataSourceType();
      unsaved.value = false;
      emit('saved', currentSourceId.value);
      Message.success(t('common.saveSuccess'));
      return true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      return false;
    } finally {
      loading.value = false;
    }
  }

  function handleBeforeChangeTab(newVal: string | number, oldVal: string | number | null) {
    if (newVal === 'memberPermission' && !currentSourceId.value) {
      Message.warning(t('customForm.saveFormDesignFirst'));
      return false;
    }

    if (oldVal !== 'design' || newVal === oldVal || !unsaved.value) {
      return true;
    }

    return new Promise<boolean>((resolve) => {
      let resolved = false;
      const resolveOnce = (value: boolean) => {
        if (resolved) {
          return;
        }
        resolved = true;
        resolve(value);
      };

      openModal({
        type: 'warning',
        title: t('customForm.formUnsavedTitle'),
        content: t('customForm.formUnsavedSwitchTip'),
        negativeText: t('common.cancel'),
        positiveText: t('common.save'),
        onPositiveClick: async () => {
          const saved = await handleSaveFormDesign();
          resolveOnce(saved);
        },
        onNegativeClick: () => {
          resolveOnce(true);
        },
      });
    });
  }

  async function handleEditTitle(value: string, done?: () => void) {
    const name = value.trim();
    if (name === customFormName.value) {
      done?.();
      return;
    }

    try {
      titleSaving.value = true;
      if (!currentSourceId.value) {
        customFormName.value = name;
        customFormNameDraft.value = name;
        unsaved.value = true;
        done?.();
        return;
      }

      await updateCustomForm(buildCustomFormSaveRequest(name));
      customFormName.value = name;
      customFormNameDraft.value = name;
      emit('saved', currentSourceId.value);
      Message.success(t('common.saveSuccess'));
      done?.();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      titleSaving.value = false;
    }
  }

  function createDefaultCustomFormFields(): FormCreateField[] {
    const createDivider = (name: string): FormCreateField => ({
      ...dividerDefaultFieldConfig,
      id: getGenerateId(),
      name,
    });

    const createInput = (name: string, options?: Partial<FormCreateField>): FormCreateField => ({
      ...inputDefaultFieldConfig,
      id: getGenerateId(),
      name,
      ...options,
    });

    return [
      createDivider(t('customForm.basicInfo')),
      createInput(t('common.name'), {
        businessKey: 'name',
        rules: [{ key: FieldRuleEnum.REQUIRED }],
        disabledProps: ['readable', 'mobile', 'rules.required'],
        internalKey: 'customFormDataName',
      }),
      createInput(t('customForm.customField', { index: 1 })),
      createInput(t('customForm.customField', { index: 2 })),
      createDivider(t('customForm.ownerInfo')),
      {
        ...memberDefaultFieldConfig,
        id: getGenerateId(),
        name: t('common.head'),
        businessKey: 'owner',
        hasCurrentUser: true,
        disabledProps: ['readable', 'mobile', 'rules.required'],
        rules: [{ key: FieldRuleEnum.REQUIRED }],
        initialOptions: userStore.userInfo.id
          ? [
              {
                id: userStore.userInfo.id,
                name: userStore.userInfo.name,
              },
            ]
          : [],
        internalKey: 'customFormDataNOwner',
      },
    ];
  }

  async function initCustomFormConfig() {
    currentSourceId.value = props.sourceId || '';
    if (!currentSourceId.value) {
      customFormName.value = t('customForm.unnamedForm');
      customFormNameDraft.value = customFormName.value;
      customFormEnable.value = true;
      customFormCreator.value = {
        id: userStore.userInfo.id || '',
        name: userStore.userInfo.name || '',
      };
      setFormConfigDetail({
        fields: createDefaultCustomFormFields(),
        formProp: createDefaultFormConfig(t),
      });
      return;
    }

    try {
      loading.value = true;
      const detail = await getCustomFormDetail(currentSourceId.value);
      customFormName.value = detail.name;
      customFormNameDraft.value = detail.name;
      customFormEnable.value = detail.enable;
      customFormCreator.value = detail.creator;
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

      activeTab.value = props.defaultTab || 'design';
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
  .custom-form-design {
    .crm-form-design--composition {
      > .n-scrollbar {
        .n-scrollbar-content {
          width: 100% !important;
        }
      }
    }
  }
</style>
