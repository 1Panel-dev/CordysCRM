import { nextTick, type Ref, ref, watch } from 'vue';
import { useMessage } from 'naive-ui';

import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { safeFractionConvert } from '@lib/shared/method';
import type { FormConfig } from '@lib/shared/models/system/module';

import type CrmFormDesign from '@/components/business/crm-form-design/index.vue';

import { getFormDesignConfig } from '@/api/modules';

import type { FormCreateField } from '../crm-form-create/types';

export interface FormDesignSavePayload {
  formKey: FormDesignKeyEnum;
  formProp: FormConfig;
  fields: FormCreateField[];
}

export function createDefaultFormConfig(t: ReturnType<typeof useI18n>['t']): FormConfig {
  return {
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
    viewSize: 'small',
  };
}

function resolveFormDesignConfigKey(formKey: FormDesignKeyEnum) {
  // 跟进记录和计划 key 特殊处理，因为各模块 key 不一致但是表单配置一致
  if (formKey.includes('record')) {
    return 'record' as FormDesignKeyEnum;
  }

  if (formKey.includes('plan')) {
    return 'plan' as FormDesignKeyEnum;
  }

  return formKey;
}

export function useFormDesignConfig(options: { formKey: Ref<FormDesignKeyEnum> }) {
  const { t } = useI18n();
  const message = useMessage();

  const loading = ref(false);
  const fieldList = ref<FormCreateField[]>([]);
  const formConfig = ref<FormConfig>(createDefaultFormConfig(t));
  const formDesignRef = ref<InstanceType<typeof CrmFormDesign>>();
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

  function checkRepeat() {
    const fieldNameSet = new Set<string>();
    for (let i = 0; i < fieldList.value.length; i++) {
      const field = fieldList.value[i];
      if (fieldNameSet.has(field.name)) {
        message.error(t('crmFormDesign.repeatFieldName'));
        formDesignRef.value?.setActiveField(field);
        return false;
      }

      if ([FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(field.type) && field.subFields) {
        const subFieldNameSet = new Set<string>();
        for (let j = 0; j < field.subFields.length; j++) {
          const subField = field.subFields[j];
          if (subFieldNameSet.has(subField.name)) {
            message.error(t('crmFormDesign.repeatFieldName'));
            formDesignRef.value?.setActiveField(field);
            return false;
          }

          subFieldNameSet.add(subField.name);
        }
      }

      fieldNameSet.add(field.name);
    }

    const optionsFields = fieldList.value.filter((field) =>
      [FieldTypeEnum.RADIO, FieldTypeEnum.SELECT, FieldTypeEnum.CHECKBOX, FieldTypeEnum.SELECT_MULTIPLE].includes(
        field.type
      )
    );
    for (let i = 0; i < optionsFields.length; i++) {
      const field = optionsFields[i];
      const optionList = field.options || [];
      const optionLabelSet = new Set<string>();
      for (let j = 0; j < optionList.length; j++) {
        const option = optionList[j];
        if (optionLabelSet.has(option.label)) {
          message.error(t('crmFormDesign.repeatOptionName'));
          formDesignRef.value?.setActiveField(field);
          return false;
        }

        optionLabelSet.add(option.label);
      }
    }

    return true;
  }

  function buildSavePayload(): FormDesignSavePayload {
    return {
      formKey: options.formKey.value,
      formProp: formConfig.value,
      fields: fieldList.value.map((field) => {
        if (field.type === FieldTypeEnum.SUB_PRICE || field.type === FieldTypeEnum.SUB_PRODUCT) {
          field.subFields = field.subFields?.map(
            (subField) =>
              ({
                ...subField,
                id: subField.id,
              } as FormCreateField)
          );
        }

        return {
          ...field,
          id: field.id,
          defaultValue:
            [FieldTypeEnum.SELECT, FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.MEMBER].includes(
              field.type
            ) && Array.isArray(field.defaultValue)
              ? field.defaultValue[0] || ''
              : field.defaultValue,
        };
      }),
    };
  }

  function setFormConfigDetail(res: { fields: FormCreateField[]; formProp: FormConfig }) {
    fieldList.value = res.fields.map((item) => {
      const newSubFields = item.subFields?.map((field) => ({
        ...field,
        description: '',
        id: field.id,
      }));

      return {
        ...item,
        id: item.id,
        internalKey: item.internalKey,
        type: item.type,
        name: t(item.name),
        placeholder: t(item.placeholder || ''),
        fieldWidth: safeFractionConvert(item.fieldWidth),
        subFields: newSubFields,
        defaultValue:
          [FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.MEMBER].includes(item.type) &&
          typeof item.defaultValue === 'string'
            ? [item.defaultValue]
            : item.defaultValue,
        // 处理数据源显示字段 id
        sumColumns:
          item.sumColumns?.map((sumColumn) => {
            const newSubField = newSubFields?.find((field) => field.id.includes(sumColumn));
            return newSubField ? newSubField.id : sumColumn;
          }) || [],
      };
    });
    formConfig.value = res.formProp;
    nextTick(() => {
      unsaved.value = false;
    });
  }

  async function initFormConfig() {
    try {
      loading.value = true;
      const res = await getFormDesignConfig(resolveFormDesignConfigKey(options.formKey.value));
      setFormConfigDetail(res);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  return {
    loading,
    fieldList,
    formConfig,
    formDesignRef,
    unsaved,
    checkRepeat,
    buildSavePayload,
    setFormConfigDetail,
    initFormConfig,
  };
}
