import { useMessage } from 'naive-ui';
import { cloneDeep } from 'lodash-es';
import dayjs from 'dayjs';

import {
  FieldDataSourceTypeEnum,
  FieldRuleEnum,
  FieldTypeEnum,
  FormDesignKeyEnum,
  type FormLinkScenarioEnum,
} from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { getCityPath, getIndustryPath, safeFractionConvert } from '@lib/shared/method';
import {
  dataSourceTypes,
  departmentTypes,
  getNormalFieldValue,
  getRuleType,
  initFieldValue,
  linkAllAcceptTypes,
  memberTypes,
  multipleTypes,
  parseFormDetailValue,
  parseModuleFieldValue,
  singleTypes,
} from '@lib/shared/method/formCreate';
import type { ModuleField } from '@lib/shared/models/common';
import type { CollaborationType } from '@lib/shared/models/customer';
import type { FormConfig, FormDesignConfigDetailParams } from '@lib/shared/models/system/module';

import type { Description } from '@/components/pure/crm-description/index.vue';
import {
  createFormApi,
  getFormConfigApiMap,
  getFormDetailApiMap,
  rules,
  updateFormApi,
} from '@/components/business/crm-form-create/config';
import type { FormCreateField, FormCreateFieldRule, FormDetail } from '@/components/business/crm-form-create/types';

import { checkRepeat } from '@/api/modules';
import useUserStore from '@/store/modules/user';

export interface FormCreateApiProps {
  sourceId?: Ref<string | undefined>;
  formKey: Ref<FormDesignKeyEnum>;
  needInitDetail?: Ref<boolean>;
  initialSourceName?: Ref<string | undefined>; // 特殊字段初始化需要的资源名称
  otherSaveParams?: Ref<Record<string, any> | undefined>;
  linkFormInfo?: Ref<Record<string, any> | undefined>; // 关联表单信息
  linkFormKey?: Ref<FormDesignKeyEnum | undefined>; // 关联表单key
  linkScenario?: Ref<FormLinkScenarioEnum | undefined>; // 关联表单场景
  isContractTableDetail?: boolean;
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
  const moduleFormConfig = ref<FormDesignConfigDetailParams>();

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
      key: 'stageName',
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
        key: 'followerName',
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
    [FormDesignKeyEnum.CONTRACT_SNAPSHOT]: [
      {
        title: t('org.department'),
        key: 'departmentName',
      },
      {
        title: t('contract.alreadyPayAmount'),
        key: 'alreadyPayAmount',
      },
    ],
    [FormDesignKeyEnum.CONTRACT_PAYMENT]: [
      {
        title: t('org.department'),
        key: 'departmentName',
      },
    ],
    [FormDesignKeyEnum.CONTRACT_CONTRACT_PAYMENT]: [
      {
        title: t('org.department'),
        key: 'departmentName',
      },
    ],
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
  // 用于快照保存表单配置
  const needModuleFormConfigParamsType = [
    FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
    FormDesignKeyEnum.CONTRACT,
    FormDesignKeyEnum.INVOICE,
    FormDesignKeyEnum.ORDER,
  ];

