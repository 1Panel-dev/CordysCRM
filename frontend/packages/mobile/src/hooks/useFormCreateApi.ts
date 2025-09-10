import { showSuccessToast } from 'vant';
import { cloneDeep } from 'lodash-es';
import dayjs from 'dayjs';

import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { formatNumberValue, formatTimeValue, getCityPath, safeFractionConvert, sleep } from '@lib/shared/method';
import {
  dataSourceTypes,
  departmentTypes,
  linkAllAcceptTypes,
  memberTypes,
  multipleTypes,
  singleTypes,
} from '@lib/shared/method/formCreate';
import type { CollaborationType, ModuleField } from '@lib/shared/models/customer';
import type { FormConfig } from '@lib/shared/models/system/module';

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
  linkFormInfo?: Ref<Record<string, any> | undefined>; // 关联表单信息
  linkFormKey?: Ref<FormDesignKeyEnum | undefined>; // 关联表单key
}

export default function useFormCreateApi(props: FormCreateApiProps) {
  const { t } = useI18n();

  const sourceName = ref(props.initialSourceName); // 资源名称
  const collaborationType = ref<CollaborationType>(); // 协作类型-客户独有
  const specialInitialOptions = ref<Record<string, any>[]>([]); // 特殊字段的初始化选项列表
  const descriptions = ref<CrmDescriptionItem[]>([]); // 表单详情描述列表
  const fieldList = ref<FormCreateField[]>([]); // 表单字段列表
  const fieldShowControlMap = ref<Record<string, any>>({}); // 表单字段显示控制映射
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
  const loading = ref(false);
  const unsaved = ref(false);
  const formDetail = ref<Record<string, any>>({});
  const originFormDetail = ref<Record<string, any>>({});
  const detail = ref<Record<string, any>>({}); // 详情

  function initFormShowControl() {
    // 读取整个显隐控制映射
    Object.keys(fieldShowControlMap.value).forEach((fieldId) => {
      // 取出当前字段的所有规则
      const ruleIds = Object.keys(fieldShowControlMap.value[fieldId]);
      const field = fieldList.value.find((f) => f.id === fieldId);
      if (field) {
        // 当前字段存在，则遍历它的全部控制规则
        for (let i = 0; i < ruleIds.length; i++) {
          const ruleId = ruleIds[i];
          const controlField = fieldList.value.find((f) => f.id === ruleId);
          if (controlField) {
            // 处理显示规则
            if (fieldShowControlMap.value[fieldId][ruleId].includes(formDetail.value[controlField?.id])) {
              field.show = true;
              break; // 满足显示规则就停止，因为只需要满足一个规则字段即显示
            } else {
              field.show = false;
            }
          }
        }
      }
    });
  }

  /**
   * 表单描述显示规则处理
   * @param form 表单数据
   */
  function formDescriptionShowControlRulesSet(form: Record<string, any>) {
    // 读取整个显隐控制映射
    Object.keys(fieldShowControlMap.value).forEach((fieldId) => {
      // 取出当前字段的所有规则
      const fieldRuleIds = Object.keys(fieldShowControlMap.value[fieldId]);
      const field = fieldList.value.find((f) => f.id === fieldId);
      if (field) {
        // 当前字段存在，则遍历它的全部控制规则
        for (let i = 0; i < fieldRuleIds.length; i++) {
          const ruleId = fieldRuleIds[i];
          let value = '';
          const controlField = fieldList.value.find((f) => f.id === ruleId);
          if (controlField?.businessKey) {
            value = form[controlField.businessKey];
          } else {
            const formField = form.moduleFields?.find(
              (moduleField: ModuleField) => moduleField.fieldId === controlField?.id
            );
            value = formField?.fieldValue || '';
          }
          // 处理显示规则
          if (fieldShowControlMap.value[fieldId][ruleId].includes(value)) {
            field.show = true;
            break; // 满足显示规则就停止，因为只需要满足一个规则字段即显示
          } else {
            field.show = false;
          }
        }
      }
    });
  }

  async function initFormDescription() {
    try {
      const asyncApi = getFormDetailApiMap[props.formKey];
      if (!asyncApi || !props.sourceId) return;
      const form = await asyncApi(props.sourceId);
      descriptions.value = [];
      detail.value = form;
      collaborationType.value = form.collaborationType;
      formDescriptionShowControlRulesSet(form);
      fieldList.value.forEach((item) => {
        if (item.show === false || !item.readable) return;
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
              const addressArr = (field?.fieldValue as string)?.split('-') || [];
              value = addressArr.length
                ? `${getCityPath(addressArr[0])}-${addressArr.filter((e, i) => i > 0).join('-')}`
                : '-';
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
      originFormDetail.value = cloneDeep(formDetail.value);
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

  function fillLinkFormFieldValue(field: FormCreateField) {
    if (props.linkFormKey?.value) {
      const linkFieldId = formConfig.value.linkProp?.[props.linkFormKey.value]?.find(
        (e) => e.current === field.id
      )?.link;
      if (linkFieldId) {
        const linkField = props.linkFormInfo?.value?.[linkFieldId];
        if (linkField) {
          switch (true) {
            case dataSourceTypes.includes(field.type):
              // 数据源填充，且替换initialOptions
              field.initialOptions = linkField.initialOptions || [];
              formDetail.value[field.id] = linkField.value.map((e: Record<string, any>) => e.id);
              break;
            case multipleTypes.includes(field.type):
              // 多选填充
              if (field.type === FieldTypeEnum.INPUT_MULTIPLE) {
                // 标签直接填充
                formDetail.value[field.id] = Array.isArray(linkField.value)
                  ? linkField.value.slice(0, 10)
                  : [linkField.value];
              } else {
                // 其他多选类型需匹配名称相等的选项值
                formDetail.value[field.id] =
                  field.options?.filter((e) => linkField.value.includes(e.label)).map((e) => e.value) || [];
              }
              break;
            case singleTypes.includes(field.type):
              // 单选填充需要匹配名称相同的选项值
              formDetail.value[field.id] = field.options?.find((e) => e.label === linkField.value)?.value || '';
              break;
            case linkAllAcceptTypes.includes(field.type):
              // 文本输入类型可填充任何字段类型值
              if (dataSourceTypes.includes(linkField.type)) {
                // 联动的字段是数据源则填充选项名
                formDetail.value[field.id] = linkField.value
                  .map((e: Record<string, any>) => e.name)
                  .join(',')
                  .slice(0, 255);
              } else if (multipleTypes.includes(linkField.type)) {
                // 联动的字段是多选则拼接选项名
                formDetail.value[field.id] = linkField.value.join(',').slice(0, 255);
              } else if (linkField.type === FieldTypeEnum.DATE_TIME) {
                // 联动的字段是日期时间则转换
                if (linkField.dateType === 'month') {
                  formDetail.value[field.id] = dayjs(linkField.value).format('YYYY-MM');
                } else if (linkField.dateType === 'date') {
                  formDetail.value[field.id] = dayjs(linkField.value).format('YYYY-MM-DD');
                } else {
                  formDetail.value[field.id] = dayjs(linkField.value).format('YYYY-MM-DD HH:mm:ss');
                }
              } else if (linkField.type === FieldTypeEnum.LOCATION) {
                // 联动的字段是省市区则填充城市路径
                const addressArr: string[] = linkField.value.split('-') || [];
                formDetail.value[field.id] = addressArr.length
                  ? `${getCityPath(addressArr[0])}-${addressArr.filter((e, i) => i > 0).join('-')}`
                  : '-';
              } else if (linkField.type === FieldTypeEnum.TEXTAREA && field.type === FieldTypeEnum.INPUT) {
                formDetail.value[field.id] = linkField.value.slice(0, 255);
              } else if ([...memberTypes, ...departmentTypes].includes(linkField.type)) {
                formDetail.value[field.id] = linkField.initialOptions
                  .map((e: any) => e.name)
                  .join(',')
                  .slice(0, 255);
              } else if (linkField.type === FieldTypeEnum.INPUT_NUMBER) {
                formDetail.value[field.id] = linkField.value?.toString();
              } else {
                formDetail.value[field.id] = linkField.value;
              }
              break;
            case [...memberTypes, ...departmentTypes].includes(field.type):
              formDetail.value[field.id] = Array.isArray(linkField.value) ? linkField.value : [linkField.value];
              field.initialOptions = linkField.initialOptions || [];
              break;
            default:
              formDetail.value[field.id] = linkField.value;
              field.initialOptions = linkField.initialOptions || [];
              break;
          }
        }
      }
    }
  }

  async function initFormConfig() {
    try {
      loading.value = true;
      const res = await getFormConfigApiMap[props.formKey]();
      formConfig.value = res.formProp;
      fieldList.value = res.fields.map((item) => {
        const { defaultValue, initialOptions } = specialFormFieldInit(item);
        if (item.showControlRules?.length) {
          // 将字段的控制显隐规则存储到 fieldShowControlMap 中
          item.showControlRules?.forEach((rule) => {
            rule.fieldIds.forEach((fieldId) => {
              // 按字段 ID 存储规则，key 为字段 ID，value 为规则映射集合
              if (!fieldShowControlMap.value[fieldId]) {
                fieldShowControlMap.value[fieldId] = {};
              }
              // value 映射以控制显示隐藏的字段 id 为 key，字段值为 value 集合
              if (!fieldShowControlMap.value[fieldId][item.id]) {
                fieldShowControlMap.value[fieldId][item.id] = [];
              }
              /**
               * 最终结构为：
               * fieldShowControlMap.value = {
               *   [fieldId]: {
               *     [item.id]: [rule.value]
               *   }
               * }
               * 这样最外层存储每个字段的 key，value 为该字段的所有的控制规则集合
               */
              fieldShowControlMap.value[fieldId][item.id].push(rule.value);
            });
          });
        }
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
    formConfig,
    formDetail,
    formCreateTitle,
    collaborationType,
    sourceName,
    initFormDescription,
    initFormConfig,
    initFormDetail,
    saveForm,
    initFormShowControl,
    fillLinkFormFieldValue,
    detail,
  };
}
