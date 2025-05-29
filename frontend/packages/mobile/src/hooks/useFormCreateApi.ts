import { showSuccessToast } from 'vant';

import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { formatNumberValue, formatTimeValue, getCityPath, safeFractionConvert, sleep } from '@lib/shared/method';
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
  const detail = ref<Record<string, any>>({}); // 详情

  async function initFormDescription() {
    try {
      const asyncApi = getFormDetailApiMap[props.formKey];
      if (!asyncApi || !props.sourceId) return;
      const form = await asyncApi(props.sourceId);
      descriptions.value = [];
      detail.value = form;
      collaborationType.value = form.collaborationType;
      fieldList.value.forEach((item) => {
        if (item.businessKey) {
          const options = form.optionMap?.[item.businessKey];
          // 业务标准字段读取最外层，读取form[item.businessKey]取到 id 值，然后去 options 里取 name
          let name: string | string[] = '';
          const value = form[item.businessKey];
          // 若字段值是选项值，则取选项值的name
          if (options) {
            if (Array.isArray(value)) {
              name = value.map((e) => {
                const option = options.find((opt) => opt.id === e);
                if (option) {
                  return option.name || t('common.optionNotExist');
                }
                return t('common.optionNotExist');
              });
            } else {
              name = options.find((e) => e.id === value)?.name;
            }
          }
          if (item.type === FieldTypeEnum.DATE_TIME) {
            descriptions.value.push({
              label: item.name,
              value: formatTimeValue(name || form[item.businessKey], item.dateType),
            });
          } else if (item.type === FieldTypeEnum.INPUT_NUMBER) {
            descriptions.value.push({
              label: item.name,
              value: formatNumberValue(name || form[item.businessKey], item),
            });
          } else {
            descriptions.value.push({
              label: item.name,
              value: name || form[item.businessKey],
            });
          }
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
              isTitle: true,
              fieldInfo: item,
            });
          } else if (item.type === FieldTypeEnum.PICTURE) {
            descriptions.value.push({
              label: item.name,
              value: field?.fieldValue || [],
              isImage: true,
            });
          } else {
            let value = field?.fieldValue || '';
            if (field && options) {
              // 若字段值是选项值，则取选项值的name
              if (Array.isArray(field.fieldValue)) {
                value = field.fieldValue.map((e) => {
                  const option = options.find((opt) => opt.id === e);
                  if (option) {
                    return option.name || t('common.optionNotExist');
                  }
                  return t('common.optionNotExist');
                });
              } else {
                value = options.find((e) => e.id === field.fieldValue)?.name;
              }
            } else if (item.type === FieldTypeEnum.LOCATION) {
              const address = (field?.fieldValue as string)?.split('-');
              value = address ? `${getCityPath(address[0])}-${address[1]}` : '-';
            } else if (item.type === FieldTypeEnum.INPUT_NUMBER) {
              value = formatNumberValue(field?.fieldValue as string, item);
            } else if (item.type === FieldTypeEnum.DATE_TIME) {
              value = formatTimeValue(field?.fieldValue as string, item.dateType);
            }
            descriptions.value.push({
              label: item.name,
              isTag: [FieldTypeEnum.INPUT_MULTIPLE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(item.type),
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
    if (
      [FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(field.type) &&
      typeof value === 'string'
    ) {
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
      fieldList.value = fieldList.value.map((item) => {
        if (item.businessKey) {
          // 业务标准字段读取最外层
          formDetail.value[item.id] = initFieldValue(item, res[item.businessKey]);
          const options = res.optionMap?.[item.businessKey];
          if (
            [
              FieldTypeEnum.MEMBER,
              FieldTypeEnum.MEMBER_MULTIPLE,
              FieldTypeEnum.DEPARTMENT,
              FieldTypeEnum.DEPARTMENT_MULTIPLE,
              FieldTypeEnum.DATA_SOURCE,
              FieldTypeEnum.DATA_SOURCE_MULTIPLE,
            ].includes(item.type)
          ) {
            // 处理成员和数据源类型的字段
            item.initialOptions = options
              ?.filter((e) => formDetail.value[item.id]?.includes(e.id))
              .map((e) => ({
                ...e,
                name: e.name || t('common.optionNotExist'),
              }));
          }
        } else {
          // 其他的字段读取moduleFields
          const field = res.moduleFields?.find((moduleField: ModuleField) => moduleField.fieldId === item.id);
          if (field) {
            formDetail.value[item.id] = initFieldValue(item, field.fieldValue);
          }
          const options = res.optionMap?.[item.id];
          if (
            [
              FieldTypeEnum.MEMBER,
              FieldTypeEnum.MEMBER_MULTIPLE,
              FieldTypeEnum.DEPARTMENT,
              FieldTypeEnum.DEPARTMENT_MULTIPLE,
              FieldTypeEnum.DATA_SOURCE,
              FieldTypeEnum.DATA_SOURCE_MULTIPLE,
            ].includes(item.type)
          ) {
            // 处理成员和数据源类型的字段
            item.initialOptions = options
              ?.filter((e) => formDetail.value[item.id]?.includes(e.id))
              .map((e) => ({
                ...e,
                name: e.name || t('common.optionNotExist'),
              }));
          }
        }
        if (item.type === FieldTypeEnum.DATE_TIME) {
          // 处理时间类型的字段
          formDetail.value[item.id] = formDetail.value[item.id] ? Number(formDetail.value[item.id]) : '';
        }
        return item;
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
      [FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER, FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER].includes(props.formKey) &&
      props.sourceId
    ) {
      // 客户跟进计划和记录，需要赋予类型字段默认为客户，客户字段默认值为当前客户
      if (field.businessKey === 'type') {
        return {
          defaultValue: 'CUSTOMER',
          initialOptions: field.initialOptions,
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
    if (
      [FormDesignKeyEnum.FOLLOW_PLAN_CLUE, FormDesignKeyEnum.FOLLOW_RECORD_CLUE].includes(props.formKey) &&
      props.sourceId
    ) {
      // 线索跟进计划和记录，需要赋予类型字段默认为客户，线索字段默认值为当前线索
      if (field.businessKey === 'type') {
        return {
          defaultValue: 'CLUE',
          initialOptions: field.initialOptions,
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

    if (
      [FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS, FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS].includes(props.formKey) &&
      props.sourceId
    ) {
      // 商机跟进计划和记录，需要赋予默认跟进类型、商机、商机对应客户
      if (field.businessKey === 'type') {
        return {
          defaultValue: 'CUSTOMER',
          initialOptions: field.initialOptions,
        };
      }

      const defaultParsedSource = props.initialSourceName ? JSON.parse(props.initialSourceName) : {};
      if (Object.keys(defaultParsedSource).length) {
        if (field.businessKey === 'opportunityId') {
          specialInitialOptions.value = [
            {
              id: props.sourceId,
              name: defaultParsedSource?.name ?? '',
            },
          ];
          return {
            defaultValue: initFieldValue(field, props.sourceId || ''),
            initialOptions: specialInitialOptions.value,
          };
        }

        if (field.businessKey === 'customerId') {
          const defaultCustomerId = defaultParsedSource?.[field.businessKey] ?? '';
          specialInitialOptions.value = [
            {
              id: defaultCustomerId,
              name: defaultParsedSource?.customerName ?? '',
            },
          ];

          return {
            defaultValue: initFieldValue(field, defaultCustomerId || ''),
            initialOptions: specialInitialOptions.value,
          };
        }
      }
    }
    if (props.formKey === FormDesignKeyEnum.CUSTOMER_CONTACT && props.sourceId) {
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
    if ([FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER].includes(props.formKey)) {
      // 线索转客户带入名称
      if (field.businessKey === 'name') {
        return {
          defaultValue: props.initialSourceName,
          initialOptions: field.initialOptions,
        };
      }
    }
    if ([FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(field.type)) {
      // 数据源类型的字段，默认值需要转为数组
      return {
        defaultValue: typeof field.defaultValue === 'string' ? [field.defaultValue] : field.defaultValue,
        initialOptions: field.initialOptions,
      };
    }
    return {
      defaultValue: field.defaultValue,
      initialOptions: field.initialOptions,
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

  async function saveForm(form: Record<string, any>, callback?: () => void) {
    try {
      loading.value = true;
      const params: Record<string, any> = {
        ...props.otherSaveParams,
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
        showSuccessToast(t('common.updateSuccess'));
      } else {
        await createFormApi[props.formKey](params);
        if (props.formKey === FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER) {
          showSuccessToast(t('clue.transferredToCustomer'));
        } else {
          showSuccessToast(t('common.createSuccess'));
        }
      }
      if (callback) {
        await sleep(300);
        callback();
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
    detail,
  };
}