  function initFormShowControl(value?: any) {
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
            if (fieldShowControlMap.value[fieldId][ruleId].includes(formDetail.value[controlField.id])) {
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
    return value === undefined || value === null || value === '' ? '-' : value;
  }

  function makeDescriptionItem(item: FormCreateField, form: FormDetail) {
    if (item.show === false || !item.readable) return;
    if (item.businessKey === 'expectedEndTime' && !item.resourceFieldId) {
      // TODO:商机结束时间原位编辑
      descriptions.value.push({
        label: item.name,
        value: parseFormDetailValue(item, form),
        slotName: FieldTypeEnum.DATE_TIME,
        fieldInfo: item,
        tooltipPosition: 'top-end',
      });
    } else if (
      item.type === FieldTypeEnum.DATA_SOURCE &&
      item.dataSourceType === FieldDataSourceTypeEnum.CUSTOMER &&
      [
        FormDesignKeyEnum.CLUE,
        FormDesignKeyEnum.BUSINESS,
        FormDesignKeyEnum.CONTRACT_SNAPSHOT,
        FormDesignKeyEnum.INVOICE_SNAPSHOT,
      ].includes(props.formKey.value) &&
      !item.resourceFieldId
    ) {
      // 客户字段
      descriptions.value.push({
        label: item.name,
        value: parseFormDetailValue(item, form),
        slotName: 'dataSource',
        fieldInfo: item,
        tooltipPosition: 'top-end',
      });
    } else if (
      item.type === FieldTypeEnum.DATA_SOURCE &&
      item.dataSourceType === FieldDataSourceTypeEnum.BUSINESS_TITLE &&
      !item.resourceFieldId
    ) {
      // 工商抬头字段
      descriptions.value.push({
        label: item.name,
        value: parseFormDetailValue(item, form),
        slotName: 'dataSource',
        fieldInfo: item,
        tooltipPosition: 'top-end',
      });
    } else if (
      item.type === FieldTypeEnum.DATA_SOURCE &&
      item.dataSourceType === FieldDataSourceTypeEnum.CONTRACT &&
      [
        FormDesignKeyEnum.CONTRACT_PAYMENT,
        FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD,
        FormDesignKeyEnum.INVOICE_SNAPSHOT,
      ].includes(props.formKey.value) &&
      !item.resourceFieldId
    ) {
      descriptions.value.push({
        label: item.name,
        value: parseFormDetailValue(item, form),
        slotName: 'dataSource',
        fieldInfo: item,
        tooltipPosition: 'top-end',
      });
    } else if (
      props.isContractTableDetail &&
      [FieldTypeEnum.DATA_SOURCE].includes(item.type) &&
      props.formKey.value === FormDesignKeyEnum.CONTRACT_SNAPSHOT &&
      [FieldDataSourceTypeEnum.BUSINESS, FieldDataSourceTypeEnum.QUOTATION].includes(
        item.dataSourceType as FieldDataSourceTypeEnum
      ) &&
      !item.resourceFieldId
    ) {
      descriptions.value.push({
        label: item.name,
        value: parseFormDetailValue(item, form),
        slotName: 'dataSource',
        fieldInfo: item,
        tooltipPosition: 'top-end',
      });
    } else if (
      props.isContractTableDetail &&
      item.type === FieldTypeEnum.DATA_SOURCE_MULTIPLE &&
      props.formKey.value === FormDesignKeyEnum.CONTRACT_SNAPSHOT &&
      [
        FieldDataSourceTypeEnum.BUSINESS,
        FieldDataSourceTypeEnum.CUSTOMER,
        FieldDataSourceTypeEnum.BUSINESS_TITLE,
        FieldDataSourceTypeEnum.QUOTATION,
      ].includes(item.dataSourceType as FieldDataSourceTypeEnum) &&
      !item.resourceFieldId
    ) {
      descriptions.value.push({
        label: item.name,
        value: parseFormDetailValue(item, form),
        slotName: 'dataSourceMultiple',
        fieldInfo: item,
        tooltipPosition: 'top-end',
      });
    } else if (
      [
        FieldTypeEnum.DATA_SOURCE,
        FieldTypeEnum.DATA_SOURCE_MULTIPLE,
        FieldTypeEnum.DEPARTMENT,
        FieldTypeEnum.DEPARTMENT_MULTIPLE,
        FieldTypeEnum.MEMBER,
        FieldTypeEnum.MEMBER_MULTIPLE,
        FieldTypeEnum.SELECT,
        FieldTypeEnum.SELECT_MULTIPLE,
        FieldTypeEnum.RADIO,
        FieldTypeEnum.CHECKBOX,
      ].includes(item.type)
    ) {
      descriptions.value.push({
        label: item.name,
        value: parseFormDetailValue(item, form),
        fieldInfo: item,
        tooltipPosition: 'top-end',
      });
    } else if (item.type === FieldTypeEnum.DATE_TIME) {
      descriptions.value.push({
        label: item.name,
        value: parseFormDetailValue(item, form),
        fieldInfo: item,
        tooltipPosition: 'top-end',
      });
    } else if (item.type === FieldTypeEnum.INPUT_NUMBER) {
      descriptions.value.push({
        label: item.name,
        value: parseFormDetailValue(item, form),
        fieldInfo: item,
        tooltipPosition: 'top-end',
      });
    } else if (item.type === FieldTypeEnum.TEXTAREA) {
      descriptions.value.push({
        label: item.name,
        value: parseFormDetailValue(item, form),
        slotName: FieldTypeEnum.TEXTAREA,
        fieldInfo: item,
        tooltipPosition: 'top-end',
      });
    } else if (item.type === FieldTypeEnum.ATTACHMENT) {
      descriptions.value.push({
        label: item.name,
        value: parseFormDetailValue(item, form),
        slotName: FieldTypeEnum.ATTACHMENT,
        fieldInfo: item,
        tooltipPosition: 'top-end',
      });
    } else if (item.type === FieldTypeEnum.DIVIDER) {
      descriptions.value.push({
        label: item.name,
        value: parseFormDetailValue(item, form),
        slotName: 'divider',
        fieldInfo: item,
        tooltipPosition: 'top-end',
      });
    } else if (item.type === FieldTypeEnum.PICTURE) {
      descriptions.value.push({
        label: item.name,
        value: parseFormDetailValue(item, form),
        valueSlotName: 'image',
        fieldInfo: item,
        tooltipPosition: 'top-end',
      });
    } else if (item.type === FieldTypeEnum.LINK) {
      descriptions.value.push({
        label: item.name,
        value: parseFormDetailValue(item, form),
        slotName: FieldTypeEnum.LINK,
        fieldInfo: item,
        tooltipPosition: 'top-end',
      });
    } else {
      descriptions.value.push({
        label: item.name,
        value: parseFormDetailValue(item, form),
        fieldInfo: item,
        tooltipPosition: 'top-end',
      });
    }
    if (item.businessKey === 'name') {
      sourceName.value = parseFormDetailValue(item, form);
    }
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
        const value = item.businessKey
          ? form[item.businessKey]
          : form.moduleFields?.find((mf) => mf.fieldId === item.id)?.fieldValue;
        if ([FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(item.type) && item.subFields?.length) {
          if (item.show === false || !item.readable || !value || value?.length === 0) return;
          descriptions.value.push({
            label: item.name,
            value,
            slotName: item.type,
            fieldInfo: item,
            optionMap: form.optionMap,
          });
        } else {
          makeDescriptionItem(item, form);
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

  function fillLinkFormFieldValue(field: FormCreateField, scenario: FormLinkScenarioEnum) {
    if (props.linkFormKey?.value) {
      const linkFieldId = formConfig.value.linkProp?.[props.linkFormKey.value]
        ?.find((e) => e.key === scenario)
        ?.linkFields?.find((e) => e.current === field.id && e.enable)?.link;
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
              const limitLength = field.type === FieldTypeEnum.INPUT ? 255 : 3000;
              if (dataSourceTypes.includes(linkField.type)) {
                // 联动的字段是数据源则填充选项名
                formDetail.value[field.id] = linkField.value
                  .map((e: Record<string, any>) => e.name)
                  .join(',')
                  .slice(0, limitLength);
              } else if (multipleTypes.includes(linkField.type)) {
                // 联动的字段是多选则拼接选项名
                formDetail.value[field.id] = linkField.value.join(',').slice(0, limitLength);
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
              } else if (linkField.type === FieldTypeEnum.INDUSTRY) {
                formDetail.value[field.id] = linkField.value ? getIndustryPath(linkField.value as string) : '-';
              } else if (linkField.type === FieldTypeEnum.TEXTAREA && field.type === FieldTypeEnum.INPUT) {
                formDetail.value[field.id] = linkField.value.slice(0, limitLength);
              } else if ([...memberTypes, ...departmentTypes].includes(linkField.type)) {
                formDetail.value[field.id] = linkField.initialOptions
                  .map((e: any) => e.name)
                  .join(',')
                  .slice(0, limitLength);
              } else if ([FieldTypeEnum.INPUT_NUMBER, FieldTypeEnum.FORMULA].includes(linkField.type)) {
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

  function transformFormDetailValue(item: FormCreateField, res: FormDetail) {
    if (item.resourceFieldId) {
      // 数据源引用字段直接解析值
      formDetail.value[item.id] = parseFormDetailValue(item, res);
    } else if (item.businessKey) {
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
  }

  function makeSubFieldInitialOptions(subField: FormCreateField, parentFieldId: string, res: FormDetail) {
    if (subField.businessKey) {
      const options = res.optionMap?.[subField.businessKey];
      if ([FieldTypeEnum.DATA_SOURCE].includes(subField.type)) {
        // 处理成员和数据源类型的字段
        subField.initialOptions = options
          ?.filter((e) =>
            formDetail.value[parentFieldId]?.some((item: Record<string, any>) =>
              item[subField.businessKey!]?.includes(e.id)
            )
          )
          .map((e) => ({
            ...e,
            name: e.name || t('common.optionNotExist'),
          }));
      }
    } else {
      const options = res.optionMap?.[subField.id];
      if ([FieldTypeEnum.DATA_SOURCE].includes(subField.type)) {
        // 处理成员和数据源类型的字段
        subField.initialOptions = options
          ?.filter((e) =>
            formDetail.value[parentFieldId]?.some((item: Record<string, any>) => item[subField.id]?.includes(e.id))
          )
          .map((e) => ({
            ...e,
            name: e.name || t('common.optionNotExist'),
          }));
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
      formDetail.value = {};
      if (needInitFormDescription) {
        await initFormDescription(res);
      }
      collaborationType.value = res.collaborationType;
      sourceName.value = res.name;
      fieldList.value.forEach((item) => {
        if ([FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(item.type) && item.subFields?.length) {
          // 子表字段处理
          if (item.businessKey) {
            // 业务标准字段读取最外层
            formDetail.value[item.id] = res[item.businessKey];
          } else {
            // 其他的字段读取moduleFields
            const field = res.moduleFields?.find((moduleField: ModuleField) => moduleField.fieldId === item.id);
            if (field) {
              formDetail.value[item.id] = field.fieldValue;
            }
          }
          item.subFields.forEach((subField) => {
            makeSubFieldInitialOptions(subField, item.id, res);
            formDetail.value[item.id]?.forEach((subItem: Record<string, any>) => {
              const isPriceField = subField.dataSourceType === FieldDataSourceTypeEnum.PRICE && subItem.price_sub;
              if (isPriceField) {
                // 处理子表格里的价格表字段，填充行号到数据源字段选中值中以供回显
                subItem[subField.businessKey || subField.id] = [
                  subItem[subField.businessKey || subField.id],
                  subItem.price_sub,
                ];
                // 同时在initialOptions里填充行号子项以区分父子
                subField.initialOptions?.push({
                  id: subItem.price_sub,
                  parentId: subItem[subField.businessKey || subField.id],
                });
              }
              if (subField.resourceFieldId) {
                subItem[subField.id] = parseModuleFieldValue(
                  subField,
                  subItem[subField.id],
                  res.optionMap?.[subField.id]
                );
              } else {
                subItem[subField.businessKey || subField.id] = initFieldValue(
                  subField,
                  subItem[subField.businessKey || subField.id]
                );
              }
            });
          });
          return;
        }
        transformFormDetailValue(item, res);
        // transformFormDetailValue里已经处理了item.resourceFieldId的时间格式
        if (item.type === FieldTypeEnum.DATE_TIME && !item.resourceFieldId) {
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
    if (props.formKey.value === FormDesignKeyEnum.BUSINESS && props.sourceId?.value) {
      // 客户详情下创建商机，自动带入客户信息
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
      [FormDesignKeyEnum.CONTRACT_PAYMENT, FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD, FormDesignKeyEnum.ORDER].includes(
        props.formKey.value
      ) &&
      props.sourceId?.value
    ) {
      // 合同详情下创建计划，自动带入合同信息
      if (field.businessKey === 'contractId') {
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

    if (props.formKey.value === FormDesignKeyEnum.OPPORTUNITY_QUOTATION && props.sourceId?.value) {
      if (field.businessKey === 'opportunityId') {
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
    if (props.formKey.value === FormDesignKeyEnum.INVOICE && props.sourceId?.value) {
      // 合同下创建发票，自动带入合同信息
      if (field.businessKey === 'contractId') {
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
      const api = getFormConfigApiMap[props.formKey.value];
      const res = await api(props.sourceId?.value ?? '');
      moduleFormConfig.value = cloneDeep(res);
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

  function replaceRule(item: FormCreateField, parentFieldId?: string) {
    const fullRules: FormCreateFieldRule[] = [];
    (item.rules || []).forEach((rule) => {
      // 遍历规则集合，将全量的规则配置载入
      const staticRule = cloneDeep(rules.find((e) => e.key === rule.key));
      if (staticRule) {
        // 重复校验
        if (staticRule.key === FieldRuleEnum.UNIQUE) {
          if (parentFieldId) {
            staticRule.validator = async (_rule: any, value: string) => {
              if (!value || !value.length) {
                return Promise.resolve();
              }
              const subFieldValues = formDetail.value[parentFieldId]?.map(
                (subItem: Record<string, any>) => subItem[item.id]
              );
              const valueCount = subFieldValues.filter((v: string) => v === value).length;
              if (valueCount > 1) {
                return Promise.reject(
                  new Error(
                    item.name.length
                      ? t('crmFormCreate.repeatTip', { name: item.name })
                      : t('crmFormCreate.repeatTipWithoutName')
                  )
                );
              }
            };
          } else {
            staticRule.validator = async (_rule: any, value: string) => {
              if (!value || !value.length || formDetail.value[item.id] === originFormDetail.value[item.id]) {
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
          }
        } else {
          staticRule.regex = rule.regex; // 正则表达式(目前没有)是配置到后台存储的，需要读取
          staticRule.message = t(staticRule.message as string, { value: t(item.name) });
          staticRule.type = getRuleType(item);
          if (
            [
              FieldTypeEnum.DATA_SOURCE,
              FieldTypeEnum.DATA_SOURCE_MULTIPLE,
              FieldTypeEnum.PICTURE,
              FieldTypeEnum.ATTACHMENT,
            ].includes(item.type)
          ) {
            staticRule.trigger = 'none';
          }
        }
        fullRules.push(staticRule);
      }
    });
    item.rules = fullRules;
  }

  function subFieldInit(field: FormCreateField) {
    let defaultValue = field.defaultValue || '';
    if (field.resourceFieldId && field.defaultValue) {
      defaultValue = parseModuleFieldValue(field, field.defaultValue, field.initialOptions);
    } else if ([FieldTypeEnum.INPUT_NUMBER, FieldTypeEnum.FORMULA].includes(field.type)) {
      defaultValue = Number.isNaN(Number(defaultValue)) || defaultValue === '' ? null : Number(defaultValue);
    } else if ([FieldTypeEnum.PICTURE, FieldTypeEnum.ATTACHMENT].includes(field.type)) {
      defaultValue = defaultValue || [];
    } else if (getRuleType(field) === 'array') {
      defaultValue =
        field.type === FieldTypeEnum.DATA_SOURCE && typeof field.defaultValue === 'string'
          ? [defaultValue]
          : defaultValue || [];
    }
    field.defaultValue = defaultValue;
  }

  function initForm(linkScenario?: FormLinkScenarioEnum) {
    fieldList.value.forEach((item) => {
      // const initLine: Record<string, any> = {};
      if ([FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(item.type)) {
        item.subFields?.forEach((subField) => {
          subFieldInit(subField);
          replaceRule(subField, item.id);
          // initLine[subField.businessKey || subField.id] = subField.defaultValue;
        });
        if (!formDetail.value[item.id]) {
          formDetail.value[item.id] = [];
        }
        return;
      }
      if (props.needInitDetail?.value) {
        // 详情页编辑时，从详情获取值，不需要默认值
        item.defaultValue = undefined;
      }
      let defaultValue = item.defaultValue || '';
      if (item.resourceFieldId && item.defaultValue) {
        defaultValue = parseModuleFieldValue(
          item,
          item.defaultValue,
          item.initialOptions || item.options?.map((opt) => ({ id: opt.value, name: opt.label }))
        );
        formDetail.value[item.id] = defaultValue;
        return;
      }
      if ([FieldTypeEnum.DATE_TIME, FieldTypeEnum.INPUT_NUMBER, FieldTypeEnum.FORMULA].includes(item.type)) {
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
      replaceRule(item);
      if ([FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(item.type) && item.hasCurrentUser) {
        item.defaultValue = item.resourceFieldId ? userStore.userInfo.name : userStore.userInfo.id;
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
        item.defaultValue = item.resourceFieldId ? userStore.userInfo.departmentName : userStore.userInfo.departmentId;
        item.initialOptions = [
          ...(item.initialOptions || []),
          {
            id: userStore.userInfo.departmentId,
            name: userStore.userInfo.departmentName,
          },
        ].filter((option, index, self) => self.findIndex((o) => o.id === option.id) === index);
      }
      if (props.linkFormInfo?.value && linkScenario) {
        // 如果有关联表单信息，则填充关联表单字段值
        fillLinkFormFieldValue(item, linkScenario);
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
    initForm(props.linkScenario?.value);
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
        if (item.resourceFieldId) {
          return;
        }
        if (item.subFields?.length) {
          const refResourceFieldIds: string[] = [];
          item.subFields.forEach((subField) => {
            if (subField.resourceFieldId) {
              refResourceFieldIds.push(subField.id);
            }
          });
          const parentFieldDetail = form[item.id];
          if (parentFieldDetail) {
            parentFieldDetail.forEach((subItem: Record<string, any>) => {
              refResourceFieldIds.forEach((id) => {
                delete subItem[id];
              });
            });
          }
        }
        if (item.businessKey) {
          // 存在业务字段，则按照业务字段的key存储
          params[item.businessKey] = form[item.id] ?? '';
        } else {
          params.moduleFields.push({
            fieldId: item.id,
            fieldValue: getNormalFieldValue(item, form[item.id]),
          });
        }
      });

      if (needModuleFormConfigParamsType.includes(props.formKey.value)) {
        params.moduleFormConfigDTO = moduleFormConfig.value;
      }
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
    moduleFormConfig,
    detail,
  };
}
