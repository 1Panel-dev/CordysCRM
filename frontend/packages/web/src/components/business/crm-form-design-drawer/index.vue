<template>
  <CrmDrawer
    v-model:show="visible"
    width="100%"
    :footer="false"
    :closable="false"
    :close-on-esc="false"
    :loading="loading"
    header-class="crm-form-drawer-header"
    body-content-class="!p-0"
  >
    <template #header>
      <div class="flex items-center justify-between">
        <div class="flex items-center">
          <n-button text class="mr-[4px] w-[32px]" @click="handleBack">
            <n-icon size="16">
              <ChevronBackOutline />
            </n-icon>
          </n-button>
          <div class="text-[14px] font-normal"> {{ props.title }}</div>
          <!-- <n-input
            v-model:value="name"
            type="text"
            :placeholder="t('common.pleaseInput')"
            size="medium"
            clearable
            class="crm-form-drawer-title"
            autosize
            :status="name.trim() === '' ? 'error' : undefined"
            :maxlength="255"
          ></n-input> -->
        </div>
        <n-button type="primary" :loading="loading" @click="handleSave">{{ t('common.save') }}</n-button>
      </div>
    </template>
    <CrmFormDesign
      v-if="visible"
      ref="formDesignRef"
      v-model:form-config="formConfig"
      v-model:field-list="fieldList"
      :form-key="props.formKey"
    />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { NButton, NIcon, useMessage } from 'naive-ui';
  import { ChevronBackOutline } from '@vicons/ionicons5';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { safeFractionConvert } from '@lib/shared/method';
  import { FormConfig } from '@lib/shared/models/system/module';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';

  import { getFormDesignConfig, saveFormDesignConfig } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  import { FormCreateField } from '../crm-form-create/types';

  const CrmFormDesign = defineAsyncComponent(() => import('@/components/business/crm-form-design/index.vue'));

  const props = defineProps<{
    title: string;
    formKey: FormDesignKeyEnum;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const loading = ref(false);
  const fieldList = ref<FormCreateField[]>([]);
  const formConfig = ref<FormConfig>({
    layout: 1,
    labelPos: 'top',
    inputWidth: 'custom',
    optBtnContent: [
      {
        text: t('common.save'),
        enable: true,
      },
      {
        text: t('common.saveAndContinue'),
        enable: false,
      },
      {
        text: t('common.cancel'),
        enable: true,
      },
    ],
    optBtnPos: 'flex-row',
  });
  const unsaved = ref(false);

  watch(
    () => [fieldList.value, formConfig.value],
    () => {
      unsaved.value = true;
    },
    {
      deep: true,
    }
  );

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
    if (!loading.value) {
      if (unsaved.value) {
        showUnsavedLeaveTip();
      } else {
        visible.value = false;
      }
    }
  }

  const formDesignRef = ref<InstanceType<typeof CrmFormDesign>>();

  function checkRepeat() {
    const fieldNameSet = new Set<string>();
    for (let i = 0; i < fieldList.value.length; i++) {
      const field = fieldList.value[i];
      if (fieldNameSet.has(field.name) && !field.resourceFieldId) {
        Message.error(t('crmFormDesign.repeatFieldName'));
        formDesignRef.value?.setActiveField(field);
        return false;
      }
      if ([FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(field.type) && field.subFields) {
        for (let j = 0; j < field.subFields.length; j++) {
          const subField = field.subFields[j];
          if (fieldNameSet.has(subField.name) && !subField.resourceFieldId) {
            Message.error(t('crmFormDesign.repeatFieldName'));
            formDesignRef.value?.setActiveField(field);
            return false;
          }
          if (!subField.resourceFieldId) {
            fieldNameSet.add(subField.name);
          }
        }
      }
      if (!field.resourceFieldId) {
        fieldNameSet.add(field.name);
      }
    }
    const optionsFields = fieldList.value.filter((e) =>
      [FieldTypeEnum.RADIO, FieldTypeEnum.SELECT, FieldTypeEnum.CHECKBOX, FieldTypeEnum.SELECT_MULTIPLE].includes(
        e.type
      )
    );
    for (let i = 0; i < optionsFields.length; i++) {
      const field = optionsFields[i];
      if (field.options) {
        const optionLabelSet = new Set<string>();
        for (let j = 0; j < field.options.length; j++) {
          const option = field.options[j];
          if (optionLabelSet.has(option.label)) {
            Message.error(t('crmFormDesign.repeatOptionName'));
            formDesignRef.value?.setActiveField(field);
            return false;
          }
          optionLabelSet.add(option.label);
        }
      }
    }
    return true;
  }

  async function handleSave() {
    if (!checkRepeat()) {
      return;
    }
    try {
      loading.value = true;
      await saveFormDesignConfig({
        formKey: props.formKey,
        formProp: formConfig.value,
        fields: fieldList.value.map((e) => ({
          ...e,
          defaultValue:
            [FieldTypeEnum.SELECT, FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.MEMBER].includes(
              e.type
            ) && Array.isArray(e.defaultValue)
              ? e.defaultValue[0] || ''
              : e.defaultValue,
        })),
      });
      Message.success(t('common.saveSuccess'));
      visible.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function initFormConfig() {
    try {
      loading.value = true;
      let key = props.formKey;
      // 跟进记录和计划 key 特殊处理，因为各模块 key 不一致但是表单配置一致
      if (key.includes('record')) {
        key = 'record' as FormDesignKeyEnum;
      } else if (key.includes('plan')) {
        key = 'plan' as FormDesignKeyEnum;
      }
      const res = await getFormDesignConfig(key);
      fieldList.value = res.fields.map((item) => ({
        ...item,
        id: item.id,
        internalKey: item.internalKey,
        type: item.type,
        name: t(item.name),
        placeholder: t(item.placeholder || ''),
        fieldWidth: safeFractionConvert(item.fieldWidth),
        subFields: item.subFields?.map((e) => ({ ...e, description: item.resourceFieldId ? '' : item.description })),
        defaultValue:
          [FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.MEMBER].includes(item.type) &&
          typeof item.defaultValue === 'string'
            ? [item.defaultValue]
            : item.defaultValue,
      }));
      formConfig.value = res.formProp;
      nextTick(() => {
        unsaved.value = false;
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        initFormConfig();
      }
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less">
  .crm-form-drawer-header {
    padding: 12px 16px !important;
    .n-drawer-header__main {
      max-width: 100%;
      .crm-form-drawer-title {
        --n-border: none !important;
        --n-border-hover: none !important;
        --n-border-focus: none !important;
        --n-box-shadow-focus: none !important;

        min-width: 80px;
        border-bottom: 2px solid var(--text-n8);
      }
    }
  }
</style>
