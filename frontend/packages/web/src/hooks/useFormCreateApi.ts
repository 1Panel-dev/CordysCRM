import { useMessage } from 'naive-ui';

import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { getCityPath, safeFractionConvert } from '@lib/shared/method';
import type { CollaborationType, ModuleField } from '@lib/shared/models/customer';
import type { FormConfig } from '@lib/shared/models/system/module';

import type { Description } from '@/components/pure/crm-description/index.vue';
import {
  createFormApi,
  getFormConfigApiMap,
  getFormDetailApiMap,
  updateFormApi,
} from '@/components/business/crm-form-create/config';
import type { FormCreateField } from '@/components/business/crm-form-create/types';

export interface FormCreateApiProps {
  sourceId?: Ref<string | undefined>;
  formKey: Ref<FormDesignKeyEnum>;
  needInitDetail?: Ref<boolean>;
  initialSourceName?: Ref<string | undefined>; // 特殊字段初始化需要的资源名称
  otherSaveParams?: Ref<Record<string, any> | undefined>;
}

export default function useFormCreateApi(props: FormCreateApiProps) {
  const { t } = useI18n();
  const Message = useMessage();

  const sourceName = ref(props.initialSourceName?.value); // 资源名称
  const collaborationType = ref<CollaborationType>(); // 协作类型-客户独有
  const specialInitialOptions = ref<Record<string, any>[]>([]); // 特殊字段的初始化选项列表
  const descriptions = ref<Description[]>([]); // 表单详情描述列表
  const fieldList = ref<FormCreateField[]>([]); // 表单字段列表
  const loading = ref(false);
  const unsaved = ref(false);
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
  }); // 表单属性配置
  const formDetail = ref<Record<string, any>>({});

  async function initFormDescription() {
    try {
      const asyncApi = getFormDetailApiMap[props.formKey.value];
      if (!asyncApi || !props.sourceId?.value) return;
      const form = await asyncApi(props.sourceId?.value);
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
              value = address ? `${getCityPath(address[0])}${address[1] ? `-${address[1]}` : ''}` : '-';
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
      const asyncApi = getFormDetailApiMap[props.formKey.value];
      if (!asyncApi || !props.sourceId?.value) return;
      const res = await asyncApi(props.sourceId?.value);
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
          formDetail.value[item.id] = formDetail.value[item.id] ? Number(formDetail.value[item.id]) : null;
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
    if (
      [FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER, FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER].includes(
        props.formKey.value
      ) &&
      props.sourceId?.value
    ) {
      // 客户跟进计划和记录，需要赋予类型字段默认为客户，客户字段默认值为当前客户
      if (field.businessKey === 'type') {
        return {
          defaultValue: 'CUSTOMER',
        };
      }
      if (field.businessKey === 'customerId') {
        specialInitialOptions.value = [
          {
            id: props.sourceId?.value,
            name: sourceName.value || props.initialSourceName?.value,
          },
        ];
        return {
          defaultValue: initFieldValue(field, props.sourceId?.value || ''),
          initialOptions: specialInitialOptions.value,
        };
      }
    }
    if (
      [FormDesignKeyEnum.FOLLOW_PLAN_CLUE, FormDesignKeyEnum.FOLLOW_RECORD_CLUE].includes(props.formKey.value) &&
      props.sourceId?.value
    ) {
      // 线索跟进计划和记录，需要赋予类型字段默认为客户，线索字段默认值为当前线索
      if (field.businessKey === 'type') {
        return {
          defaultValue: 'CLUE',
        };
      }
      if (field.businessKey === 'clueId') {
        specialInitialOptions.value = [
          {
            id: props.sourceId?.value,
            name: sourceName.value || props.initialSourceName?.value,
          },
        ];
        return {
          defaultValue: initFieldValue(field, props.sourceId?.value || ''),
          initialOptions: specialInitialOptions.value,
        };
      }
    }
    if (props.formKey.value === FormDesignKeyEnum.CONTACT && props.sourceId?.value) {
      // 联系人表单，赋予客户字段默认值为当前客户
      if (field.businessKey === 'customerId') {
        specialInitialOptions.value = [
          {
            id: props.sourceId?.value,
            name: sourceName.value || props.initialSourceName?.value,
          },
        ];
        return {
          defaultValue: initFieldValue(field, props.sourceId?.value || ''),
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
      const res = await getFormConfigApiMap[props.formKey.value]();
      fieldList.value = res.fields.map((item) => {
        const { defaultValue, initialOptions } = specialFormFieldInit(item);
        return {
          ...item,
          defaultValue,
          initialOptions,
          fieldWidth: safeFractionConvert(item.fieldWidth),
        };
      });
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

  function getNormalFieldValue(item: FormCreateField, value: any) {
    if (item.type === FieldTypeEnum.DATA_SOURCE && !value) {
      return item.multiple ? [] : '';
    }
    if (item.type === FieldTypeEnum.MULTIPLE_INPUT && !value) {
      return [];
    }
    if (item.multiple && !value) {
      return [];
    }
    return value;
  }

  async function saveForm(form: Record<string, any>, isContinue: boolean, callback?: (_isContinue: boolean) => void) {
    try {
      loading.value = true;
      const params: Record<string, any> = {
        ...props.otherSaveParams?.value,
        moduleFields: [],
        id: props.sourceId?.value,
      };
      fieldList.value.forEach((item) => {
        if (item.businessKey) {
          // 存在业务字段，则按照业务字段的key存储
          params[item.businessKey] = form[item.id];
        } else {
          params.moduleFields.push({
            fieldId: item.id,
            fieldValue: getNormalFieldValue(item, form[item.id]),
          });
        }
      });
      if (props.sourceId?.value && props.needInitDetail?.value) {
        await updateFormApi[props.formKey.value](params);
        Message.success(t('common.updateSuccess'));
      } else {
        await createFormApi[props.formKey.value](params);
        if (props.formKey.value === FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER) {
          Message.success(t('clue.transferredToCustomer'));
        } else if (props.formKey.value === FormDesignKeyEnum.CLUE_TRANSITION_BUSINESS) {
          Message.success(t('clue.transferredToOpportunity'));
        } else {
          Message.success(t('common.createSuccess'));
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
    if (props.formKey.value === FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER) {
      return t('clue.convertToCustomer');
    }
    if (props.formKey.value === FormDesignKeyEnum.CLUE_TRANSITION_BUSINESS) {
      return t('clue.convertToOpportunity');
    }
    const prefix = props.sourceId?.value && props.needInitDetail?.value ? t('common.edit') : t('common.add');
    return `${prefix}${t(`crmFormCreate.drawer.${props.formKey.value}`)}`;
  });

  return {
    descriptions,
    fieldList,
    loading,
    unsaved,
    formConfig,
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
