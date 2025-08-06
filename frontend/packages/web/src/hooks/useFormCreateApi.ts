import { useMessage } from 'naive-ui';
import { cloneDeep } from 'lodash-es';

import { FieldDataSourceTypeEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { formatNumberValue, formatTimeValue, getCityPath, safeFractionConvert } from '@lib/shared/method';
import type { CollaborationType, ModuleField } from '@lib/shared/models/customer';
import type { FormConfig } from '@lib/shared/models/system/module';

import type { Description } from '@/components/pure/crm-description/index.vue';
import {
  createFormApi,
  getFormConfigApiMap,
  getFormDetailApiMap,
  rules,
  updateFormApi,
} from '@/components/business/crm-form-create/config';
import type { FormCreateField, FormCreateFieldRule, FormDetail } from '@/components/business/crm-form-create/types';
import {
  dataSourceTypes,
  linkAllAcceptTypes,
  multipleTypes,
  singleTypes,
} from '@/components/business/crm-form-design/linkFormConfig';

import useUserStore from '@/store/modules/user';

export interface FormCreateApiProps {
  sourceId?: Ref<string | undefined>;
  formKey: Ref<FormDesignKeyEnum>;
  needInitDetail?: Ref<boolean>;
  initialSourceName?: Ref<string | undefined>; // 特殊字段初始化需要的资源名称
  otherSaveParams?: Ref<Record<string, any> | undefined>;
  linkFormInfo?: Ref<Record<string, any> | undefined>; // 关联表单信息
}

export default function useFormCreateApi(props: FormCreateApiProps) {
  const { t } = useI18n();
  const Message = useMessage();
  const userStore = useUserStore();

  const sourceName = ref(props.initialSourceName?.value); // 资源名称
  const collaborationType = ref<CollaborationType>(); // 协作类型-客户独有
  const specialInitialOptions = ref<Record<string, any>[]>([]); // 特殊字段的初始化选项列表
  const descriptions = ref<Description[]>([]); // 表单详情描述列表
  const fieldList = ref<FormCreateField[]>([]); // 表单字段列表
  const fieldShowControlMap = ref<Record<string, any>>({}); // 表单字段显示控制映射
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
  // 详情
  const detail = ref<Record<string, any>>({});
  const linkFormFieldMap = ref<Record<string, any>>({}); // 关联表单字段信息映射

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

  async function initFormDescription(formData?: FormDetail) {
    try {
      let form = cloneDeep(formData || ({} as FormDetail));
      if (!formData) {
        const asyncApi = getFormDetailApiMap[props.formKey.value];
        if (!asyncApi || !props.sourceId?.value) return;
        form = await asyncApi(props.sourceId?.value);
      }
      descriptions.value = [];
      detail.value = form;
      collaborationType.value = form.collaborationType;
      formDescriptionShowControlRulesSet(form);

      fieldList.value.forEach((item) => {
        if (item.show === false) return;
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
              name = options.find((e) => e.id === value)?.name || t('common.optionNotExist');
            }
          }
          if (item.businessKey === 'expectedEndTime') {
            // TODO:商机结束时间原位编辑
            descriptions.value.push({
              label: item.name,
              value: name || form[item.businessKey],
              slotName: FieldTypeEnum.DATE_TIME,
              fieldInfo: item,
            });
          } else if (
            item.type === FieldTypeEnum.DATA_SOURCE &&
            item.dataSourceType === FieldDataSourceTypeEnum.CUSTOMER &&
            [FormDesignKeyEnum.CLUE, FormDesignKeyEnum.BUSINESS].includes(props.formKey.value)
          ) {
            // 客户字段
            descriptions.value.push({
              label: item.name,
              value: name || form[item.businessKey],
              slotName: FieldDataSourceTypeEnum.CUSTOMER,
              fieldInfo: item,
            });
          } else if (item.type === FieldTypeEnum.DATE_TIME) {
            descriptions.value.push({
              label: item.name,
              value: formatTimeValue(name || form[item.businessKey], item.dateType),
              fieldInfo: item,
            });
          } else if (item.type === FieldTypeEnum.INPUT_NUMBER) {
            descriptions.value.push({
              label: item.name,
              value: formatNumberValue(name || form[item.businessKey], item),
              fieldInfo: item,
            });
          } else {
            descriptions.value.push({
              label: item.name,
              value: name || form[item.businessKey],
              fieldInfo: item,
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
              slotName: 'divider',
              fieldInfo: item,
            });
          } else if (item.type === FieldTypeEnum.PICTURE) {
            descriptions.value.push({
              label: item.name,
              value: field?.fieldValue || [],
              valueSlotName: 'image',
              fieldInfo: item,
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
                value = options.find((e) => e.id === field.fieldValue)?.name || t('common.optionNotExist');
              }
            } else if (item.type === FieldTypeEnum.LOCATION) {
              const address = (field?.fieldValue as string)?.split('-');
              value = address ? `${getCityPath(address[0])}${address[1] ? `-${address[1]}` : ''}` : '-';
            } else if (item.type === FieldTypeEnum.INPUT_NUMBER) {
              value = formatNumberValue(field?.fieldValue as string, item);
            } else if (item.type === FieldTypeEnum.DATE_TIME) {
              value = formatTimeValue(field?.fieldValue as string, item.dateType);
            }
            descriptions.value.push({
              label: item.name,
              value,
              fieldInfo: item,
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

  function makeLinkFormFields(field: FormCreateField) {
    switch (true) {
      case dataSourceTypes.includes(field.type):
        // 数据源字段填充
        linkFormFieldMap.value[field.id] = {
          ...field,
          value: field.initialOptions?.filter((e) => formDetail.value[field.id].includes(e.id)),
        };
        break;
      case multipleTypes.includes(field.type):
        // 多选字段填充
        if (field.type === FieldTypeEnum.INPUT_MULTIPLE) {
          linkFormFieldMap.value[field.id] = {
            ...field,
            value: formDetail.value[field.id],
          };
        } else {
          linkFormFieldMap.value[field.id] = {
            ...field,
            value: formDetail.value[field.id].map((id: string) => field.options?.find((e) => e.value === id)?.label),
          };
        }
        break;
      case singleTypes.includes(field.type):
        // 单选字段填充
        linkFormFieldMap.value[field.id] = {
          ...field,
          value: field.options?.find((e) => e.value === formDetail.value[field.id])?.label,
        };
        break;
      default:
        linkFormFieldMap.value[field.id] = {
          ...field,
          value: formDetail.value[field.id],
        };
        break;
    }
  }

  function fillLinkFormFieldValue(field: FormCreateField) {
    const linkFieldId = formConfig.value.linkProp?.linkFields?.find((e) => e.current === field.id)?.link;
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
              formDetail.value[field.id] = linkField.value;
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
              formDetail.value[field.id] = linkField.value.map((e: Record<string, any>) => e.name);
            } else if (linkField.type === FieldTypeEnum.INPUT_MULTIPLE) {
              // 联动的字段是输入多选则直接拼接字符串
              formDetail.value[field.id] = linkField.value.join(',');
            } else if (multipleTypes.includes(linkField.type)) {
              // 联动的字段是多选则拼接选项名
              formDetail.value[field.id] = field.options?.filter((e) => linkField.value.includes(e.label)).join(',');
            } else if (singleTypes.includes(linkField.type)) {
              // 联动的字段是单选则直接填充选项名
              formDetail.value[field.id] = field.options?.find((e) => e.label === linkField.value)?.label || '';
            } else if (linkField.type === FieldTypeEnum.LOCATION) {
              // 联动的字段是省市区则填充城市路径
              const address = linkField.value.split('-');
              formDetail.value[field.id] = `${getCityPath(address[0])}${address[1] ? `-${address[1]}` : ''}`;
            } else {
              formDetail.value[field.id] = linkField.value;
            }
            break;
          default:
            formDetail.value[field.id] = linkField.value;
            field.initialOptions = linkField.initialOptions || [];
            break;
        }
      }
    }
  }

  /**
   * 初始化表单详情
   * @param needInitFormDescription 是否需要初始化表单描述列表
   * @param needMakeLinkFormFields 是否需要初始化表单联动字段信息映射
   */
  async function initFormDetail(needInitFormDescription = false, needMakeLinkFormFields = false) {
    try {
      const asyncApi = getFormDetailApiMap[props.formKey.value];
      if (!asyncApi || !props.sourceId?.value) return;
      const res = await asyncApi(props.sourceId?.value);
      if (needInitFormDescription) {
        await initFormDescription(res);
      }
      collaborationType.value = res.collaborationType;
      sourceName.value = res.name;
      fieldList.value.forEach((item) => {
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
              ?.filter((e) => formDetail.value[item.id].includes(e.id))
              .map((e) => ({
                ...e,
                name: e.name || t('common.optionNotExist'),
              }));
          }
        }
        if (item.type === FieldTypeEnum.DATE_TIME) {
          // 处理时间类型的字段
          formDetail.value[item.id] = formDetail.value[item.id] ? Number(formDetail.value[item.id]) : null;
        }
        if (needMakeLinkFormFields) {
          makeLinkFormFields(item);
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
          initialOptions: field.initialOptions,
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
          initialOptions: field.initialOptions,
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
    if (
      [FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS, FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS].includes(
        props.formKey.value
      ) &&
      props.sourceId?.value
    ) {
      // 商机跟进计划和记录，需要赋予默认跟进类型、商机、商机对应客户
      if (field.businessKey === 'type') {
        return {
          defaultValue: 'CUSTOMER',
          initialOptions: field.initialOptions,
        };
      }

      const defaultParsedSource = props.initialSourceName?.value ? JSON.parse(props.initialSourceName.value) : {};
      if (Object.keys(defaultParsedSource).length) {
        if (field.businessKey === 'opportunityId') {
          specialInitialOptions.value = [
            {
              id: props.sourceId?.value,
              name: defaultParsedSource?.name ?? '',
            },
          ];
          return {
            defaultValue: initFieldValue(field, props.sourceId?.value || ''),
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
    if ([FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER].includes(props.formKey.value)) {
      // 线索转客户带入名称
      if (field.businessKey === 'name') {
        return {
          defaultValue: props.initialSourceName?.value,
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

  function initFormFieldConfig(fields: FormCreateField[]) {
    fieldList.value = fields.map((item) => {
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
  }

  async function initFormConfig() {
    try {
      loading.value = true;
      const res = await getFormConfigApiMap[props.formKey.value]();
      initFormFieldConfig(res.fields);
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
      return '';
    }
    if (
      [
        FieldTypeEnum.SELECT_MULTIPLE,
        FieldTypeEnum.MEMBER_MULTIPLE,
        FieldTypeEnum.DEPARTMENT_MULTIPLE,
        FieldTypeEnum.DATA_SOURCE_MULTIPLE,
        FieldTypeEnum.INPUT_MULTIPLE,
      ].includes(item.type) &&
      !value
    ) {
      return [];
    }
    if (item.type === FieldTypeEnum.INPUT_MULTIPLE && !value) {
      return [];
    }
    if (item.multiple && !value) {
      return [];
    }
    return value;
  }

  function getRuleType(item: FormCreateField) {
    if (
      item.type === FieldTypeEnum.SELECT_MULTIPLE ||
      item.type === FieldTypeEnum.CHECKBOX ||
      item.type === FieldTypeEnum.INPUT_MULTIPLE ||
      item.type === FieldTypeEnum.MEMBER_MULTIPLE ||
      item.type === FieldTypeEnum.DEPARTMENT_MULTIPLE ||
      item.type === FieldTypeEnum.DATA_SOURCE ||
      item.type === FieldTypeEnum.DATA_SOURCE_MULTIPLE ||
      item.type === FieldTypeEnum.PICTURE
    ) {
      return 'array';
    }
    if (item.type === FieldTypeEnum.DATE_TIME) {
      return 'date';
    }
    if (item.type === FieldTypeEnum.INPUT_NUMBER) {
      return 'number';
    }
    return 'string';
  }

  function initForm() {
    fieldList.value.forEach((item) => {
      if (props.needInitDetail?.value) {
        // 详情页编辑时，从详情获取值，不需要默认值
        item.defaultValue = undefined;
      }
      let defaultValue = item.defaultValue || '';
      if ([FieldTypeEnum.DATE_TIME, FieldTypeEnum.INPUT_NUMBER].includes(item.type)) {
        defaultValue = Number.isNaN(Number(defaultValue)) || defaultValue === '' ? null : Number(defaultValue);
      } else if (getRuleType(item) === 'array') {
        defaultValue =
          [FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.MEMBER].includes(item.type) &&
          typeof item.defaultValue === 'string'
            ? [defaultValue]
            : defaultValue || [];
      } else if (item.type === FieldTypeEnum.PICTURE) {
        defaultValue = defaultValue || [];
      }
      if (!formDetail.value[item.id]) {
        formDetail.value[item.id] = defaultValue;
      }
      const fullRules: FormCreateFieldRule[] = [];
      (item.rules || []).forEach((rule) => {
        // 遍历规则集合，将全量的规则配置载入
        const staticRule = cloneDeep(rules.find((e) => e.key === rule.key));
        if (staticRule) {
          staticRule.regex = rule.regex; // 正则表达式(目前没有)是配置到后台存储的，需要读取
          staticRule.message = t(staticRule.message as string, { value: t(item.name) });
          staticRule.type = getRuleType(item);
          if ([FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(item.type)) {
            staticRule.trigger = 'none';
          }
          fullRules.push(staticRule);
        }
      });
      item.rules = fullRules;
      if ([FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(item.type) && item.hasCurrentUser) {
        item.defaultValue = userStore.userInfo.id;
        item.initialOptions = [
          ...(item.initialOptions || []),
          {
            id: userStore.userInfo.id,
            name: userStore.userInfo.name,
          },
        ].filter((option, index, self) => self.findIndex((o) => o.id === option.id) === index);
      } else if (
        [FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(item.type) &&
        item.hasCurrentUserDept
      ) {
        item.defaultValue = userStore.userInfo.departmentId;
        item.initialOptions = [
          ...(item.initialOptions || []),
          {
            id: userStore.userInfo.departmentId,
            name: userStore.userInfo.departmentName,
          },
        ].filter((option, index, self) => self.findIndex((o) => o.id === option.id) === index);
      }
      if (props.linkFormInfo?.value) {
        // 如果有关联表单信息，则填充关联表单字段值
        fillLinkFormFieldValue(item);
      }
    });
    nextTick(() => {
      initFormShowControl();
      unsaved.value = false;
    });
  }

  function resetForm() {
    formDetail.value = {};
    fieldList.value.forEach((item) => {
      item.initialOptions = [];
    });
    initFormFieldConfig(fieldList.value);
    initForm();
  }

  async function saveForm(
    form: Record<string, any>,
    isContinue: boolean,
    callback?: (_isContinue: boolean) => void,
    noReset = false
  ) {
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
        } else {
          Message.success(t('common.createSuccess'));
        }
      }
      if (callback) {
        callback(isContinue);
      }
      if (!noReset) {
        resetForm();
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
    const prefix = props.sourceId?.value && props.needInitDetail?.value ? t('common.edit') : t('common.newCreate');
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
    fieldShowControlMap,
    linkFormFieldMap,
    initFormDescription,
    initFormConfig,
    initFormDetail,
    saveForm,
    initForm,
    resetForm,
    initFormShowControl,
    makeLinkFormFields,
    detail,
  };
}
