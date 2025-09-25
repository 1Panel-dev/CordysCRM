import { useMessage } from 'naive-ui';
import { cloneDeep } from 'lodash-es';
import dayjs from 'dayjs';

import {
  FieldDataSourceTypeEnum,
  FieldRuleEnum,
  FieldTypeEnum,
  FormDesignKeyEnum,
} from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { formatNumberValue, formatTimeValue, getCityPath, safeFractionConvert } from '@lib/shared/method';
import {
  dataSourceTypes,
  departmentTypes,
  getNormalFieldValue,
  getRuleType,
  linkAllAcceptTypes,
  memberTypes,
  multipleTypes,
  singleTypes,
} from '@lib/shared/method/formCreate';
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
import type {
  AttachmentInfo,
  FormCreateField,
  FormCreateFieldRule,
  FormDetail,
} from '@/components/business/crm-form-create/types';

import { checkRepeat } from '@/api/modules';
import { lastOpportunitySteps } from '@/config/opportunity';
import useUserStore from '@/store/modules/user';

export interface FormCreateApiProps {
  sourceId?: Ref<string | undefined>;
  formKey: Ref<FormDesignKeyEnum>;
  needInitDetail?: Ref<boolean>;
  initialSourceName?: Ref<string | undefined>; // 特殊字段初始化需要的资源名称
  otherSaveParams?: Ref<Record<string, any> | undefined>;
  linkFormInfo?: Ref<Record<string, any> | undefined>; // 关联表单信息
  linkFormKey?: Ref<FormDesignKeyEnum | undefined>; // 关联表单key
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
    viewSize: 'large',
  }); // 表单属性配置
  const formDetail = ref<Record<string, any>>({});
  const originFormDetail = ref<Record<string, any>>({});

  // 详情
  const detail = ref<Record<string, any>>({});
  const linkFormFieldMap = ref<Record<string, any>>({}); // 关联表单字段信息映射
  const opportunityInternalFields = [
    {
      title: t('org.department'),
      key: 'departmentName',
    },
    {
      title: t('opportunity.stage'),
      key: 'stage',
    },
    {
      title: t('customer.lastFollowUps'),
      key: 'followerName',
    },
    {
      title: t('customer.lastFollowUpDate'),
      key: 'followTime',
    },
    {
      title: t('customer.remainingVesting'),
      key: 'reservedDays',
    },
    {
      title: t('opportunity.actualEndTime'),
      key: 'actualEndTime',
    },
    {
      title: t('opportunity.failureReason'),
      key: 'failureReason',
    },
  ];
  const customerInternalFields = [
    {
      title: t('org.department'),
      key: 'departmentName',
    },
    {
      title: t('customer.collectionTime'),
      key: 'collectionTime',
    },
    {
      title: t('customer.recycleOpenSea'),
      key: 'recyclePoolName',
    },
    {
      title: t('customer.remainingVesting'),
      key: 'reservedDays',
    },
    {
      title: t('customer.lastFollowUps'),
      key: 'followerName',
    },
    {
      title: t('customer.lastFollowUpDate'),
      key: 'followTime',
    },
  ];

  const contactInternalFields = [
    {
      title: t('common.status'),
      key: 'enable',
    },
    {
      title: t('customer.disableReason'),
      key: 'disableReason',
    },
    {
      title: t('org.department'),
      key: 'departmentName',
    },
  ];

  const internalFieldMap: Partial<Record<FormDesignKeyEnum, any[]>> = {
    [FormDesignKeyEnum.CUSTOMER]: customerInternalFields,
    [FormDesignKeyEnum.CONTACT]: contactInternalFields,
    [FormDesignKeyEnum.CUSTOMER_CONTACT]: [
      {
        title: t('common.status'),
        key: 'enable',
      },
      {
        title: t('customer.disableReason'),
        key: 'disableReason',
      },
      {
        title: t('org.department'),
        key: 'departmentName',
      },
    ],
    [FormDesignKeyEnum.BUSINESS_CONTACT]: [
      {
        title: t('common.status'),
        key: 'enable',
      },
      {
        title: t('customer.disableReason'),
        key: 'disableReason',
      },
      {
        title: t('org.department'),
        key: 'departmentName',
      },
    ],
    [FormDesignKeyEnum.BUSINESS]: opportunityInternalFields,
    [FormDesignKeyEnum.CLUE]: [
      {
        title: t('org.department'),
        key: 'departmentName',
      },
      {
        title: t('customer.collectionTime'),
        key: 'collectionTime',
      },
      {
        title: t('clue.recyclePool'),
        key: 'recyclePoolName',
      },
      {
        title: t('customer.remainingVesting'),
        key: 'reservedDays',
      },
      {
        title: t('customer.lastFollowUps'),
        key: 'follower',
      },
      {
        title: t('customer.lastFollowUpDate'),
        key: 'followTime',
      },
    ],
    [FormDesignKeyEnum.PRODUCT]: [],
    [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: [
      {
        title: t('customer.recycleReason'),
        key: 'reasonName',
      },
    ],
    [FormDesignKeyEnum.CLUE_POOL]: [
      {
        title: t('customer.recycleReason'),
        key: 'reasonName',
      },
    ],
    [FormDesignKeyEnum.CUSTOMER_OPPORTUNITY]: opportunityInternalFields,
    [FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER]: customerInternalFields,
  };
  const staticFields = [
    {
      title: t('common.creator'),
      key: 'createUserName',
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUserName',
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
    },
  ];

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

  function formatInternalFieldValue(key: string, value: any) {
    if (key.includes('Time')) {
      return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-';
    }
    if (key === 'enable') {
      return value ? t('common.open') : t('common.close');
    }
    if (key === 'stage') {
      const step = lastOpportunitySteps.find((e: any) => e.value === value);
      return step ? step.label : '-';
    }
    return value || '-';
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
              tooltipPosition: 'top-end',
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
              tooltipPosition: 'top-end',
            });
          } else if (item.type === FieldTypeEnum.DATE_TIME) {
            descriptions.value.push({
              label: item.name,
              value: formatTimeValue(name || form[item.businessKey], item.dateType),
              fieldInfo: item,
              tooltipPosition: 'top-end',
            });
          } else if (item.type === FieldTypeEnum.INPUT_NUMBER) {
            descriptions.value.push({
              label: item.name,
              value: formatNumberValue(name || form[item.businessKey], item),
              fieldInfo: item,
              tooltipPosition: 'top-end',
            });
          } else if (item.type === FieldTypeEnum.TEXTAREA) {
            descriptions.value.push({
              label: item.name,
              value: name || form[item.businessKey],
              slotName: FieldTypeEnum.TEXTAREA,
              fieldInfo: item,
              tooltipPosition: 'top-end',
            });
          } else if (item.type === FieldTypeEnum.ATTACHMENT) {
            descriptions.value.push({
              label: item.name,
              value: form.attachmentMap?.[item.businessKey] || [],
              slotName: FieldTypeEnum.ATTACHMENT,
              fieldInfo: item,
              tooltipPosition: 'top-end',
            });
          } else {
            descriptions.value.push({
              label: item.name,
              value: name || form[item.businessKey],
              fieldInfo: item,
              tooltipPosition: 'top-end',
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
              tooltipPosition: 'top-end',
            });
          } else if (item.type === FieldTypeEnum.PICTURE) {
            descriptions.value.push({
              label: item.name,
              value: field?.fieldValue || [],
              valueSlotName: 'image',
              fieldInfo: item,
              tooltipPosition: 'top-end',
            });
          } else if (item.type === FieldTypeEnum.TEXTAREA) {
            descriptions.value.push({
              label: item.name,
              value: field?.fieldValue || '',
              slotName: FieldTypeEnum.TEXTAREA,
              fieldInfo: item,
              tooltipPosition: 'top-end',
            });
          } else if (item.type === FieldTypeEnum.LINK) {
            descriptions.value.push({
              label: item.name,
              value: field?.fieldValue || '',
              slotName: FieldTypeEnum.LINK,
              fieldInfo: item,
              tooltipPosition: 'top-end',
            });
          } else if (item.type === FieldTypeEnum.ATTACHMENT) {
            descriptions.value.push({
              label: item.name,
              value: form.attachmentMap?.[item.id] || [],
              slotName: FieldTypeEnum.ATTACHMENT,
              fieldInfo: item,
              tooltipPosition: 'top-end',
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
              const addressArr: string[] = (field?.fieldValue as string)?.split('-') || [];
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
              value,
              fieldInfo: item,
              tooltipPosition: 'top-end',
            });
          }
        }
      });
      [...(internalFieldMap[props.formKey.value] || []), ...staticFields].forEach((field) => {
        descriptions.value.push({
          label: field.title,
          value: formatInternalFieldValue(field.key, form[field.key]),
          fieldInfo: {
            name: field.title,
            type: FieldTypeEnum.INPUT,
          },
          tooltipPosition: 'top-end',
        });
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
    if (!formDetail.value[field.id]) return;
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
        } else if (item.type === FieldTypeEnum.ATTACHMENT) {
          item.initialOptions = res.attachmentMap?.[item.id];
        }
        if (needMakeLinkFormFields) {
          makeLinkFormFields(item);
        }
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
      } else if ([FieldTypeEnum.PICTURE, FieldTypeEnum.ATTACHMENT].includes(item.type)) {
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
          // 重复校验
          if (staticRule.key === FieldRuleEnum.UNIQUE) {
            staticRule.validator = async (_rule: any, value: string) => {
              if (
                !value.length ||
                formDetail.value[item.id].replace(/[\s\uFEFF\xA0]+/g, '') === originFormDetail.value[item.id]
              ) {
                return Promise.resolve();
              }

              try {
                const info = await checkRepeat({
                  id: item.id,
                  value,
                  formKey: props.formKey.value,
                });
                if (info.repeat) {
                  return Promise.reject(
                    new Error(
                      info.name.length
                        ? t('crmFormCreate.repeatTip', { name: info.name })
                        : t('crmFormCreate.repeatTipWithoutName')
                    )
                  );
                }
                return Promise.resolve();
              } catch (error) {
                // eslint-disable-next-line no-console
                console.log(error);
              }
            };
          } else {
            staticRule.regex = rule.regex; // 正则表达式(目前没有)是配置到后台存储的，需要读取
            staticRule.message = t(staticRule.message as string, { value: t(item.name) });
            staticRule.type = getRuleType(item);
            if ([FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(item.type)) {
              staticRule.trigger = 'none';
            }
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
    callback?: (_isContinue: boolean, res: any) => void,
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
      let res;
      if (props.sourceId?.value && props.needInitDetail?.value) {
        res = await updateFormApi[props.formKey.value](params);
        Message.success(t('common.updateSuccess'));
      } else {
        res = await createFormApi[props.formKey.value](params);
        if (props.formKey.value === FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER) {
          Message.success(t('clue.transferredToCustomer'));
        } else {
          Message.success(t('common.createSuccess'));
        }
      }
      if (callback) {
        callback(isContinue, res);
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
    originFormDetail,
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
