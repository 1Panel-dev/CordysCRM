import { showToast } from 'vant';

import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { getCityPath, safeFractionConvert } from '@lib/shared/method';
import type { CollaborationType, ModuleField } from '@lib/shared/models/customer';

import type { CrmDescriptionItem } from '@/components/pure/crm-description/index.vue';

import {
  createFormApi,
  getFormConfigApiMap,
  getFormDetailApiMap,
  updateFormApi,
} from '@cordys/web/src/components/business/crm-form-create/config';
import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

export interface FormCreateApiProps {
  sourceId?: string;
  formKey: FormDesignKeyEnum;
  needInitDetail?: boolean;
  initialSourceName?: string; // 特殊字段初始化需要的资源名称
  otherSaveParams?: Record<string, any>;
}

export default function useFormCreateApi(props: FormCreateApiProps) {
  const { t } = useI18n();

  const sourceName = ref(props.initialSourceName); // 资源名称
  const collaborationType = ref<CollaborationType>(); // 协作类型-客户独有
  const specialInitialOptions = ref<Record<string, any>[]>([]); // 特殊字段的初始化选项列表
  const descriptions = ref<CrmDescriptionItem[]>([]); // 表单详情描述列表
  const fieldList = ref<FormCreateField[]>([]); // 表单字段列表
  const loading = ref(false);
  const unsaved = ref(false);
  const formDetail = ref<Record<string, any>>({});

  async function initFormDescription() {
    try {
      const asyncApi = getFormDetailApiMap[props.formKey];
      if (!asyncApi || !props.sourceId) return;
      const form = await asyncApi(props.sourceId);
      descriptions.value = [];
      collaborationType.value = form.collaborationType;
      fieldList.value.forEach((item) => {
        if (item.businessKey) {
          const options = form.optionMap?.[item.businessKey];
          // 业务标准字段读取最外层，读取form[item.businessKey]取到 id 值，然后去 options 里取 name
          const name = options?.find((e) => e.id === form[item.businessKey as string])?.name;
          descriptions.value.push({
            label: item.name,
            value: name || form[item.businessKey],
          });
          if (item.businessKey === 'name') {
            sourceName.value = name || form[item.businessKey];
          }
        } else {
          const options = form.optionMap?.[item.id];
          // 其他的字段读取moduleFields
          const field = form.moduleFields?.find((moduleField: ModuleField) => moduleField.fieldId === item.id);
          if (item.type === FieldTypeEnum.DIVIDER) {
            descriptions.value.push({
              label: item.name,
              value: field?.fieldValue || [],
              slotName: 'divider',
              fieldInfo: item,
            });
          } else {
            let value = field?.fieldValue || '';
            if (field && options) {
              // 若字段值是选项值，则取选项值的name
              if (Array.isArray(field.fieldValue)) {
                value = options.filter((e) => field.fieldValue.includes(e.id)).map((e) => e.name);
              } else {
                value = options.find((e) => e.id === field.fieldValue)?.name;
              }
            } else if (item.type === FieldTypeEnum.LOCATION) {
              const address = (field?.fieldValue as string)?.split('-');
              value = address ? `${getCityPath(address[0])}-${address[1]}` : '-';
            }
            descriptions.value.push({
              label: item.name,
              value,
            });
          }
        }
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function initFieldValue(field: FormCreateField, value: string | number | (string | number)[]) {
    if (field.type === FieldTypeEnum.DATA_SOURCE && typeof value === 'string') {
      return [value];
    }
    return value;
  }

  async function initFormDetail() {
    try {
      const asyncApi = getFormDetailApiMap[props.formKey];
      if (!asyncApi || !props.sourceId) return;
      const res = await asyncApi(props.sourceId);
      collaborationType.value = res.collaborationType;
      sourceName.value = res.name;
      fieldList.value.forEach((item) => {
        if (item.businessKey) {
          const options = res.optionMap?.[item.businessKey];
          if ([FieldTypeEnum.MEMBER, FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DATA_SOURCE].includes(item.type)) {
            // 处理成员和数据源类型的字段
            item.initialOptions = options;
          }
          // 业务标准字段读取最外层
          formDetail.value[item.id] = initFieldValue(item, res[item.businessKey]);
        } else {
          const options = res.optionMap?.[item.id];
          if ([FieldTypeEnum.MEMBER, FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DATA_SOURCE].includes(item.type)) {
            // 处理成员和数据源类型的字段
            item.initialOptions = options;
          }
          // 其他的字段读取moduleFields
          const field = res.moduleFields?.find((moduleField: ModuleField) => moduleField.fieldId === item.id);
          if (field) {
            formDetail.value[item.id] = initFieldValue(item, field.fieldValue);
          }
        }
        if (item.type === FieldTypeEnum.DATE_TIME) {
          // 处理时间类型的字段
          formDetail.value[item.id] = formDetail.value[item.id] ? Number(formDetail.value[item.id]) : '';
        }
      });
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

  /**
   * 处理业务表单的特殊字段在特定场景下的初始化默认值
   */
  function specialFormFieldInit(field: FormCreateField) {
    if ([FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER, FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER].includes(props.formKey)) {
      // 客户跟进计划和记录，需要赋予类型字段默认为客户，客户字段默认值为当前客户
      if (field.businessKey === 'type') {
        return {
          defaultValue: 'CUSTOMER',
        };
      }
      if (field.businessKey === 'customerId') {
        specialInitialOptions.value = [
          {
            id: props.sourceId,
            name: sourceName.value || props.initialSourceName,
          },
        ];
        return {
          defaultValue: initFieldValue(field, props.sourceId || ''),
          initialOptions: specialInitialOptions.value,
        };
      }
    }
    if ([FormDesignKeyEnum.FOLLOW_PLAN_CLUE, FormDesignKeyEnum.FOLLOW_RECORD_CLUE].includes(props.formKey)) {
      // 线索跟进计划和记录，需要赋予类型字段默认为客户，线索字段默认值为当前线索
      if (field.businessKey === 'type') {
        return {
          defaultValue: 'CLUE',
        };
      }
      if (field.businessKey === 'clueId') {
        specialInitialOptions.value = [
          {
            id: props.sourceId,
            name: sourceName.value || props.initialSourceName,
          },
        ];
        return {
          defaultValue: initFieldValue(field, props.sourceId || ''),
          initialOptions: specialInitialOptions.value,
        };
      }
    }
    if (props.formKey === FormDesignKeyEnum.CONTACT) {
      // 联系人表单，赋予客户字段默认值为当前客户
      if (field.businessKey === 'customerId') {
        specialInitialOptions.value = [
          {
            id: props.sourceId,
            name: sourceName.value || props.initialSourceName,
          },
        ];
        return {
          defaultValue: initFieldValue(field, props.sourceId || ''),
          initialOptions: specialInitialOptions.value,
        };
      }
    }
    if (field.type === FieldTypeEnum.DATA_SOURCE) {
      // 数据源类型的字段，默认值需要转为数组
      return {
        defaultValue: typeof field.defaultValue === 'string' ? [field.defaultValue] : field.defaultValue,
      };
    }
    return {
      defaultValue: field.defaultValue,
    };
  }

  async function initFormConfig() {
    try {
      loading.value = true;
      const res = await getFormConfigApiMap[props.formKey]();
      fieldList.value = res.fields.map((item) => {
        const { defaultValue, initialOptions } = specialFormFieldInit(item);
        return {
          ...item,
          defaultValue,
          initialOptions,
          fieldWidth: safeFractionConvert(item.fieldWidth),
        };
      });
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

  async function saveForm(form: Record<string, any>, isContinue: boolean, callback?: (_isContinue: boolean) => void) {
    try {
      loading.value = true;
      const params: Record<string, any> = {
        ...props.otherSaveParams?.value,
        moduleFields: [],
        id: props.sourceId,
      };
      fieldList.value.forEach((item) => {
        if (item.businessKey) {
          // 存在业务字段，则按照业务字段的key存储
          params[item.businessKey] = form[item.id];
        } else {
          params.moduleFields.push({
            fieldId: item.id,
            fieldValue: form[item.id],
          });
        }
      });
      if (props.sourceId && props.needInitDetail) {
        await updateFormApi[props.formKey](params);
        showToast(t('common.updateSuccess'));
      } else {
        await createFormApi[props.formKey](params);
        if (props.formKey === FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER) {
          showToast(t('clue.transferredToCustomer'));
        } else if (props.formKey === FormDesignKeyEnum.CLUE_TRANSITION_BUSINESS) {
          showToast(t('clue.transferredToOpportunity'));
        } else {
          showToast(t('common.createSuccess'));
        }
      }
      if (callback) {
        callback(isContinue);
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  const formCreateTitle = computed(() => {
    if (props.formKey === FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER) {
      return t('common.convertToCustomer');
    }
    if (props.formKey === FormDesignKeyEnum.CLUE_TRANSITION_BUSINESS) {
      return t('common.convertToOpportunity');
    }
    const prefix = props.sourceId && props.needInitDetail ? t('common.edit') : t('common.create');
    return `${prefix}${t(`common.${props.formKey}`)}`;
  });

  return {
    descriptions,
    fieldList,
    loading,
    unsaved,
    formDetail,
    formCreateTitle,
    collaborationType,
    sourceName,
    initFormDescription,
    initFormConfig,
    initFormDetail,
    saveForm,
  };
}
